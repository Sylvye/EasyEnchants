package me.easyenchants.command;

import me.easyenchants.BukkitTestSupport;
import me.easyenchants.EasyEnchantsPlugin;
import me.easyenchants.gui.EasyEnchantsSettingsMenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyEnchantsCommandTest extends BukkitTestSupport {
    @Test
    void easyEnchantsCommandOpensSettingsForAdmin() {
        MockBukkit.load(EasyEnchantsPlugin.class);
        PlayerMock player = MockBukkit.getMock().addPlayer("Admin");
        player.setOp(true);

        boolean dispatched = Bukkit.dispatchCommand(player, "easyenchants");

        assertTrue(dispatched);
        assertInstanceOf(EasyEnchantsSettingsMenuHolder.class, player.getOpenInventory().getTopInventory().getHolder());
    }

    @Test
    void eeAliasOpensSettingsForAdmin() {
        MockBukkit.load(EasyEnchantsPlugin.class);
        PlayerMock player = MockBukkit.getMock().addPlayer("Admin");
        player.setOp(true);

        boolean dispatched = Bukkit.dispatchCommand(player, "ee");

        assertTrue(dispatched);
        assertInstanceOf(EasyEnchantsSettingsMenuHolder.class, player.getOpenInventory().getTopInventory().getHolder());
    }

    @Test
    void nonAdminIsDeniedSettingsMenu() {
        MockBukkit.load(EasyEnchantsPlugin.class);
        PlayerMock player = MockBukkit.getMock().addPlayer("Player");
        player.setOp(false);

        boolean dispatched = Bukkit.dispatchCommand(player, "easyenchants");

        assertTrue(dispatched);
        assertFalse(hasSettingsMenuOpen(player));
    }

    private boolean hasSettingsMenuOpen(PlayerMock player) {
        InventoryView view = player.getOpenInventory();
        return view != null
            && view.getTopInventory() != null
            && view.getTopInventory().getHolder() instanceof EasyEnchantsSettingsMenuHolder;
    }
}
