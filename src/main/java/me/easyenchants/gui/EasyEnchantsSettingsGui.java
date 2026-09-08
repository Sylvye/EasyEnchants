package me.easyenchants.gui;

import me.easyenchants.settings.EasyEnchantsSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class EasyEnchantsSettingsGui {
    private static final int INVENTORY_SIZE = 27;
    private static final int DRAG_DROP_BOOKS_SLOT = 11;
    private static final int LIBRARIAN_ROLLING_SLOT = 15;
    private static final int VILLAGER_ACCELERATION_SLOT = 13;
    private static final int ACCELERATION_TOGGLE_SLOT = 10;
    private static final int GROWTH_MULTIPLIER_SLOT = 12;
    private static final int CURING_MULTIPLIER_SLOT = 14;
    private static final int BREEDING_COOLDOWN_SLOT = 16;
    private static final int BACK_SLOT = 22;
    private static final String ADMIN_PERMISSION = "easyenchants.admin";

    private final EasyEnchantsSettings settings;
    private final ChatPromptManager promptManager;

    public EasyEnchantsSettingsGui(EasyEnchantsSettings settings) {
        this(settings, null);
    }

    public EasyEnchantsSettingsGui(EasyEnchantsSettings settings, ChatPromptManager promptManager) {
        this.settings = settings;
        this.promptManager = promptManager;
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
        inventory.setItem(VILLAGER_ACCELERATION_SLOT, villagerAccelerationEntry());
        player.openInventory(inventory);
    }

    public void handleClick(Player player, InventoryHolder holder, int rawSlot) {
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.closeInventory();
            return;
        }
        if (holder instanceof VillagerAccelerationMenuHolder) {
            handleAccelerationClick(player, rawSlot);
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
            return;
        }
        if (rawSlot == VILLAGER_ACCELERATION_SLOT) {
            openVillagerAcceleration(player);
        }
    }

    public void handleClick(Player player, int rawSlot) {
        handleClick(player, player.getOpenInventory().getTopInventory().getHolder(), rawSlot);
    }

    public void openVillagerAcceleration(Player player) {
        VillagerAccelerationMenuHolder holder = new VillagerAccelerationMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, Component.text("Villager Acceleration", NamedTextColor.GOLD));
        holder.setInventory(inventory);
        boolean enabled = settings.villagerAccelerationEnabled();
        inventory.setItem(ACCELERATION_TOGGLE_SLOT, GuiItems.namedItem(
            enabled ? Material.LIME_DYE : Material.GRAY_DYE,
            Component.text("Module: " + (enabled ? "Enabled" : "Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED),
            List.of(Component.text("Click to toggle.", NamedTextColor.GRAY))
        ));
        inventory.setItem(GROWTH_MULTIPLIER_SLOT, numericItem(
            Material.CLOCK, "Growth Speed", formatMultiplier(settings.villagerGrowthSpeedMultiplier()), "Click to enter a multiplier (minimum 1.0)."
        ));
        inventory.setItem(CURING_MULTIPLIER_SLOT, numericItem(
            Material.GOLDEN_APPLE, "Curing Speed", formatMultiplier(settings.villagerCuringSpeedMultiplier()), "Click to enter a multiplier (minimum 1.0)."
        ));
        inventory.setItem(BREEDING_COOLDOWN_SLOT, numericItem(
            Material.BREAD, "Breeding Cooldown", settings.villagerBreedingCooldownSeconds() + " seconds", "Click to enter non-negative whole seconds."
        ));
        inventory.setItem(BACK_SLOT, GuiItems.namedItem(
            Material.ARROW, Component.text("Back", NamedTextColor.YELLOW), List.of(Component.text("Return to EasyEnchants settings.", NamedTextColor.GRAY))
        ));
        player.openInventory(inventory);
    }

    private void handleAccelerationClick(Player player, int rawSlot) {
        if (rawSlot == ACCELERATION_TOGGLE_SLOT) {
            settings.toggleVillagerAcceleration();
            openVillagerAcceleration(player);
        } else if (rawSlot == GROWTH_MULTIPLIER_SLOT) {
            promptMultiplier(player, "growth speed", settings::setVillagerGrowthSpeedMultiplier);
        } else if (rawSlot == CURING_MULTIPLIER_SLOT) {
            promptMultiplier(player, "curing speed", settings::setVillagerCuringSpeedMultiplier);
        } else if (rawSlot == BREEDING_COOLDOWN_SLOT) {
            promptCooldown(player);
        } else if (rawSlot == BACK_SLOT) {
            open(player);
        }
    }

    private void promptMultiplier(Player player, String label, java.util.function.DoubleConsumer setter) {
        if (promptManager == null) {
            return;
        }
        promptManager.prompt(player, "Enter the " + label + " multiplier (minimum 1.0).", text -> {
            if (text.equalsIgnoreCase("cancel")) {
                openVillagerAcceleration(player);
                return;
            }
            try {
                double value = Double.parseDouble(text.trim());
                setter.accept(value);
            } catch (IllegalArgumentException exception) {
                player.sendMessage(Component.text("Enter a finite number of at least 1.0.", NamedTextColor.RED));
            }
            openVillagerAcceleration(player);
        });
    }

    private void promptCooldown(Player player) {
        if (promptManager == null) {
            return;
        }
        promptManager.prompt(player, "Enter the breeding cooldown in whole seconds (minimum 0).", text -> {
            if (text.equalsIgnoreCase("cancel")) {
                openVillagerAcceleration(player);
                return;
            }
            try {
                settings.setVillagerBreedingCooldownSeconds(Integer.parseInt(text.trim()));
            } catch (IllegalArgumentException exception) {
                player.sendMessage(Component.text("Enter a non-negative whole number of seconds.", NamedTextColor.RED));
            }
            openVillagerAcceleration(player);
        });
    }

    private ItemStack villagerAccelerationEntry() {
        boolean enabled = settings.villagerAccelerationEnabled();
        return GuiItems.namedItem(
            Material.VILLAGER_SPAWN_EGG,
            Component.text("Villager Acceleration: " + (enabled ? "Enabled" : "Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED),
            List.of(Component.text("Click to configure.", NamedTextColor.GRAY))
        );
    }

    private ItemStack numericItem(Material material, String name, String value, String help) {
        return GuiItems.namedItem(
            material,
            Component.text(name + ": " + value, NamedTextColor.AQUA),
            List.of(Component.text(help, NamedTextColor.GRAY))
        );
    }

    private String formatMultiplier(double multiplier) {
        return multiplier + "x";
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
