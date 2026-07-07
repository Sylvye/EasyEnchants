package me.easyenchants.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.enchantments.Enchantment;

public record LibrarianBookOption(Enchantment enchantment, int level, String searchText) {
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    public static LibrarianBookOption of(Enchantment enchantment, int level) {
        String display = PLAIN_TEXT.serialize(enchantment.displayName(level));
        String key = enchantment.getKey().toString();
        return new LibrarianBookOption(enchantment, level, (display + " " + key).toLowerCase());
    }

    public String displayName() {
        return PLAIN_TEXT.serialize(enchantment.displayName(level));
    }
}
