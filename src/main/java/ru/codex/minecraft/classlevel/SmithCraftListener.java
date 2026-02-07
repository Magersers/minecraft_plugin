package ru.codex.minecraft.classlevel;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SmithCraftListener implements Listener {
    private static final Set<Material> CRAFTER_REFUND_EXCLUDED = EnumSet.of(
            Material.IRON_INGOT,
            Material.GOLD_INGOT,
            Material.COPPER_INGOT,
            Material.NETHERITE_INGOT,
            Material.IRON_NUGGET,
            Material.GOLD_NUGGET,
            Material.DIAMOND,
            Material.EMERALD,
            Material.LAPIS_LAZULI,
            Material.REDSTONE,
            Material.COAL,
            Material.CHARCOAL,
            Material.QUARTZ,
            Material.AMETHYST_SHARD
    );

    private final ClassLevelPlugin plugin;

    public SmithCraftListener(ClassLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack crafted = event.getCurrentItem();
        if (crafted == null || crafted.getType().isAir()) {
            return;
        }

        PlayerProgress progress = plugin.getDataManager().getOrCreate(player.getUniqueId());
        if (progress.getPlayerClass() == PlayerClass.BLACKSMITH) {
            processBlacksmithCraft(event, player, progress, crafted);
            return;
        }

        if (progress.getPlayerClass() == PlayerClass.CRAFTER) {
            processCrafterCraft(event, player, progress, crafted);
        }
    }

    private void processBlacksmithCraft(CraftItemEvent event, Player player, PlayerProgress progress, ItemStack crafted) {
        if (isArmor(crafted.getType())) {
            plugin.giveClassXp(player, armorCraftXp(crafted.getType()), PlayerClass.BLACKSMITH);
        }

        if (isWeaponOrArmor(crafted.getType())) {
            int enchantCount = rollEnchantCount(progress.getLevel());
            if (enchantCount > 0) {
                ItemStack enchanted = crafted.clone();
                applyRandomEnchants(enchanted, enchantCount);
                event.setCurrentItem(enchanted);
                player.sendMessage("§dКузнец: предмет получил §e" + enchantCount + " §dслучайных зачарований случайного уровня.");
            }
        }
    }

    private void processCrafterCraft(CraftItemEvent event, Player player, PlayerProgress progress, ItemStack crafted) {
        plugin.giveClassXp(player, crafterCraftXp(crafted.getType()), PlayerClass.CRAFTER);

        ItemStack[] matrix = event.getInventory().getMatrix();
        if (!isCrafterRefundAllowed(crafted.getType(), matrix)) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (plugin.crafterFreeCraftChance(progress.getLevel()) > 0
                && random.nextDouble() < plugin.crafterFreeCraftChance(progress.getLevel())) {
            refundCraftIngredients(player, matrix, 1.0);
            player.sendMessage("§6Крафтер: бесплатный крафт! Ресурсы полностью возвращены.");
            return;
        }

        if (random.nextDouble() < plugin.crafterResourceReturnChance(progress.getLevel())) {
            double maxRefundPortion = plugin.crafterMaxRefundPortion(progress.getLevel());
            int refunded = refundCraftIngredients(player, matrix, maxRefundPortion);
            if (refunded > 0) {
                player.sendMessage("§bКрафтер: вернулась часть ресурсов (до " + (int) Math.round(maxRefundPortion * 100.0) + "%).");
            }
        }
    }

    private int refundCraftIngredients(Player player, ItemStack[] matrix, double portion) {
        if (matrix == null || portion <= 0.0) {
            return 0;
        }

        List<ItemStack> refundableUnits = new ArrayList<>();
        for (ItemStack ingredient : matrix) {
            if (ingredient == null || ingredient.getType().isAir()) {
                continue;
            }

            ItemStack unit = ingredient.clone();
            unit.setAmount(1);
            refundableUnits.add(unit);
        }

        if (refundableUnits.isEmpty()) {
            return 0;
        }

        int total = refundableUnits.size();
        int refundCount;
        if (portion >= 1.0) {
            refundCount = total;
        } else {
            double expected = total * portion;
            refundCount = (int) Math.floor(expected);
            double fractional = expected - refundCount;
            if (ThreadLocalRandom.current().nextDouble() < fractional) {
                refundCount++;
            }
            refundCount = Math.min(refundCount, total);
        }

        if (refundCount <= 0) {
            return 0;
        }

        Collections.shuffle(refundableUnits, ThreadLocalRandom.current());
        for (int i = 0; i < refundCount; i++) {
            ItemStack refund = refundableUnits.get(i);
            player.getInventory().addItem(refund).values().forEach(leftover ->
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }

        return refundCount;
    }

    private boolean isCrafterRefundAllowed(Material result, ItemStack[] matrix) {
        if (isCompactingMaterial(result) || CRAFTER_REFUND_EXCLUDED.contains(result)) {
            return false;
        }

        if (matrix == null) {
            return true;
        }

        for (ItemStack ingredient : matrix) {
            if (ingredient == null || ingredient.getType().isAir()) {
                continue;
            }
            Material type = ingredient.getType();
            if (isCompactingMaterial(type) || CRAFTER_REFUND_EXCLUDED.contains(type)) {
                return false;
            }
        }

        return true;
    }

    private boolean isCompactingMaterial(Material material) {
        return material.name().endsWith("_BLOCK");
    }

    private int crafterCraftXp(Material result) {
        if (result.name().endsWith("_CHESTPLATE") || result.name().endsWith("_LEGGINGS")) {
            return 10;
        }
        if (result.name().endsWith("_HELMET") || result.name().endsWith("_BOOTS") || result.name().endsWith("_SWORD")
                || result.name().endsWith("_AXE") || result.name().endsWith("_PICKAXE")) {
            return 8;
        }
        if (result.name().endsWith("_PLATE") || result == Material.PISTON || result == Material.OBSERVER
                || result == Material.COMPARATOR || result == Material.REPEATER) {
            return 6;
        }
        if (result == Material.ENCHANTING_TABLE || result == Material.ANVIL || result == Material.SMITHING_TABLE
                || result == Material.BREWING_STAND) {
            return 12;
        }
        return 4;
    }

    private int rollEnchantCount(int level) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() < plugin.smithChanceForTenEnchants(level)) {
            return 10;
        }
        if (random.nextDouble() < plugin.smithChanceForFiveEnchants(level)) {
            return 5;
        }
        if (random.nextDouble() < plugin.smithChanceForThreeEnchants(level)) {
            return 3;
        }
        if (random.nextDouble() < plugin.smithChanceForOneEnchant(level)) {
            return 1;
        }
        return 0;
    }

    private void applyRandomEnchants(ItemStack item, int count) {
        List<Enchantment> pool = new ArrayList<>();
        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment.canEnchantItem(item)) {
                pool.add(enchantment);
            }
        }

        if (pool.isEmpty()) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int rolls = Math.min(count, pool.size());
        for (int i = 0; i < rolls; i++) {
            int index = random.nextInt(pool.size());
            Enchantment enchantment = pool.remove(index);
            int randomLevel = 1 + random.nextInt(enchantment.getMaxLevel());
            item.addUnsafeEnchantment(enchantment, randomLevel);
        }
    }

    private boolean isWeaponOrArmor(Material material) {
        return isArmor(material)
                || material.name().endsWith("_SWORD")
                || material.name().endsWith("_AXE")
                || material == Material.BOW
                || material == Material.CROSSBOW
                || material == Material.MACE;
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS");
    }

    private int armorCraftXp(Material material) {
        String name = material.name();
        if (name.startsWith("LEATHER_")) {
            return 60;
        }
        if (name.startsWith("CHAINMAIL_")) {
            return 80;
        }
        if (name.startsWith("IRON_")) {
            return 100;
        }
        if (name.startsWith("GOLDEN_")) {
            return 110;
        }
        if (name.startsWith("DIAMOND_")) {
            return 140;
        }
        if (name.startsWith("NETHERITE_")) {
            return 200;
        }
        return 70;
    }
}
