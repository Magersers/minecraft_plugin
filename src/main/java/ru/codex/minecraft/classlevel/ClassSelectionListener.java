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

public class ClassSelectionListener implements Listener {
    public static final String CLASS_MENU_TITLE = "§0Выбор класса";

    private final ClassLevelPlugin plugin;

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
        }
    }

    @EventHandler
    public void onClassMenuClick(InventoryClickEvent event) {
        if (!CLASS_MENU_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        PlayerClass selectedClass = switch (event.getCurrentItem().getType()) {
            case IRON_PICKAXE -> PlayerClass.HAPPY_MINER;
            case ANVIL -> PlayerClass.BLACKSMITH;
            default -> null;
        };

        if (selectedClass == null) {
            return;
        }

        PlayerProgress progress = plugin.getDataManager().getOrCreate(player.getUniqueId());
        if (progress.getPlayerClass() != null) {
            player.closeInventory();
            return;
        }

        progress.setPlayerClass(selectedClass);
        progress.setLevel(1);
        progress.setXp(0);

        plugin.applyClassEffects(player);
        plugin.getDataManager().save();
        player.closeInventory();
        player.sendMessage("§aВы выбрали класс: §e" + selectedClass.displayName() + "§a. Откройте меню развития командой §6/lvl§a.");
    }

    @EventHandler
    public void onClassMenuClose(InventoryCloseEvent event) {
        if (!CLASS_MENU_TITLE.equals(event.getView().getTitle())) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        PlayerProgress progress = plugin.getDataManager().getOrCreate(player.getUniqueId());
        if (progress.getPlayerClass() == null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(createClassMenu()), 1L);
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
                "§7На 10 уровне: §bНочное зрение",
                "§7Прокачка: добывайте руду",
                "§eНажмите, чтобы выбрать"
        ));
        minerMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        miner.setItemMeta(minerMeta);

        ItemStack smith = new ItemStack(Material.ANVIL);
        ItemMeta smithMeta = smith.getItemMeta();
        smithMeta.setDisplayName("§6Кузнец");
        smithMeta.setLore(List.of(
                "§75% на 1 ур.: 1 случайное зачарование I",
                "§7Шанс 3/5/10 зачарований растет с уровнем",
                "§7Прокачка: крафт брони",
                "§eНажмите, чтобы выбрать"
        ));
        smith.setItemMeta(smithMeta);

        inventory.setItem(11, miner);
        inventory.setItem(15, smith);
        return inventory;
    }
}
