package ru.codex.minecraft.classlevel;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class ClassLevelPlugin extends JavaPlugin {
    private static final int MAX_LEVEL = 10;
    private static final int[] NEXT_LEVEL_REQUIREMENTS = {0, 1000, 2200, 3600, 5200, 7000, 9000, 11300, 13800, 16500};

    private PlayerDataManager dataManager;
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
        dataManager = new PlayerDataManager(this);
        dataManager.load();

        Bukkit.getPluginManager().registerEvents(new ClassSelectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MiningListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SmithCraftListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LevelMenuListener(), this);

        LevelMenuCommand levelMenuCommand = new LevelMenuCommand(this);
        if (getCommand("lvl") != null) {
            getCommand("lvl").setExecutor(levelMenuCommand);
            getCommand("lvl").setTabCompleter(levelMenuCommand);
        } else {
            getLogger().severe("Command /lvl is missing in plugin.yml");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyClassEffects(player);
        }
    }

    @Override
    public void onDisable() {
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
            applyClassEffects(player);
            dataManager.save();
        }
    }

    public void applyClassEffects(Player player) {
        PlayerProgress progress = dataManager.getOrCreate(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.LUCK);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);

        if (progress.getPlayerClass() == PlayerClass.HAPPY_MINER) {
            int luckLevel = 1 + (progress.getLevel() / 3);
            int amplifier = Math.max(0, luckLevel - 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, PotionEffect.INFINITE_DURATION, amplifier, true, false, true));

            if (progress.getLevel() >= MAX_LEVEL) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, true, false, true));
            }
        }
    }

    public double smithChanceForOneEnchant(int level) {
        return Math.min(0.05 + (Math.max(1, level) - 1) * 0.01, 0.14);
    }

    public double smithChanceForThreeEnchants(int level) {
        return Math.min(Math.max(0, level - 2) * 0.003, 0.024);
    }

    public double smithChanceForFiveEnchants(int level) {
        return Math.min(Math.max(0, level - 5) * 0.0015, 0.0075);
    }

    public double smithChanceForTenEnchants(int level) {
        return Math.min(Math.max(0, level - 8) * 0.0005, 0.001);
    }
}
