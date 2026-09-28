package me.easyenchants.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.Material;
import me.easyenchants.trade.TradeOption;
import java.util.Locale;

public record LibrarianBookOption(Enchantment enchantment, int level, String searchText) implements TradeOption {
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    public static LibrarianBookOption of(Enchantment enchantment, int level) {
        String display = PLAIN_TEXT.serialize(enchantment.displayName(level));
        String key = enchantment.getKey().toString();
        return new LibrarianBookOption(enchantment, level, (display + " " + key).toLowerCase(Locale.ROOT));
    }

    public Villager.Profession profession() { return Villager.Profession.LIBRARIAN; }

    public String id() { return enchantment.getKey().toString(); }

    public ItemStack item() {
        var item = new ItemStack(Material.ENCHANTED_BOOK);
        var meta = (EnchantmentStorageMeta) item.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        item.setItemMeta(meta);
        return item;
    }

    public String displayName() {
        return PLAIN_TEXT.serialize(enchantment.displayName(level));
    }
}
