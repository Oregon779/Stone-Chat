package dev.stonechat.plugin.command;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IgnoreCommand implements CommandExecutor, TabCompleter {

    private final StoneChat plugin;

    public IgnoreCommand(StoneChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.player-only"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("ignore.usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sendIgnoreList(player);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("private-message.player-offline"));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("ignore.cannot-ignore-self"));
            return true;
        }

        if (target.hasPermission("stonechat.ignore.immune")) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("ignore.cannot-ignore-immune"));
            return true;
        }

        boolean nowIgnoring = plugin.getIgnoreManager().toggleIgnore(player.getUniqueId(), target.getUniqueId());
        String path = nowIgnoring ? "ignore.now-ignoring" : "ignore.no-longer-ignoring";
        player.sendMessage(plugin.getLanguageManager().getPrefixed(path,
                LanguageManager.placeholders("%player%", target.getName())));
        return true;
    }

    private void sendIgnoreList(Player player) {
        var ignored = plugin.getIgnoreManager().getIgnored(player.getUniqueId());
        if (ignored.isEmpty()) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("ignore.list-empty"));
            return;
        }

        String names = ignored.stream()
                .map(uuid -> {
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
                    String name = offline.getName();
                    return name != null ? name : uuid.toString();
                })
                .collect(Collectors.joining(", "));

        player.sendMessage(plugin.getLanguageManager().getPrefixed("ignore.list-header",
                LanguageManager.placeholders("%players%", names)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();

        List<String> options = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(sender.getName()))
                .collect(Collectors.toList()));
        options.add("list");

        return options.stream()
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
    }
}
