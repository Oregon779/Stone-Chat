package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrivateMessageManager {

    private final StoneChat plugin;
    private final Map<UUID, UUID> lastMessagedBy = new ConcurrentHashMap<>();

    public PrivateMessageManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    public UUID getReplyTarget(UUID playerUuid) {
        return lastMessagedBy.get(playerUuid);
    }

    /** Removes this player's own reply-target entry on quit - a departed player has no use for it, and leaving it forever would grow this map unbounded on a long-running server with many unique visitors. */
    public void clear(UUID playerUuid) {
        lastMessagedBy.remove(playerUuid);
    }

    public boolean send(Player sender, Player target, String rawMessage) {
        if (plugin.getIgnoreManager().isIgnoring(target.getUniqueId(), sender.getUniqueId())) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("private-message.ignored",
                    LanguageManager.placeholders("%player%", target.getName())));
            return false;
        }

        String message = ColorUtil.sanitizeUserInput(rawMessage);

        var senderPlaceholders = LanguageManager.placeholders("%player%", target.getName(), "%message%", message);
        var targetPlaceholders = LanguageManager.placeholders("%player%", sender.getName(), "%message%", message);

        Component toSender = ColorUtil.parse(plugin.getLanguageManager().getRaw("private-message.sender-format", senderPlaceholders));
        Component toTarget = buildReceiverComponent(sender, targetPlaceholders);

        sender.sendMessage(toSender);
        target.sendMessage(toTarget);

        plugin.getSoundManager().play(target, "private-messages.sound");

        lastMessagedBy.put(target.getUniqueId(), sender.getUniqueId());
        lastMessagedBy.put(sender.getUniqueId(), target.getUniqueId());
        return true;
    }

    private Component buildReceiverComponent(Player sender, Map<String, String> placeholders) {
        Component base = ColorUtil.parse(plugin.getLanguageManager().getRaw("private-message.receiver-format", placeholders));

        Component hoverText = ColorUtil.parse(plugin.getLanguageManager().getRaw("private-message.reply-hover",
                LanguageManager.placeholders("%player%", sender.getName())));

        return base
                .hoverEvent(HoverEvent.showText(hoverText))
                .clickEvent(ClickEvent.suggestCommand("/r "));
    }
}
