package me.easyenchants.enchant;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class EnchantedBookApplicator {
    public ApplicationResult apply(ItemStack target, ItemStack bookCursor) {
        if (!isValidTarget(target) || !isValidBook(bookCursor)) {
            return ApplicationResult.notApplied();
        }

        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) bookCursor.getItemMeta();
        Map<Enchantment, Integer> storedEnchants = bookMeta.getStoredEnchants();
        if (storedEnchants.isEmpty()) {
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
