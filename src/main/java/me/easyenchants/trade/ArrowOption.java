package me.easyenchants.trade;

import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public record ArrowOption(PotionType potion) implements TradeOption {
    public Villager.Profession profession() { return Villager.Profession.FLETCHER; }
    public String id() { return potion.getKey().toString(); }
    public int level() { return 1; }
    public String displayName() {
        String name = potion.name().replace("STRONG_", "").replace("LONG_", "");
        String title = Arrays.stream(name.split("_")).map(word -> word.charAt(0) + word.substring(1).toLowerCase(Locale.ROOT))
            .collect(Collectors.joining(" "));
        if (potion.name().startsWith("STRONG_")) title += potion == PotionType.STRONG_SLOWNESS ? " IV" : " II";
        if (potion.name().startsWith("LONG_")) title += " (Extended)";
        return "Arrows of " + title;
    }
    public String searchText() { return (displayName() + " " + id()).toLowerCase(Locale.ROOT); }
    public ItemStack item() {
        ItemStack item = new ItemStack(Material.TIPPED_ARROW, 5);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(potion);
        item.setItemMeta(meta);
        return item;
    }
}
