package me.easyenchants.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class LibrarianRollingMenuHolder implements InventoryHolder {
    private final UUID villagerUuid;
    private final int page;
    private final String query;
    private Inventory inventory;

    public LibrarianRollingMenuHolder(UUID villagerUuid, int page, String query) {
        this.villagerUuid = villagerUuid;
        this.page = page;
        this.query = query == null ? "" : query;
    }

    public UUID villagerUuid() {
        return villagerUuid;
    }

    public int page() {
        return page;
    }

    public String query() {
        return query;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
