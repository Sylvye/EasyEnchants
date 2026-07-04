package me.easyenchants;

import me.easyenchants.command.EasyEnchantsCommand;
import me.easyenchants.enchant.EnchantedBookApplicator;
import me.easyenchants.gui.EasyEnchantsSettingsGui;
import me.easyenchants.listener.EasyEnchantsListener;
import me.easyenchants.settings.EasyEnchantsSettings;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class EasyEnchantsPlugin extends JavaPlugin {
    private EasyEnchantsSettings settings;
    private EasyEnchantsSettingsGui settingsGui;

    @Override
    public void onEnable() {
        settings = new EasyEnchantsSettings(this);
        settings.load();

        settingsGui = new EasyEnchantsSettingsGui(settings);

        EasyEnchantsCommand commandExecutor = new EasyEnchantsCommand(settingsGui);
        PluginCommand easyEnchantsCommand = Objects.requireNonNull(getCommand("easyenchants"), "easyenchants command missing from plugin.yml");
        easyEnchantsCommand.setExecutor(commandExecutor);
        easyEnchantsCommand.setTabCompleter(commandExecutor);

        getServer().getPluginManager().registerEvents(
            new EasyEnchantsListener(settings, settingsGui, new EnchantedBookApplicator()),
            this
        );
    }
}
