package me.easyenchants.listener;

import me.easyenchants.BukkitTestSupport;
import me.easyenchants.EasyEnchantsPlugin;
import me.easyenchants.enchant.EnchantedBookApplicator;
import me.easyenchants.gui.ChatPromptManager;
import me.easyenchants.gui.EasyEnchantsSettingsGui;
import me.easyenchants.gui.LibrarianBookOption;
import me.easyenchants.gui.LibrarianRollingGui;
import me.easyenchants.gui.LibrarianRollingMenuHolder;
import me.easyenchants.librarian.LibrarianRollingService;
import me.easyenchants.settings.EasyEnchantsFeatureSettings;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyEnchantsListenerTest extends BukkitTestSupport {
    @Test
    void disabledBranchDoesNotInterceptBookClick() {
        PlayerMock player = MockBukkit.getMock().addPlayer("Player");
        Inventory chest = Bukkit.createInventory(null, 27);
        InventoryClickEvent event = playerInventoryClick(player, chest);
        EasyEnchantsListener listener = listener(false, null);

        listener.onInventoryClick(event);

        assertFalse(event.isCancelled());
        assertEquals(0, event.getCurrentItem().getEnchantmentLevel(Enchantment.SHARPNESS));
    }

    @Test
    void playerInventoryClickAppliesBook() {
        PlayerMock player = MockBukkit.getMock().addPlayer("Player");
        Inventory chest = Bukkit.createInventory(null, 27);
        InventoryClickEvent event = playerInventoryClick(player, chest);
        EasyEnchantsListener listener = listener(true, null);

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertEquals(3, event.getCurrentItem().getEnchantmentLevel(Enchantment.SHARPNESS));
        assertEquals(1, event.getCursor().getAmount());
    }

    @Test
    void topInventoryClickIsNotProcessed() {
        PlayerMock player = MockBukkit.getMock().addPlayer("Player");
        Inventory chest = Bukkit.createInventory(null, 27);
        InventoryView view = new TestInventoryView(player, chest, InventoryType.CHEST);
        InventoryClickEvent event = new InventoryClickEvent(
            view,
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.SWAP_WITH_CURSOR
        );
        event.setCurrentItem(new ItemStack(Material.DIAMOND_SWORD));
        event.setCursor(book(1, Enchantment.SHARPNESS, 3));
        EasyEnchantsListener listener = listener(true, null);

        listener.onInventoryClick(event);

        assertFalse(event.isCancelled());
        assertEquals(0, event.getCurrentItem().getEnchantmentLevel(Enchantment.SHARPNESS));
    }

    @Test
    void eligibleLuckRightClickOpensLibrarianRollingGui() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        LibrarianRollingService service = new LibrarianRollingService(plugin);
        EasyEnchantsListener listener = librarianRollingListener(plugin, true, service);
        PlayerMock player = luckyPlayer();
        Villager villager = librarian();
        PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, villager);

        listener.onPlayerInteractEntity(event);

        assertTrue(event.isCancelled());
        assertInstanceOf(LibrarianRollingMenuHolder.class, player.getOpenInventory().getTopInventory().getHolder());
    }

    @Test
    void librarianRollingDoesNotInterceptIneligibleInteractions() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        LibrarianRollingService service = new LibrarianRollingService(plugin);
        EasyEnchantsListener enabledListener = librarianRollingListener(plugin, true, service);
        EasyEnchantsListener disabledListener = librarianRollingListener(plugin, false, service);

        PlayerMock playerWithoutLuck = MockBukkit.getMock().addPlayer("NoLuck");
        PlayerInteractEntityEvent noLuck = new PlayerInteractEntityEvent(playerWithoutLuck, librarian());
        enabledListener.onPlayerInteractEntity(noLuck);
        assertFalse(noLuck.isCancelled());

        PlayerMock luckyPlayer = luckyPlayer();
        Villager farmer = librarian();
        farmer.setProfession(Villager.Profession.FARMER);
        PlayerInteractEntityEvent nonLibrarian = new PlayerInteractEntityEvent(luckyPlayer, farmer);
        enabledListener.onPlayerInteractEntity(nonLibrarian);
        assertFalse(nonLibrarian.isCancelled());

        PlayerMock tradedPlayer = luckyPlayer();
        Villager traded = librarian();
        traded.setRecipes(List.of(recipe(book(1, Enchantment.MENDING, 1), 1)));
        PlayerInteractEntityEvent tradedEvent = new PlayerInteractEntityEvent(tradedPlayer, traded);
        enabledListener.onPlayerInteractEntity(tradedEvent);
        assertFalse(tradedEvent.isCancelled());

        PlayerMock disabledPlayer = luckyPlayer();
        PlayerInteractEntityEvent disabled = new PlayerInteractEntityEvent(disabledPlayer, librarian());
        disabledListener.onPlayerInteractEntity(disabled);
        assertFalse(disabled.isCancelled());
    }

    @Test
    void pendingSelectionIsAppliedByAcquireTradeEvent() {
        EasyEnchantsPlugin plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        LibrarianRollingService service = new LibrarianRollingService(plugin);
        EasyEnchantsListener listener = librarianRollingListener(plugin, true, service);
        PlayerMock player = luckyPlayer();
        Villager villager = librarian();

        assertTrue(service.applySelection(player, villager.getUniqueId(), LibrarianBookOption.of(Enchantment.UNBREAKING, 3)));

        MerchantRecipe acquired = recipe(book(1, Enchantment.MENDING, 1), 0);
        VillagerAcquireTradeEvent event = new VillagerAcquireTradeEvent(villager, acquired);
        listener.onVillagerAcquireTrade(event);

        assertBook(event.getRecipe().getResult(), Enchantment.UNBREAKING, 3);
    }

    private EasyEnchantsListener listener(boolean enabled, EasyEnchantsSettingsGui settingsGui) {
        EasyEnchantsFeatureSettings settings = () -> enabled;
        return new EasyEnchantsListener(settings, settingsGui, new EnchantedBookApplicator());
    }

    private EasyEnchantsListener librarianRollingListener(EasyEnchantsPlugin plugin, boolean rollingEnabled, LibrarianRollingService service) {
        EasyEnchantsFeatureSettings settings = new EasyEnchantsFeatureSettings() {
            @Override
            public boolean dragAndDropBooksEnabled() {
                return true;
            }

            @Override
            public boolean librarianRollingEnabled() {
                return rollingEnabled;
            }
        };
        LibrarianRollingGui gui = new LibrarianRollingGui(new ChatPromptManager(plugin), service);
        return new EasyEnchantsListener(settings, null, new EnchantedBookApplicator(), gui, service);
    }

    private PlayerMock luckyPlayer() {
        PlayerMock player = MockBukkit.getMock().addPlayer("Lucky" + System.nanoTime());
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 200, 0));
        return player;
    }

    private Villager librarian() {
        WorldMock world = MockBukkit.getMock().addSimpleWorld("world-" + System.nanoTime());
        Villager villager = (Villager) world.spawnEntity(new Location(world, 0, 64, 0), EntityType.VILLAGER);
        villager.setProfession(Villager.Profession.LIBRARIAN);
        villager.setVillagerExperience(0);
        return villager;
    }

    private MerchantRecipe recipe(ItemStack result, int uses) {
        MerchantRecipe recipe = new MerchantRecipe(result, uses, 12, true, 1, 0.05F);
        recipe.setIngredients(List.of(new ItemStack(Material.EMERALD), new ItemStack(Material.BOOK)));
        return recipe;
    }

    private void assertBook(ItemStack item, Enchantment enchantment, int level) {
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        assertTrue(meta.hasStoredEnchant(enchantment));
        assertEquals(level, meta.getStoredEnchantLevel(enchantment));
    }

    private InventoryClickEvent playerInventoryClick(PlayerMock player, Inventory chest) {
        InventoryView view = new TestInventoryView(player, chest, InventoryType.CHEST);
        int rawBottomSlot = chest.getSize();
        InventoryClickEvent event = new InventoryClickEvent(
            view,
            InventoryType.SlotType.CONTAINER,
            rawBottomSlot,
            ClickType.LEFT,
            InventoryAction.SWAP_WITH_CURSOR
        );
        event.setCurrentItem(new ItemStack(Material.DIAMOND_SWORD));
        event.setCursor(book(2, Enchantment.SHARPNESS, 3));
        return event;
    }

    private ItemStack book(int amount, Enchantment enchantment, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, amount);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);
        return book;
    }

    private static final class TestInventoryView implements InventoryView {
        private final PlayerMock player;
        private final Inventory topInventory;
        private final InventoryType type;
        private ItemStack cursor;

        private TestInventoryView(PlayerMock player, Inventory topInventory, InventoryType type) {
            this.player = player;
            this.topInventory = topInventory;
            this.type = type;
        }

        @Override
        public Inventory getTopInventory() {
            return topInventory;
        }

        @Override
        public Inventory getBottomInventory() {
            return player.getInventory();
        }

        @Override
        public HumanEntity getPlayer() {
            return player;
        }

        @Override
        public InventoryType getType() {
            return type;
        }

        @Override
        public void setItem(int slot, ItemStack item) {
            Inventory inventory = getInventory(slot);
            if (inventory != null) {
                inventory.setItem(convertSlot(slot), item);
            }
        }

        @Override
        public ItemStack getItem(int slot) {
            Inventory inventory = getInventory(slot);
            return inventory == null ? null : inventory.getItem(convertSlot(slot));
        }

        @Override
        public void setCursor(ItemStack item) {
            cursor = item;
        }

        @Override
        public ItemStack getCursor() {
            return cursor;
        }

        @Override
        public Inventory getInventory(int rawSlot) {
            if (rawSlot < 0 || rawSlot >= countSlots()) {
                return null;
            }
            return rawSlot < topInventory.getSize() ? topInventory : getBottomInventory();
        }

        @Override
        public int convertSlot(int rawSlot) {
            return rawSlot < topInventory.getSize() ? rawSlot : rawSlot - topInventory.getSize();
        }

        @Override
        public InventoryType.SlotType getSlotType(int slot) {
            return InventoryType.SlotType.CONTAINER;
        }

        @Override
        public void open() {
        }

        @Override
        public void close() {
        }

        @Override
        public int countSlots() {
            return topInventory.getSize() + getBottomInventory().getSize();
        }

        @Override
        public boolean setProperty(Property prop, int value) {
            return false;
        }

        @Override
        public String getTitle() {
            return "Test";
        }

        @Override
        public String getOriginalTitle() {
            return "Test";
        }

        @Override
        public void setTitle(String title) {
        }

        @Override
        public MenuType getMenuType() {
            return null;
        }
    }
}
