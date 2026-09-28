package me.easyenchants.trade;

import me.easyenchants.BukkitTestSupport;
import me.easyenchants.EasyEnchantsPlugin;
import me.easyenchants.gui.LibrarianBookOption;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerRollingServiceTest extends BukkitTestSupport {
    @Test
    void selectionConsumesLuckAndReplacesExistingBookTrade() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        VillagerRollingService service = service(plugin, 4);
        PlayerMock player = luckyPlayer();
        Villager villager = librarian();
        MerchantRecipe original = recipe(book(Enchantment.MENDING, 1), 0, 12, true, 7, 0.2F, 3, -1, true);
        villager.setRecipes(List.of(original));

        assertTrue(service.applySelection(player, villager.getUniqueId(), LibrarianBookOption.of(Enchantment.SHARPNESS, 5)));

        assertFalse(player.hasPotionEffect(PotionEffectType.LUCK));
        MerchantRecipe replacement = villager.getRecipe(0);
        assertBook(replacement.getResult(), Enchantment.SHARPNESS, 5);
        assertEquals(original.getUses(), replacement.getUses());
        assertEquals(original.getMaxUses(), replacement.getMaxUses());
        assertEquals(original.hasExperienceReward(), replacement.hasExperienceReward());
        assertEquals(original.getVillagerExperience(), replacement.getVillagerExperience());
        assertEquals(original.getPriceMultiplier(), replacement.getPriceMultiplier());
        assertEquals(original.getDemand(), replacement.getDemand());
        assertEquals(original.getSpecialPrice(), replacement.getSpecialPrice());
        assertEquals(original.shouldIgnoreDiscounts(), replacement.shouldIgnoreDiscounts());
        assertIngredients(replacement, 21);
    }

    @Test
    void selectionImmediatelyConvertsNonBookSale() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        VillagerRollingService service = service(plugin, 8);
        PlayerMock player = luckyPlayer();
        Villager villager = librarian();
        MerchantRecipe nonBook = recipe(new ItemStack(Material.BOOKSHELF), 0, 16, true, 1, 0.05F, 0, 0, false);
        villager.setRecipes(List.of(nonBook));

        assertTrue(service.applySelection(player, villager.getUniqueId(), LibrarianBookOption.of(Enchantment.UNBREAKING, 3)));

        assertBook(villager.getRecipe(0).getResult(), Enchantment.UNBREAKING, 3);
        assertIngredients(villager.getRecipe(0), 19);
        assertFalse(villager.hasPotionEffect(PotionEffectType.LUCK));
    }

    @Test
    void nonTreasureCostUsesVanillaLevelRange() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        VillagerRollingService minimumService = service(plugin, 0);
        VillagerRollingService maximumService = new VillagerRollingService(plugin, bound -> bound - 1);
        LibrarianBookOption option = LibrarianBookOption.of(Enchantment.SHARPNESS, 3);

        assertEquals(11, minimumService.rollEmeraldCost(option));
        assertEquals(45, maximumService.rollEmeraldCost(option));
    }

    @Test
    void treasureCostDoublesBeforeCap() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        VillagerRollingService service = service(plugin, 4);

        assertEquals(18, service.rollEmeraldCost(LibrarianBookOption.of(Enchantment.MENDING, 1)));
    }

    @Test
    void emeraldCostCapsAtStackSize() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        VillagerRollingService service = new VillagerRollingService(plugin, bound -> bound - 1);

        assertEquals(64, service.rollEmeraldCost(LibrarianBookOption.of(Enchantment.SHARPNESS, 5)));
    }

    @Test
    void selectionFailsWithoutLuck() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        VillagerRollingService service = new VillagerRollingService(plugin);
        PlayerMock player = MockBukkit.getMock().addPlayer("Player");
        Villager villager = librarian();
        villager.setRecipes(List.of(recipe(book(Enchantment.MENDING, 1), 0, 12, true, 3, 0.2F, 0, 0, false)));

        assertFalse(service.applySelection(player, villager.getUniqueId(), LibrarianBookOption.of(Enchantment.SHARPNESS, 5)));

        assertBook(villager.getRecipe(0).getResult(), Enchantment.MENDING, 1);
    }

    private VillagerRollingService service(EasyEnchantsPlugin plugin, int roll) {
        return new VillagerRollingService(plugin, fixedRoll(roll));
    }

    private IntUnaryOperator fixedRoll(int roll) {
        return bound -> {
            assertTrue(roll >= 0 && roll < bound);
            return roll;
        };
    }

    private PlayerMock luckyPlayer() {
        PlayerMock player = MockBukkit.getMock().addPlayer("Player");
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 200, 0));
        return player;
    }

    private Villager librarian() {
        WorldMock world = tradeWorld("world-" + System.nanoTime());
        Villager villager = (Villager) world.spawnEntity(new Location(world, 0, 64, 0), org.bukkit.entity.EntityType.VILLAGER);
        villager.setProfession(Villager.Profession.LIBRARIAN);
        villager.setVillagerExperience(0);
        return villager;
    }

    private MerchantRecipe recipe(ItemStack result, int uses, int maxUses, boolean experienceReward, int villagerExperience, float priceMultiplier, int demand, int specialPrice, boolean ignoreDiscounts) {
        MerchantRecipe recipe = new MerchantRecipe(result, uses, maxUses, experienceReward, villagerExperience, priceMultiplier, demand, specialPrice, ignoreDiscounts);
        recipe.setIngredients(List.of(new ItemStack(Material.EMERALD), new ItemStack(Material.BOOK)));
        return recipe;
    }

    private ItemStack book(Enchantment enchantment, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);
        return book;
    }

    private void assertIngredients(MerchantRecipe recipe, int emeralds) {
        List<ItemStack> ingredients = recipe.getIngredients();
        assertEquals(2, ingredients.size());
        assertEquals(Material.EMERALD, ingredients.get(0).getType());
        assertEquals(emeralds, ingredients.get(0).getAmount());
        assertEquals(Material.BOOK, ingredients.get(1).getType());
        assertEquals(1, ingredients.get(1).getAmount());
    }

    private void assertBook(ItemStack item, Enchantment enchantment, int level) {
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        assertEquals(level, meta.getStoredEnchants().entrySet().stream()
            .filter(entry -> entry.getKey().getKey().equals(enchantment.getKey())).findFirst().orElseThrow().getValue());
    }
}
