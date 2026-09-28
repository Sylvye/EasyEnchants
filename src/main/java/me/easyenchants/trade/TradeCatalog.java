package me.easyenchants.trade;

import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.easyenchants.gui.LibrarianBookOption;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class TradeCatalog {
    // All vanilla 1.21.11 brewable potions with effects. Water/base potions and Luck cannot be traded.
    private static final Set<PotionType> ARROWS = EnumSet.of(
        PotionType.NIGHT_VISION, PotionType.LONG_NIGHT_VISION,
        PotionType.INVISIBILITY, PotionType.LONG_INVISIBILITY,
        PotionType.LEAPING, PotionType.LONG_LEAPING, PotionType.STRONG_LEAPING,
        PotionType.FIRE_RESISTANCE, PotionType.LONG_FIRE_RESISTANCE,
        PotionType.SWIFTNESS, PotionType.LONG_SWIFTNESS, PotionType.STRONG_SWIFTNESS,
        PotionType.SLOWNESS, PotionType.LONG_SLOWNESS, PotionType.STRONG_SLOWNESS,
        PotionType.WATER_BREATHING, PotionType.LONG_WATER_BREATHING,
        PotionType.HEALING, PotionType.STRONG_HEALING, PotionType.HARMING, PotionType.STRONG_HARMING,
        PotionType.POISON, PotionType.LONG_POISON, PotionType.STRONG_POISON,
        PotionType.REGENERATION, PotionType.LONG_REGENERATION, PotionType.STRONG_REGENERATION,
        PotionType.STRENGTH, PotionType.LONG_STRENGTH, PotionType.STRONG_STRENGTH,
        PotionType.WEAKNESS, PotionType.LONG_WEAKNESS,
        PotionType.TURTLE_MASTER, PotionType.LONG_TURTLE_MASTER, PotionType.STRONG_TURTLE_MASTER,
        PotionType.SLOW_FALLING, PotionType.LONG_SLOW_FALLING,
        PotionType.WIND_CHARGED, PotionType.WEAVING, PotionType.OOZING, PotionType.INFESTED);

    private TradeCatalog() {}

    public static List<TradeOption> options(Villager.Profession profession) {
        List<TradeOption> options = new ArrayList<>();
        if (profession == Villager.Profession.LIBRARIAN) {
            try {
                for (var enchantment : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).getTagValues(EnchantmentTagKeys.TRADEABLE)) {
                    for (int level = enchantment.getStartLevel(); level <= enchantment.getMaxLevel(); level++) {
                        options.add(LibrarianBookOption.of(enchantment, level));
                    }
                }
            } catch (RuntimeException unavailableRegistry) {
                return List.of(); // Never broaden the catalog when eligibility cannot be established.
            }
        } else if (profession == Villager.Profession.FLETCHER) {
            ARROWS.forEach(potion -> options.add(new ArrowOption(potion)));
        }
        options.sort(Comparator.comparing(TradeOption::displayName).thenComparing(TradeOption::id).thenComparingInt(TradeOption::level));
        return List.copyOf(options);
    }

    public static boolean allowed(TradeOption option) {
        if (option instanceof ArrowOption arrow) return ARROWS.contains(arrow.potion());
        if (!(option instanceof LibrarianBookOption book)) return false;
        if (book.level() < book.enchantment().getStartLevel() || book.level() > book.enchantment().getMaxLevel()) return false;
        try {
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).getTagValues(EnchantmentTagKeys.TRADEABLE).stream().anyMatch(value -> value.getKey().equals(book.enchantment().getKey()));
        } catch (RuntimeException unavailableRegistry) {
            return false;
        }
    }

    public static TradeOption decode(String profession, String id, int level) {
        if ("librarian".equals(profession)) {
            var enchantment = RegistryLookup.enchantment(id);
            return enchantment == null ? null : LibrarianBookOption.of(enchantment, level);
        }
        if ("fletcher".equals(profession)) {
            for (PotionType potion : ARROWS) if (potion.getKey().toString().equals(id)) return new ArrowOption(potion);
        }
        return null;
    }
}
