package me.easyenchants.listener;

import me.easyenchants.BukkitTestSupport;
import me.easyenchants.enchant.EnchantedBookApplicator;
import me.easyenchants.gui.EasyEnchantsSettingsGui;
import me.easyenchants.settings.EasyEnchantsFeatureSettings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyEnchantsListenerTest extends BukkitTestSupport {
    @Test
    void disabledBranchDoesNotInterceptBookClick() {
        PlayerMock player = MockBukkit.getMock().addPlayer("Player");
        Inventory chest = Bukkit.createInventory(null, 27);
        InventoryClickEvent event = playerInventoryClick(player, chest);
        EasyEnchantsListener listener = listener(false, null);

        listener.onInventoryClick(event);

        assertFalse(event.isCancelled());
        assertEquals(0, event.getCurrentItem().getEnchantmentLevel(Enchantment.SHARPNESS));
    }

    @Test
    void playerInventoryClickAppliesBook() {
        PlayerMock player = MockBukkit.getMock().addPlayer("Player");
        Inventory chest = Bukkit.createInventory(null, 27);
        InventoryClickEvent event = playerInventoryClick(player, chest);
        EasyEnchantsListener listener = listener(true, null);

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(3, event.getCurrentItem().getEnchantmentLevel(Enchantment.SHARPNESS));
        assertEquals(1, event.getCursor().getAmount());
    }

    @Test
    void topInventoryClickIsNotProcessed() {
        PlayerMock player = MockBukkit.getMock().addPlayer("Player");
        Inventory chest = Bukkit.createInventory(null, 27);
        InventoryView view = new TestInventoryView(player, chest, InventoryType.CHEST);
        InventoryClickEvent event = new InventoryClickEvent(
            view,
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.SWAP_WITH_CURSOR
        );
        event.setCurrentItem(new ItemStack(Material.DIAMOND_SWORD));
        event.setCursor(book(1, Enchantment.SHARPNESS, 3));
        EasyEnchantsListener listener = listener(true, null);

        listener.onInventoryClick(event);

        assertFalse(event.isCancelled());
        assertEquals(0, event.getCurrentItem().getEnchantmentLevel(Enchantment.SHARPNESS));
    }

    private EasyEnchantsListener listener(boolean enabled, EasyEnchantsSettingsGui settingsGui) {
        EasyEnchantsFeatureSettings settings = () -> enabled;
        return new EasyEnchantsListener(settings, settingsGui, new EnchantedBookApplicator());
    }

    private InventoryClickEvent playerInventoryClick(PlayerMock player, Inventory chest) {
        InventoryView view = new TestInventoryView(player, chest, InventoryType.CHEST);
        int rawBottomSlot = chest.getSize();
        InventoryClickEvent event = new InventoryClickEvent(
            view,
            InventoryType.SlotType.CONTAINER,
            rawBottomSlot,
            ClickType.LEFT,
            InventoryAction.SWAP_WITH_CURSOR
        );
        event.setCurrentItem(new ItemStack(Material.DIAMOND_SWORD));
        event.setCursor(book(2, Enchantment.SHARPNESS, 3));
        return event;
    }

    private ItemStack book(int amount, Enchantment enchantment, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, amount);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);
        return book;
    }

    private static final class TestInventoryView implements InventoryView {
        private final PlayerMock player;
        private final Inventory topInventory;
        private final InventoryType type;
        private ItemStack cursor;

        private TestInventoryView(PlayerMock player, Inventory topInventory, InventoryType type) {
            this.player = player;
            this.topInventory = topInventory;
            this.type = type;
        }

        @Override
        public Inventory getTopInventory() {
            return topInventory;
        }

        @Override
        public Inventory getBottomInventory() {
            return player.getInventory();
        }

        @Override
        public HumanEntity getPlayer() {
            return player;
        }

        @Override
        public InventoryType getType() {
            return type;
        }

        @Override
        public void setItem(int slot, ItemStack item) {
            Inventory inventory = getInventory(slot);
            if (inventory != null) {
                inventory.setItem(convertSlot(slot), item);
            }
        }

        @Override
        public ItemStack getItem(int slot) {
            Inventory inventory = getInventory(slot);
            return inventory == null ? null : inventory.getItem(convertSlot(slot));
        }

        @Override
        public void setCursor(ItemStack item) {
            cursor = item;
        }

        @Override
        public ItemStack getCursor() {
            return cursor;
        }

        @Override
        public Inventory getInventory(int rawSlot) {
            if (rawSlot < 0 || rawSlot >= countSlots()) {
                return null;
            }
            return rawSlot < topInventory.getSize() ? topInventory : getBottomInventory();
        }

        @Override
        public int convertSlot(int rawSlot) {
            return rawSlot < topInventory.getSize() ? rawSlot : rawSlot - topInventory.getSize();
        }

        @Override
        public InventoryType.SlotType getSlotType(int slot) {
            return InventoryType.SlotType.CONTAINER;
        }

        @Override
        public void open() {
        }

        @Override
        public void close() {
        }

        @Override
        public int countSlots() {
            return topInventory.getSize() + getBottomInventory().getSize();
        }

        @Override
        public boolean setProperty(Property prop, int value) {
            return false;
        }

        @Override
        public String getTitle() {
            return "Test";
        }

        @Override
        public String getOriginalTitle() {
            return "Test";
        }

        @Override
        public void setTitle(String title) {
        }

        @Override
        public MenuType getMenuType() {
            return null;
        }
    }
}
