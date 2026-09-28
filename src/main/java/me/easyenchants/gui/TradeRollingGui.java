package me.easyenchants.gui;

import me.easyenchants.trade.TradeOption;
import me.easyenchants.trade.TradeCatalog;
import me.easyenchants.trade.VillagerRollingService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TradeRollingGui {
    private static final int INVENTORY_SIZE = 54;
    private static final int[] OPTION_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int BACK_SLOT = 45;
    private static final int PREVIOUS_SLOT = 48;
    private static final int SEARCH_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int PAGE_SLOT = 53;

    private final ChatPromptManager promptManager;
    private final VillagerRollingService rollingService;
    private final List<TradeOption> options;

    public TradeRollingGui(ChatPromptManager promptManager, VillagerRollingService rollingService) {
        this(promptManager, rollingService, null);
    }

    TradeRollingGui(ChatPromptManager promptManager, VillagerRollingService rollingService, List<? extends TradeOption> options) {
        this.promptManager = promptManager;
        this.rollingService = rollingService;
        this.options = options == null ? null : List.copyOf(options);
    }

    public void open(Player player, Villager villager) {
        open(player, villager.getUniqueId(), 0, "");
    }

    public void open(Player player, UUID villagerUuid, int page, String query) {
        if (!(Bukkit.getEntity(villagerUuid) instanceof Villager villager) || !rollingService.canSelect(villager)) {
            player.closeInventory();
            player.sendMessage(Component.text("This villager has no eligible trade slots, or rolling is disabled.", NamedTextColor.RED));
            return;
        }
        List<TradeOption> filtered = filter(options == null ? TradeCatalog.options(villager.getProfession()) : options, query);
        int maxPage = maxPage(filtered.size());
        int safePage = Math.max(0, Math.min(page, maxPage));

        TradeRollingMenuHolder holder = new TradeRollingMenuHolder(villagerUuid, safePage, query, filtered);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, Component.text(villager.getProfession() == Villager.Profession.LIBRARIAN ? "Choose Librarian Book" : "Choose Fletcher Arrows", NamedTextColor.GOLD));
        holder.setInventory(inventory);

        for (int index = 0; index < OPTION_SLOTS.length; index++) {
            int optionIndex = safePage * OPTION_SLOTS.length + index;
            if (optionIndex >= filtered.size()) {
                break;
            }
            inventory.setItem(OPTION_SLOTS[index], optionItem(filtered.get(optionIndex)));
        }

        inventory.setItem(BACK_SLOT, GuiItems.namedItem(Material.BARRIER, Component.text("Close", NamedTextColor.RED), List.of()));
        inventory.setItem(PREVIOUS_SLOT, GuiItems.namedItem(Material.ARROW, Component.text("Previous Page", NamedTextColor.YELLOW), List.of()));
        inventory.setItem(SEARCH_SLOT, GuiItems.namedItem(
            Material.OAK_SIGN,
            Component.text("Search", NamedTextColor.YELLOW),
            List.of(Component.text(query == null || query.isBlank() ? "No search active." : "Search: " + query, NamedTextColor.GRAY))
        ));
        inventory.setItem(NEXT_SLOT, GuiItems.namedItem(Material.ARROW, Component.text("Next Page", NamedTextColor.YELLOW), List.of()));
        inventory.setItem(PAGE_SLOT, GuiItems.namedItem(
            Material.PAPER,
            Component.text("Page " + (safePage + 1) + " / " + (maxPage + 1), NamedTextColor.WHITE),
            List.of(Component.text("Remaining slots: " + rollingService.remaining(villager), NamedTextColor.GRAY),
                Component.text(rollingService.selectionTiming(villager), NamedTextColor.GRAY))
        ));

        player.openInventory(inventory);
    }

    public void handleClick(Player player, TradeRollingMenuHolder holder, int rawSlot) {
        if (rawSlot == BACK_SLOT) {
            player.closeInventory();
            return;
        }
        if (rawSlot == PREVIOUS_SLOT) {
            open(player, holder.villagerUuid(), holder.page() - 1, holder.query());
            return;
        }
        if (rawSlot == NEXT_SLOT) {
            open(player, holder.villagerUuid(), holder.page() + 1, holder.query());
            return;
        }
        if (rawSlot == SEARCH_SLOT) {
            promptSearch(player, holder);
            return;
        }

        int listIndex = indexOf(OPTION_SLOTS, rawSlot);
        if (listIndex < 0) {
            return;
        }
        List<TradeOption> filtered = holder.options();
        int optionIndex = holder.page() * OPTION_SLOTS.length + listIndex;
        if (optionIndex >= filtered.size()) {
            return;
        }
        rollingService.applySelection(player, holder.villagerUuid(), filtered.get(optionIndex));
    }

    List<TradeOption> filteredOptions(String query) {
        return filter(options == null ? TradeCatalog.options(Villager.Profession.LIBRARIAN) : options, query);
    }

    private List<TradeOption> filter(List<TradeOption> options, String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return options;
        }
        return options.stream()
            .filter(option -> option.searchText().contains(normalized))
            .toList();
    }

    ItemStack bookItem(LibrarianBookOption option) { return optionItem(option); }

    private ItemStack optionItem(TradeOption option) {
        ItemStack item = option.item();
        var meta = item.getItemMeta();
        meta.displayName(Component.text(option.displayName(), NamedTextColor.AQUA).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text(option.id(), NamedTextColor.GRAY).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
            Component.text("Click to guarantee this trade. Costs Luck.", NamedTextColor.DARK_GRAY).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private void promptSearch(Player player, TradeRollingMenuHolder holder) {
        promptManager.prompt(player, "Type a trade search query. Type clear to clear search.", text -> {
            if (text.equalsIgnoreCase("cancel")) {
                open(player, holder.villagerUuid(), holder.page(), holder.query());
                return;
            }
            open(player, holder.villagerUuid(), 0, text.equalsIgnoreCase("clear") ? "" : text);
        });
    }

    private int maxPage(int itemCount) {
        return Math.max(0, (itemCount - 1) / OPTION_SLOTS.length);
    }

    private int indexOf(int[] slots, int slot) {
        for (int index = 0; index < slots.length; index++) {
            if (slots[index] == slot) {
                return index;
            }
        }
        return -1;
    }
}
