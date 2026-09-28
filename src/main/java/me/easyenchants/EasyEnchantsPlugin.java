package me.easyenchants;

import me.easyenchants.command.EasyEnchantsCommand;
import me.easyenchants.enchant.EnchantedBookApplicator;
import me.easyenchants.gui.ChatPromptManager;
import me.easyenchants.gui.EasyEnchantsSettingsGui;
import me.easyenchants.gui.TradeRollingGui;
import me.easyenchants.trade.VillagerRollingService;
import me.easyenchants.listener.EasyEnchantsListener;
import me.easyenchants.listener.TradeGuaranteeListener;
import org.bukkit.entity.Villager;
import me.easyenchants.listener.VillagerAccelerationListener;
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

        ChatPromptManager promptManager = new ChatPromptManager(this);
        settingsGui = new EasyEnchantsSettingsGui(settings, promptManager);
        VillagerRollingService rollingService = new VillagerRollingService(this, settings);
        TradeRollingGui rollingGui = new TradeRollingGui(promptManager, rollingService);

        EasyEnchantsCommand commandExecutor = new EasyEnchantsCommand(settingsGui);
        PluginCommand easyEnchantsCommand = Objects.requireNonNull(getCommand("easyenchants"), "easyenchants command missing from plugin.yml");
        easyEnchantsCommand.setExecutor(commandExecutor);
        easyEnchantsCommand.setTabCompleter(commandExecutor);

        getServer().getPluginManager().registerEvents(
            new EasyEnchantsListener(settings, settingsGui, new EnchantedBookApplicator(), rollingGui, rollingService),
            this
        );
        getServer().getPluginManager().registerEvents(new TradeGuaranteeListener(this, rollingService), this);
        getServer().getWorlds().forEach(world -> world.getEntitiesByClass(Villager.class).forEach(rollingService::scheduleReconcile));
        getServer().getPluginManager().registerEvents(promptManager, this);
        getServer().getPluginManager().registerEvents(new VillagerAccelerationListener(this, settings), this);
    }
}
