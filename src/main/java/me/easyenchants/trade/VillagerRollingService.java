package me.easyenchants.trade;

import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.easyenchants.gui.LibrarianBookOption;
import me.easyenchants.settings.EasyEnchantsFeatureSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FeatureFlag;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;

public final class VillagerRollingService {
    private static final PotionEffect WAITING_LUCK = new PotionEffect(PotionEffectType.LUCK, PotionEffect.INFINITE_DURATION, 0, false, true, true);
    private final Plugin plugin;
    private final EasyEnchantsFeatureSettings settings;
    private final IntUnaryOperator randomInt;
    private final NamespacedKey stateKey;
    private final NamespacedKey luckKey;
    private final NamespacedKey legacyEnchantmentKey;
    private final NamespacedKey legacyLevelKey;
    private final Set<UUID> scheduled = new HashSet<>();

    public VillagerRollingService(Plugin plugin) {
        this(plugin, () -> true);
    }

    public VillagerRollingService(Plugin plugin, EasyEnchantsFeatureSettings settings) {
        this(plugin, settings, bound -> ThreadLocalRandom.current().nextInt(bound));
    }

    public VillagerRollingService(Plugin plugin, IntUnaryOperator randomInt) {
        this(plugin, () -> true, randomInt);
    }

    public VillagerRollingService(Plugin plugin, EasyEnchantsFeatureSettings settings, IntUnaryOperator randomInt) {
        this.plugin = plugin;
        this.settings = settings;
        this.randomInt = randomInt;
        stateKey = key("trade_guarantees");
        luckKey = key("trade_luck_owned");
        legacyEnchantmentKey = key("pending_librarian_enchantment");
        legacyLevelKey = key("pending_librarian_level");
    }

    private NamespacedKey key(String name) { return new NamespacedKey(plugin, name); }
    private String profession(Villager villager) { return villager.getProfession().getKey().getKey(); }

    public boolean supports(Villager villager) {
        return villager.isAdult() && (villager.getProfession() == Villager.Profession.LIBRARIAN
            || villager.getProfession() == Villager.Profession.FLETCHER);
    }

    private boolean standardTrades(Villager villager) {
        return !villager.getWorld().getFeatureFlags().contains(FeatureFlag.TRADE_REBALANCE);
    }

    private boolean enabled(Villager villager) {
        return villager.getProfession() == Villager.Profession.LIBRARIAN ? settings.librarianRollingEnabled() : settings.fletcherRollingEnabled();
    }

    public boolean canSelect(Villager villager) {
        if (!supportedState(villager)) return false;
        reconcile(villager);
        return supports(villager) && standardTrades(villager) && enabled(villager)
            && !(villager.getProfession() == Villager.Profession.FLETCHER && villager.getVillagerLevel() >= 5)
            && remaining(villager) > 0;
    }

    public int remaining(Villager villager) {
        if (!supports(villager) || !supportedState(villager)) return 0;
        return (villager.getProfession() == Villager.Profession.LIBRARIAN ? 4 : 1) - read(villager).slots.size();
    }

    public String selectionTiming(Villager villager) {
        State state = read(villager);
        int tier = nextTier(villager, state);
        return tier < 0 ? "All trade slots are guaranteed."
            : tier <= villager.getVillagerLevel() ? "Available immediately."
            : "Guaranteed at " + tierName(tier) + ".";
    }

    private String tierName(int tier) {
        return switch (tier) { case 1 -> "Novice"; case 2 -> "Apprentice"; case 3 -> "Journeyman"; case 4 -> "Expert"; default -> "Master"; };
    }

    public boolean applySelection(Player player, UUID villagerUuid, TradeOption option) {
        Entity entity = Bukkit.getEntity(villagerUuid);
        if (!(entity instanceof Villager villager) || !entity.isValid()) return reject(player, "That villager is no longer available.");
        if (!canSelect(villager)) return reject(player, "This villager has no eligible trade slots, or rolling is disabled.");
        if (option.profession() != villager.getProfession() || !TradeCatalog.allowed(option)) return reject(player, "That trade is not available to this villager.");
        if (!player.hasPotionEffect(PotionEffectType.LUCK)) return reject(player, "You need Luck to choose a trade.");

        State state = read(villager);
        int tier = nextTier(villager, state);
        if (tier < 0) return reject(player, "All eligible trade slots are already guaranteed.");
        int cost = option instanceof LibrarianBookOption book ? rollEmeraldCost(book) : 2;
        state.slots.add(new Slot(tier, option, cost, -1, false));
        write(villager, state);
        reconcile(villager);
        player.removePotionEffect(PotionEffectType.LUCK);
        player.closeInventory();
        player.playSound(player.getLocation(), "minecraft:block.lectern.use", org.bukkit.SoundCategory.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private boolean reject(Player player, String message) {
        player.sendMessage(Component.text(message, NamedTextColor.RED));
        return false;
    }

    private int nextTier(Villager villager, State state) {
        Set<Integer> reserved = new HashSet<>();
        state.slots.forEach(slot -> reserved.add(slot.tier));
        if (villager.getProfession() == Villager.Profession.FLETCHER) return reserved.contains(5) ? -1 : 5;
        List<MerchantRecipe> recipes = villager.getRecipes();
        // Existing books take precedence over missed earlier tiers.
        for (int index = 0; index < recipes.size(); index++) {
            int tier = recipeTier(villager, recipes.get(index), index);
            if (tier >= 1 && tier <= Math.min(4, villager.getVillagerLevel()) && !reserved.contains(tier)
                && recipes.get(index).getResult().getType() == Material.ENCHANTED_BOOK) return tier;
        }
        for (int tier = 1; tier <= 4; tier++) if (!reserved.contains(tier)) return tier;
        return -1;
    }

    /** Runs after vanilla has committed its entire trade batch, not inside acquisition callbacks. */
    public void scheduleReconcile(Villager villager) {
        UUID uuid = villager.getUniqueId();
        if (!scheduled.add(uuid)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            scheduled.remove(uuid);
            Entity current = Bukkit.getEntity(uuid);
            if (current instanceof Villager loaded && loaded.isValid()) reconcile(loaded);
        });
    }

    public void reconcile(Villager villager) {
        if (!supportedState(villager)) return;
        PersistentDataContainer data = villager.getPersistentDataContainer();
        State state = read(villager);
        if (!state.profession.equals(profession(villager))) {
            clear(villager);
            return;
        }
        if (state.slots.isEmpty()) {
            removeOwnedLuck(villager);
            return;
        }
        if (!supports(villager) || !standardTrades(villager)) {
            removeOwnedLuck(villager);
            return;
        }
        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
        boolean changed = false;
        for (int n = 0; n < state.slots.size(); n++) {
            Slot slot = state.slots.get(n);
            if (slot.tier > villager.getVillagerLevel()) continue;
            int index = replacementIndex(villager, recipes, slot.tier);
            if (index < 0) continue; // No committed recipe: canceled/missing generation must not consume a reservation.
            MerchantRecipe original = recipes.get(index);
            if (!slot.fulfilled || !slot.option.item().isSimilar(original.getResult())) {
                recipes.set(index, replacementRecipe(original, slot));
                changed = true;
            }
            state.slots.set(n, new Slot(slot.tier, slot.option, slot.cost, index, false));
        }
        if (changed) villager.setRecipes(recipes);
        List<MerchantRecipe> committed = villager.getRecipes();
        for (int n = 0; n < state.slots.size(); n++) {
            Slot slot = state.slots.get(n);
            boolean fulfilled = slot.index >= 0 && slot.index < committed.size()
                && slot.tier <= villager.getVillagerLevel() && slot.option.item().isSimilar(committed.get(slot.index).getResult());
            state.slots.set(n, new Slot(slot.tier, slot.option, slot.cost, slot.index, fulfilled));
        }
        state.tiers = new int[committed.size()];
        for (int index = 0; index < committed.size(); index++) state.tiers[index] = recipeTier(villager, committed.get(index), index);
        write(villager, state);
        if (state.slots.stream().anyMatch(slot -> !slot.fulfilled)) {
            if (!villager.hasPotionEffect(PotionEffectType.LUCK) && villager.addPotionEffect(WAITING_LUCK)) {
                data.set(luckKey, PersistentDataType.BYTE, (byte) 1);
            }
        } else removeOwnedLuck(villager);
    }

    private int replacementIndex(Villager villager, List<MerchantRecipe> recipes, int tier) {
        int fallback = -1;
        int sale = -1;
        Material selected = villager.getProfession() == Villager.Profession.LIBRARIAN ? Material.ENCHANTED_BOOK : Material.TIPPED_ARROW;
        for (int index = 0; index < recipes.size(); index++) {
            MerchantRecipe recipe = recipes.get(index);
            if (recipeTier(villager, recipe, index) != tier) continue;
            if (recipe.getResult().getType() == selected) return index;
            if (fallback < 0) fallback = index;
            if (sale < 0 && recipe.getIngredients().stream().anyMatch(item -> item.getType() == Material.EMERALD)) sale = index;
        }
        return sale >= 0 ? sale : fallback;
    }

    /** Vanilla trades have unique item identities per tier; books additionally encode tier in XP. */
    private int recipeTier(Villager villager, MerchantRecipe recipe, int index) {
        Material result = recipe.getResult().getType();
        if (villager.getProfession() == Villager.Profession.LIBRARIAN) {
            int known = switch (result) {
                case BOOKSHELF -> 1; case LANTERN -> 2; case GLASS -> 3; case CLOCK, COMPASS -> 4; case NAME_TAG -> 5;
                case ENCHANTED_BOOK -> switch (recipe.getVillagerExperience()) { case 1 -> 1; case 5 -> 2; case 10 -> 3; case 15 -> 4; default -> rememberedBookTier(villager, index); };
                case EMERALD -> switch (firstIngredient(recipe)) { case PAPER -> 1; case BOOK -> 2; case INK_SAC -> 3; case WRITABLE_BOOK -> 4; default -> 0; };
                default -> 0;
            };
            return known;
        }
        if (villager.getProfession() == Villager.Profession.FLETCHER) {
            return switch (result) {
                case ARROW, FLINT -> 1;
                case BOW -> recipe.getResult().getEnchantments().isEmpty() ? 2 : 4;
                case CROSSBOW -> recipe.getResult().getEnchantments().isEmpty() ? 3 : 5;
                case TIPPED_ARROW -> 5;
                case EMERALD -> switch (firstIngredient(recipe)) { case STICK -> 1; case FLINT -> 2; case STRING -> 3; case FEATHER -> 4; case TRIPWIRE_HOOK -> 5; default -> 0; };
                default -> 0;
            };
        }
        return 0;
    }

    private int rememberedBookTier(Villager villager, int index) {
        var stored = villager.getPersistentDataContainer().get(stateKey, PersistentDataType.TAG_CONTAINER);
        if (stored != null) {
            int[] tiers = stored.get(key("tiers"), PersistentDataType.INTEGER_ARRAY);
            if (tiers != null && index < tiers.length && tiers[index] >= 1 && tiers[index] <= 4) return tiers[index];
        }
        // Legacy selections copied arbitrary XP. Standard Java trades are ordered in pairs by tier.
        return Math.min(4, index / 2 + 1);
    }

    private Material firstIngredient(MerchantRecipe recipe) {
        return recipe.getIngredients().isEmpty() ? Material.AIR : recipe.getIngredients().getFirst().getType();
    }

    private MerchantRecipe replacementRecipe(MerchantRecipe original, Slot slot) {
        boolean book = slot.option instanceof LibrarianBookOption;
        boolean existingBook = book && original.getResult().getType() == Material.ENCHANTED_BOOK;
        MerchantRecipe replacement = existingBook
            ? new MerchantRecipe(slot.option.item(), original.getUses(), original.getMaxUses(), original.hasExperienceReward(),
                original.getVillagerExperience(), original.getPriceMultiplier(), original.getDemand(), original.getSpecialPrice(), original.shouldIgnoreDiscounts())
            : new MerchantRecipe(slot.option.item(), 0, 12, true, book ? new int[]{0, 1, 5, 10, 15}[slot.tier] : 30, book ? 0.2F : 0.05F);
        replacement.setIngredients(List.of(new ItemStack(Material.EMERALD, slot.cost), new ItemStack(book ? Material.BOOK : Material.ARROW, book ? 1 : 5)));
        return replacement;
    }

    public int rollEmeraldCost(LibrarianBookOption option) {
        int level = option.level();
        int cost = 2 + randomInt.applyAsInt(5 + level * 10) + 3 * level;
        if (RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).getTagValues(EnchantmentTagKeys.DOUBLE_TRADE_PRICE).stream().anyMatch(value -> value.getKey().equals(option.enchantment().getKey()))) cost *= 2;
        return Math.min(cost, 64);
    }

    public boolean hasGuarantees(LivingEntity entity) {
        var data = entity.getPersistentDataContainer();
        return data.has(stateKey) || data.has(legacyEnchantmentKey);
    }

    public void clear(LivingEntity entity) {
        var data = entity.getPersistentDataContainer();
        data.remove(stateKey);
        data.remove(legacyEnchantmentKey);
        data.remove(legacyLevelKey);
        removeOwnedLuck(entity);
    }

    private void removeOwnedLuck(LivingEntity entity) {
        var data = entity.getPersistentDataContainer();
        if (!data.has(luckKey)) return;
        data.remove(luckKey); // Remove marker first: potion event callbacks may schedule reconciliation.
        if (WAITING_LUCK.equals(entity.getPotionEffect(PotionEffectType.LUCK))) entity.removePotionEffect(PotionEffectType.LUCK);
    }

    public void transfer(LivingEntity from, LivingEntity to) {
        var source = from.getPersistentDataContainer();
        var target = to.getPersistentDataContainer();
        var value = source.get(stateKey, PersistentDataType.TAG_CONTAINER);
        if (value != null) target.set(stateKey, PersistentDataType.TAG_CONTAINER, value);
        String legacy = source.get(legacyEnchantmentKey, PersistentDataType.STRING);
        Integer level = source.get(legacyLevelKey, PersistentDataType.INTEGER);
        if (legacy != null && level != null) {
            target.set(legacyEnchantmentKey, PersistentDataType.STRING, legacy);
            target.set(legacyLevelKey, PersistentDataType.INTEGER, level);
        }
        if (source.has(luckKey)) {
            target.set(luckKey, PersistentDataType.BYTE, (byte) 1);
            if (!to.hasPotionEffect(PotionEffectType.LUCK)) to.addPotionEffect(WAITING_LUCK);
        }
    }

    private boolean supportedState(Villager villager) {
        var stored = villager.getPersistentDataContainer().get(stateKey, PersistentDataType.TAG_CONTAINER);
        return stored == null || stored.getOrDefault(key("version"), PersistentDataType.INTEGER, 0) == 1;
    }

    private State read(Villager villager) {
        var data = villager.getPersistentDataContainer();
        var stored = data.get(stateKey, PersistentDataType.TAG_CONTAINER);
        if (stored == null) {
            State state = new State(profession(villager));
            String legacy = data.get(legacyEnchantmentKey, PersistentDataType.STRING);
            Integer level = data.get(legacyLevelKey, PersistentDataType.INTEGER);
            if (legacy != null && level != null && villager.getProfession() == Villager.Profession.LIBRARIAN) {
                TradeOption option = TradeCatalog.decode("librarian", legacy, level);
                if (option != null && TradeCatalog.allowed(option)) {
                    state.slots.add(new Slot(nextTier(villager, state), option, rollEmeraldCost((LibrarianBookOption) option), -1, false));
                    write(villager, state);
                }
                data.remove(legacyEnchantmentKey);
                data.remove(legacyLevelKey);
            }
            return state;
        }
        State state = new State(stored.getOrDefault(key("profession"), PersistentDataType.STRING, ""));
        state.tiers = stored.getOrDefault(key("tiers"), PersistentDataType.INTEGER_ARRAY, new int[0]);
        for (int tier = 1; tier <= 5; tier++) {
            var slot = stored.get(key("slot_" + tier), PersistentDataType.TAG_CONTAINER);
            if (slot == null) continue;
            TradeOption option = TradeCatalog.decode(state.profession, slot.getOrDefault(key("id"), PersistentDataType.STRING, ""),
                slot.getOrDefault(key("level"), PersistentDataType.INTEGER, 1));
            if (option == null) continue;
            state.slots.add(new Slot(tier, option, slot.getOrDefault(key("cost"), PersistentDataType.INTEGER, 2),
                slot.getOrDefault(key("index"), PersistentDataType.INTEGER, -1), slot.getOrDefault(key("fulfilled"), PersistentDataType.BOOLEAN, false)));
        }
        return state;
    }

    private void write(Villager villager, State state) {
        var data = villager.getPersistentDataContainer();
        var stored = data.getAdapterContext().newPersistentDataContainer();
        stored.set(key("version"), PersistentDataType.INTEGER, 1);
        stored.set(key("profession"), PersistentDataType.STRING, state.profession);
        stored.set(key("tiers"), PersistentDataType.INTEGER_ARRAY, state.tiers);
        for (Slot value : state.slots) {
            var slot = data.getAdapterContext().newPersistentDataContainer();
            slot.set(key("id"), PersistentDataType.STRING, value.option.id());
            slot.set(key("level"), PersistentDataType.INTEGER, value.option.level());
            slot.set(key("cost"), PersistentDataType.INTEGER, value.cost);
            slot.set(key("index"), PersistentDataType.INTEGER, value.index);
            slot.set(key("fulfilled"), PersistentDataType.BOOLEAN, value.fulfilled);
            stored.set(key("slot_" + value.tier), PersistentDataType.TAG_CONTAINER, slot);
        }
        data.set(stateKey, PersistentDataType.TAG_CONTAINER, stored);
    }

    private record Slot(int tier, TradeOption option, int cost, int index, boolean fulfilled) {}
    private static final class State {
        final String profession;
        final List<Slot> slots = new ArrayList<>();
        int[] tiers = new int[0];
        State(String profession) { this.profession = profession; }
    }
}
