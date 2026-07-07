package me.easyenchants.listener;

import me.easyenchants.enchant.EnchantedBookApplicator;
import me.easyenchants.gui.EasyEnchantsSettingsGui;
import me.easyenchants.gui.EasyEnchantsSettingsMenuHolder;
import me.easyenchants.gui.LibrarianRollingGui;
import me.easyenchants.gui.LibrarianRollingMenuHolder;
import me.easyenchants.librarian.LibrarianRollingService;
import me.easyenchants.settings.EasyEnchantsFeatureSettings;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffectType;

public final class EasyEnchantsListener implements Listener {
    private final EasyEnchantsFeatureSettings settings;
    private final EasyEnchantsSettingsGui settingsGui;
    private final EnchantedBookApplicator applicator;
    private final LibrarianRollingGui librarianRollingGui;
    private final LibrarianRollingService librarianRollingService;

    public EasyEnchantsListener(EasyEnchantsFeatureSettings settings, EasyEnchantsSettingsGui settingsGui, EnchantedBookApplicator applicator) {
        this(settings, settingsGui, applicator, null, null);
    }

    public EasyEnchantsListener(
        EasyEnchantsFeatureSettings settings,
        EasyEnchantsSettingsGui settingsGui,
        EnchantedBookApplicator applicator,
        LibrarianRollingGui librarianRollingGui,
        LibrarianRollingService librarianRollingService
    ) {
        this.settings = settings;
        this.settingsGui = settingsGui;
        this.applicator = applicator;
        this.librarianRollingGui = librarianRollingGui;
        this.librarianRollingService = librarianRollingService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (protectPluginGui(event)) {
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
        if (!isProtectedMenu(topInventory.getHolder())) {
            return;
        }
        int topInventorySize = topInventory.getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topInventorySize)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!settings.librarianRollingEnabled() || librarianRollingGui == null || librarianRollingService == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPotionEffect(PotionEffectType.LUCK) || !librarianRollingService.isUnlockedLibrarian(villager)) {
            return;
        }
        event.setCancelled(true);
        librarianRollingGui.open(player, villager);
    }

    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (!settings.librarianRollingEnabled() || librarianRollingService == null) {
            return;
        }
        AbstractVillager entity = event.getEntity();
        if (!(entity instanceof Villager villager) || villager.getProfession() != Villager.Profession.LIBRARIAN) {
            return;
        }
        MerchantRecipe replacement = librarianRollingService.pendingReplacement(villager, event.getRecipe());
        if (replacement != null) {
            event.setRecipe(replacement);
        }
    }

    private boolean protectPluginGui(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!isProtectedMenu(holder)) {
            return false;
        }
        boolean topClick = event.getClickedInventory() == event.getView().getTopInventory();
        if (topClick) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                if (holder instanceof EasyEnchantsSettingsMenuHolder && settingsGui != null) {
                    settingsGui.handleClick(player, event.getRawSlot());
                } else if (holder instanceof LibrarianRollingMenuHolder librarianHolder && librarianRollingGui != null) {
                    librarianRollingGui.handleClick(player, librarianHolder, event.getRawSlot());
                }
            }
        } else if (movesItemsAcrossInventories(event.getAction())) {
            event.setCancelled(true);
        }
        return true;
    }

    private boolean isProtectedMenu(InventoryHolder holder) {
        return holder instanceof EasyEnchantsSettingsMenuHolder
            || holder instanceof LibrarianRollingMenuHolder;
    }

    private boolean movesItemsAcrossInventories(InventoryAction action) {
        return action == InventoryAction.MOVE_TO_OTHER_INVENTORY
            || action == InventoryAction.COLLECT_TO_CURSOR
            || action == InventoryAction.UNKNOWN;
    }

    private boolean isPlayerInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        return clickedInventory != null && clickedInventory == event.getView().getBottomInventory();
    }
}
