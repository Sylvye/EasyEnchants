package me.easyenchants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;

public abstract class BukkitTestSupport {
    protected boolean tradeTagsUnavailable;
    @BeforeEach
    void setUpBukkit() {
        MockBukkit.mock();
        installTradeTags();
    }

    // MockBukkit 4.110 lacks feature flags and registry tags. Keep those gaps in test fixtures.
    protected org.mockbukkit.mockbukkit.world.WorldMock tradeWorld(String name) {
        var world = new TradeWorld();
        world.setName(name);
        MockBukkit.getMock().addWorld(world);
        return world;
    }

    public static final class TradeWorld extends org.mockbukkit.mockbukkit.world.WorldMock {
        public final java.util.Set<org.bukkit.FeatureFlag> flags = new java.util.HashSet<>();
        @Override
        public java.util.Set<org.bukkit.FeatureFlag> getFeatureFlags() { return flags; }
    }

    @SuppressWarnings("unchecked")
    private void installTradeTags() {
        try {
            var access = io.papermc.paper.registry.RegistryAccess.registryAccess();
            var field = access.getClass().getDeclaredField("registries");
            field.setAccessible(true);
            var registries = (java.util.Map<io.papermc.paper.registry.RegistryKey<?>, org.bukkit.Registry<?>>) field.get(access);
            var registry = new org.mockbukkit.mockbukkit.registry.RegistryMock<org.bukkit.enchantments.Enchantment>(io.papermc.paper.registry.RegistryKey.ENCHANTMENT) {
                @Override
                public java.util.Collection<org.bukkit.enchantments.Enchantment> getTagValues(io.papermc.paper.registry.tag.TagKey<org.bukkit.enchantments.Enchantment> tag) {
                    if (tradeTagsUnavailable) throw new IllegalStateException("Registry unavailable");
                    var excluded = java.util.Set.of("soul_speed", "swift_sneak", "wind_burst");
                    var doubled = java.util.Set.of("mending", "frost_walker", "binding_curse", "vanishing_curse", "soul_speed", "swift_sneak", "wind_burst");
                    if (tag.equals(io.papermc.paper.registry.keys.tags.EnchantmentTagKeys.TRADEABLE)) {
                        return org.bukkit.Registry.ENCHANTMENT.stream().filter(e -> !excluded.contains(e.getKey().getKey())).toList();
                    }
                    if (tag.equals(io.papermc.paper.registry.keys.tags.EnchantmentTagKeys.DOUBLE_TRADE_PRICE)) {
                        return org.bukkit.Registry.ENCHANTMENT.stream().filter(e -> doubled.contains(e.getKey().getKey())).toList();
                    }
                    throw new IllegalArgumentException("Unexpected tag " + tag);
                }
            };
            registries.put(io.papermc.paper.registry.RegistryKey.ENCHANTMENT, registry);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot install missing MockBukkit trade tags", exception);
        }
    }

    @AfterEach
    void tearDownBukkit() {
        MockBukkit.unmock();
    }
}
