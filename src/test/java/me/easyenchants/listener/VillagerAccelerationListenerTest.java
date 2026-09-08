package me.easyenchants.listener;

import me.easyenchants.BukkitTestSupport;
import me.easyenchants.EasyEnchantsPlugin;
import me.easyenchants.settings.EasyEnchantsFeatureSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.entity.ZombieVillagerMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerAccelerationListenerTest extends BukkitTestSupport {
    @Test
    void acceleratesChildGrowthAndParentCooldown() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        World world = MockBukkit.getMock().addSimpleWorld("world");
        Villager child = world.spawn(new Location(world, 0, 64, 0), Villager.class);
        Villager mother = world.spawn(new Location(world, 1, 64, 0), Villager.class);
        Villager father = world.spawn(new Location(world, 2, 64, 0), Villager.class);
        child.setBaby();
        VillagerAccelerationListener listener = new VillagerAccelerationListener(plugin, settings(true));

        listener.onVillagerBreed(new EntityBreedEvent(child, mother, father, null, new ItemStack(Material.BREAD), 0));

        assertEquals(-4_800, child.getAge());
        assertEquals(1_200, mother.getAge());
        assertEquals(1_200, father.getAge());
    }

    @Test
    void disabledModuleLeavesBreedingTimersUnchanged() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        World world = MockBukkit.getMock().addSimpleWorld("world");
        Villager child = world.spawn(new Location(world, 0, 64, 0), Villager.class);
        Villager mother = world.spawn(new Location(world, 1, 64, 0), Villager.class);
        Villager father = world.spawn(new Location(world, 2, 64, 0), Villager.class);
        child.setBaby();
        int originalAge = child.getAge();

        new VillagerAccelerationListener(plugin, settings(false)).onVillagerBreed(
            new EntityBreedEvent(child, mother, father, null, new ItemStack(Material.BREAD), 0)
        );

        assertEquals(originalAge, child.getAge());
        assertEquals(0, mother.getAge());
        assertEquals(0, father.getAge());
    }

    @Test
    void acceleratesCureAfterVanillaInitializesConversion() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        ZombieVillager zombie = new ZombieVillagerMock(MockBukkit.getMock(), UUID.randomUUID()) {
            private int conversionTime = -1;

            @Override
            public boolean isConverting() {
                return conversionTime >= 0;
            }

            @Override
            public int getConversionTime() {
                return conversionTime;
            }

            @Override
            public void setConversionTime(int time) {
                conversionTime = time;
            }
        };
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0));
        PlayerMock player = MockBukkit.getMock().addPlayer("Healer");
        player.getInventory().setItemInMainHand(new ItemStack(Material.GOLDEN_APPLE));
        VillagerAccelerationListener listener = new VillagerAccelerationListener(plugin, settings(true));

        assertFalse(zombie.isConverting());
        assertTrue(zombie.hasPotionEffect(PotionEffectType.WEAKNESS));
        assertEquals(Material.GOLDEN_APPLE, player.getInventory().getItemInMainHand().getType());
        PlayerInteractEntityEvent interaction = new PlayerInteractEntityEvent(player, zombie, EquipmentSlot.HAND);
        listener.onZombieVillagerCure(interaction);
        listener.onZombieVillagerCure(interaction);
        zombie.setConversionTime(5_000);
        assertTrue(zombie.isConverting());
        MockBukkit.getMock().getScheduler().performTicks(2);

        assertEquals(1_000, zombie.getConversionTime());
    }

    private EasyEnchantsFeatureSettings settings(boolean enabled) {
        return new EasyEnchantsFeatureSettings() {
            @Override
            public boolean dragAndDropBooksEnabled() {
                return true;
            }

            @Override
            public boolean villagerAccelerationEnabled() {
                return enabled;
            }
        };
    }
}
