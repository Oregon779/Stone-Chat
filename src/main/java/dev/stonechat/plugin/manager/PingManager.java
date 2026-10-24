package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.util.DataFiles;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingManager {

    private final StoneChat plugin;
    private final Set<UUID> ignoringPings = ConcurrentHashMap.newKeySet();
    private final File file;
    // The "@Name" chat suggestions this plugin gave each online player, so exactly those can be taken back
    // again. Join, quit and reload all run on the main thread, which is the only one touching this map.
    private final Map<UUID, Set<String>> offeredCompletions = new HashMap<>();
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
        refreshCompletions();
    }

    /**
     * Gives every online player a chat suggestion "@Name" for everyone they can ping, so typing "@St" and pressing
     * Tab completes "@Steve". Uses the client's own chat completion, nothing is sent while typing.
     */
    public void refreshCompletions() {
        clearCompletions();
        if (!completionsEnabled()) return;
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        for (Player viewer : online) {
            offer(viewer, online);
        }
    }

    public void handleJoin(Player joined) {
        if (!completionsEnabled()) return;
        offer(joined, Bukkit.getOnlinePlayers());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(joined)) offer(viewer, List.of(joined));
        }
    }

    public void handleQuit(Player left) {
        offeredCompletions.remove(left.getUniqueId());
        // Offered entries always use the current trigger: a reload that changes it re-offers everything.
        String entry = currentTrigger() + left.getName();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Set<String> offered = offeredCompletions.get(viewer.getUniqueId());
            if (offered != null && offered.remove(entry)) {
                viewer.removeCustomChatCompletions(List.of(entry));
            }
        }
    }

    /** Takes back every suggestion this plugin added, e.g. before a reload or when the plugin is disabled. */
    public void clearCompletions() {
        for (var entry : offeredCompletions.entrySet()) {
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer != null && !entry.getValue().isEmpty()) {
                viewer.removeCustomChatCompletions(entry.getValue());
            }
        }
        offeredCompletions.clear();
    }

    private void offer(Player viewer, Collection<? extends Player> targets) {
        List<String> entries = new ArrayList<>();
        for (Player target : targets) {
            if (canBeSuggested(viewer, target)) {
                entries.add(currentTrigger() + target.getName());
            }
        }
        if (entries.isEmpty()) return;
        offeredCompletions.computeIfAbsent(viewer.getUniqueId(), uuid -> new HashSet<>()).addAll(entries);
        viewer.addCustomChatCompletions(entries);
    }

    private boolean canBeSuggested(Player viewer, Player target) {
        if (target.equals(viewer)) return plugin.getConfigManager().isAllowSelfPing();
        return viewer.canSee(target) && !target.hasPermission(plugin.getConfigManager().getPingImmunePermission());
    }

    private boolean completionsEnabled() {
        return plugin.getConfigManager().isPingEnabled() && plugin.getConfigManager().isPingTabComplete();
    }

    private String currentTrigger() {
        return plugin.getConfigManager().getPingTriggerSymbol();
    }

    public boolean isIgnoringPings(Player player) {
        return isIgnoringPings(player.getUniqueId());
    }

    public boolean isIgnoringPings(UUID uuid) {
        return ignoringPings.contains(uuid);
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
