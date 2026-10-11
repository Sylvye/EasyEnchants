package me.easyenchants.event;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Fired synchronously before gear is changed or a cursor book is consumed.
 * The target is a working copy; the source book is never modified by this event.
 * Limits apply after level merging, with zero rejecting an enchantment entirely.
 */
public final class EnchantedBookApplyEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ItemStack target;
    private final ItemStack book;
    private final Map<Enchantment, Integer> maximumLevels = new HashMap<>();
    private boolean cancelled;

    public EnchantedBookApplyEvent(Player player, ItemStack target, ItemStack book) {
        this.player = player;
        this.target = target.clone();
        this.book = book.clone();
    }

    public Player getPlayer() { return player; }
    public ItemStack getTarget() { return target; }
    public ItemStack getBook() { return book.clone(); }

    public void setMaximumLevel(Enchantment enchantment, int level) {
        if (level < 0 || level > 255) throw new IllegalArgumentException("Maximum must be 0–255");
        maximumLevels.merge(enchantment, level, Math::min);
    }

    public int limitLevel(Enchantment enchantment, int proposedLevel) {
        return Math.min(proposedLevel, maximumLevels.getOrDefault(enchantment, proposedLevel));
    }

    public boolean hasLimits() { return !maximumLevels.isEmpty(); }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
