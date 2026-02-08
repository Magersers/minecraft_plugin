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
            double one = plugin.smithChanceForOneEnchant(progress.getLevel()) * 100.0;
            double three = plugin.smithChanceForThreeEnchants(progress.getLevel()) * 100.0;
            double five = plugin.smithChanceForFiveEnchants(progress.getLevel()) * 100.0;
            double ten = plugin.smithChanceForTenEnchants(progress.getLevel()) * 100.0;
            rewardMeta.setLore(List.of(
                    "§7Кузнец: случайные зачары на броню,",
                    "§7оружие §fи §7инструменты.",
                    "§7Текущие шансы: §e1 §7= §a" + oneDecimal(one) + "%",
                    "§e3 §7= §a" + oneDecimal(three) + "% §8| §e5 §7= §a" + oneDecimal(five) + "% §8| §e10 §7= §a" + oneDecimal(ten) + "%",
                    "§7Прокачка: крафт брони"
            ));
        } else if (progress.getPlayerClass() == PlayerClass.CRAFTER) {
            double refundChance = plugin.crafterResourceReturnChance(progress.getLevel()) * 100.0;
            double refundMax = plugin.crafterMaxRefundPortion(progress.getLevel()) * 100.0;
            double free = plugin.crafterFreeCraftChance(progress.getLevel()) * 100.0;
            rewardMeta.setLore(List.of(
                    "§7Крафтер: возврат части ресурсов",
                    "§7Шанс возврата: §b" + oneDecimal(refundChance) + "%",
                    "§7Макс. возврат: §bдо " + oneDecimal(refundMax) + "%",
                    "§7Бесплатный крафт: §b" + oneDecimal(free) + "%"
            ));
        } else if (progress.getPlayerClass() == PlayerClass.HAPPY_MINER) {
            int luckLevel = plugin.minerLuckAmplifier(progress.getLevel()) + 1;
            int nightVisionUnlock = plugin.minerNightVisionUnlockLevel();
            int hasteUnlock = plugin.minerHasteUnlockLevel();
            rewardMeta.setLore(List.of(
                    "§7Шахтёр: Везение §a" + toRoman(luckLevel),
                    "§7Ночное зрение: " + (progress.getLevel() >= nightVisionUnlock ? "§aактивно" : "§cс " + nightVisionUnlock + " уровня"),
                    "§7Скорость копания: " + (progress.getLevel() >= hasteUnlock ? "§aактивно" : "§cс " + hasteUnlock + " уровня")
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
            int speedUnlock = plugin.warriorSpeedUnlockLevel();
            int jumpUnlock = plugin.warriorJumpUnlockLevel();
            String speedStatus = progress.getCombatLevel() >= speedUnlock ? "§aактивно" : "§cс " + speedUnlock + " уровня";
            String jumpStatus = progress.getCombatLevel() >= jumpUnlock ? "§aактивно" : "§cс " + jumpUnlock + " уровня";
            combatRewardsMeta.setLore(List.of(
                    "§7Воин: постоянная §cСила " + toRoman(strength),
                    "§7Скорость: " + speedStatus,
                    "§7Прыгучесть: " + jumpStatus,
                    "§7Прокачка: убийства в ближнем бою"
            ));
        } else if (progress.getCombatClass() == CombatClass.ARCHER) {
            double dmg = plugin.archerDamageBonus(progress.getCombatLevel()) * 100.0;
            double save = plugin.archerArrowSaveChance(progress.getCombatLevel()) * 100.0;
            double lightning5 = plugin.archerLightningChance(5) * 100.0;
            double lightning10 = plugin.archerLightningChance(10) * 100.0;
            double debuff5 = plugin.archerDebuffChance(5) * 100.0;
            double debuff10 = plugin.archerDebuffChance(10) * 100.0;
            combatRewardsMeta.setLore(List.of(
                    "§7Бонус урона из лука: §a+" + oneDecimal(dmg) + "%",
                    "§7Шанс не потратить стрелу: §e" + oneDecimal(save) + "%",
                    "§75+ ур.: §e" + oneDecimal(lightning5) + "% §7молния, §e" + oneDecimal(debuff5) + "% §7дебафф",
                    "§710 ур.: §e" + oneDecimal(lightning10) + "% §7молния, §e" + oneDecimal(debuff10) + "% §7дебафф",
                    "§7Прокачка: убийства дальним оружием"
            ));
        } else if (progress.getCombatClass() == CombatClass.TANK) {
            int resistLevel = plugin.tankResistanceAmplifier(progress.getCombatLevel()) + 1;
            int regen1Unlock = plugin.tankRegenLevel1UnlockLevel();
            int regen2Unlock = plugin.tankRegenLevel2UnlockLevel();
            String regen = progress.getCombatLevel() >= regen2Unlock
                    ? "§dРегенерация " + toRoman(plugin.tankRegenLevel2Amplifier() + 1)
                    : progress.getCombatLevel() >= regen1Unlock
                    ? "§dРегенерация " + toRoman(plugin.tankRegenLevel1Amplifier() + 1)
                    : "§cнет";
            combatRewardsMeta.setLore(List.of(
                    "§7Танк: бонус к HP §a+" + oneDecimal(plugin.tankBonusHealth(progress.getCombatLevel()) / 2.0) + " ❤",
                    "§7Текущее сопротивление: §b" + toRoman(resistLevel),
                    "§7Регенерация: " + regen,
                    "§7Пороги: " + regen1Unlock + " ур. -> реген " + toRoman(plugin.tankRegenLevel1Amplifier() + 1) + ", " + regen2Unlock + " ур. -> реген " + toRoman(plugin.tankRegenLevel2Amplifier() + 1),
                    "§7Прогресс по полученному урону"
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
