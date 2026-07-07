package me.easyenchants.gui;

import me.easyenchants.settings.EasyEnchantsSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class EasyEnchantsSettingsGui {
    private static final int INVENTORY_SIZE = 27;
    private static final int DRAG_DROP_BOOKS_SLOT = 11;
    private static final int LIBRARIAN_ROLLING_SLOT = 15;
    private static final String ADMIN_PERMISSION = "easyenchants.admin";

    private final EasyEnchantsSettings settings;

    public EasyEnchantsSettingsGui(EasyEnchantsSettings settings) {
        this.settings = settings;
    }

    public void open(Player player) {
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage(Component.text("You do not have permission to manage EasyEnchants settings.", NamedTextColor.RED));
            return;
        }

        EasyEnchantsSettingsMenuHolder holder = new EasyEnchantsSettingsMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, Component.text("EasyEnchants Settings", NamedTextColor.GOLD));
        holder.setInventory(inventory);
        inventory.setItem(DRAG_DROP_BOOKS_SLOT, dragDropBooksToggle());
        inventory.setItem(LIBRARIAN_ROLLING_SLOT, librarianRollingToggle());
        player.openInventory(inventory);
    }

    public void handleClick(Player player, int rawSlot) {
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.closeInventory();
            return;
        }
        if (rawSlot == DRAG_DROP_BOOKS_SLOT) {
            settings.toggleDragAndDropBooks();
            open(player);
            return;
        }
        if (rawSlot == LIBRARIAN_ROLLING_SLOT) {
            settings.toggleLibrarianRolling();
            open(player);
        }
    }

    private ItemStack dragDropBooksToggle() {
        boolean enabled = settings.dragAndDropBooksEnabled();
        return GuiItems.namedItem(
            enabled ? Material.LIME_DYE : Material.GRAY_DYE,
            Component.text("Drag & Drop Books: " + (enabled ? "Enabled" : "Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED),
            List.of(Component.text("Click to toggle.", NamedTextColor.GRAY))
        );
    }

    private ItemStack librarianRollingToggle() {
        boolean enabled = settings.librarianRollingEnabled();
        return GuiItems.namedItem(
            enabled ? Material.LIME_DYE : Material.GRAY_DYE,
            Component.text("Librarian Rolling: " + (enabled ? "Enabled" : "Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED),
            List.of(Component.text("Click to toggle.", NamedTextColor.GRAY))
        );
    }
}
