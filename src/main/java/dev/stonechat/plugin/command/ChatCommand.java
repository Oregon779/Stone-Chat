package dev.stonechat.plugin.command;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("settings", "color", "ignore", "msg", "r");

    private final StoneChat plugin;

    public ChatCommand(StoneChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.player-only"));
            return true;
        }

        if (args.length == 0) {
            plugin.getPlayerSettingsGuiManager().open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        String[] rest = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (sub) {
            case "settings", "menu" -> plugin.getPlayerSettingsGuiManager().open(player);
            case "color", "colour" -> plugin.getChatColorGuiManager().open(player);
            case "ignore" -> Bukkit.dispatchCommand(player, "ignore " + String.join(" ", rest));
            case "msg", "tell", "whisper", "w" -> Bukkit.dispatchCommand(player, "msg " + String.join(" ", rest));
            case "r", "reply" -> Bukkit.dispatchCommand(player, "r " + String.join(" ", rest));
            default -> player.sendMessage(plugin.getLanguageManager().getPrefixed("general.unknown-command"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
