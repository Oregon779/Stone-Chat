package dev.stonechat.plugin.command;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ChatMuteCommand implements CommandExecutor {

    private final StoneChat plugin;

    public ChatMuteCommand(StoneChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("stonechat.mute")) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.no-permission"));
            return true;
        }

        boolean nowMuted = plugin.getMuteManager().toggle();
        String basePath = nowMuted ? "chat-mute.now-muted" : "chat-mute.now-unmuted";
        var placeholders = LanguageManager.placeholders("%player%", sender.getName());

        plugin.getNotificationManager().broadcast(plugin.getConfigManager().getMuteNotificationType(), basePath, placeholders);
        plugin.getSoundManager().playToAll("chat-mute.sound-toggle");
        return true;
    }
}
