package dev.stonechat.plugin.command;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.SocialSpyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SocialSpyCommand implements CommandExecutor {

    private final StoneChat plugin;

    public SocialSpyCommand(StoneChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var lm = plugin.getLanguageManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lm.getPrefixed("general.player-only"));
            return true;
        }
        if (!player.hasPermission(SocialSpyManager.PERMISSION)) {
            player.sendMessage(lm.getPrefixed("general.no-permission"));
            return true;
        }
        if (!plugin.getConfigManager().isSocialSpyEnabled()) {
            player.sendMessage(lm.getPrefixed("social-spy.disabled-in-config"));
            return true;
        }

        boolean nowSpying = plugin.getSocialSpyManager().toggle(player);
        player.sendMessage(lm.getPrefixed(nowSpying ? "social-spy.enabled" : "social-spy.disabled"));
        return true;
    }
}
