package me.easyenchants.integration;

import me.easyenchants.gui.LibrarianBookOption;
import me.easyenchants.trade.ArrowOption;
import me.easyenchants.trade.TradeOption;
import me.easyenchants.trade.VillagerRollingService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Install ONLY on an isolated test server: spawns test villagers and shuts the server down. */
public final class TradeIntegrationPlugin extends JavaPlugin implements Listener {
    private VillagerRollingService service;
    private Villager librarian;
    private Villager fletcher;
    private ZombieVillager zombie;
    private Villager cured;
    private int librarianLuckAdds;
    private final List<Enchantment> books = List.of(Enchantment.MENDING, Enchantment.UNBREAKING, Enchantment.SHARPNESS, Enchantment.EFFICIENCY);

    @Override
    public void onEnable() {
        service = new VillagerRollingService(Bukkit.getPluginManager().getPlugin("EasyEnchants"));
        Bukkit.getPluginManager().registerEvents(this, this);
        later(this::start, 5);
    }

    private void start() throws Exception {
        Files.deleteIfExists(getDataFolder().toPath().resolve("PASS"));
        librarian = spawn(Villager.Profession.LIBRARIAN, 0);
        fletcher = spawn(Villager.Profession.FLETCHER, 3);
        later(this::selectTrades, 3);
    }

    private void selectTrades() {
        // Force the no-book novice case while retaining vanilla trade count and ingredients.
        librarian.setRecipes(List.of(recipe(Material.EMERALD, Material.PAPER, 24), recipe(Material.BOOKSHELF, Material.EMERALD, 9)));
        select(librarian, LibrarianBookOption.of(books.getFirst(), 1));
        check(librarianLuckAdds == 0 && !librarian.hasPotionEffect(PotionEffectType.LUCK), "immediate selection applied visible Luck");
        for (int index = 1; index < 4; index++) select(librarian, LibrarianBookOption.of(books.get(index), 1));
        check(librarian.getPotionEffect(PotionEffectType.LUCK).hasParticles(), "pending Luck must have particles");
        check(librarian.getPotionEffect(PotionEffectType.LUCK).isInfinite(), "pending Luck must not expire");
        check(service.remaining(librarian) == 0, "novice must reserve all four tiers");
        select(fletcher, new ArrowOption(PotionType.STRONG_HARMING));
        levelBooks(2);
    }

    private void levelBooks(int tier) {
        check(librarian.increaseLevel(1), "vanilla librarian level-up failed");
        later(() -> {
            List<MerchantRecipe> found = librarian.getRecipes().stream().filter(r -> r.getResult().getType() == Material.ENCHANTED_BOOK).toList();
            check(found.size() == tier, "wrong book count at tier " + tier);
            for (int index = 0; index < tier; index++) {
                check(((EnchantmentStorageMeta) found.get(index).getResult().getItemMeta()).hasStoredEnchant(books.get(index)), "wrong promised book");
            }
            check(librarian.hasPotionEffect(PotionEffectType.LUCK) == (tier < 4), "Luck lifetime wrong");
            if (tier < 4) levelBooks(tier + 1); else masterFletcher();
        }, 3);
    }

    private void masterFletcher() {
        check(fletcher.increaseLevel(4), "vanilla fletcher level-up failed");
        // Simulate vanilla choosing the two non-arrow master offers.
        var recipes = new ArrayList<>(fletcher.getRecipes());
        recipes.set(8, recipe(Material.EMERALD, Material.TRIPWIRE_HOOK, 8));
        var crossbow = new ItemStack(Material.CROSSBOW);
        crossbow.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        var sale = new MerchantRecipe(crossbow, 0, 3, true, 15, 0.05F);
        sale.setIngredients(List.of(new ItemStack(Material.EMERALD, 10)));
        recipes.set(9, sale);
        fletcher.setRecipes(recipes);
        later(() -> {
            var arrows = fletcher.getRecipe(9);
            check(arrows.getResult().getType() == Material.TIPPED_ARROW, "missing forced master arrows");
            check(((PotionMeta) arrows.getResult().getItemMeta()).getBasePotionType() == PotionType.STRONG_HARMING, "wrong arrow potion");
            check(arrows.getResult().getAmount() == 5 && arrows.getIngredients().get(1).getAmount() == 5, "wrong arrow quantities");
            check(!fletcher.hasPotionEffect(PotionEffectType.LUCK), "master fletcher still has Luck");
            check(!service.canSelect(fletcher), "master fletcher accepts new selections");
            pdcAndZombie();
        }, 3);
    }

    private void pdcAndZombie() throws Exception {
        Villager pending = spawn(Villager.Profession.LIBRARIAN, 6);
        select(pending, LibrarianBookOption.of(Enchantment.MENDING, 1));
        select(pending, LibrarianBookOption.of(Enchantment.UNBREAKING, 1));
        byte[] saved = pending.getPersistentDataContainer().serializeToBytes();
        pending.getPersistentDataContainer().readFromBytes(saved, true);
        check(new VillagerRollingService(Bukkit.getPluginManager().getPlugin("EasyEnchants")).remaining(pending) == 2, "PDC round trip lost reservations");
        zombie = pending.zombify();
        check(zombie != null, "zombification failed");
        later(() -> {
            zombie.setConversionTime(1);
            later(this::checkCured, 25);
        }, 3);
    }

    private void checkCured() {
        check(cured != null && cured.isValid(), "zombie did not cure");
        check(service.remaining(cured) == 2, "conversion lost guarantees");
        check(cured.hasPotionEffect(PotionEffectType.LUCK), "conversion lost pending Luck");
        check(cured.increaseLevel(1), "cured level-up failed");
        later(() -> {
            check(!cured.hasPotionEffect(PotionEffectType.LUCK), "cured fulfillment left Luck");
            check(cured.getRecipes().stream().filter(r -> r.getResult().getType() == Material.ENCHANTED_BOOK).count() == 2, "cured pending book missing");
            Files.writeString(getDataFolder().toPath().resolve("PASS"), "Paper trade generation, Luck metadata, PDC, and conversion passed.\n");
            getLogger().info("EASYENCHANTS_INTEGRATION_PASS");
            Bukkit.shutdown();
        }, 3);
    }

    @EventHandler
    public void onTransform(org.bukkit.event.entity.EntityTransformEvent event) {
        if (event.getEntity().equals(zombie) && event.getTransformedEntity() instanceof Villager villager) cured = villager;
    }

    @EventHandler
    public void onEffect(EntityPotionEffectEvent event) {
        if (event.getEntity().equals(librarian) && event.getModifiedType() == PotionEffectType.LUCK && event.getNewEffect() != null) librarianLuckAdds++;
    }

    private Villager spawn(Villager.Profession profession, int offset) {
        var world = Bukkit.getWorlds().getFirst();
        world.getChunkAt(0, 0).setForceLoaded(true);
        return world.spawn(new Location(world, offset, 70, 0), Villager.class, entity -> {
            entity.setAdult();
            entity.setProfession(profession);
            entity.setAI(false);
            entity.setGravity(false);
            entity.setInvulnerable(true);
        });
    }

    private MerchantRecipe recipe(Material output, Material input, int amount) {
        var recipe = new MerchantRecipe(new ItemStack(output), 0, 12, true, 1, 0.05F);
        recipe.setIngredients(List.of(new ItemStack(input, amount)));
        return recipe;
    }

    private void select(Villager villager, TradeOption option) {
        var luck = new AtomicBoolean(true);
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "hasPotionEffect" -> luck.get();
            case "removePotionEffect" -> { luck.set(false); yield null; }
            case "getLocation" -> villager.getLocation();
            case "closeInventory", "playSound" -> null;
            case "sendMessage" -> { getLogger().info("Selection message: " + java.util.Arrays.toString(args)); yield null; }
            default -> throw new UnsupportedOperationException(method.getName());
        });
        check(service.applySelection(player, villager.getUniqueId(), option), "selection rejected: " + option);
        check(!luck.get(), "selection did not consume Luck");
    }

    private void check(boolean condition, String failure) { if (!condition) throw new AssertionError(failure); }
    private void later(CheckedStep action, long ticks) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                Files.createDirectories(getDataFolder().toPath());
                action.run();
            } catch (Throwable failure) {
                getLogger().log(java.util.logging.Level.SEVERE, "EASYENCHANTS_INTEGRATION_FAIL", failure);
                Bukkit.shutdown();
            }
        }, ticks);
    }
    private interface CheckedStep { void run() throws Exception; }
}
