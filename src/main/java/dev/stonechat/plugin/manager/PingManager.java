package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.util.DataFiles;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingManager {

    private final StoneChat plugin;
    private final Set<UUID> ignoringPings = ConcurrentHashMap.newKeySet();
    private final File file;
    private volatile Pattern triggerPattern;

    public PingManager(StoneChat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pingsettings.yml");
        loadToggles();
        reload();
    }

    /** Who turned ping notifications off is kept in pingsettings.yml, so the choice survives restarts. */
    private void loadToggles() {
        for (String entry : DataFiles.loadYaml(plugin.getLogger(), file).getStringList("pings-off")) {
            try {
                ignoringPings.add(UUID.fromString(entry));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveToggles() {
        plugin.getDataFiles().writeLater(file.toPath(), () -> {
            YamlConfiguration data = new YamlConfiguration();
            data.set("pings-off", ignoringPings.stream().map(UUID::toString).sorted().toList());
            return data.saveToString();
        });
    }

    public void reload() {
        String trigger = plugin.getConfigManager().getPingTriggerSymbol();
        this.triggerPattern = Pattern.compile(Pattern.quote(trigger) + "([A-Za-z0-9_]{2,16})");
    }

    public boolean isIgnoringPings(Player player) {
        return ignoringPings.contains(player.getUniqueId());
    }

    public boolean toggleIgnorePings(Player player) {
        boolean nowIgnoring = !ignoringPings.remove(player.getUniqueId());
        if (nowIgnoring) {
            ignoringPings.add(player.getUniqueId());
        }
        saveToggles();
        return nowIgnoring;
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
