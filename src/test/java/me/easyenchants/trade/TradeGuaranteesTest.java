package me.easyenchants.trade;

import me.easyenchants.BukkitTestSupport;
import me.easyenchants.EasyEnchantsPlugin;
import me.easyenchants.gui.LibrarianBookOption;
import me.easyenchants.listener.TradeGuaranteeListener;
import me.easyenchants.settings.EasyEnchantsFeatureSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TradeGuaranteesTest extends BukkitTestSupport {
    private EasyEnchantsPlugin plugin;
    private VillagerRollingService service;
    private WorldMock world;
    private PlayerMock player;

    @BeforeEach
    void setupTrades() {
        plugin = MockBukkit.load(EasyEnchantsPlugin.class);
        service = new VillagerRollingService(plugin, bound -> 0);
        world = tradeWorld("trades");
        player = MockBukkit.getMock().addPlayer();
    }

    @Test
    void fourNoviceSelectionsFulfillOnePerTierWithoutOverwriting() {
        Villager villager = villager(Villager.Profession.LIBRARIAN, 1);
        villager.setRecipes(noBooks(1));
        List<Enchantment> books = List.of(Enchantment.MENDING, Enchantment.UNBREAKING, Enchantment.SHARPNESS, Enchantment.EFFICIENCY);
        for (Enchantment book : books) select(villager, LibrarianBookOption.of(book, 1));
        assertEquals(0, service.remaining(villager));
        assertEquals(1, bookCount(villager));
        assertTrue(villager.hasPotionEffect(PotionEffectType.LUCK));
        lucky();
        assertFalse(service.applySelection(player, villager.getUniqueId(), LibrarianBookOption.of(Enchantment.SMITE, 1)));
        assertTrue(player.hasPotionEffect(PotionEffectType.LUCK));
        for (int tier = 2; tier <= 4; tier++) {
            var recipes = new ArrayList<>(villager.getRecipes());
            recipes.addAll(noBooks(tier).subList((tier - 1) * 2, tier * 2));
            villager.setVillagerLevel(tier);
            villager.setRecipes(recipes);
            service.reconcile(villager);
            assertEquals(tier, bookCount(villager));
            assertEquals(tier < 4, villager.hasPotionEffect(PotionEffectType.LUCK));
            for (int i = 0; i < tier; i++) assertBook(villager.getRecipe(i * 2 + 1), books.get(i));
        }
    }

    @Test
    void existingBooksAreChosenBeforeMissedEarlierTier() {
        Villager villager = villager(Villager.Profession.LIBRARIAN, 3);
        var recipes = noBooks(3);
        recipes.set(5, book(Enchantment.SMITE, 10));
        villager.setRecipes(recipes);
        select(villager, LibrarianBookOption.of(Enchantment.MENDING, 1));
        assertBook(villager.getRecipe(5), Enchantment.MENDING);
        assertEquals(Material.BOOKSHELF, villager.getRecipe(1).getResult().getType());
        assertFalse(villager.hasPotionEffect(PotionEffectType.LUCK));
        select(villager, LibrarianBookOption.of(Enchantment.UNBREAKING, 1));
        assertBook(villager.getRecipe(1), Enchantment.UNBREAKING);
    }

    @Test
    void masterCanFillFourMissedSlotsAndDuplicatesUseSeparateSlots() {
        Villager villager = villager(Villager.Profession.LIBRARIAN, 5);
        var recipes = noBooks(4);
        recipes.add(sale(Material.NAME_TAG, 30));
        villager.setRecipes(recipes);
        for (int i = 0; i < 4; i++) select(villager, LibrarianBookOption.of(Enchantment.MENDING, 1));
        assertEquals(4, bookCount(villager));
        assertEquals(9, villager.getRecipeCount());
        assertEquals(Material.NAME_TAG, villager.getRecipe(8).getResult().getType());
        assertFalse(villager.hasPotionEffect(PotionEffectType.LUCK));
    }

    @Test
    void fletcherWaitsUntilMasterAndUsesVanillaRecipeEvenWhenArrowsWereOmitted() {
        for (int tier = 1; tier <= 4; tier++) {
            Villager villager = villager(Villager.Profession.FLETCHER, tier);
            villager.setRecipes(List.of(buy(Material.STICK), sale(Material.ARROW, 1)));
            select(villager, new ArrowOption(PotionType.STRONG_HARMING));
            assertTrue(villager.hasPotionEffect(PotionEffectType.LUCK));
            assertEquals(Material.ARROW, villager.getRecipe(1).getResult().getType());
            var recipes = new ArrayList<>(villager.getRecipes());
            recipes.add(buy(Material.TRIPWIRE_HOOK));
            var crossbow = sale(Material.CROSSBOW, 15);
            var enchanted = crossbow.getResult();
            enchanted.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
            crossbow = new MerchantRecipe(enchanted, 3);
            crossbow.setIngredients(List.of(new ItemStack(Material.EMERALD, 10)));
            recipes.add(crossbow);
            villager.setVillagerLevel(5);
            villager.setRecipes(recipes);
            service.reconcile(villager);
            MerchantRecipe arrows = villager.getRecipe(3);
            assertEquals(Material.TIPPED_ARROW, arrows.getResult().getType());
            assertEquals(PotionType.STRONG_HARMING, ((PotionMeta) arrows.getResult().getItemMeta()).getBasePotionType());
            assertEquals(5, arrows.getResult().getAmount());
            assertEquals(List.of(new ItemStack(Material.EMERALD, 2), new ItemStack(Material.ARROW, 5)), arrows.getIngredients());
            assertEquals(12, arrows.getMaxUses());
            assertEquals(30, arrows.getVillagerExperience());
            assertEquals(0.05F, arrows.getPriceMultiplier());
            assertFalse(villager.hasPotionEffect(PotionEffectType.LUCK));
            assertFalse(service.canSelect(villager));
        }
    }

    @Test
    void masterFletcherAndUntradeableOptionsRejectWithoutConsumingLuck() {
        Villager master = villager(Villager.Profession.FLETCHER, 5);
        lucky();
        assertFalse(service.applySelection(player, master.getUniqueId(), new ArrowOption(PotionType.HARMING)));
        Villager novice = villager(Villager.Profession.FLETCHER, 1);
        assertFalse(service.applySelection(player, novice.getUniqueId(), new ArrowOption(PotionType.LUCK)));
        assertFalse(service.applySelection(player, novice.getUniqueId(), new ArrowOption(PotionType.WATER)));
        Villager librarian = villager(Villager.Profession.LIBRARIAN, 1);
        assertFalse(service.applySelection(player, librarian.getUniqueId(), LibrarianBookOption.of(Enchantment.SOUL_SPEED, 1)));
        assertFalse(service.applySelection(player, librarian.getUniqueId(), new LibrarianBookOption(Enchantment.SHARPNESS, 6, "invalid")));
        assertTrue(player.hasPotionEffect(PotionEffectType.LUCK));
    }

    @Test
    void stateSurvivesNewServiceAndPdcRoundTrip() throws Exception {
        Villager original = villager(Villager.Profession.LIBRARIAN, 1);
        original.setRecipes(noBooks(1));
        select(original, LibrarianBookOption.of(Enchantment.MENDING, 1));
        select(original, LibrarianBookOption.of(Enchantment.UNBREAKING, 1));
        Villager loaded = villager(Villager.Profession.LIBRARIAN, 2);
        original.getPersistentDataContainer().copyTo(loaded.getPersistentDataContainer(), true);
        var recipes = new ArrayList<>(original.getRecipes());
        recipes.addAll(noBooks(2).subList(2, 4));
        loaded.setRecipes(recipes);
        var restarted = new VillagerRollingService(plugin);
        restarted.reconcile(loaded);
        assertBook(loaded.getRecipe(1), Enchantment.MENDING);
        assertBook(loaded.getRecipe(3), Enchantment.UNBREAKING);
        assertEquals(2, restarted.remaining(loaded));
        assertFalse(loaded.hasPotionEffect(PotionEffectType.LUCK));
    }

    @Test
    void clearsOnProfessionLossButPreservesUnrelatedLuck() {
        Villager villager = villager(Villager.Profession.LIBRARIAN, 1);
        villager.setRecipes(noBooks(1));
        select(villager, LibrarianBookOption.of(Enchantment.MENDING, 1));
        select(villager, LibrarianBookOption.of(Enchantment.UNBREAKING, 1));
        villager.setProfession(Villager.Profession.NONE);
        service.reconcile(villager);
        assertFalse(villager.hasPotionEffect(PotionEffectType.LUCK));
        villager.setProfession(Villager.Profession.LIBRARIAN);
        assertEquals(4, service.remaining(villager));
        PotionEffect external = new PotionEffect(PotionEffectType.LUCK, 1200, 2);
        villager.addPotionEffect(external);
        select(villager, LibrarianBookOption.of(Enchantment.MENDING, 1));
        assertEquals(external, villager.getPotionEffect(PotionEffectType.LUCK));
    }

    @Test
    void zombieRoundTripPreservesBothCompletedAndPendingGuarantees() {
        Villager original = villager(Villager.Profession.LIBRARIAN, 1);
        original.setRecipes(noBooks(1));
        select(original, LibrarianBookOption.of(Enchantment.MENDING, 1));
        select(original, LibrarianBookOption.of(Enchantment.UNBREAKING, 1));
        ZombieVillager zombie = world.spawn(new Location(world, 5, 64, 5), ZombieVillager.class);
        service.transfer(original, zombie);
        Villager cured = villager(Villager.Profession.LIBRARIAN, 2);
        cured.setRecipes(noBooks(2));
        service.transfer(zombie, cured);
        service.reconcile(cured);
        assertEquals(2, service.remaining(cured));
        assertBook(cured.getRecipe(1), Enchantment.MENDING);
        assertBook(cured.getRecipe(3), Enchantment.UNBREAKING);
        assertFalse(cured.hasPotionEffect(PotionEffectType.LUCK));
    }

    @Test
    void legacyPendingBookMigratesAndFulfillsImmediately() {
        Villager villager = villager(Villager.Profession.LIBRARIAN, 1);
        villager.setRecipes(noBooks(1));
        var data = villager.getPersistentDataContainer();
        data.set(new NamespacedKey(plugin, "pending_librarian_enchantment"), PersistentDataType.STRING, "minecraft:mending");
        data.set(new NamespacedKey(plugin, "pending_librarian_level"), PersistentDataType.INTEGER, 1);
        service.reconcile(villager);
        assertBook(villager.getRecipe(1), Enchantment.MENDING);
        assertEquals(3, service.remaining(villager));
        assertFalse(data.has(new NamespacedKey(plugin, "pending_librarian_enchantment")));
    }

    @Test
    void acquisitionBatchCommitsBeforeFulfillmentAndCanceledTradesDoNotFulfill() {
        Villager villager = villager(Villager.Profession.LIBRARIAN, 1);
        villager.setRecipes(noBooks(1));
        select(villager, LibrarianBookOption.of(Enchantment.MENDING, 1));
        select(villager, LibrarianBookOption.of(Enchantment.UNBREAKING, 1));
        villager.setVillagerLevel(2);
        VillagerAcquireTradeEvent canceled = new VillagerAcquireTradeEvent(villager, sale(Material.LANTERN, 5));
        canceled.setCancelled(true);
        MockBukkit.getMock().getPluginManager().callEvent(canceled);
        MockBukkit.getMock().getScheduler().performTicks(2);
        assertTrue(villager.hasPotionEffect(PotionEffectType.LUCK));
        var listener = new TradeGuaranteeListener(plugin, service);
        var recipes = new ArrayList<>(villager.getRecipes());
        for (var recipe : noBooks(2).subList(2, 4)) {
            listener.onAcquire(new VillagerAcquireTradeEvent(villager, recipe));
            recipes.add(recipe);
        }
        assertEquals(1, bookCount(villager));
        villager.setRecipes(recipes);
        MockBukkit.getMock().getScheduler().performTicks(2);
        assertEquals(2, bookCount(villager));
        assertFalse(villager.hasPotionEffect(PotionEffectType.LUCK));
        assertBook(villager.getRecipe(3), Enchantment.UNBREAKING);
    }

    @Test
    void disabledFeatureRejectsNewChoicesButHonorsPaidReservations() {
        Villager villager = villager(Villager.Profession.LIBRARIAN, 1);
        villager.setRecipes(noBooks(1));
        select(villager, LibrarianBookOption.of(Enchantment.MENDING, 1));
        select(villager, LibrarianBookOption.of(Enchantment.UNBREAKING, 1));
        var disabled = new VillagerRollingService(plugin, new EasyEnchantsFeatureSettings() {
            public boolean dragAndDropBooksEnabled() { return true; }
            public boolean librarianRollingEnabled() { return false; }
        });
        lucky();
        assertFalse(disabled.applySelection(player, villager.getUniqueId(), LibrarianBookOption.of(Enchantment.SHARPNESS, 1)));
        assertTrue(player.hasPotionEffect(PotionEffectType.LUCK));
        villager.setVillagerLevel(2);
        var recipes = new ArrayList<>(villager.getRecipes());
        recipes.addAll(noBooks(2).subList(2, 4));
        villager.setRecipes(recipes);
        disabled.reconcile(villager);
        assertBook(villager.getRecipe(3), Enchantment.UNBREAKING);
        assertFalse(villager.hasPotionEffect(PotionEffectType.LUCK));
    }

    @Test
    void staleSelectionAfterLastSlotDoesNotChargeSecondPlayer() {
        Villager villager = villager(Villager.Profession.FLETCHER, 1);
        PlayerMock second = MockBukkit.getMock().addPlayer();
        second.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 200, 0));
        select(villager, new ArrowOption(PotionType.STRONG_HARMING));
        assertFalse(service.applySelection(second, villager.getUniqueId(), new ArrowOption(PotionType.HEALING)));
        assertTrue(second.hasPotionEffect(PotionEffectType.LUCK));
    }

    @Test
    void rebalanceAndMissingRegistryTagsFailClosed() {
        Villager villager = villager(Villager.Profession.LIBRARIAN, 1);
        lucky();
        ((TradeWorld) world).flags.add(org.bukkit.FeatureFlag.TRADE_REBALANCE);
        assertFalse(service.applySelection(player, villager.getUniqueId(), LibrarianBookOption.of(Enchantment.MENDING, 1)));
        ((TradeWorld) world).flags.clear();
        tradeTagsUnavailable = true;
        assertTrue(TradeCatalog.options(Villager.Profession.LIBRARIAN).isEmpty());
        assertFalse(service.applySelection(player, villager.getUniqueId(), LibrarianBookOption.of(Enchantment.MENDING, 1)));
        assertTrue(player.hasPotionEffect(PotionEffectType.LUCK));
    }

    @Test
    void removedPendingLuckReturnsAndExternalReplacementIsNotRemoved() {
        Villager villager = villager(Villager.Profession.FLETCHER, 1);
        select(villager, new ArrowOption(PotionType.HARMING));
        villager.removePotionEffect(PotionEffectType.LUCK);
        service.reconcile(villager);
        assertTrue(villager.getPotionEffect(PotionEffectType.LUCK).isInfinite());
        PotionEffect external = new PotionEffect(PotionEffectType.LUCK, 900, 2);
        villager.addPotionEffect(external);
        villager.setProfession(Villager.Profession.NONE);
        service.reconcile(villager);
        assertEquals(external, villager.getPotionEffect(PotionEffectType.LUCK));
    }

    @Test
    void replacingAnExhaustedBookPreservesUsesAndDiscounts() {
        Villager villager = villager(Villager.Profession.LIBRARIAN, 1);
        var original = book(Enchantment.SMITE, 1);
        original.setUses(12);
        original.setDemand(3);
        original.setSpecialPrice(-2);
        villager.setRecipes(List.of(buy(Material.PAPER), original));
        select(villager, LibrarianBookOption.of(Enchantment.MENDING, 1));
        var replacement = villager.getRecipe(1);
        assertEquals(12, replacement.getUses());
        assertEquals(3, replacement.getDemand());
        assertEquals(-2, replacement.getSpecialPrice());
        service.reconcile(villager);
        assertEquals(replacement.getIngredients(), villager.getRecipe(1).getIngredients());
        assertEquals(12, villager.getRecipe(1).getUses());
    }

    @Test
    void deletedEntityOrChangedProfessionRejectsStaleChoice() {
        Villager villager = villager(Villager.Profession.LIBRARIAN, 1);
        lucky();
        villager.setProfession(Villager.Profession.FLETCHER);
        assertFalse(service.applySelection(player, villager.getUniqueId(), LibrarianBookOption.of(Enchantment.MENDING, 1)));
        villager.remove();
        assertFalse(service.applySelection(player, villager.getUniqueId(), new ArrowOption(PotionType.HARMING)));
        assertTrue(player.hasPotionEffect(PotionEffectType.LUCK));
    }

    @Test
    void entityLoadRestoresPendingEffectAndCareerLossClearsState() {
        Villager villager = villager(Villager.Profession.LIBRARIAN, 1);
        villager.setRecipes(noBooks(1));
        select(villager, LibrarianBookOption.of(Enchantment.MENDING, 1));
        select(villager, LibrarianBookOption.of(Enchantment.UNBREAKING, 1));
        var listener = new TradeGuaranteeListener(plugin, new VillagerRollingService(plugin));
        villager.removePotionEffect(PotionEffectType.LUCK);
        listener.onLoad(new org.bukkit.event.world.EntitiesLoadEvent(world.getChunkAt(0, 0), List.of(villager)));
        MockBukkit.getMock().getScheduler().performTicks(2);
        assertTrue(villager.hasPotionEffect(PotionEffectType.LUCK));
        var loss = new org.bukkit.event.entity.VillagerCareerChangeEvent(villager, Villager.Profession.NONE,
            org.bukkit.event.entity.VillagerCareerChangeEvent.ChangeReason.LOSING_JOB);
        listener.onCareer(loss);
        villager.setProfession(Villager.Profession.NONE);
        // Even reacquiring the original job before the next tick must not restore cleared guarantees.
        villager.setProfession(Villager.Profession.LIBRARIAN);
        MockBukkit.getMock().getScheduler().performTicks(2);
        assertEquals(4, service.remaining(villager));
        assertFalse(villager.hasPotionEffect(PotionEffectType.LUCK));
    }

    @Test
    void futureStateVersionIsNotOverwrittenOrCharged() {
        Villager villager = villager(Villager.Profession.FLETCHER, 1);
        var state = villager.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();
        state.set(new NamespacedKey(plugin, "version"), PersistentDataType.INTEGER, 2);
        villager.getPersistentDataContainer().set(new NamespacedKey(plugin, "trade_guarantees"), PersistentDataType.TAG_CONTAINER, state);
        lucky();
        assertFalse(service.applySelection(player, villager.getUniqueId(), new ArrowOption(PotionType.HARMING)));
        assertTrue(player.hasPotionEffect(PotionEffectType.LUCK));
        assertEquals(2, villager.getPersistentDataContainer().get(new NamespacedKey(plugin, "trade_guarantees"), PersistentDataType.TAG_CONTAINER)
            .get(new NamespacedKey(plugin, "version"), PersistentDataType.INTEGER));
    }

    @Test
    void newlyEmployedVillagerKeepsSelectionsMadeAfterEmployment() {
        Villager villager = villager(Villager.Profession.NONE, 1);
        var employment = new org.bukkit.event.entity.VillagerCareerChangeEvent(villager, Villager.Profession.FLETCHER,
            org.bukkit.event.entity.VillagerCareerChangeEvent.ChangeReason.EMPLOYED);
        new TradeGuaranteeListener(plugin, service).onCareer(employment);
        villager.setProfession(Villager.Profession.FLETCHER);
        select(villager, new ArrowOption(PotionType.HARMING));
        MockBukkit.getMock().getScheduler().performTicks(2);
        assertEquals(0, service.remaining(villager));
        assertTrue(villager.hasPotionEffect(PotionEffectType.LUCK));
    }

    private Villager villager(Villager.Profession profession, int tier) {
        Villager villager = world.spawn(new Location(world, 0, 64, 0), Villager.class);
        villager.setProfession(profession);
        villager.setVillagerLevel(tier);
        return villager;
    }
    private void lucky() { player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 200, 0)); }
    private void select(Villager villager, TradeOption option) {
        lucky();
        assertTrue(service.applySelection(player, villager.getUniqueId(), option));
        assertFalse(player.hasPotionEffect(PotionEffectType.LUCK));
    }
    private List<MerchantRecipe> noBooks(int tier) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        Material[] inputs = {Material.PAPER, Material.BOOK, Material.INK_SAC, Material.WRITABLE_BOOK};
        Material[] sales = {Material.BOOKSHELF, Material.LANTERN, Material.GLASS, Material.CLOCK};
        int[] xp = {1, 5, 10, 15};
        for (int i = 0; i < tier; i++) { recipes.add(buy(inputs[i])); recipes.add(sale(sales[i], xp[i])); }
        return recipes;
    }
    private MerchantRecipe buy(Material input) {
        MerchantRecipe recipe = new MerchantRecipe(new ItemStack(Material.EMERALD), 16);
        recipe.setIngredients(List.of(new ItemStack(input, 4)));
        return recipe;
    }
    private MerchantRecipe sale(Material result, int xp) {
        MerchantRecipe recipe = new MerchantRecipe(new ItemStack(result), 0, 16, true, xp, 0.05F);
        recipe.setIngredients(List.of(new ItemStack(Material.EMERALD, 4)));
        return recipe;
    }
    private MerchantRecipe book(Enchantment enchantment, int xp) {
        MerchantRecipe recipe = new MerchantRecipe(LibrarianBookOption.of(enchantment, 1).item(), 0, 12, true, xp, 0.2F);
        recipe.setIngredients(List.of(new ItemStack(Material.EMERALD, 5), new ItemStack(Material.BOOK)));
        return recipe;
    }
    private long bookCount(Villager villager) { return villager.getRecipes().stream().filter(r -> r.getResult().getType() == Material.ENCHANTED_BOOK).count(); }
    private void assertBook(MerchantRecipe recipe, Enchantment enchantment) {
        assertEquals(Material.ENCHANTED_BOOK, recipe.getResult().getType());
        assertEquals(1, ((EnchantmentStorageMeta) recipe.getResult().getItemMeta()).getStoredEnchants().entrySet().stream()
            .filter(entry -> entry.getKey().getKey().equals(enchantment.getKey())).findFirst().orElseThrow().getValue());
    }
}
