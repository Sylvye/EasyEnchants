package me.easyenchants.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

final class GuiItems {
    private static final NamedTextColor DEFAULT_LORE_COLOR = NamedTextColor.GRAY;

    private GuiItems() {
    }

    static ItemStack namedItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(name));
        meta.lore(lore.stream()
            .map(line -> plain(line).colorIfAbsent(DEFAULT_LORE_COLOR))
            .toList());
        item.setItemMeta(meta);
        return item;
    }

    private static Component plain(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
