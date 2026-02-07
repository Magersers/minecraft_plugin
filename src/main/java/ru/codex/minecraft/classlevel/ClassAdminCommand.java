package ru.codex.minecraft.classlevel;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassAdminCommand implements CommandExecutor, TabCompleter {
    private final ClassLevelPlugin plugin;

    public ClassAdminCommand(ClassLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("classlevel.admin.reset")) {
            sender.sendMessage("§cУ вас нет прав на эту команду.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§eИспользование: /resetclass <игрок>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cИгрок не найден или не в сети.");
            return true;
        }

        PlayerProgress progress = plugin.getDataManager().getOrCreate(target.getUniqueId());
        progress.setPlayerClass(null);
        progress.setLevel(1);
        progress.setXp(0);

        plugin.applyClassEffects(target);
        plugin.getDataManager().save();

        target.sendMessage("§cВаш класс был сброшен администратором. Выберите новый класс.");
        target.openInventory(ClassSelectionListener.createClassMenu());
        sender.sendMessage("§aКласс игрока §e" + target.getName() + " §aуспешно сброшен.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(prefix)) {
                    result.add(player.getName());
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
