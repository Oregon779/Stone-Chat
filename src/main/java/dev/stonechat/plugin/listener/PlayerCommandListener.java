package dev.stonechat.plugin.listener;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.LanguageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class PlayerCommandListener implements Listener {

    private final StoneChat plugin;

    public PlayerCommandListener(StoneChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String label = event.getMessage().substring(1).split(" ")[0].toLowerCase();
        int namespaceEnd = label.indexOf(':');
        if (namespaceEnd >= 0) {
            label = label.substring(namespaceEnd + 1);
        }

        if (label.equals("chatmute") || label.equals("stonechat") || label.equals("sc") || label.equals("stonechatplugin")) {
            return;
        }

        if (plugin.getMuteManager().isMuted() && plugin.getConfigManager().isMuteBlockingCommands()
                && !player.hasPermission(plugin.getConfigManager().getMuteBypassPermission())) {
            event.setCancelled(true);
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getMuteNotificationType(),
                    "chat-mute.command-blocked", java.util.Map.of());
            plugin.getSoundManager().play(player, "chat-mute.sound-blocked");
            return;
        }

        int remaining = plugin.getCooldownManager().tryCommand(player, label);
        if (remaining > 0) {
            event.setCancelled(true);
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getCooldownNotificationType(),
                    "cooldown.command-wait", LanguageManager.placeholders("%seconds%", String.valueOf(remaining)));
            plugin.getSoundManager().play(player, "cooldown.sound");
        }
    }
}
