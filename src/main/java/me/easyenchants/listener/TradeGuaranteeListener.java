package me.easyenchants.listener;

import me.easyenchants.trade.VillagerRollingService;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public final class TradeGuaranteeListener implements Listener {
    private final Plugin plugin;
    private final VillagerRollingService service;

    public TradeGuaranteeListener(Plugin plugin, VillagerRollingService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAcquire(VillagerAcquireTradeEvent event) {
        if (event.getEntity() instanceof Villager villager) service.scheduleReconcile(villager);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCareer(VillagerCareerChangeEvent event) {
        Villager villager = event.getEntity();
        Villager.Profession original = villager.getProfession();
        boolean hadGuarantees = service.hasGuarantees(villager);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || !villager.isValid()) return;
            // Capture the transition even if another career change happens before the next tick.
            if (hadGuarantees && event.getProfession() != original) service.clear(villager);
            service.reconcile(villager);
        });
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent event) {
        event.getEntities().forEach(entity -> {
            if (entity instanceof Villager villager) service.scheduleReconcile(villager);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotion(EntityPotionEffectEvent event) {
        if (event.getModifiedType() == PotionEffectType.LUCK && event.getEntity() instanceof Villager villager) {
            service.scheduleReconcile(villager);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (!(event.getEntity() instanceof Villager || event.getEntity() instanceof ZombieVillager)) return;
        LivingEntity source = (LivingEntity) event.getEntity();
        event.getTransformedEntities().forEach(target -> {
            if (!(target instanceof Villager || target instanceof ZombieVillager)) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!target.isValid() || event.isCancelled()) return;
                service.transfer(source, (LivingEntity) target);
                if (target instanceof Villager villager) service.reconcile(villager);
            });
        });
    }
}
