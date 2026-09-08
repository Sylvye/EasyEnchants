package me.easyenchants.listener;

import me.easyenchants.settings.EasyEnchantsFeatureSettings;
import org.bukkit.Material;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class VillagerAccelerationListener implements Listener {
    static final int VANILLA_BABY_TICKS = 24_000;

    private final Plugin plugin;
    private final EasyEnchantsFeatureSettings settings;
    private final Set<UUID> pendingCures = new HashSet<>();

    public VillagerAccelerationListener(Plugin plugin, EasyEnchantsFeatureSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerBreed(EntityBreedEvent event) {
        if (!settings.villagerAccelerationEnabled() || !(event.getEntity() instanceof Villager child)) {
            return;
        }
        if (!child.getAgeLock() && !child.isAdult()) {
            child.setAge(-scaledTicks(VANILLA_BABY_TICKS, settings.villagerGrowthSpeedMultiplier()));
        }
        int cooldownTicks = secondsToTicks(settings.villagerBreedingCooldownSeconds());
        applyCooldown(event.getMother(), cooldownTicks);
        applyCooldown(event.getFather(), cooldownTicks);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onZombieVillagerCure(PlayerInteractEntityEvent event) {
        if (!settings.villagerAccelerationEnabled()
            || event.getHand() != EquipmentSlot.HAND
            || !(event.getRightClicked() instanceof ZombieVillager zombieVillager)
            || zombieVillager.isConverting()
            || !zombieVillager.hasPotionEffect(PotionEffectType.WEAKNESS)
            || event.getPlayer().getInventory().getItemInMainHand().getType() != Material.GOLDEN_APPLE) {
            return;
        }

        if (!pendingCures.add(zombieVillager.getUniqueId())) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                accelerateCure(zombieVillager);
            } finally {
                pendingCures.remove(zombieVillager.getUniqueId());
            }
        });
    }

    private void accelerateCure(ZombieVillager zombieVillager) {
        if (!settings.villagerAccelerationEnabled() || !zombieVillager.isValid() || !zombieVillager.isConverting()) {
            return;
        }
        zombieVillager.setConversionTime(scaledTicks(
            zombieVillager.getConversionTime(),
            settings.villagerCuringSpeedMultiplier()
        ));
    }

    private void applyCooldown(Entity entity, int cooldownTicks) {
        if (entity instanceof Ageable ageable && !ageable.getAgeLock() && ageable.isAdult()) {
            ageable.setAge(cooldownTicks);
        }
    }

    static int scaledTicks(int ticks, double multiplier) {
        return Math.max(1, (int) Math.ceil(ticks / multiplier));
    }

    static int secondsToTicks(int seconds) {
        return seconds > Integer.MAX_VALUE / 20 ? Integer.MAX_VALUE : seconds * 20;
    }
}
