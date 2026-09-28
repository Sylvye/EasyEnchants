package me.easyenchants.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.List;
import me.easyenchants.trade.TradeOption;

public final class TradeRollingMenuHolder implements InventoryHolder {
    private final UUID villagerUuid;
    private final int page;
    private final String query;
    private Inventory inventory;
    private final List<TradeOption> options;

    public TradeRollingMenuHolder(UUID villagerUuid, int page, String query) {
        this(villagerUuid, page, query, List.of());
    }

    public TradeRollingMenuHolder(UUID villagerUuid, int page, String query, List<TradeOption> options) {
        this.options = List.copyOf(options);
        this.villagerUuid = villagerUuid;
        this.page = page;
        this.query = query == null ? "" : query;
    }

    public List<TradeOption> options() { return options; }

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
