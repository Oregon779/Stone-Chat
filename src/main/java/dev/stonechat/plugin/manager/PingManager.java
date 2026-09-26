package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingManager {

    private final StoneChat plugin;
    private final Set<UUID> ignoringPings = ConcurrentHashMap.newKeySet();
    private volatile Pattern triggerPattern;

    public PingManager(StoneChat plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String trigger = plugin.getConfigManager().getPingTriggerSymbol();
        this.triggerPattern = Pattern.compile(Pattern.quote(trigger) + "([A-Za-z0-9_]{2,16})");
    }

    public boolean isIgnoringPings(Player player) {
        return ignoringPings.contains(player.getUniqueId());
    }

    public boolean toggleIgnorePings(Player player) {
        if (ignoringPings.contains(player.getUniqueId())) {
            ignoringPings.remove(player.getUniqueId());
            return false;
        } else {
            ignoringPings.add(player.getUniqueId());
            return true;
        }
    }

    public String applyPings(Player sender, String message) {
        if (!plugin.getConfigManager().isPingEnabled()) {
            return message;
        }

        String trigger = plugin.getConfigManager().getPingTriggerSymbol();
        Matcher matcher = triggerPattern.matcher(message);
        if (!matcher.find()) {

            return message;
        }
        matcher.reset();

        StringBuilder result = new StringBuilder(message.length() + 16);
        Set<UUID> notified = new java.util.HashSet<>();
        int lastEnd = 0;

        while (matcher.find()) {
            String targetName = matcher.group(1);
            Player target = Bukkit.getPlayerExact(targetName);

            result.append(message, lastEnd, matcher.start());

            if (target == null) {
                result.append(matcher.group());
                lastEnd = matcher.end();
                continue;
            }

            if (target.equals(sender) && !plugin.getConfigManager().isAllowSelfPing()) {
                result.append(matcher.group());
                lastEnd = matcher.end();
                continue;
            }

            if (target.hasPermission(plugin.getConfigManager().getPingImmunePermission())) {
                result.append(matcher.group());
                lastEnd = matcher.end();
                continue;
            }

            String highlightTemplate = plugin.getConfigManager().getPingHighlightTemplate();
            result.append(highlightTemplate.replace("%player%", trigger + target.getName()));
            lastEnd = matcher.end();

            if (notified.add(target.getUniqueId())) {
                notifyTarget(sender, target);
            }
        }
        result.append(message.substring(lastEnd));

        return result.toString();
    }

    private void notifyTarget(Player sender, Player target) {
        if (plugin.getConfigManager().isPingIgnorable() && isIgnoringPings(target)) {
            return;
        }
        if (plugin.getIgnoreManager().isIgnoring(target.getUniqueId(), sender.getUniqueId())) {
            return;
        }

        var placeholders = LanguageManager.placeholders("%player%", sender.getName());
        plugin.getNotificationManager().dispatch(target, plugin.getConfigManager().getPingNotificationType(),
                "ping.sound-notify", placeholders);

        plugin.getSoundManager().play(target, "ping.sound");
    }
}
