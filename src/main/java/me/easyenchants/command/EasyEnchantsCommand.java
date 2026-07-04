package me.easyenchants.command;

import me.easyenchants.gui.EasyEnchantsSettingsGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class EasyEnchantsCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "easyenchants.admin";

    private final EasyEnchantsSettingsGui settingsGui;

    public EasyEnchantsCommand(EasyEnchantsSettingsGui settingsGui) {
        this.settingsGui = settingsGui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can open the EasyEnchants settings menu.", NamedTextColor.RED));
            return true;
        }
        if (args.length > 0) {
            player.sendMessage(Component.text("Usage: /" + label, NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage(Component.text("You do not have permission to manage EasyEnchants settings.", NamedTextColor.RED));
            return true;
        }
        settingsGui.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        return List.of();
    }
}
