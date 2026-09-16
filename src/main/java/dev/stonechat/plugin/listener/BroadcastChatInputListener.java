package dev.stonechat.plugin.listener;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.PendingBroadcastManager;
import dev.stonechat.plugin.manager.PendingBroadcastManager.PendingBroadcast;
import dev.stonechat.plugin.manager.PendingBroadcastManager.Stage;
import dev.stonechat.plugin.util.ColorUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class BroadcastChatInputListener implements Listener {

    private final StoneChat plugin;

    public BroadcastChatInputListener(StoneChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getPendingBroadcastManager().hasPending(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        PendingBroadcast pending = plugin.getPendingBroadcastManager().peek(player.getUniqueId());
        if (pending == null) return;

        if (text.equalsIgnoreCase("cancel")) {
            plugin.getPendingBroadcastManager().cancel(player.getUniqueId());
            player.sendMessage(ColorUtil.parse("&cBroadcast cancelled."));
            return;
        }

        if (pending.stage() == Stage.AWAITING_MESSAGE) {
            handleMessageStage(player, pending, text);
        } else {
            handleConfirmStage(player, pending, text);
        }
    }

    private void handleMessageStage(Player player, PendingBroadcast pending, String text) {
        if (text.isBlank()) {
            plugin.getPendingBroadcastManager().cancel(player.getUniqueId());
            player.sendMessage(ColorUtil.parse("&cBroadcast cancelled - empty message."));
            return;
        }

        plugin.getPendingBroadcastManager().advanceToConfirm(player.getUniqueId(), text);

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage(ColorUtil.parse("&8&m                                                "));
            player.sendMessage(ColorUtil.parse("&6&lPreview &7(shown only to you):"));
            plugin.getBroadcastManager().preview(player, pending.type(), text, pending.durationSeconds());
            player.sendMessage(ColorUtil.parse("&7Type &aconfirm &7to send this to everyone, or &ccancel &7to abort."));
            player.sendMessage(ColorUtil.parse("&8&m                                                "));
        });
    }

    private void handleConfirmStage(Player player, PendingBroadcast pending, String text) {
        if (!text.equalsIgnoreCase("confirm")) {
            player.sendMessage(ColorUtil.parse("&7Type &aconfirm &7to send, or &ccancel &7to abort."));
            return;
        }

        plugin.getPendingBroadcastManager().consume(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getBroadcastManager().broadcast(pending.type(), pending.message(), pending.durationSeconds());
            plugin.getSoundManager().playToAll("broadcast.sound");
            player.sendMessage(ColorUtil.parse("&aBroadcast sent!"));
        });
    }
}
