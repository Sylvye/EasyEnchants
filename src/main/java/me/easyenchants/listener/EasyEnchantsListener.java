package me.easyenchants.listener;

import me.easyenchants.enchant.EnchantedBookApplicator;
import me.easyenchants.gui.EasyEnchantsSettingsGui;
import me.easyenchants.gui.EasyEnchantsSettingsMenuHolder;
import me.easyenchants.gui.TradeRollingGui;
import me.easyenchants.gui.TradeRollingMenuHolder;
import me.easyenchants.gui.VillagerAccelerationMenuHolder;
import me.easyenchants.trade.VillagerRollingService;
import me.easyenchants.settings.EasyEnchantsFeatureSettings;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffectType;

public final class EasyEnchantsListener implements Listener {
    private final EasyEnchantsFeatureSettings settings;
    private final EasyEnchantsSettingsGui settingsGui;
    private final EnchantedBookApplicator applicator;
    private final TradeRollingGui rollingGui;
    private final VillagerRollingService rollingService;

    public EasyEnchantsListener(EasyEnchantsFeatureSettings settings, EasyEnchantsSettingsGui settingsGui, EnchantedBookApplicator applicator) {
        this(settings, settingsGui, applicator, null, null);
    }

    public EasyEnchantsListener(
        EasyEnchantsFeatureSettings settings,
        EasyEnchantsSettingsGui settingsGui,
        EnchantedBookApplicator applicator,
        TradeRollingGui rollingGui,
        VillagerRollingService rollingService
    ) {
        this.settings = settings;
        this.settingsGui = settingsGui;
        this.applicator = applicator;
        this.rollingGui = rollingGui;
        this.rollingService = rollingService;
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
        if (result.incompatible()) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), "minecraft:entity.villager.no", SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        if (!result.applied()) {
            return;
        }

        event.setCancelled(true);
        event.setCurrentItem(result.targetAfter());
        event.getView().setCursor(result.cursorAfter());
        if (!result.incompatible()) {
            player.playSound(player.getLocation(), "minecraft:block.enchantment_table.use", SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
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
        if (rollingGui == null || rollingService == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPotionEffect(PotionEffectType.LUCK) || !rollingService.canSelect(villager)
            || (villager.getProfession() == Villager.Profession.LIBRARIAN ? !settings.librarianRollingEnabled() : !settings.fletcherRollingEnabled())) {
            return;
        }
        event.setCancelled(true);
        rollingGui.open(player, villager);
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
                    settingsGui.handleClick(player, holder, event.getRawSlot());
                } else if (holder instanceof VillagerAccelerationMenuHolder && settingsGui != null) {
                    settingsGui.handleClick(player, holder, event.getRawSlot());
                } else if (holder instanceof TradeRollingMenuHolder tradeHolder && rollingGui != null) {
                    rollingGui.handleClick(player, tradeHolder, event.getRawSlot());
                }
            }
        } else if (movesItemsAcrossInventories(event.getAction())) {
            event.setCancelled(true);
        }
        return true;
    }

    private boolean isProtectedMenu(InventoryHolder holder) {
        return holder instanceof EasyEnchantsSettingsMenuHolder
            || holder instanceof VillagerAccelerationMenuHolder
            || holder instanceof TradeRollingMenuHolder;
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
