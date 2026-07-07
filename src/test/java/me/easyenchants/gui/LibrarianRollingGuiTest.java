package me.easyenchants.gui;

import me.easyenchants.BukkitTestSupport;
import me.easyenchants.EasyEnchantsPlugin;
import me.easyenchants.librarian.LibrarianRollingService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibrarianRollingGuiTest extends BukkitTestSupport {
    @Test
    void filtersOptionsByDisplayNameAndKey() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        LibrarianRollingGui gui = gui(plugin);

        List<LibrarianBookOption> filtered = gui.filteredOptions("sharp");

        assertEquals(1, filtered.size());
        assertEquals(Enchantment.SHARPNESS, filtered.getFirst().enchantment());
        assertEquals(5, filtered.getFirst().level());
    }

    @Test
    void bookItemContainsExactStoredEnchant() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        LibrarianRollingGui gui = gui(plugin);

        ItemStack item = gui.bookItem(LibrarianBookOption.of(Enchantment.UNBREAKING, 3));

        assertEquals(Material.ENCHANTED_BOOK, item.getType());
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        assertTrue(meta.hasStoredEnchant(Enchantment.UNBREAKING));
        assertEquals(3, meta.getStoredEnchantLevel(Enchantment.UNBREAKING));
    }

    private LibrarianRollingGui gui(EasyEnchantsPlugin plugin) {
        return new LibrarianRollingGui(
            new ChatPromptManager(plugin),
            new LibrarianRollingService(plugin),
            List.of(
                LibrarianBookOption.of(Enchantment.SHARPNESS, 5),
                LibrarianBookOption.of(Enchantment.UNBREAKING, 3)
            )
        );
    }
}
