package me.easyenchants.settings;

import me.easyenchants.BukkitTestSupport;
import me.easyenchants.EasyEnchantsPlugin;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyEnchantsSettingsTest extends BukkitTestSupport {
    @Test
    void loadsVillagerAccelerationDefaults() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        EasyEnchantsSettings settings = new EasyEnchantsSettings(plugin);
        settings.load();

        assertTrue(settings.villagerAccelerationEnabled());
        assertEquals(5.0D, settings.villagerGrowthSpeedMultiplier());
        assertEquals(5.0D, settings.villagerCuringSpeedMultiplier());
        assertEquals(60, settings.villagerBreedingCooldownSeconds());
    }

    @Test
    void persistsCustomVillagerAccelerationValues() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        EasyEnchantsSettings settings = new EasyEnchantsSettings(plugin);
        settings.load();

        settings.setVillagerGrowthSpeedMultiplier(8.5D);
        settings.setVillagerCuringSpeedMultiplier(3.0D);
        settings.setVillagerBreedingCooldownSeconds(30);

        assertEquals(8.5D, plugin.getConfig().getDouble("branches.villager-acceleration.growth-speed-multiplier"));
        assertEquals(3.0D, plugin.getConfig().getDouble("branches.villager-acceleration.curing-speed-multiplier"));
        assertEquals(30, plugin.getConfig().getInt("branches.villager-acceleration.breeding-cooldown-seconds"));
    }

    @Test
    void invalidConfiguredValuesUseDefaults() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        plugin.getConfig().set("branches.villager-acceleration.growth-speed-multiplier", 0.5D);
        plugin.getConfig().set("branches.villager-acceleration.curing-speed-multiplier", Double.POSITIVE_INFINITY);
        plugin.getConfig().set("branches.villager-acceleration.breeding-cooldown-seconds", -1);
        plugin.saveConfig();
        EasyEnchantsSettings settings = new EasyEnchantsSettings(plugin);

        settings.load();

        assertEquals(5.0D, settings.villagerGrowthSpeedMultiplier());
        assertEquals(5.0D, settings.villagerCuringSpeedMultiplier());
        assertEquals(60, settings.villagerBreedingCooldownSeconds());
    }
}
