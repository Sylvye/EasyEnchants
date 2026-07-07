package me.easyenchants.settings;

import org.bukkit.plugin.java.JavaPlugin;

public final class EasyEnchantsSettings implements EasyEnchantsFeatureSettings {
    private static final String DRAG_DROP_BOOKS_PATH = "branches.drag-and-drop-books.enabled";
    private static final String LIBRARIAN_ROLLING_PATH = "branches.librarian-rolling.enabled";

    private final JavaPlugin plugin;
    private boolean dragAndDropBooksEnabled;
    private boolean librarianRollingEnabled;

    public EasyEnchantsSettings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        dragAndDropBooksEnabled = plugin.getConfig().getBoolean(DRAG_DROP_BOOKS_PATH, true);
        librarianRollingEnabled = plugin.getConfig().getBoolean(LIBRARIAN_ROLLING_PATH, true);
    }

    @Override
    public boolean dragAndDropBooksEnabled() {
        return dragAndDropBooksEnabled;
    }

    @Override
    public boolean librarianRollingEnabled() {
        return librarianRollingEnabled;
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

    public boolean toggleDragAndDropBooks() {
        setDragAndDropBooksEnabled(!dragAndDropBooksEnabled);
        return dragAndDropBooksEnabled;
    }

    public boolean toggleLibrarianRolling() {
        setLibrarianRollingEnabled(!librarianRollingEnabled);
        return librarianRollingEnabled;
    }
}
