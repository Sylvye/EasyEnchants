package me.easyenchants.librarian;

import me.easyenchants.gui.LibrarianBookOption;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;
import java.util.UUID;

public final class LibrarianRollingService {
    private final NamespacedKey pendingEnchantmentKey;
    private final NamespacedKey pendingLevelKey;
    private final IntUnaryOperator randomInt;

    public LibrarianRollingService(Plugin plugin) {
        this(plugin, bound -> ThreadLocalRandom.current().nextInt(bound));
    }

    LibrarianRollingService(Plugin plugin, IntUnaryOperator randomInt) {
        pendingEnchantmentKey = new NamespacedKey(plugin, "pending_librarian_enchantment");
        pendingLevelKey = new NamespacedKey(plugin, "pending_librarian_level");
        this.randomInt = randomInt;
    }

    public boolean isUnlockedLibrarian(Villager villager) {
        return villager.getProfession() == Villager.Profession.LIBRARIAN
            && villager.getVillagerExperience() == 0
            && villager.getRecipes().stream().noneMatch(recipe -> recipe.getUses() > 0);
    }

    public boolean applySelection(Player player, UUID villagerUuid, LibrarianBookOption option) {
        Entity entity = Bukkit.getEntity(villagerUuid);
        if (!(entity instanceof Villager villager) || !entity.isValid()) {
            player.sendMessage(Component.text("That librarian is no longer available.", NamedTextColor.RED));
            return false;
        }
        if (!isUnlockedLibrarian(villager)) {
            player.sendMessage(Component.text("That librarian has already been locked in.", NamedTextColor.RED));
            return false;
        }
        if (!player.hasPotionEffect(PotionEffectType.LUCK)) {
            player.sendMessage(Component.text("You need Luck to choose a librarian book.", NamedTextColor.RED));
            return false;
        }

        if (!replaceFirstBookTrade(villager, option)) {
            storePending(villager, option);
        }

        player.removePotionEffect(PotionEffectType.LUCK);
        player.closeInventory();
        player.playSound(player.getLocation(), "minecraft:block.lectern.use", org.bukkit.SoundCategory.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public MerchantRecipe pendingReplacement(Villager villager, MerchantRecipe recipe) {
        LibrarianBookOption option = pendingSelection(villager);
        if (option == null || recipe.getResult().getType() != Material.ENCHANTED_BOOK) {
            return null;
        }
        clearPending(villager);
        return replacementRecipe(recipe, option);
    }

    private boolean replaceFirstBookTrade(Villager villager, LibrarianBookOption option) {
        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
        for (int index = 0; index < recipes.size(); index++) {
            MerchantRecipe recipe = recipes.get(index);
            if (recipe.getResult().getType() != Material.ENCHANTED_BOOK) {
                continue;
            }
            recipes.set(index, replacementRecipe(recipe, option));
            villager.setRecipes(recipes);
            return true;
        }
        return false;
    }

    private MerchantRecipe replacementRecipe(MerchantRecipe original, LibrarianBookOption option) {
        MerchantRecipe replacement = new MerchantRecipe(
            enchantedBook(option),
            original.getUses(),
            original.getMaxUses(),
            original.hasExperienceReward(),
            original.getVillagerExperience(),
            original.getPriceMultiplier(),
            original.getDemand(),
            original.getSpecialPrice(),
            original.shouldIgnoreDiscounts()
        );
        replacement.setIngredients(List.of(
            new ItemStack(Material.EMERALD, rollEmeraldCost(option)),
            new ItemStack(Material.BOOK)
        ));
        return replacement;
    }

    int rollEmeraldCost(LibrarianBookOption option) {
        int level = option.level();
        int cost = 2 + randomInt.applyAsInt(5 + level * 10) + 3 * level;
        if (option.enchantment().isTreasure()) {
            cost *= 2;
        }
        return Math.min(cost, 64);
    }

    public ItemStack enchantedBook(LibrarianBookOption option) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(option.enchantment(), option.level(), true);
        book.setItemMeta(meta);
        return book;
    }

    private void storePending(Villager villager, LibrarianBookOption option) {
        PersistentDataContainer container = villager.getPersistentDataContainer();
        container.set(pendingEnchantmentKey, PersistentDataType.STRING, option.enchantment().getKey().toString());
        container.set(pendingLevelKey, PersistentDataType.INTEGER, option.level());
    }

    private LibrarianBookOption pendingSelection(Villager villager) {
        PersistentDataContainer container = villager.getPersistentDataContainer();
        String key = container.get(pendingEnchantmentKey, PersistentDataType.STRING);
        Integer level = container.get(pendingLevelKey, PersistentDataType.INTEGER);
        if (key == null || level == null) {
            return null;
        }
        Enchantment enchantment = RegistryLookup.enchantment(key);
        return enchantment == null ? null : LibrarianBookOption.of(enchantment, level);
    }

    private void clearPending(Villager villager) {
        PersistentDataContainer container = villager.getPersistentDataContainer();
        container.remove(pendingEnchantmentKey);
        container.remove(pendingLevelKey);
    }

}
