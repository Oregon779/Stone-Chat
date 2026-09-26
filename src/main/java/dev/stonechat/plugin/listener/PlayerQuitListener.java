package dev.stonechat.plugin.listener;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final StoneChat plugin;

    public PlayerQuitListener(StoneChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuitEarly(PlayerQuitEvent event) {
        suppress(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        plugin.getJoinDelayManager().clear(uuid);
        plugin.getCooldownManager().clear(uuid);
        plugin.getChatColorGuiManager().clearState(uuid);
        plugin.getPrivateMessageManager().clear(uuid);
        plugin.getAnvilInputManager().cancelPending(uuid);
        plugin.getPendingBroadcastManager().cancel(uuid);
        plugin.getChatLogManager().forget(uuid);
        suppress(event);
    }

    private void suppress(PlayerQuitEvent event) {
        event.quitMessage(null);

        event.setQuitMessage(null);
    }
}
