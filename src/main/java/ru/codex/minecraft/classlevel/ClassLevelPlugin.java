package ru.codex.minecraft.classlevel;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ClassLevelPlugin extends JavaPlugin {
    private static final int MAX_LEVEL = 10;
    private static final int[] NEXT_LEVEL_REQUIREMENTS = {0, 1000, 2200, 3600, 5200, 7000, 9000, 11300, 13800, 16500};
    private static final int[] WARRIOR_KILL_REQUIREMENTS = {0, 12, 20, 30, 42, 56, 72, 90, 110, 135};
    private static final int[] ARCHER_KILL_REQUIREMENTS = {0, 14, 24, 36, 50, 66, 84, 104, 126, 150};
    private static final int[] TANK_DAMAGE_REQUIREMENTS = {0, 60, 90, 130, 180, 240, 310, 390, 480, 580};
    private static final UUID TANK_HEALTH_MODIFIER_UUID = UUID.fromString("0ef4e9db-033b-4da7-8670-7d15d7f33062");

    private PlayerDataManager dataManager;
    private int effectRefreshTaskId = -1;
    private final Map<Material, Integer> oreXp = Map.ofEntries(
            Map.entry(Material.COAL_ORE, 8),
            Map.entry(Material.DEEPSLATE_COAL_ORE, 10),
            Map.entry(Material.COPPER_ORE, 10),
            Map.entry(Material.DEEPSLATE_COPPER_ORE, 12),
            Map.entry(Material.IRON_ORE, 15),
            Map.entry(Material.DEEPSLATE_IRON_ORE, 18),
            Map.entry(Material.GOLD_ORE, 20),
            Map.entry(Material.DEEPSLATE_GOLD_ORE, 24),
            Map.entry(Material.REDSTONE_ORE, 16),
            Map.entry(Material.DEEPSLATE_REDSTONE_ORE, 19),
            Map.entry(Material.LAPIS_ORE, 18),
            Map.entry(Material.DEEPSLATE_LAPIS_ORE, 21),
            Map.entry(Material.DIAMOND_ORE, 40),
            Map.entry(Material.DEEPSLATE_DIAMOND_ORE, 48),
            Map.entry(Material.EMERALD_ORE, 55),
            Map.entry(Material.DEEPSLATE_EMERALD_ORE, 65),
            Map.entry(Material.NETHER_QUARTZ_ORE, 12),
            Map.entry(Material.NETHER_GOLD_ORE, 16),
            Map.entry(Material.ANCIENT_DEBRIS, 100)
    );

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dataManager = new PlayerDataManager(this);
        dataManager.load();

        Bukkit.getPluginManager().registerEvents(new ClassSelectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MiningListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SmithCraftListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatProgressListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LevelMenuListener(), this);

        LevelMenuCommand levelMenuCommand = new LevelMenuCommand(this);
        if (getCommand("lvl") != null) {
            getCommand("lvl").setExecutor(levelMenuCommand);
            getCommand("lvl").setTabCompleter(levelMenuCommand);
        } else {
            getLogger().severe("Command /lvl is missing in plugin.yml");
        }

        ClassAdminCommand classAdminCommand = new ClassAdminCommand(this);
        if (getCommand("resetclass") != null) {
            getCommand("resetclass").setExecutor(classAdminCommand);
            getCommand("resetclass").setTabCompleter(classAdminCommand);
        } else {
            getLogger().severe("Command /resetclass is missing in plugin.yml");
        }

        AdminLevelCommand adminLevelCommand = new AdminLevelCommand(this);
        if (getCommand("givelevel") != null) {
            getCommand("givelevel").setExecutor(adminLevelCommand);
            getCommand("givelevel").setTabCompleter(adminLevelCommand);
        } else {
            getLogger().severe("Command /givelevel is missing in plugin.yml");
        }

        effectRefreshTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                applyClassEffects(online);
            }
        }, 20L, 200L);

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyClassEffects(player);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ClassLevelPlaceholderExpansion(this).register();
        }
    }

    @Override
    public void onDisable() {
        if (effectRefreshTaskId != -1) {
            Bukkit.getScheduler().cancelTask(effectRefreshTaskId);
            effectRefreshTaskId = -1;
        }

        if (dataManager != null) {
            dataManager.save();
        }
    }

    public PlayerDataManager getDataManager() {
        return dataManager;
    }

    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    public int xpForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return 0;
        }
        return NEXT_LEVEL_REQUIREMENTS[currentLevel];
    }

    public int combatRequirementForNextLevel(CombatClass combatClass, int currentLevel) {
        if (combatClass == null || currentLevel >= MAX_LEVEL) {
            return 0;
        }

        return switch (combatClass) {
            case WARRIOR -> WARRIOR_KILL_REQUIREMENTS[currentLevel];
            case ARCHER -> ARCHER_KILL_REQUIREMENTS[currentLevel];
            case TANK -> TANK_DAMAGE_REQUIREMENTS[currentLevel];
        };
    }

    public int getOreXp(Material material) {
        return oreXp.getOrDefault(material, 0);
    }

    public void giveClassXp(Player player, int amount, PlayerClass expectedClass) {
        if (amount <= 0) {
            return;
        }

        PlayerProgress progress = dataManager.getOrCreate(player.getUniqueId());
        if (progress.getPlayerClass() != expectedClass || progress.getLevel() >= MAX_LEVEL) {
            return;
        }

        progress.setXp(progress.getXp() + amount);
        int oldLevel = progress.getLevel();
        while (progress.getLevel() < MAX_LEVEL && progress.getXp() >= xpForNextLevel(progress.getLevel())) {
            progress.setLevel(progress.getLevel() + 1);
        }

        if (progress.getLevel() > oldLevel) {
            player.sendMessage("§aКласс §e" + expectedClass.displayName() + " §aулучшен до уровня §e" + progress.getLevel() + "§a!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            applyClassEffects(player);
            dataManager.save();
        }
    }

    public void giveCombatProgress(Player player, int amount, CombatClass expectedClass) {
        if (amount <= 0) {
            return;
        }

        PlayerProgress progress = dataManager.getOrCreate(player.getUniqueId());
        if (progress.getCombatClass() != expectedClass || progress.getCombatLevel() >= MAX_LEVEL) {
            return;
        }

        progress.setCombatXp(progress.getCombatXp() + amount);
        int oldLevel = progress.getCombatLevel();
        while (progress.getCombatLevel() < MAX_LEVEL) {
            int required = combatRequirementForNextLevel(expectedClass, progress.getCombatLevel());
            if (required <= 0 || progress.getCombatXp() < required) {
                break;
            }
            progress.setCombatLevel(progress.getCombatLevel() + 1);
            progress.setCombatXp(0);
        }

        if (progress.getCombatLevel() > oldLevel) {
            player.sendMessage("§aБоевой класс §e" + expectedClass.displayName() + " §aулучшен до уровня §e" + progress.getCombatLevel() + "§a!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.9f);
            applyClassEffects(player);
            dataManager.save();
        }
    }

    public void applyClassEffects(Player player) {
        PlayerProgress progress = dataManager.getOrCreate(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.LUCK);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.HASTE);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.RESISTANCE);

        if (progress.getPlayerClass() == PlayerClass.HAPPY_MINER) {
            int luckAmplifier = minerLuckAmplifier(progress.getLevel());
            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, PotionEffect.INFINITE_DURATION, luckAmplifier, true, false, true));

            if (progress.getLevel() >= minerNightVisionUnlockLevel()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, minerNightVisionAmplifier(), true, false, true));
            }

            if (progress.getLevel() >= minerHasteUnlockLevel()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, PotionEffect.INFINITE_DURATION, minerHasteAmplifier(), true, false, true));
            }
        }

        if (progress.getCombatClass() == CombatClass.WARRIOR) {
            int strengthAmplifier = warriorStrengthAmplifier(progress.getCombatLevel());
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, strengthAmplifier, true, false, true));

            if (progress.getCombatLevel() >= warriorSpeedUnlockLevel()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, warriorSpeedAmplifier(), true, false, true));
            }

            if (progress.getCombatLevel() >= warriorJumpUnlockLevel()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, warriorJumpAmplifier(), true, false, true));
            }
        }

        if (progress.getCombatClass() == CombatClass.TANK) {
            int resistanceAmplifier = tankResistanceAmplifier(progress.getCombatLevel());
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, resistanceAmplifier, true, false, true));

            if (progress.getCombatLevel() >= tankRegenLevel1UnlockLevel()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, tankRegenLevel1Amplifier(), true, false, true));
            }

            if (progress.getCombatLevel() >= tankRegenLevel2UnlockLevel()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, tankRegenLevel2Amplifier(), true, false, true));
            }
        }

        applyTankHealth(player, progress);
    }

    private void applyTankHealth(Player player, PlayerProgress progress) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        AttributeModifier existing = maxHealth.getModifier(TANK_HEALTH_MODIFIER_UUID);
        if (existing != null) {
            maxHealth.removeModifier(existing);
        }

        if (progress.getCombatClass() != CombatClass.TANK) {
            if (player.getHealth() > maxHealth.getValue()) {
                player.setHealth(maxHealth.getValue());
            }
            return;
        }

        double bonusHealth = tankBonusHealth(progress.getCombatLevel());
        if (bonusHealth > 0) {
            AttributeModifier modifier = new AttributeModifier(
                    TANK_HEALTH_MODIFIER_UUID,
                    "classlevel_tank_health_bonus",
                    bonusHealth,
                    AttributeModifier.Operation.ADD_NUMBER
            );
            maxHealth.addModifier(modifier);
        }

        if (player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }

    public int minerLuckAmplifier(int mainLevel) {
        int level = Math.max(1, mainLevel);
        int min = getConfig().getInt("effects.miner.luck.min-amplifier", 0);
        int max = getConfig().getInt("effects.miner.luck.max-amplifier", 3);
        return linearIntByLevel(level, min, max);
    }

    public int minerNightVisionUnlockLevel() {
        return Math.max(1, getConfig().getInt("effects.miner.night-vision.unlock-level", 5));
    }

    public int minerNightVisionAmplifier() {
        return Math.max(0, getConfig().getInt("effects.miner.night-vision.amplifier", 0));
    }

    public int minerHasteUnlockLevel() {
        return Math.max(1, getConfig().getInt("effects.miner.haste.unlock-level", 10));
    }

    public int minerHasteAmplifier() {
        return Math.max(0, getConfig().getInt("effects.miner.haste.amplifier", 0));
    }

    public int warriorStrengthAmplifier(int combatLevel) {
        int level = Math.max(1, combatLevel);
        int min = getConfig().getInt("effects.warrior.strength.min-amplifier", 0);
        int max = getConfig().getInt("effects.warrior.strength.max-amplifier", 4);
        return linearIntByLevel(level, min, max);
    }

    public int warriorStrengthLevel(int combatLevel) {
        return warriorStrengthAmplifier(combatLevel) + 1;
    }

    public int warriorSpeedUnlockLevel() {
        return Math.max(1, getConfig().getInt("effects.warrior.speed.unlock-level", 5));
    }

    public int warriorSpeedAmplifier() {
        return Math.max(0, getConfig().getInt("effects.warrior.speed.amplifier", 1));
    }

    public int warriorJumpUnlockLevel() {
        return Math.max(1, getConfig().getInt("effects.warrior.jump-boost.unlock-level", 5));
    }

    public int warriorJumpAmplifier() {
        return Math.max(0, getConfig().getInt("effects.warrior.jump-boost.amplifier", 0));
    }

    public int tankResistanceAmplifier(int combatLevel) {
        int base = Math.max(0, getConfig().getInt("effects.tank.resistance.base-amplifier", 0));
        int highUnlock = Math.max(1, getConfig().getInt("effects.tank.resistance.high-level.unlock-level", 5));
        int highAmplifier = Math.max(base, getConfig().getInt("effects.tank.resistance.high-level.amplifier", 1));
        return combatLevel >= highUnlock ? highAmplifier : base;
    }

    public int tankRegenLevel1UnlockLevel() {
        return Math.max(1, getConfig().getInt("effects.tank.regeneration.level1.unlock-level", 5));
    }

    public int tankRegenLevel1Amplifier() {
        return Math.max(0, getConfig().getInt("effects.tank.regeneration.level1.amplifier", 0));
    }

    public int tankRegenLevel2UnlockLevel() {
        return Math.max(1, getConfig().getInt("effects.tank.regeneration.level2.unlock-level", 10));
    }

    public int tankRegenLevel2Amplifier() {
        return Math.max(0, getConfig().getInt("effects.tank.regeneration.level2.amplifier", 1));
    }

    public double archerArrowSaveChance(int combatLevel) {
        int lvl = Math.max(1, combatLevel);
        double min = getConfig().getDouble("combat.archer.arrow-save-chance.min", 0.10);
        double max = getConfig().getDouble("combat.archer.arrow-save-chance.max", 0.30);
        return linearByLevel(lvl, min, max);
    }

    public double archerDamageBonus(int combatLevel) {
        int lvl = Math.max(1, combatLevel);
        double min = getConfig().getDouble("combat.archer.damage-bonus.min", 0.10);
        double max = getConfig().getDouble("combat.archer.damage-bonus.max", 2.50);
        return linearByLevel(lvl, min, max);
    }

    public double archerLightningChance(int combatLevel) {
        if (combatLevel >= 10) {
            return getConfig().getDouble("combat.archer.proc.lightning.level10", 0.30);
        }
        if (combatLevel >= 5) {
            return getConfig().getDouble("combat.archer.proc.lightning.level5", 0.20);
        }
        return 0.0;
    }

    public double archerDebuffChance(int combatLevel) {
        if (combatLevel >= 10) {
            return getConfig().getDouble("combat.archer.proc.debuff.level10", 0.50);
        }
        if (combatLevel >= 5) {
            return getConfig().getDouble("combat.archer.proc.debuff.level5", 0.20);
        }
        return 0.0;
    }

    public List<PotionEffectType> archerNegativeEffects() {
        List<String> configured = getConfig().getStringList("combat.archer.proc.debuff.effects");
        if (!configured.isEmpty()) {
            List<PotionEffectType> result = new ArrayList<>();
            for (String effectName : configured) {
                PotionEffectType effect = PotionEffectType.getByName(effectName.toUpperCase(Locale.ROOT));
                if (effect != null) {
                    result.add(effect);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }

        return List.of(
                PotionEffectType.SLOWNESS,
                PotionEffectType.WEAKNESS,
                PotionEffectType.POISON,
                PotionEffectType.BLINDNESS,
                PotionEffectType.WITHER,
                PotionEffectType.HUNGER
        );
    }

    public int archerDebuffDurationTicks(int combatLevel) {
        int level = Math.max(1, combatLevel);
        double min = getConfig().getDouble("combat.archer.proc.debuff.duration-seconds.min", 5.0);
        double max = getConfig().getDouble("combat.archer.proc.debuff.duration-seconds.max", 8.0);
        return Math.max(20, (int) Math.round(linearByLevel(level, min, max) * 20.0));
    }

    public int archerDebuffAmplifier(int combatLevel) {
        int level = Math.max(1, combatLevel);
        int min = getConfig().getInt("combat.archer.proc.debuff.amplifier.min", 1);
        int max = getConfig().getInt("combat.archer.proc.debuff.amplifier.max", 4);
        return linearIntByLevel(level, min, max);
    }

    public double tankBonusHealth(int combatLevel) {
        int lvl = Math.max(1, combatLevel);
        double base = getConfig().getDouble("combat.tank.health-bonus.base", 4.0);
        double perLevel = getConfig().getDouble("combat.tank.health-bonus.per-level", 2.15);
        return base + (lvl - 1) * perLevel;
    }

    private double linearByLevel(int level, double min, double max) {
        if (level >= MAX_LEVEL) {
            return max;
        }
        double step = (max - min) / (MAX_LEVEL - 1);
        return min + (level - 1) * step;
    }

    private int linearIntByLevel(int level, int min, int max) {
        return (int) Math.round(linearByLevel(level, min, max));
    }

    public double smithChanceForOneEnchant(int level) {
        return Math.min(0.12 + (Math.max(1, level) - 1) * 0.025, 0.35);
    }

    public double smithChanceForThreeEnchants(int level) {
        return Math.min(0.02 + (Math.max(1, level) - 1) * 0.014, 0.15);
    }

    public double smithChanceForFiveEnchants(int level) {
        return Math.min(Math.max(0, level - 2) * 0.0075, 0.06);
    }

    public double smithChanceForTenEnchants(int level) {
        return Math.min(Math.max(0, level - 4) * 0.0035, 0.02);
    }

    public double crafterResourceReturnChance(int level) {
        return Math.min(0.12 + (Math.max(1, level) - 1) * 0.02, 0.30);
    }

    public double crafterMaxRefundPortion(int level) {
        if (level >= MAX_LEVEL) {
            return 1.0;
        }
        return Math.min(0.20 + (Math.max(1, level) - 1) * 0.035, 0.50);
    }

    public double crafterFreeCraftChance(int level) {
        return level >= MAX_LEVEL ? 0.08 : 0.0;
    }
}
