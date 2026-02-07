package ru.codex.minecraft.classlevel;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SmithCraftListener implements Listener {
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
        if (progress.getPlayerClass() != PlayerClass.BLACKSMITH) {
            return;
        }

        if (isArmor(crafted.getType())) {
            plugin.giveClassXp(player, armorCraftXp(crafted.getType()), PlayerClass.BLACKSMITH);
        }

        if (isWeaponOrArmor(crafted.getType())) {
            int enchantCount = rollEnchantCount(progress.getLevel());
            if (enchantCount > 0) {
                ItemStack enchanted = crafted.clone();
                applyRandomEnchants(enchanted, enchantCount);
                event.setCurrentItem(enchanted);
                player.sendMessage("§dКузнец: предмет получил §e" + enchantCount + " §dслучайных зачарований I уровня.");
            }
        }
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
            item.addUnsafeEnchantment(enchantment, 1);
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
