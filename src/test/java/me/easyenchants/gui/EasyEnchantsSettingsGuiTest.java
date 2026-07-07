package me.easyenchants.gui;

import me.easyenchants.BukkitTestSupport;
import me.easyenchants.EasyEnchantsPlugin;
import me.easyenchants.enchant.EnchantedBookApplicator;
import me.easyenchants.listener.EasyEnchantsListener;
import me.easyenchants.settings.EasyEnchantsSettings;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyEnchantsSettingsGuiTest extends BukkitTestSupport {
    @Test
    void guiClickIsCancelledAndToggleUpdatesSetting() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        EasyEnchantsSettings settings = new EasyEnchantsSettings(plugin);
        settings.load();
        EasyEnchantsSettingsGui gui = new EasyEnchantsSettingsGui(settings);
        EasyEnchantsListener listener = new EasyEnchantsListener(settings, gui, new EnchantedBookApplicator());
        PlayerMock player = MockBukkit.getMock().addPlayer("Admin");
        player.setOp(true);
        gui.open(player);

        InventoryView view = player.getOpenInventory();
        InventoryClickEvent event = new InventoryClickEvent(
            view,
            InventoryType.SlotType.CONTAINER,
            11,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        assertTrue(settings.dragAndDropBooksEnabled());
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertFalse(settings.dragAndDropBooksEnabled());
        assertInstanceOf(EasyEnchantsSettingsMenuHolder.class, player.getOpenInventory().getTopInventory().getHolder());
    }

    @Test
    void librarianRollingToggleUpdatesSetting() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        EasyEnchantsSettings settings = new EasyEnchantsSettings(plugin);
        settings.load();
        EasyEnchantsSettingsGui gui = new EasyEnchantsSettingsGui(settings);
        EasyEnchantsListener listener = new EasyEnchantsListener(settings, gui, new EnchantedBookApplicator());
        PlayerMock player = MockBukkit.getMock().addPlayer("Admin");
        player.setOp(true);
        gui.open(player);

        InventoryView view = player.getOpenInventory();
        InventoryClickEvent event = new InventoryClickEvent(
            view,
            InventoryType.SlotType.CONTAINER,
            15,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        assertTrue(settings.librarianRollingEnabled());
        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertFalse(settings.librarianRollingEnabled());
        assertFalse(plugin.getConfig().getBoolean("branches.librarian-rolling.enabled"));
    }

    @Test
    void guiDragIntoTopInventoryIsCancelled() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        EasyEnchantsSettings settings = new EasyEnchantsSettings(plugin);
        settings.load();
        EasyEnchantsSettingsGui gui = new EasyEnchantsSettingsGui(settings);
        EasyEnchantsListener listener = new EasyEnchantsListener(settings, gui, new EnchantedBookApplicator());
        PlayerMock player = MockBukkit.getMock().addPlayer("Admin");
        player.setOp(true);
        gui.open(player);

        InventoryDragEvent event = new InventoryDragEvent(
            player.getOpenInventory(),
            new ItemStack(Material.DIAMOND, 1),
            new ItemStack(Material.DIAMOND, 2),
            false,
            Map.of(0, new ItemStack(Material.DIAMOND, 1))
        );

        listener.onInventoryDrag(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void shiftClickFromBottomInventoryIsCancelled() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        EasyEnchantsSettings settings = new EasyEnchantsSettings(plugin);
        settings.load();
        EasyEnchantsSettingsGui gui = new EasyEnchantsSettingsGui(settings);
        EasyEnchantsListener listener = new EasyEnchantsListener(settings, gui, new EnchantedBookApplicator());
        PlayerMock player = MockBukkit.getMock().addPlayer("Admin");
        player.setOp(true);
        gui.open(player);

        InventoryView view = player.getOpenInventory();
        InventoryClickEvent event = new InventoryClickEvent(
            view,
            InventoryType.SlotType.CONTAINER,
            view.getTopInventory().getSize(),
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        );
        event.setCurrentItem(new ItemStack(Material.DIAMOND));

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
    }
}
