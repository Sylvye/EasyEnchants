package me.easyenchants.listener;

import me.easyenchants.enchant.EnchantedBookApplicator;
import me.easyenchants.gui.EasyEnchantsSettingsGui;
import me.easyenchants.gui.EasyEnchantsSettingsMenuHolder;
import me.easyenchants.settings.EasyEnchantsFeatureSettings;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class EasyEnchantsListener implements Listener {
    private final EasyEnchantsFeatureSettings settings;
    private final EasyEnchantsSettingsGui settingsGui;
    private final EnchantedBookApplicator applicator;

    public EasyEnchantsListener(EasyEnchantsFeatureSettings settings, EasyEnchantsSettingsGui settingsGui, EnchantedBookApplicator applicator) {
        this.settings = settings;
        this.settingsGui = settingsGui;
        this.applicator = applicator;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (protectSettingsGui(event)) {
            return;
        }
        if (event instanceof InventoryCreativeEvent || !settings.dragAndDropBooksEnabled()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!isPlayerInventoryClick(event)) {
            return;
        }

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if (cursor == null || cursor.getType() != Material.ENCHANTED_BOOK || current == null || current.getType() == Material.AIR) {
            return;
        }

        EnchantedBookApplicator.ApplicationResult result = applicator.apply(current, cursor);
        if (!result.applied()) {
            return;
        }

        event.setCancelled(true);
        event.setCurrentItem(result.targetAfter());
        event.getView().setCursor(result.cursorAfter());
        player.playSound(player.getLocation(), "minecraft:block.enchantment_table.use", SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof EasyEnchantsSettingsMenuHolder)) {
            return;
        }
        int topInventorySize = topInventory.getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topInventorySize)) {
            event.setCancelled(true);
        }
    }

    private boolean protectSettingsGui(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof EasyEnchantsSettingsMenuHolder)) {
            return false;
        }
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                settingsGui.handleClick(player, event.getRawSlot());
            }
        }
        return true;
    }

    private boolean isPlayerInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        return clickedInventory != null && clickedInventory == event.getView().getBottomInventory();
    }
}
