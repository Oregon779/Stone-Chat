package dev.stonechat.plugin.listener;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final StoneChat plugin;

    public PlayerJoinListener(StoneChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoinEarly(PlayerJoinEvent event) {
        suppress(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getJoinDelayManager().markJoined(event.getPlayer());
        suppress(event);
    }

    private void suppress(PlayerJoinEvent event) {
        event.joinMessage(null);

        event.setJoinMessage(null);
    }
}
