package me.easyenchants.settings;

import org.bukkit.plugin.java.JavaPlugin;

public final class EasyEnchantsSettings implements EasyEnchantsFeatureSettings {
    private static final String DRAG_DROP_BOOKS_PATH = "branches.drag-and-drop-books.enabled";
    private static final String LIBRARIAN_ROLLING_PATH = "branches.librarian-rolling.enabled";
    private static final String VILLAGER_ACCELERATION_PATH = "branches.villager-acceleration";
    private static final double DEFAULT_MULTIPLIER = 5.0D;
    private static final int DEFAULT_BREEDING_COOLDOWN_SECONDS = 60;

    private final JavaPlugin plugin;
    private boolean dragAndDropBooksEnabled;
    private boolean librarianRollingEnabled;
    private boolean fletcherRollingEnabled;
    private boolean villagerAccelerationEnabled;
    private double villagerGrowthSpeedMultiplier;
    private double villagerCuringSpeedMultiplier;
    private int villagerBreedingCooldownSeconds;

    public EasyEnchantsSettings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        dragAndDropBooksEnabled = plugin.getConfig().getBoolean(DRAG_DROP_BOOKS_PATH, true);
        fletcherRollingEnabled = plugin.getConfig().getBoolean("branches.fletcher-rolling.enabled", true);
        librarianRollingEnabled = plugin.getConfig().getBoolean(LIBRARIAN_ROLLING_PATH, true);
        villagerAccelerationEnabled = plugin.getConfig().getBoolean(VILLAGER_ACCELERATION_PATH + ".enabled", true);
        villagerGrowthSpeedMultiplier = validMultiplier(
            plugin.getConfig().getDouble(VILLAGER_ACCELERATION_PATH + ".growth-speed-multiplier", DEFAULT_MULTIPLIER),
            DEFAULT_MULTIPLIER
        );
        villagerCuringSpeedMultiplier = validMultiplier(
            plugin.getConfig().getDouble(VILLAGER_ACCELERATION_PATH + ".curing-speed-multiplier", DEFAULT_MULTIPLIER),
            DEFAULT_MULTIPLIER
        );
        int configuredCooldown = plugin.getConfig().getInt(
            VILLAGER_ACCELERATION_PATH + ".breeding-cooldown-seconds",
            DEFAULT_BREEDING_COOLDOWN_SECONDS
        );
        villagerBreedingCooldownSeconds = configuredCooldown >= 0 ? configuredCooldown : DEFAULT_BREEDING_COOLDOWN_SECONDS;
    }

    @Override
    public boolean dragAndDropBooksEnabled() {
        return dragAndDropBooksEnabled;
    }

    @Override
    public boolean librarianRollingEnabled() {
        return librarianRollingEnabled;
    }

    @Override
    public boolean fletcherRollingEnabled() { return fletcherRollingEnabled; }

    public boolean toggleFletcherRolling() {
        fletcherRollingEnabled = !fletcherRollingEnabled;
        save("branches.fletcher-rolling.enabled", fletcherRollingEnabled);
        return fletcherRollingEnabled;
    }

    public void setDragAndDropBooksEnabled(boolean enabled) {
        dragAndDropBooksEnabled = enabled;
        plugin.getConfig().set(DRAG_DROP_BOOKS_PATH, enabled);
        plugin.saveConfig();
    }

    public void setLibrarianRollingEnabled(boolean enabled) {
        librarianRollingEnabled = enabled;
        plugin.getConfig().set(LIBRARIAN_ROLLING_PATH, enabled);
        plugin.saveConfig();
    }

    @Override
    public boolean villagerAccelerationEnabled() {
        return villagerAccelerationEnabled;
    }

    @Override
    public double villagerGrowthSpeedMultiplier() {
        return villagerGrowthSpeedMultiplier;
    }

    @Override
    public double villagerCuringSpeedMultiplier() {
        return villagerCuringSpeedMultiplier;
    }

    @Override
    public int villagerBreedingCooldownSeconds() {
        return villagerBreedingCooldownSeconds;
    }

    public void setVillagerAccelerationEnabled(boolean enabled) {
        villagerAccelerationEnabled = enabled;
        save(VILLAGER_ACCELERATION_PATH + ".enabled", enabled);
    }

    public void setVillagerGrowthSpeedMultiplier(double multiplier) {
        requireValidMultiplier(multiplier);
        villagerGrowthSpeedMultiplier = multiplier;
        save(VILLAGER_ACCELERATION_PATH + ".growth-speed-multiplier", multiplier);
    }

    public void setVillagerCuringSpeedMultiplier(double multiplier) {
        requireValidMultiplier(multiplier);
        villagerCuringSpeedMultiplier = multiplier;
        save(VILLAGER_ACCELERATION_PATH + ".curing-speed-multiplier", multiplier);
    }

    public void setVillagerBreedingCooldownSeconds(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Breeding cooldown cannot be negative");
        }
        villagerBreedingCooldownSeconds = seconds;
        save(VILLAGER_ACCELERATION_PATH + ".breeding-cooldown-seconds", seconds);
    }

    public boolean toggleDragAndDropBooks() {
        setDragAndDropBooksEnabled(!dragAndDropBooksEnabled);
        return dragAndDropBooksEnabled;
    }

    public boolean toggleLibrarianRolling() {
        setLibrarianRollingEnabled(!librarianRollingEnabled);
        return librarianRollingEnabled;
    }

    public boolean toggleVillagerAcceleration() {
        setVillagerAccelerationEnabled(!villagerAccelerationEnabled);
        return villagerAccelerationEnabled;
    }

    private void save(String path, Object value) {
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
    }

    private static double validMultiplier(double value, double fallback) {
        return Double.isFinite(value) && value >= 1.0D ? value : fallback;
    }

    private static void requireValidMultiplier(double value) {
        if (!Double.isFinite(value) || value < 1.0D) {
            throw new IllegalArgumentException("Multiplier must be a finite number of at least 1.0");
        }
    }
}
