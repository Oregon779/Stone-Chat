package dev.stonechat.plugin.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.CapsManager;
import dev.stonechat.plugin.manager.LanguageManager;
import dev.stonechat.plugin.manager.WordFilterManager;
import org.bukkit.Bukkit;
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

        String message = dev.stonechat.plugin.util.ColorUtil.sanitizeUserInput(
                PlainTextComponentSerializer.plainText().serialize(event.message()));

        int joinDelayRemaining = plugin.getJoinDelayManager().remainingSeconds(player);
        if (joinDelayRemaining > 0) {
            event.setCancelled(true);
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getJoinDelayNotificationType(),
                    "join-delay.wait", LanguageManager.placeholders("%seconds%", String.valueOf(joinDelayRemaining)));
            plugin.getSoundManager().play(player, "join-delay.sound");
            return;
        }

        if (plugin.getMuteManager().isMuted() && !player.hasPermission(plugin.getConfigManager().getMuteBypassPermission())) {
            event.setCancelled(true);
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getMuteNotificationType(),
                    "chat-mute.chat-blocked", Map.of());
            plugin.getSoundManager().play(player, "chat-mute.sound-blocked");
            return;
        }

        if (plugin.getConfigManager().isMaxLengthEnabled() && message.length() > plugin.getConfigManager().getMaxLength()) {
            event.setCancelled(true);
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getMaxLengthNotificationType(),
                    "max-length.blocked", LanguageManager.placeholders("%max%", String.valueOf(plugin.getConfigManager().getMaxLength())));
            plugin.getSoundManager().play(player, "max-message-length.sound");
            return;
        }

        int cooldownRemaining = plugin.getCooldownManager().tryChat(player);
        if (cooldownRemaining > 0) {
            event.setCancelled(true);
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getCooldownNotificationType(),
                    "cooldown.chat-wait", LanguageManager.placeholders("%seconds%", String.valueOf(cooldownRemaining)));
            plugin.getSoundManager().play(player, "cooldown.sound");
            return;
        }

        if (plugin.getChatGameManager().checkAnswer(player, message)) {
            event.setCancelled(true);
            return;
        }

        CapsManager.CapsCheck capsCheck = plugin.getCapsManager().check(player, message);
        if (capsCheck.result == CapsManager.Result.BLOCKED) {
            event.setCancelled(true);
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getAntiCapsNotificationType(),
                    "anti-caps.blocked", Map.of());
            plugin.getSoundManager().play(player, "anti-caps.sound");
            plugin.getPunishmentManager().execute(player, "anti-caps.punish-command");
            return;
        }
        message = capsCheck.correctedMessage;

        WordFilterManager.FilterResult filterResult = plugin.getWordFilterManager().check(message);
        if (filterResult.triggered) {
            plugin.getSoundManager().play(player, "word-filter.sound");
            plugin.getPunishmentManager().execute(player, "word-filter.punish-command");
            if (!plugin.getConfigManager().isWordFilterCensorMode()) {
                event.setCancelled(true);
                plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getWordFilterNotificationType(),
                        "word-filter.blocked", Map.of());
                plugin.getWordFilterManager().notifyAdmins(player, message, filterResult.viaEvasionDetection);
                return;
            }
            plugin.getWordFilterManager().notifyAdmins(player, message, filterResult.viaEvasionDetection);
            message = filterResult.filteredMessage;
        }

        if (!player.hasPermission(plugin.getConfigManager().getLinkBypassPermission())
                && plugin.getLinkFilterManager().containsLink(message)) {
            event.setCancelled(true);
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getLinkBlockerNotificationType(),
                    "link-blocker.blocked", Map.of());
            plugin.getSoundManager().play(player, "link-blocker.sound");
            plugin.getPunishmentManager().execute(player, "link-blocker.punish-command");
            return;
        }

        String withPings = plugin.getPingManager().applyPings(player, message);

        Component finalMessage = plugin.getChatFormatManager().buildMessage(player, withPings);

        event.setCancelled(true);
        Bukkit.broadcast(finalMessage);
    }
}
