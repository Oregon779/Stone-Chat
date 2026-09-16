package dev.stonechat.plugin.command;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.BroadcastManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.stream.Collectors;

public class BroadcastCommand implements CommandExecutor, TabCompleter {

    private final StoneChat plugin;

    public BroadcastCommand(StoneChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("stonechat.broadcast")) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("broadcast.usage"));
            return true;
        }

        BroadcastManager.Type type;
        try {
            type = BroadcastManager.Type.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("broadcast.usage"));
            return true;
        }

        String message;
        Integer durationSeconds = null;

        if (args.length >= 2) {
            try {
                durationSeconds = Integer.parseInt(args[1]);
                if (args.length < 3) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("broadcast.usage"));
                    return true;
                }
                message = String.join(" ", List.of(args).subList(2, args.length));
            } catch (NumberFormatException e) {

                message = String.join(" ", List.of(args).subList(1, args.length));
            }
        } else {
            message = String.join(" ", List.of(args).subList(1, args.length));
        }

        plugin.getBroadcastManager().broadcast(type, message, durationSeconds);
        plugin.getSoundManager().playToAll("broadcast.sound");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("stonechat.broadcast")) return List.of();

        if (args.length == 1) {
            return List.of("chat", "actionbar", "title", "bossbar").stream()
                    .filter(o -> o.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
