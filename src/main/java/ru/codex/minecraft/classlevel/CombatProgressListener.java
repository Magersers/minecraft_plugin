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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class CombatProgressListener implements Listener {
    private final ClassLevelPlugin plugin;
    private final Map<UUID, Integer> archerShotsWithoutProc = new ConcurrentHashMap<>();

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

        if (event.getEntity() instanceof LivingEntity target) {
            rollArcherHitEffects(player, progress, target);
        }
    }

    private void rollArcherHitEffects(Player player, PlayerProgress progress, LivingEntity target) {
        double lightningChance = plugin.archerLightningChance(progress.getCombatLevel());
        if (lightningChance > 0 && ThreadLocalRandom.current().nextDouble() < lightningChance) {
            target.getWorld().strikeLightning(target.getLocation());
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.4f);
        }

        double debuffChance = plugin.archerDebuffChance(progress.getCombatLevel());
        if (debuffChance <= 0 || ThreadLocalRandom.current().nextDouble() >= debuffChance) {
            return;
        }

        PotionEffectType randomEffect = randomNegativeEffect();
        if (randomEffect == null) {
            return;
        }

        int durationTicks = 5 * 20;
        int amplifier = randomEffect.equals(PotionEffectType.POISON) ? 0 : 4;
        target.addPotionEffect(new PotionEffect(randomEffect, durationTicks, amplifier, true, true, true));
        player.playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 0.65f, 1.25f);
    }

    private PotionEffectType randomNegativeEffect() {
        var effects = plugin.archerNegativeEffects();
        if (effects.isEmpty()) {
            return null;
        }
        return effects.get(ThreadLocalRandom.current().nextInt(effects.size()));
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

        if (!rollArrowSave(player, progress)) {
            return;
        }

        event.setConsumeItem(false);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.35f);

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack refund = consumable.asOne();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.getInventory().addItem(refund);
        });
    }

    private boolean rollArrowSave(Player player, PlayerProgress progress) {
        UUID uuid = player.getUniqueId();
        double chance = plugin.archerArrowSaveChance(progress.getCombatLevel());

        if (ThreadLocalRandom.current().nextDouble() < chance) {
            archerShotsWithoutProc.put(uuid, 0);
            return true;
        }

        int shots = archerShotsWithoutProc.getOrDefault(uuid, 0) + 1;
        int guaranteeEvery = Math.max(1, (int) Math.ceil(1.0 / Math.max(0.0001, chance)));
        if (shots >= guaranteeEvery) {
            archerShotsWithoutProc.put(uuid, 0);
            return true;
        }

        archerShotsWithoutProc.put(uuid, shots);
        return false;
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
