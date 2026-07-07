package me.easyenchants.gui;

import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import me.easyenchants.librarian.LibrarianRollingService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class LibrarianRollingGui {
    private static final int INVENTORY_SIZE = 54;
    private static final int[] BOOK_SLOTS = {
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
    private final LibrarianRollingService rollingService;
    private final List<LibrarianBookOption> options;

    public LibrarianRollingGui(ChatPromptManager promptManager, LibrarianRollingService rollingService) {
        this(promptManager, rollingService, loadOptions());
    }

    LibrarianRollingGui(ChatPromptManager promptManager, LibrarianRollingService rollingService, List<LibrarianBookOption> options) {
        this.promptManager = promptManager;
        this.rollingService = rollingService;
        this.options = List.copyOf(options);
    }

    public void open(Player player, Villager villager) {
        open(player, villager.getUniqueId(), 0, "");
    }

    public void open(Player player, UUID villagerUuid, int page, String query) {
        List<LibrarianBookOption> filtered = filteredOptions(query);
        int maxPage = maxPage(filtered.size());
        int safePage = Math.max(0, Math.min(page, maxPage));

        LibrarianRollingMenuHolder holder = new LibrarianRollingMenuHolder(villagerUuid, safePage, query);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, Component.text("Choose Librarian Book", NamedTextColor.GOLD));
        holder.setInventory(inventory);

        for (int index = 0; index < BOOK_SLOTS.length; index++) {
            int optionIndex = safePage * BOOK_SLOTS.length + index;
            if (optionIndex >= filtered.size()) {
                break;
            }
            inventory.setItem(BOOK_SLOTS[index], bookItem(filtered.get(optionIndex)));
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
            List.of()
        ));

        player.openInventory(inventory);
    }

    public void handleClick(Player player, LibrarianRollingMenuHolder holder, int rawSlot) {
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

        int listIndex = indexOf(BOOK_SLOTS, rawSlot);
        if (listIndex < 0) {
            return;
        }
        List<LibrarianBookOption> filtered = filteredOptions(holder.query());
        int optionIndex = holder.page() * BOOK_SLOTS.length + listIndex;
        if (optionIndex >= filtered.size()) {
            return;
        }
        rollingService.applySelection(player, holder.villagerUuid(), filtered.get(optionIndex));
    }

    List<LibrarianBookOption> filteredOptions(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return options;
        }
        return options.stream()
            .filter(option -> option.searchText().contains(normalized))
            .toList();
    }

    ItemStack bookItem(LibrarianBookOption option) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        meta.addStoredEnchant(option.enchantment(), option.level(), true);
        meta.displayName(Component.text(option.displayName(), NamedTextColor.AQUA).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text(option.enchantment().getKey().toString(), NamedTextColor.GRAY).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
            Component.text("Click to guarantee this book.", NamedTextColor.DARK_GRAY).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private void promptSearch(Player player, LibrarianRollingMenuHolder holder) {
        promptManager.prompt(player, "Type an enchanted book search query. Type clear to clear search.", text -> {
            if (text.equalsIgnoreCase("cancel")) {
                open(player, holder.villagerUuid(), holder.page(), holder.query());
                return;
            }
            open(player, holder.villagerUuid(), 0, text.equalsIgnoreCase("clear") ? "" : text);
        });
    }

    private static List<LibrarianBookOption> loadOptions() {
        Collection<Enchantment> enchantments;
        try {
            enchantments = Registry.ENCHANTMENT.getTagValues(EnchantmentTagKeys.TRADEABLE);
        } catch (RuntimeException exception) {
            enchantments = Registry.ENCHANTMENT.stream().toList();
        }
        List<LibrarianBookOption> loaded = new ArrayList<>();
        for (Enchantment enchantment : enchantments) {
            for (int level = enchantment.getStartLevel(); level <= enchantment.getMaxLevel(); level++) {
                loaded.add(LibrarianBookOption.of(enchantment, level));
            }
        }
        loaded.sort(Comparator
            .comparing(LibrarianBookOption::displayName)
            .thenComparing(option -> option.enchantment().getKey().toString())
            .thenComparingInt(LibrarianBookOption::level));
        return loaded;
    }

    private int maxPage(int itemCount) {
        return Math.max(0, (itemCount - 1) / BOOK_SLOTS.length);
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
