package me.easyenchants.enchant;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class EnchantedBookApplicator {
    public ApplicationResult combineBooks(ItemStack target, ItemStack bookCursor) {
        if (!isValidBook(target) || target.getAmount() != 1 || !isValidBook(bookCursor)) {
            return ApplicationResult.notApplied();
        }

        EnchantmentStorageMeta targetMeta = (EnchantmentStorageMeta) target.getItemMeta();
        EnchantmentStorageMeta cursorMeta = (EnchantmentStorageMeta) bookCursor.getItemMeta();
        Map<Enchantment, Integer> targetEnchants = targetMeta.getStoredEnchants();
        Map<Enchantment, Integer> cursorEnchants = cursorMeta.getStoredEnchants();
        boolean upgradeAvailable = cursorEnchants.entrySet().stream().anyMatch(entry -> {
            int level = entry.getValue();
            return level > 0 && level < entry.getKey().getMaxLevel()
                && targetEnchants.getOrDefault(entry.getKey(), 0) == level;
        });
        if (!upgradeAvailable) {
            return ApplicationResult.notApplied();
        }

        ItemStack combined = target.clone();
        EnchantmentStorageMeta combinedMeta = (EnchantmentStorageMeta) combined.getItemMeta();
        for (Map.Entry<Enchantment, Integer> entry : cursorEnchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int cursorLevel = entry.getValue();
            int targetLevel = targetEnchants.getOrDefault(enchantment, 0);
            int resultingLevel = targetLevel == cursorLevel && targetLevel > 0 && targetLevel < enchantment.getMaxLevel()
                ? targetLevel + 1 : Math.max(targetLevel, cursorLevel);
            combinedMeta.addStoredEnchant(enchantment, resultingLevel, true);
        }
        combined.setItemMeta(combinedMeta);
        return new ApplicationResult(true, false, combined, consumeOneBook(bookCursor));
    }

    public ApplicationResult apply(ItemStack target, ItemStack bookCursor) {
        if (!isValidTarget(target) || !isValidBook(bookCursor)) {
            return ApplicationResult.notApplied();
        }

        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) bookCursor.getItemMeta();
        Map<Enchantment, Integer> storedEnchants = bookMeta.getStoredEnchants();
        if (storedEnchants.isEmpty()) {
            return ApplicationResult.notApplied();
        }
        if (!isEnchantableTarget(target)) {
            return ApplicationResult.notApplied();
        }

        ItemStack updatedTarget = target.clone();
        ItemMeta targetMeta = updatedTarget.getItemMeta();
        boolean changed = false;
        boolean incompatible = false;

        for (Map.Entry<Enchantment, Integer> entry : storedEnchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            if (level <= 0 || !enchantment.canEnchantItem(updatedTarget)) {
                incompatible = true;
                continue;
            }

            int clampedLevel = Math.min(level, enchantment.getMaxLevel());
            int currentLevel = targetMeta.getEnchantLevel(enchantment);
            if (conflictsWithExisting(targetMeta, enchantment)) {
                incompatible = true;
                continue;
            }
            if (currentLevel > clampedLevel || currentLevel == enchantment.getMaxLevel()) {
                continue;
            }

            int resultingLevel = currentLevel == clampedLevel ? currentLevel + 1 : clampedLevel;
            if (targetMeta.addEnchant(enchantment, resultingLevel, false)) {
                changed = true;
            }
        }

        if (!changed) {
            return ApplicationResult.notApplied(incompatible);
        }

        updatedTarget.setItemMeta(targetMeta);
        return new ApplicationResult(true, incompatible, updatedTarget, consumeOneBook(bookCursor));
    }

    private boolean isValidTarget(ItemStack target) {
        return target != null && target.getType() != Material.AIR && target.getType().isItem() && target.getItemMeta() != null;
    }

    private boolean isValidBook(ItemStack bookCursor) {
        return bookCursor != null
            && bookCursor.getType() == Material.ENCHANTED_BOOK
            && bookCursor.getAmount() > 0
            && bookCursor.getItemMeta() instanceof EnchantmentStorageMeta;
    }

    private boolean isEnchantableTarget(ItemStack target) {
        return Registry.ENCHANTMENT.stream().anyMatch(enchantment -> enchantment.canEnchantItem(target));
    }

    private boolean conflictsWithExisting(ItemMeta targetMeta, Enchantment candidate) {
        for (Enchantment existing : targetMeta.getEnchants().keySet()) {
            if (!existing.equals(candidate) && existing.conflictsWith(candidate)) {
                return true;
            }
        }
        return false;
    }

    private ItemStack consumeOneBook(ItemStack bookCursor) {
        if (bookCursor.getAmount() <= 1) {
            return null;
        }
        ItemStack remaining = bookCursor.clone();
        remaining.setAmount(bookCursor.getAmount() - 1);
        return remaining;
    }

    public record ApplicationResult(boolean applied, boolean incompatible, ItemStack targetAfter, ItemStack cursorAfter) {
        private static ApplicationResult notApplied() {
            return notApplied(false);
        }

        private static ApplicationResult notApplied(boolean incompatible) {
            return new ApplicationResult(false, incompatible, null, null);
        }
    }
}
