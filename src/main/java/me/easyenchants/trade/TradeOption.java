package me.easyenchants.trade;

import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

public interface TradeOption {
    Villager.Profession profession();
    String id();
    int level();
    String displayName();
    String searchText();
    ItemStack item();
}
