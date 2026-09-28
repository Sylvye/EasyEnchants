package me.easyenchants.settings;

public interface EasyEnchantsFeatureSettings {
    boolean dragAndDropBooksEnabled();

    default boolean fletcherRollingEnabled() { return true; }

    default boolean librarianRollingEnabled() {
        return true;
    }

    default boolean villagerAccelerationEnabled() {
        return true;
    }

    default double villagerGrowthSpeedMultiplier() {
        return 5.0D;
    }

    default double villagerCuringSpeedMultiplier() {
        return 5.0D;
    }

    default int villagerBreedingCooldownSeconds() {
        return 60;
    }
}
