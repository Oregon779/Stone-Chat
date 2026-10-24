package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.ChatLogManager.Status;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Anti-caps, word filter and link blocker - the content checks shared by public chat and private messages.
 * Each check keeps its own settings, bypass permission, notification, sound and punish command.
 */
public class ContentFilter {

    /** {@code message} is the text to deliver: lower-cased by anti-caps or censored by the word filter. */
    public record Outcome(Status status, String message) {

        public boolean blocked() {
            return status.isBlocked();
        }
    }

    private final StoneChat plugin;

    public ContentFilter(StoneChat plugin) {
        this.plugin = plugin;
    }

    public Outcome check(Player player, String message, boolean antiCaps, boolean wordFilter, boolean linkBlocker) {
        if (antiCaps) {
            CapsManager.CapsCheck capsCheck = plugin.getCapsManager().check(player, message);
            if (capsCheck.result == CapsManager.Result.BLOCKED) {
                plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getAntiCapsNotificationType(),
                        "anti-caps.blocked", Map.of());
                plugin.getSoundManager().play(player, "anti-caps.sound");
                plugin.getPunishmentManager().execute(player, "anti-caps.punish-command");
                return new Outcome(Status.BLOCKED_CAPS, message);
            }
            message = capsCheck.correctedMessage;
        }

        boolean censored = false;
        if (wordFilter) {
            WordFilterManager.FilterResult filterResult = plugin.getWordFilterManager().check(message);
            if (filterResult.triggered) {
                plugin.getSoundManager().play(player, "word-filter.sound");
                plugin.getPunishmentManager().execute(player, "word-filter.punish-command");
                plugin.getWordFilterManager().notifyAdmins(player, message, filterResult.viaEvasionDetection);
                if (!plugin.getConfigManager().isWordFilterCensorMode()) {
                    plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getWordFilterNotificationType(),
                            "word-filter.blocked", Map.of());
                    return new Outcome(Status.BLOCKED_WORD_FILTER, message);
                }
                message = filterResult.filteredMessage;
                censored = true;
            }
        }

        if (linkBlocker && !player.hasPermission(plugin.getConfigManager().getLinkBypassPermission())
                && plugin.getLinkFilterManager().containsLink(message)) {
            plugin.getNotificationManager().dispatch(player, plugin.getConfigManager().getLinkBlockerNotificationType(),
                    "link-blocker.blocked", Map.of());
            plugin.getSoundManager().play(player, "link-blocker.sound");
            plugin.getPunishmentManager().execute(player, "link-blocker.punish-command");
            return new Outcome(Status.BLOCKED_LINK, message);
        }

        return new Outcome(censored ? Status.CENSORED : Status.SENT, message);
    }
}
