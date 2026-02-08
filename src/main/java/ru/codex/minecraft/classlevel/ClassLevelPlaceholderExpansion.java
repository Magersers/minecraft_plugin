package ru.codex.minecraft.classlevel;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class ClassLevelPlaceholderExpansion extends PlaceholderExpansion {
    private final ClassLevelPlugin plugin;

    public ClassLevelPlaceholderExpansion(ClassLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "classlevel";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) {
            return "";
        }

        PlayerProgress progress = plugin.getDataManager().getOrCreate(player.getUniqueId());
        return switch (params.toLowerCase()) {
            case "main_class" -> progress.getPlayerClass() == null ? "Нет" : progress.getPlayerClass().displayName();
            case "main_class_key" -> progress.getPlayerClass() == null ? "none" : progress.getPlayerClass().name().toLowerCase();
            case "main_level" -> String.valueOf(progress.getLevel());
            case "combat_class" -> progress.getCombatClass() == null ? "Нет" : progress.getCombatClass().displayName();
            case "combat_class_key" -> progress.getCombatClass() == null ? "none" : progress.getCombatClass().name().toLowerCase();
            case "combat_level" -> String.valueOf(progress.getCombatLevel());
            default -> null;
        };
    }
}
