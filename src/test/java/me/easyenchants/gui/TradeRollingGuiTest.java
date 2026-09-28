package me.easyenchants.gui;

import me.easyenchants.BukkitTestSupport;
import me.easyenchants.EasyEnchantsPlugin;
import me.easyenchants.trade.VillagerRollingService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeRollingGuiTest extends BukkitTestSupport {
    @Test
    void filtersOptionsByDisplayNameAndKey() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        TradeRollingGui gui = gui(plugin);

        List<me.easyenchants.trade.TradeOption> filtered = gui.filteredOptions("sharp");

        assertEquals(1, filtered.size());
        assertEquals(Enchantment.SHARPNESS, ((LibrarianBookOption) filtered.getFirst()).enchantment());
        assertEquals(5, filtered.getFirst().level());
    }

    @Test
    void bookItemContainsExactStoredEnchant() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        TradeRollingGui gui = gui(plugin);

        ItemStack item = gui.bookItem(LibrarianBookOption.of(Enchantment.UNBREAKING, 3));

        assertEquals(Material.ENCHANTED_BOOK, item.getType());
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        assertTrue(meta.hasStoredEnchant(Enchantment.UNBREAKING));
        assertEquals(3, meta.getStoredEnchantLevel(Enchantment.UNBREAKING));
    }

    @Test
    void fletcherMenuSearchAndClickPreserveExactPotion() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        var service = new VillagerRollingService(plugin);
        var gui = new TradeRollingGui(new ChatPromptManager(plugin), service);
        var world = tradeWorld("arrows");
        var villager = world.spawn(new org.bukkit.Location(world, 0, 64, 0), org.bukkit.entity.Villager.class);
        villager.setProfession(org.bukkit.entity.Villager.Profession.FLETCHER);
        var player = MockBukkit.getMock().addPlayer();
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LUCK, 200, 0));
        gui.open(player, villager.getUniqueId(), 0, "strong_harming");
        var holder = (TradeRollingMenuHolder) player.getOpenInventory().getTopInventory().getHolder();
        assertEquals(1, holder.options().size());
        var item = holder.getInventory().getItem(0);
        assertEquals(org.bukkit.potion.PotionType.STRONG_HARMING, ((org.bukkit.inventory.meta.PotionMeta) item.getItemMeta()).getBasePotionType());
        gui.handleClick(player, holder, 0);
        assertEquals(0, service.remaining(villager));
        org.junit.jupiter.api.Assertions.assertFalse(player.hasPotionEffect(org.bukkit.potion.PotionEffectType.LUCK));
        assertTrue(villager.hasPotionEffect(org.bukkit.potion.PotionEffectType.LUCK));
    }

    @Test
    void arrowCatalogExcludesUnbrewableAndIncludesExtendedAndStrongVariants() {
        var options = me.easyenchants.trade.TradeCatalog.options(org.bukkit.entity.Villager.Profession.FLETCHER);
        assertTrue(options.stream().anyMatch(option -> option.id().equals("minecraft:strong_harming")));
        assertTrue(options.stream().anyMatch(option -> option.id().equals("minecraft:long_poison")));
        org.junit.jupiter.api.Assertions.assertFalse(options.stream().anyMatch(option -> option.id().equals("minecraft:luck") || option.id().equals("minecraft:water")));
    }

    private TradeRollingGui gui(EasyEnchantsPlugin plugin) {
        return new TradeRollingGui(
            new ChatPromptManager(plugin),
            new VillagerRollingService(plugin),
            List.of(
                LibrarianBookOption.of(Enchantment.SHARPNESS, 5),
                LibrarianBookOption.of(Enchantment.UNBREAKING, 3)
            )
        );
    }
}
