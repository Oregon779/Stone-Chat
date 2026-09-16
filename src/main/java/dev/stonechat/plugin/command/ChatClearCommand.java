package dev.stonechat.plugin.command;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.LanguageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatClearCommand implements CommandExecutor {

    private final StoneChat plugin;

    public ChatClearCommand(StoneChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("stonechat.clear")) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.no-permission"));
            return true;
        }

        int lines = plugin.getConfigManager().getChatClearLines();
        Component blank = Component.empty();

        for (Player online : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < lines; i++) {
                online.sendMessage(blank);
            }
        }

        plugin.getNotificationManager().broadcast(plugin.getConfigManager().getChatClearNotificationType(),
                "chat-clear.cleared", LanguageManager.placeholders("%player%", sender.getName()));
        plugin.getSoundManager().playToAll("chat-clear.sound");
        return true;
    }
}
