package ru.codex.minecraft.classlevel;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class CombatProgressListener implements Listener {
    private final ClassLevelPlugin plugin;

    public CombatProgressListener(ClassLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }

        PlayerProgress progress = plugin.getDataManager().getOrCreate(killer.getUniqueId());
        CombatClass combatClass = progress.getCombatClass();
        if (combatClass == null) {
            return;
        }

        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        Entity damager = (lastDamage instanceof EntityDamageByEntityEvent damageByEntity) ? damageByEntity.getDamager() : null;
        boolean victimPlayer = victim.getType() == EntityType.PLAYER;

        if (combatClass == CombatClass.WARRIOR && isMeleeKill(killer, damager)) {
            plugin.giveCombatProgress(killer, victimPlayer ? 2 : 1, CombatClass.WARRIOR);
            return;
        }

        if (combatClass == CombatClass.ARCHER && isRangedKillByPlayer(killer, damager)) {
            plugin.giveCombatProgress(killer, victimPlayer ? 2 : 1, CombatClass.ARCHER);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile projectile)) {
            return;
        }

        if (!(projectile.getShooter() instanceof Player player)) {
            return;
        }

        PlayerProgress progress = plugin.getDataManager().getOrCreate(player.getUniqueId());
        if (progress.getCombatClass() != CombatClass.ARCHER) {
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isBow(hand)) {
            return;
        }

        double bonus = plugin.archerDamageBonus(progress.getCombatLevel());
        event.setDamage(event.getDamage() * (1.0 + bonus));
    }

    @EventHandler(ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PlayerProgress progress = plugin.getDataManager().getOrCreate(player.getUniqueId());
        if (progress.getCombatClass() != CombatClass.ARCHER) {
            return;
        }

        ItemStack consumable = event.getConsumable();
        if (consumable == null || consumable.getType().isAir()) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() >= plugin.archerArrowSaveChance(progress.getCombatLevel())) {
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.35f);
            return;
        }

        ItemStack refund = consumable.asOne();
        Bukkit.getScheduler().runTask(plugin, () -> player.getInventory().addItem(refund));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.35f);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PlayerProgress progress = plugin.getDataManager().getOrCreate(player.getUniqueId());
        if (progress.getCombatClass() != CombatClass.TANK) {
            return;
        }

        int damagePoints = (int) Math.max(1, Math.round(event.getFinalDamage()));
        plugin.giveCombatProgress(player, damagePoints, CombatClass.TANK);
    }

    private boolean isMeleeKill(Player killer, Entity damager) {
        if (damager instanceof Player attackingPlayer) {
            return attackingPlayer.getUniqueId().equals(killer.getUniqueId()) && isMeleeWeapon(killer.getInventory().getItemInMainHand());
        }

        if (damager instanceof Projectile) {
            return false;
        }

        return isMeleeWeapon(killer.getInventory().getItemInMainHand());
    }

    private boolean isMeleeWeapon(ItemStack hand) {
        if (hand == null) {
            return false;
        }
        String name = hand.getType().name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("TRIDENT") || name.equals("MACE");
    }

    private boolean isRangedKillByPlayer(Player killer, Entity damager) {
        if (!(damager instanceof AbstractArrow arrow)) {
            return false;
        }
        return arrow.getShooter() instanceof Player shooter && shooter.getUniqueId().equals(killer.getUniqueId());
    }

    private boolean isBow(ItemStack item) {
        if (item == null) {
            return false;
        }
        return item.getType().name().equals("BOW") || item.getType().name().equals("CROSSBOW");
    }
}
