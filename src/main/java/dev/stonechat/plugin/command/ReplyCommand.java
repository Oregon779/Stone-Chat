package dev.stonechat.plugin.command;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ReplyCommand implements CommandExecutor {

    private final StoneChat plugin;

    public ReplyCommand(StoneChat plugin) {
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

        if (args.length < 1) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("private-message.reply-usage"));
            return true;
        }

        UUID replyTargetUuid = plugin.getPrivateMessageManager().getReplyTarget(player.getUniqueId());
        if (replyTargetUuid == null) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("private-message.no-reply-target"));
            return true;
        }

        Player target = Bukkit.getPlayer(replyTargetUuid);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("private-message.player-offline"));
            return true;
        }

        String message = String.join(" ", args);
        plugin.getPrivateMessageManager().send(player, target, message);
        return true;
    }
}
