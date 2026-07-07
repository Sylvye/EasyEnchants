package me.easyenchants.settings;

public interface EasyEnchantsFeatureSettings {
    boolean dragAndDropBooksEnabled();

    default boolean librarianRollingEnabled() {
        return true;
    }
}
