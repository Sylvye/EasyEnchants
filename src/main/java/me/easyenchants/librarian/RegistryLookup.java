package me.easyenchants.librarian;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

final class RegistryLookup {
    private RegistryLookup() {
    }

    static Enchantment enchantment(String key) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(key);
        return namespacedKey == null ? null : Registry.ENCHANTMENT.get(namespacedKey);
    }
}
