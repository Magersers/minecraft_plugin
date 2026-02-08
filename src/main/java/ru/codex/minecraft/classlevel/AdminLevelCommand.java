package ru.codex.minecraft.classlevel;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminLevelCommand implements CommandExecutor, TabCompleter {
    private final ClassLevelPlugin plugin;

    public AdminLevelCommand(ClassLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("classlevel.admin.givelevel")) {
            sender.sendMessage("§cУ вас нет прав на эту команду.");
            return true;
        }

        if (args.length != 4) {
            sender.sendMessage("§eИспользование: /givelevel <игрок> <main|combat> <set|add> <число>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cИгрок не найден или не в сети.");
            return true;
        }

        String track = args[1].toLowerCase();
        String mode = args[2].toLowerCase();
        int value;
        try {
            value = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cЧисло уровня указано неверно.");
            return true;
        }

        if (value < 0) {
            sender.sendMessage("§cЧисло должно быть положительным.");
            return true;
        }

        if (!mode.equals("set") && !mode.equals("add")) {
            sender.sendMessage("§cТретий аргумент должен быть set или add.");
            return true;
        }

        PlayerProgress progress = plugin.getDataManager().getOrCreate(target.getUniqueId());
        int maxLevel = plugin.getMaxLevel();

        if (track.equals("main")) {
            int current = progress.getLevel();
            int newLevel = mode.equals("add") ? current + value : value;
            newLevel = Math.min(maxLevel, Math.max(1, newLevel));
            progress.setLevel(newLevel);
            progress.setXp(0);
            sender.sendMessage("§aОсновной уровень игрока §e" + target.getName() + " §aтеперь §e" + newLevel + "§a.");
            target.sendMessage("§6Администратор изменил ваш основной уровень на §e" + newLevel + "§6.");
        } else if (track.equals("combat")) {
            int current = progress.getCombatLevel();
            int newLevel = mode.equals("add") ? current + value : value;
            newLevel = Math.min(maxLevel, Math.max(1, newLevel));
            progress.setCombatLevel(newLevel);
            progress.setCombatXp(0);
            sender.sendMessage("§aБоевой уровень игрока §e" + target.getName() + " §aтеперь §e" + newLevel + "§a.");
            target.sendMessage("§6Администратор изменил ваш боевой уровень на §e" + newLevel + "§6.");
        } else {
            sender.sendMessage("§cВторой аргумент должен быть main или combat.");
            return true;
        }

        plugin.applyClassEffects(target);
        plugin.getDataManager().save();
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
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

        if (args.length == 2) {
            return partial(args[1], List.of("main", "combat"));
        }

        if (args.length == 3) {
            return partial(args[2], List.of("set", "add"));
        }

        return Collections.emptyList();
    }

    private List<String> partial(String typed, List<String> values) {
        String prefix = typed.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.startsWith(prefix)) {
                result.add(value);
            }
        }
        return result;
    }
}
