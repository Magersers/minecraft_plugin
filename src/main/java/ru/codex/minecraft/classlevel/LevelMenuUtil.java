package ru.codex.minecraft.classlevel;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class LevelMenuUtil {
    public static final String LEVEL_MENU_TITLE = "§0Прокачка класса";

    private LevelMenuUtil() {
    }

    public static Inventory createLevelMenu(ClassLevelPlugin plugin, Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, LEVEL_MENU_TITLE);
        PlayerProgress progress = plugin.getDataManager().getOrCreate(player.getUniqueId());

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§eВаш прогресс");
        List<String> infoLore = new ArrayList<>();

        if (progress.getPlayerClass() == null) {
            infoLore.add("§cКласс еще не выбран.");
            infoLore.add("§7Выберите класс во всплывающем меню.");
        } else {
            infoLore.add("§7Класс: §6" + progress.getPlayerClass().displayName());
            infoLore.add("§7Уровень: §a" + progress.getLevel() + "§7/" + plugin.getMaxLevel());

            if (progress.getLevel() >= plugin.getMaxLevel()) {
                infoLore.add("§aМаксимальный уровень достигнут!");
            } else {
                int next = plugin.xpForNextLevel(progress.getLevel());
                infoLore.add("§7Опыт: §b" + progress.getXp() + "§7/§b" + next);
                infoLore.add("§7До следующего уровня: §f" + Math.max(0, next - progress.getXp()));
            }

            int luckLevel = 1 + (progress.getLevel() / 3);
            infoLore.add("§7Текущая награда: §aУдача " + toRoman(luckLevel));
        }

        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inventory.setItem(11, info);

        ItemStack rewards = new ItemStack(Material.BOOK);
        ItemMeta rewardMeta = rewards.getItemMeta();
        rewardMeta.setDisplayName("§6Награды за уровни");
        rewardMeta.setLore(List.of(
                "§7Ур. 1: §aУдача I",
                "§7Ур. 3: §aУдача II",
                "§7Ур. 6: §aУдача III",
                "§7Ур. 9: §aУдача IV",
                "§7Ур. 10: §eМаксимум развития"
        ));
        rewards.setItemMeta(rewardMeta);
        inventory.setItem(15, rewards);

        return inventory;
    }

    private static String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(number);
        };
    }
}
