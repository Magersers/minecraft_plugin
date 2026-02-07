package ru.codex.minecraft.classlevel;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class MiningListener implements Listener {
    private final ClassLevelPlugin plugin;

    public MiningListener(ClassLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerProgress progress = plugin.getDataManager().getOrCreate(player.getUniqueId());
        if (progress.getPlayerClass() != PlayerClass.MINER) {
            return;
        }

        Material blockType = event.getBlock().getType();
        int xp = plugin.getOreXp(blockType);
        if (xp <= 0) {
            return;
        }

        plugin.giveMiningXp(player, xp);
    }
}
