package dev.stonechat.plugin.listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.ChatLogManager.Status;
import dev.stonechat.plugin.manager.ContentFilter;
import dev.stonechat.plugin.manager.LanguageManager;
import dev.stonechat.plugin.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;

public class PlayerChatListener implements Listener {

    private final StoneChat plugin;

    public PlayerChatListener(StoneChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String typed = PlainTextComponentSerializer.plainText().serialize(event.message());

        Status status = handle(event, player, typed);
        plugin.getChatLogManager().record(player, typed, status);
    }

    private Status handle(AsyncChatEvent event, Player player, String typed) {
        String message = ColorUtil.sanitizeUserInput(typed);

        int joinDelayRemaining = plugin.getJoinDelayManager().remainingSeconds(player);
        if (joinDelayRemaining > 0) {
            event.setCancelled(true);
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getJoinDelayNotificationType(),
                    "join-delay.wait", LanguageManager.placeholders("%seconds%", String.valueOf(joinDelayRemaining)));
            plugin.getSoundManager().play(player, "join-delay.sound");
            return Status.BLOCKED_JOIN_DELAY;
        }

        if (plugin.getMuteManager().isMuted() && !player.hasPermission(plugin.getConfigManager().getMuteBypassPermission())) {
            event.setCancelled(true);
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getMuteNotificationType(),
                    "chat-mute.chat-blocked", Map.of());
            plugin.getSoundManager().play(player, "chat-mute.sound-blocked");
            return Status.BLOCKED_MUTE;
        }

        if (plugin.getConfigManager().isMaxLengthEnabled() && typed.length() > plugin.getConfigManager().getMaxLength()) {
            event.setCancelled(true);
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getMaxLengthNotificationType(),
                    "max-length.blocked", LanguageManager.placeholders("%max%", String.valueOf(plugin.getConfigManager().getMaxLength())));
            plugin.getSoundManager().play(player, "max-message-length.sound");
            return Status.BLOCKED_LENGTH;
        }

        int cooldownRemaining = plugin.getCooldownManager().tryChat(player);
        if (cooldownRemaining > 0) {
            event.setCancelled(true);
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getCooldownNotificationType(),
                    "cooldown.chat-wait", LanguageManager.placeholders("%seconds%", String.valueOf(cooldownRemaining)));
            plugin.getSoundManager().play(player, "cooldown.sound");
            return Status.BLOCKED_COOLDOWN;
        }

        if (plugin.getChatGameManager().checkAnswer(player, typed)) {
            event.setCancelled(true);
            return Status.CHAT_GAME;
        }

        ContentFilter.Outcome outcome = plugin.getContentFilter().check(player, message, true, true, true);
        if (outcome.blocked()) {
            event.setCancelled(true);
            return outcome.status();
        }
        message = outcome.message();

        String withPings = plugin.getPingManager().applyPings(player, message);
        Component formattedLine = plugin.getChatFormatManager().buildMessage(player, withPings);

        // Render instead of cancel + broadcast: the message stays a real player chat message, so client
        // chat settings, player blocking and other chat listeners (e.g. Discord bridges) keep working.
        // viewerUnaware: Paper renders once and reuses it for every viewer instead of once per player.
        event.message(plugin.getChatFormatManager().buildMessageContent(player, withPings));
        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, renderedMessage) -> formattedLine));

        return outcome.status();
    }
}
