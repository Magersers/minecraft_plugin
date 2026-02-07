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

            if (progress.getPlayerClass() == PlayerClass.HAPPY_MINER) {
                int luckLevel = 1 + (progress.getLevel() / 3);
                infoLore.add("§7Текущая награда: §aВезение " + toRoman(luckLevel));
                if (progress.getLevel() >= 5) {
                    infoLore.add("§7Доп. награда: §bНочное зрение");
                }
                if (progress.getLevel() >= plugin.getMaxLevel()) {
                    infoLore.add("§7Финальная награда: §6Скорость копания I");
                }
            }

            if (progress.getPlayerClass() == PlayerClass.BLACKSMITH) {
                infoLore.add("§7Шансы зачарований:");
                infoLore.add("§f1 шт: §a" + asPercent(plugin.smithChanceForOneEnchant(progress.getLevel())));
                infoLore.add("§f3 шт: §a" + asPercent(plugin.smithChanceForThreeEnchants(progress.getLevel())));
                infoLore.add("§f5 шт: §a" + asPercent(plugin.smithChanceForFiveEnchants(progress.getLevel())));
                infoLore.add("§f10 шт: §a" + asPercent(plugin.smithChanceForTenEnchants(progress.getLevel())));
            }

            if (progress.getPlayerClass() == PlayerClass.CRAFTER) {
                infoLore.add("§7Шанс возврата ресурсов: §a" + asPercent(plugin.crafterResourceReturnChance(progress.getLevel())));
                infoLore.add("§7Макс. возврат за крафт: §e" + asPercent(plugin.crafterMaxRefundPortion(progress.getLevel())));
                infoLore.add("§7Шанс бесплатного крафта: §6" + asPercent(plugin.crafterFreeCraftChance(progress.getLevel())));
            }
        }

        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inventory.setItem(11, info);

        ItemStack rewards = new ItemStack(Material.BOOK);
        ItemMeta rewardMeta = rewards.getItemMeta();
        rewardMeta.setDisplayName("§6Награды за уровни");
        if (progress.getPlayerClass() == PlayerClass.BLACKSMITH) {
            rewardMeta.setLore(List.of(
                    "§7Базово (ур.1): 12%/2%/0%/0%",
                    "§7На 10 ур.: до 35%/15%/6%/2%",
                    "§7шансы на 1/3/5/10 зачарований",
                    "§7Уровень зачарований выбирается случайно",
                    "§7Прокачка: крафт любой брони"
            ));
        } else if (progress.getPlayerClass() == PlayerClass.CRAFTER) {
            rewardMeta.setLore(List.of(
                    "§7До 9 ур.: возврат части ресурсов",
                    "§7максимум до §e50% §7за крафт",
                    "§7На 10 ур.: §aдо 100% возврата",
                    "§7и §68% шанс бесплатного крафта",
                    "§7Анти-дюп: отключено для блоков,",
                    "§7слитков/самоцветов/самородков"
            ));
        } else {
            rewardMeta.setLore(List.of(
                    "§7Ур. 1: §aВезение I",
                    "§7Ур. 3: §aВезение II",
                    "§7Ур. 5: §bНочное зрение",
                    "§7Ур. 6: §aВезение III",
                    "§7Ур. 9: §aВезение IV",
                    "§7Ур. 10: §6Скорость копания I"
            ));
        }
        rewards.setItemMeta(rewardMeta);
        inventory.setItem(15, rewards);

        return inventory;
    }

    private static String asPercent(double value) {
        return String.format("%.2f%%", value * 100.0);
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
