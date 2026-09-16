package dev.stonechat.plugin.command;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class MessageCommand implements CommandExecutor, TabCompleter {

    private final StoneChat plugin;

    public MessageCommand(StoneChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.player-only"));
            return true;
        }

        if (!plugin.getConfigManager().isPrivateMessagesEnabled()) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("general.unknown-command"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("private-message.usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("private-message.player-offline"));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("private-message.cannot-message-self"));
            return true;
        }

        String message = String.join(" ", List.of(args).subList(1, args.length));
        plugin.getPrivateMessageManager().send(player, target, message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();

        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(sender.getName()))
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
    }
}
