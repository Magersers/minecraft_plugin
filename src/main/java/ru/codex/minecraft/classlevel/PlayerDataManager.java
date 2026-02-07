package ru.codex.minecraft.classlevel;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private final ClassLevelPlugin plugin;
    private final Map<UUID, PlayerProgress> data = new HashMap<>();
    private File dataFile;
    private YamlConfiguration config;

    public PlayerDataManager(ClassLevelPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "player-data.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Не удалось создать player-data.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        for (String key : playersSection.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            String className = playersSection.getString(key + ".class");
            PlayerClass playerClass = null;
            if (className != null) {
                if (className.equals("MINER")) {
                    className = "HAPPY_MINER";
                }
                try {
                    playerClass = PlayerClass.valueOf(className);
                } catch (IllegalArgumentException ignored) {
                }
            }

            String combatClassName = playersSection.getString(key + ".combatClass");
            CombatClass combatClass = null;
            if (combatClassName != null) {
                try {
                    combatClass = CombatClass.valueOf(combatClassName);
                } catch (IllegalArgumentException ignored) {
                }
            }

            int level = Math.max(1, playersSection.getInt(key + ".level", 1));
            int xp = Math.max(0, playersSection.getInt(key + ".xp", 0));
            int combatLevel = Math.max(1, playersSection.getInt(key + ".combatLevel", 1));
            int combatXp = Math.max(0, playersSection.getInt(key + ".combatXp", 0));
            data.put(uuid, new PlayerProgress(playerClass, level, xp, combatClass, combatLevel, combatXp));
        }
    }

    public void save() {
        if (config == null) {
            config = new YamlConfiguration();
        }
        config.set("players", null);
        for (Map.Entry<UUID, PlayerProgress> entry : data.entrySet()) {
            String base = "players." + entry.getKey();
            PlayerProgress progress = entry.getValue();
            config.set(base + ".class", progress.getPlayerClass() == null ? null : progress.getPlayerClass().name());
            config.set(base + ".level", progress.getLevel());
            config.set(base + ".xp", progress.getXp());
            config.set(base + ".combatClass", progress.getCombatClass() == null ? null : progress.getCombatClass().name());
            config.set(base + ".combatLevel", progress.getCombatLevel());
            config.set(base + ".combatXp", progress.getCombatXp());
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить player-data.yml: " + e.getMessage());
        }
    }

    public PlayerProgress getOrCreate(UUID uuid) {
        return data.computeIfAbsent(uuid, ignored -> PlayerProgress.empty());
    }
}
