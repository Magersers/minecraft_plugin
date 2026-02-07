package ru.codex.minecraft.classlevel;

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
        if (!(lastDamage instanceof EntityDamageByEntityEvent damageByEntity)) {
            return;
        }

        Entity damager = damageByEntity.getDamager();
        boolean victimPlayer = victim.getType() == EntityType.PLAYER;

        if (combatClass == CombatClass.WARRIOR && isMeleeKill(killer, damager)) {
            plugin.giveCombatXp(killer, victimPlayer ? 35 : 12, CombatClass.WARRIOR);
            return;
        }

        if (combatClass == CombatClass.ARCHER && isRangedKillByPlayer(killer, damager)) {
            plugin.giveCombatXp(killer, victimPlayer ? 35 : 12, CombatClass.ARCHER);
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

        if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < plugin.archerArrowSaveChance(progress.getCombatLevel())) {
            event.setConsumeItem(false);
        }
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

        int xp = (int) Math.max(1, Math.floor(event.getFinalDamage() * 0.75));
        plugin.giveCombatXp(player, xp, CombatClass.TANK);
    }

    private boolean isMeleeKill(Player killer, Entity damager) {
        if (!(damager instanceof Player attackingPlayer) || !attackingPlayer.getUniqueId().equals(killer.getUniqueId())) {
            return false;
        }

        ItemStack hand = killer.getInventory().getItemInMainHand();
        if (hand == null) {
            return false;
        }

        String name = hand.getType().name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || hand.getType().name().equals("TRIDENT");
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
