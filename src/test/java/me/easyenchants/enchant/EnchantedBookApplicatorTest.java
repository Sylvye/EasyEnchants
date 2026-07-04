package me.easyenchants.enchant;

import me.easyenchants.BukkitTestSupport;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantedBookApplicatorTest extends BukkitTestSupport {
    private final EnchantedBookApplicator applicator = new EnchantedBookApplicator();

    @Test
    void compatibleEnchantAppliesAndConsumesOneBook() {
        ItemStack target = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack book = book(1, Enchantment.SHARPNESS, 3);

        EnchantedBookApplicator.ApplicationResult result = applicator.apply(target, book);

        assertTrue(result.applied());
        assertEquals(3, result.targetAfter().getEnchantmentLevel(Enchantment.SHARPNESS));
        assertNull(result.cursorAfter());
    }

    @Test
    void stackedBooksConsumeExactlyOne() {
        ItemStack target = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack book = book(4, Enchantment.SHARPNESS, 3);

        EnchantedBookApplicator.ApplicationResult result = applicator.apply(target, book);

        assertTrue(result.applied());
        assertEquals(3, result.cursorAfter().getAmount());
        assertEquals(Material.ENCHANTED_BOOK, result.cursorAfter().getType());
    }

    @Test
    void existingHigherLevelIsPreservedWithoutConsumption() {
        ItemStack target = new ItemStack(Material.DIAMOND_SWORD);
        target.addEnchantment(Enchantment.SHARPNESS, 5);
        ItemStack book = book(1, Enchantment.SHARPNESS, 3);

        EnchantedBookApplicator.ApplicationResult result = applicator.apply(target, book);

        assertFalse(result.applied());
        assertEquals(5, target.getEnchantmentLevel(Enchantment.SHARPNESS));
    }

    @Test
    void existingLowerLevelIsUpgraded() {
        ItemStack target = new ItemStack(Material.DIAMOND_SWORD);
        target.addEnchantment(Enchantment.SHARPNESS, 2);
        ItemStack book = book(1, Enchantment.SHARPNESS, 4);

        EnchantedBookApplicator.ApplicationResult result = applicator.apply(target, book);

        assertTrue(result.applied());
        assertEquals(4, result.targetAfter().getEnchantmentLevel(Enchantment.SHARPNESS));
    }

    @Test
    void conflictingEnchantIsVoidedWhenAnotherEnchantApplies() {
        ItemStack target = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack book = book(1, Enchantment.SHARPNESS, 3, Enchantment.SMITE, 3, Enchantment.UNBREAKING, 2);

        EnchantedBookApplicator.ApplicationResult result = applicator.apply(target, book);

        assertTrue(result.applied());
        assertNull(result.cursorAfter());
        assertEquals(2, result.targetAfter().getEnchantmentLevel(Enchantment.UNBREAKING));
        int damageEnchantCount = 0;
        if (result.targetAfter().containsEnchantment(Enchantment.SHARPNESS)) {
            damageEnchantCount++;
        }
        if (result.targetAfter().containsEnchantment(Enchantment.SMITE)) {
            damageEnchantCount++;
        }
        assertEquals(1, damageEnchantCount);
    }

    @Test
    void fullyIncompatibleBookIsRejectedUnchanged() {
        ItemStack target = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack book = book(1, Enchantment.PROTECTION, 4);

        EnchantedBookApplicator.ApplicationResult result = applicator.apply(target, book);

        assertFalse(result.applied());
        assertFalse(target.containsEnchantment(Enchantment.PROTECTION));
        assertEquals(1, book.getAmount());
    }

    @Test
    void overMaxLevelIsClamped() {
        ItemStack target = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack book = book(1, Enchantment.SHARPNESS, 100);

        EnchantedBookApplicator.ApplicationResult result = applicator.apply(target, book);

        assertTrue(result.applied());
        assertEquals(Enchantment.SHARPNESS.getMaxLevel(), result.targetAfter().getEnchantmentLevel(Enchantment.SHARPNESS));
    }

    private ItemStack book(int amount, Object... enchantmentsAndLevels) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, amount);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        for (int index = 0; index < enchantmentsAndLevels.length; index += 2) {
            meta.addStoredEnchant((Enchantment) enchantmentsAndLevels[index], (Integer) enchantmentsAndLevels[index + 1], true);
        }
        book.setItemMeta(meta);
        return book;
    }
}
