package me.easyenchants.settings;

import org.bukkit.plugin.java.JavaPlugin;

public final class EasyEnchantsSettings implements EasyEnchantsFeatureSettings {
    private static final String DRAG_DROP_BOOKS_PATH = "branches.drag-and-drop-books.enabled";

    private final JavaPlugin plugin;
    private boolean dragAndDropBooksEnabled;

    public EasyEnchantsSettings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        dragAndDropBooksEnabled = plugin.getConfig().getBoolean(DRAG_DROP_BOOKS_PATH, true);
    }

    @Override
    public boolean dragAndDropBooksEnabled() {
        return dragAndDropBooksEnabled;
    }

    public void setDragAndDropBooksEnabled(boolean enabled) {
        dragAndDropBooksEnabled = enabled;
        plugin.getConfig().set(DRAG_DROP_BOOKS_PATH, enabled);
        plugin.saveConfig();
    }

    public boolean toggleDragAndDropBooks() {
        setDragAndDropBooksEnabled(!dragAndDropBooksEnabled);
        return dragAndDropBooksEnabled;
    }
}
