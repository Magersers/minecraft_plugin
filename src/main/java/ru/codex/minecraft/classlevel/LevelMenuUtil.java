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
            infoLore.add("§cКласс развития еще не выбран.");
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
        }

        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inventory.setItem(10, info);

        ItemStack combatInfo = new ItemStack(Material.IRON_SWORD);
        ItemMeta combatInfoMeta = combatInfo.getItemMeta();
        combatInfoMeta.setDisplayName("§cБоевой прогресс");
        List<String> combatLore = new ArrayList<>();
        if (progress.getCombatClass() == null) {
            combatLore.add("§cБоевой класс еще не выбран.");
            combatLore.add("§7После выбора откроется прогресс.");
        } else {
            combatLore.add("§7Класс: §6" + progress.getCombatClass().displayName());
            combatLore.add("§7Уровень: §a" + progress.getCombatLevel() + "§7/" + plugin.getMaxLevel());
            if (progress.getCombatLevel() >= plugin.getMaxLevel()) {
                combatLore.add("§aМаксимальный боевой уровень!");
            } else {
                int next = plugin.combatRequirementForNextLevel(progress.getCombatClass(), progress.getCombatLevel());
                combatLore.add("§7Прогресс уровня: §b" + progress.getCombatXp() + "§7/§b" + next);
                if (progress.getCombatClass() == CombatClass.TANK) {
                    double heartsNow = progress.getCombatXp() / 2.0;
                    double heartsNeed = Math.max(0, (next - progress.getCombatXp()) / 2.0);
                    combatLore.add("§7Получено урона: §c" + oneDecimal(heartsNow) + " ❤");
                    combatLore.add("§7До уровня: §f" + oneDecimal(heartsNeed) + " ❤ урона");
                } else {
                    combatLore.add("§7До уровня: §f" + Math.max(0, next - progress.getCombatXp()) + " убийств");
                }
            }
        }
        combatInfoMeta.setLore(combatLore);
        combatInfo.setItemMeta(combatInfoMeta);
        inventory.setItem(12, combatInfo);

        ItemStack rewards = new ItemStack(Material.BOOK);
        ItemMeta rewardMeta = rewards.getItemMeta();
        rewardMeta.setDisplayName("§6Награды классов");
        if (progress.getPlayerClass() == PlayerClass.BLACKSMITH) {
            rewardMeta.setLore(List.of(
                    "§7Кузнец: шанс на 1/3/5/10 зачарований",
                    "§7растёт до 35%/15%/6%/2%",
                    "§7Прокачка: крафт брони"
            ));
        } else if (progress.getPlayerClass() == PlayerClass.CRAFTER) {
            rewardMeta.setLore(List.of(
                    "§7Крафтер: возврат части ресурсов",
                    "§7до 50%, на 10 ур. до 100%",
                    "§7и 8% шанс бесплатного крафта"
            ));
        } else if (progress.getPlayerClass() == PlayerClass.HAPPY_MINER) {
            rewardMeta.setLore(List.of(
                    "§7Шахтёр: рост эффекта Везения",
                    "§7на 5 ур. Ночное зрение",
                    "§7на 10 ур. Скорость копания I"
            ));
        } else {
            rewardMeta.setLore(List.of("§7Выберите класс развития."));
        }
        rewards.setItemMeta(rewardMeta);
        inventory.setItem(14, rewards);

        ItemStack combatRewards = new ItemStack(Material.SHIELD);
        ItemMeta combatRewardsMeta = combatRewards.getItemMeta();
        combatRewardsMeta.setDisplayName("§6Награды боевых классов");
        if (progress.getCombatClass() == CombatClass.WARRIOR) {
            int strength = plugin.warriorStrengthLevel(progress.getCombatLevel());
            combatRewardsMeta.setLore(List.of(
                    "§7Воин: постоянная §cСила " + toRoman(strength),
                    "§7Максимум: §cСила V",
                    "§7Прокачка: убийства в ближнем бою"
            ));
        } else if (progress.getCombatClass() == CombatClass.ARCHER) {
            double dmg = plugin.archerDamageBonus(progress.getCombatLevel()) * 100.0;
            double save = plugin.archerArrowSaveChance(progress.getCombatLevel()) * 100.0;
            combatRewardsMeta.setLore(List.of(
                    "§7Бонус урона из лука: §a+" + oneDecimal(dmg) + "%",
                    "§7Шанс не потратить стрелу: §e" + oneDecimal(save) + "%",
                    "§7Прокачка: убийства дальним оружием"
            ));
        } else if (progress.getCombatClass() == CombatClass.TANK) {
            combatRewardsMeta.setLore(List.of(
                    "§7Танк: бонус к HP §a+" + oneDecimal(plugin.tankBonusHealth(progress.getCombatLevel()) / 2.0) + " ❤",
                    "§7Прогресс по полученному урону",
                    "§7Прокачка ускорена примерно в 10 раз"
            ));
        } else {
            combatRewardsMeta.setLore(List.of("§7Выберите боевой класс."));
        }
        combatRewards.setItemMeta(combatRewardsMeta);
        inventory.setItem(16, combatRewards);

        return inventory;
    }

    private static String oneDecimal(double value) {
        return String.format("%.1f", value);
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
