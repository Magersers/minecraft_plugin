package ru.codex.minecraft.classlevel;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClassSelectionListener implements Listener {
    public static final String CLASS_MENU_TITLE = "§0Выбор класса";
    public static final String COMBAT_MENU_TITLE = "§0Выбор боевого класса";

    private final ClassLevelPlugin plugin;
    private final Set<UUID> switchingToCombat = ConcurrentHashMap.newKeySet();

    public ClassSelectionListener(ClassLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.applyClassEffects(player);

        PlayerProgress progress = plugin.getDataManager().getOrCreate(player.getUniqueId());
        if (progress.getPlayerClass() == null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createClassMenu()), 20L);
        } else if (progress.getCombatClass() == null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createCombatMenu()), 20L);
        }
    }

    @EventHandler
    public void onClassMenuClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!CLASS_MENU_TITLE.equals(title) && !COMBAT_MENU_TITLE.equals(title)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        PlayerProgress progress = plugin.getDataManager().getOrCreate(player.getUniqueId());

        if (CLASS_MENU_TITLE.equals(title)) {
            handlePrimaryClassChoice(player, progress, event.getCurrentItem().getType());
            return;
        }

        handleCombatClassChoice(player, progress, event.getCurrentItem().getType());
    }

    private void handlePrimaryClassChoice(Player player, PlayerProgress progress, Material type) {
        PlayerClass selectedClass = switch (type) {
            case IRON_PICKAXE -> PlayerClass.HAPPY_MINER;
            case ANVIL -> PlayerClass.BLACKSMITH;
            case CRAFTING_TABLE -> PlayerClass.CRAFTER;
            default -> null;
        };

        if (selectedClass == null || progress.getPlayerClass() != null) {
            return;
        }

        progress.setPlayerClass(selectedClass);
        progress.setLevel(1);
        progress.setXp(0);
        plugin.getDataManager().save();

        switchingToCombat.add(player.getUniqueId());
        player.sendMessage("§aВы выбрали класс развития: §e" + selectedClass.displayName() + "§a.");
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            switchingToCombat.remove(player.getUniqueId());
            if (player.isOnline()) {
                PlayerProgress latest = plugin.getDataManager().getOrCreate(player.getUniqueId());
                if (latest.getCombatClass() == null) {
                    player.openInventory(createCombatMenu());
                }
            }
        }, 1L);
    }

    private void handleCombatClassChoice(Player player, PlayerProgress progress, Material type) {
        CombatClass selectedClass = switch (type) {
            case IRON_SWORD -> CombatClass.WARRIOR;
            case BOW -> CombatClass.ARCHER;
            case SHIELD -> CombatClass.TANK;
            default -> null;
        };

        if (selectedClass == null || progress.getCombatClass() != null) {
            return;
        }

        progress.setCombatClass(selectedClass);
        progress.setCombatLevel(1);
        progress.setCombatXp(0);

        plugin.applyClassEffects(player);
        plugin.getDataManager().save();
        player.closeInventory();
        player.sendMessage("§aВы выбрали боевой класс: §e" + selectedClass.displayName() + "§a. Откройте меню развития командой §6/lvl§a.");
    }

    @EventHandler
    public void onClassMenuClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (!CLASS_MENU_TITLE.equals(title) && !COMBAT_MENU_TITLE.equals(title)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (switchingToCombat.contains(player.getUniqueId())) {
            return;
        }

        PlayerProgress progress = plugin.getDataManager().getOrCreate(player.getUniqueId());
        if (progress.getPlayerClass() == null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createClassMenu()), 1L);
            return;
        }

        if (progress.getCombatClass() == null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createCombatMenu()), 1L);
        }
    }

    public static Inventory createClassMenu() {
        Inventory inventory = Bukkit.createInventory(null, 27, CLASS_MENU_TITLE);

        ItemStack miner = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta minerMeta = miner.getItemMeta();
        minerMeta.setDisplayName("§6Счастливый шахтёр");
        minerMeta.setLore(List.of(
                "§7Постоянный эффект: §aВезение I",
                "§7Каждые 3 уровня: +1 к Везению",
                "§7На 5 уровне: §bНочное зрение",
                "§7На 10 уровне: §6Скорость копания I",
                "§7Прокачка: добывайте руду",
                "§eНажмите, чтобы выбрать"
        ));
        minerMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        miner.setItemMeta(minerMeta);

        ItemStack smith = new ItemStack(Material.ANVIL);
        ItemMeta smithMeta = smith.getItemMeta();
        smithMeta.setDisplayName("§6Кузнец");
        smithMeta.setLore(List.of(
                "§7На 1 уровне: ~12% (1), ~2% (3 зач.)",
                "§7С ростом уровня шансы заметно растут",
                "§7Прокачка: крафт брони",
                "§eНажмите, чтобы выбрать"
        ));
        smith.setItemMeta(smithMeta);

        ItemStack crafter = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta crafterMeta = crafter.getItemMeta();
        crafterMeta.setDisplayName("§6Крафтер");
        crafterMeta.setLore(List.of(
                "§7После крафта может вернуться часть ресурсов",
                "§7До 9 ур. максимум возврата: §e50%",
                "§7На 10 ур.: §aдо 100% + §68% бесплатный крафт",
                "§7Анти-дюп: не работает с блоками/слитками/самоцветами",
                "§7Прокачка: крафт (в 10 раз медленнее)",
                "§eНажмите, чтобы выбрать"
        ));
        crafter.setItemMeta(crafterMeta);

        inventory.setItem(10, miner);
        inventory.setItem(13, smith);
        inventory.setItem(16, crafter);
        return inventory;
    }

    public static Inventory createCombatMenu() {
        Inventory inventory = Bukkit.createInventory(null, 27, COMBAT_MENU_TITLE);

        ItemStack warrior = new ItemStack(Material.IRON_SWORD);
        ItemMeta warriorMeta = warrior.getItemMeta();
        warriorMeta.setDisplayName("§cВоин");
        warriorMeta.setLore(List.of(
                "§7Постоянный эффект: §cСила",
                "§7Прокачка: убийства в ближнем бою",
                "§7(враждебные мобы и игроки)",
                "§7Требования отображаются в /lvl",
                "§eНажмите, чтобы выбрать"
        ));
        warriorMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        warrior.setItemMeta(warriorMeta);

        ItemStack archer = new ItemStack(Material.BOW);
        ItemMeta archerMeta = archer.getItemMeta();
        archerMeta.setDisplayName("§aЛучник");
        archerMeta.setLore(List.of(
                "§7Шанс не тратить стрелы любого типа",
                "§7+ бонус к урону из лука/арбалета",
                "§7Прокачка: убийства дальним оружием",
                "§7Все проценты и требования в /lvl",
                "§eНажмите, чтобы выбрать"
        ));
        archer.setItemMeta(archerMeta);

        ItemStack tank = new ItemStack(Material.SHIELD);
        ItemMeta tankMeta = tank.getItemMeta();
        tankMeta.setDisplayName("§9Танк");
        tankMeta.setLore(List.of(
                "§7Получает бонус к максимальному HP",
                "§7Прокачка за полученный урон",
                "§7Прогресс считается в сердцах",
                "§7Требования отображаются в /lvl",
                "§eНажмите, чтобы выбрать"
        ));
        tank.setItemMeta(tankMeta);

        inventory.setItem(10, warrior);
        inventory.setItem(13, archer);
        inventory.setItem(16, tank);
        return inventory;
    }
}
