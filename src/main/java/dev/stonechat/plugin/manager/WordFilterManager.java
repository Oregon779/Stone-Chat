package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.util.TextNormalizer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WordFilterManager {

    private final StoneChat plugin;
    private volatile List<CompiledWord> compiledWords = List.of();

    public WordFilterManager(StoneChat plugin) {
        this.plugin = plugin;
        reload();
    }

    private record CompiledWord(String word, Pattern pattern) {
    }

    public void reload() {
        List<String> words = plugin.getConfigManager().getFilteredWords();
        List<CompiledWord> compiled = new ArrayList<>(words.size());

        for (String word : words) {
            if (word == null || word.isBlank()) continue;
            compiled.add(new CompiledWord(word, Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b")));
        }

        this.compiledWords = compiled;
    }

    public static class FilterResult {
        public final boolean triggered;
        public final String filteredMessage;

        public final boolean viaEvasionDetection;

        public FilterResult(boolean triggered, String filteredMessage, boolean viaEvasionDetection) {
            this.triggered = triggered;
            this.filteredMessage = filteredMessage;
            this.viaEvasionDetection = viaEvasionDetection;
        }
    }

    public FilterResult check(String originalMessage) {
        if (!plugin.getConfigManager().isWordFilterEnabled() || compiledWords.isEmpty()) {
            return new FilterResult(false, originalMessage, false);
        }

        boolean censor = plugin.getConfigManager().isWordFilterCensorMode();
        boolean evasionDetectionEnabled = plugin.getConfigManager().isEvasionDetectionEnabled();

        String result = originalMessage;
        boolean triggeredRaw = false;
        boolean triggeredViaNormalization = false;

        String normalized = evasionDetectionEnabled
                ? TextNormalizer.normalize(originalMessage, plugin.getConfigManager().isLeetspeakDetectionEnabled())
                : null;

        for (CompiledWord entry : compiledWords) {
            if (entry.pattern().matcher(result).find()) {
                triggeredRaw = true;
                if (censor) {
                    String stars = "*".repeat(entry.word().length());
                    result = entry.pattern().matcher(result).replaceAll(stars);
                } else {

                    break;
                }
                continue;
            }

            if (evasionDetectionEnabled && entry.pattern().matcher(normalized).find()) {
                triggeredViaNormalization = true;
                if (!censor) break;
            }
        }

        boolean triggered = triggeredRaw || triggeredViaNormalization;

        if (triggeredViaNormalization && censor) {

            result = "*".repeat(originalMessage.length());
        }

        return new FilterResult(triggered, result, triggeredViaNormalization && !triggeredRaw);
    }

    public void notifyAdmins(Player triggeringPlayer, String originalMessage, boolean viaEvasionDetection) {
        if (!plugin.getConfigManager().isWordFilterNotifyAdmins()) return;

        String path = viaEvasionDetection ? "word-filter.admin-notify-evasion" : "word-filter.admin-notify";
        var placeholders = LanguageManager.placeholders(
                "%player%", triggeringPlayer.getName(),
                "%message%", originalMessage
        );

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("stonechat.wordfilter.notify")) {
                online.sendMessage(plugin.getLanguageManager().getPrefixed(path, placeholders));
            }
        }
        String logNote = viaEvasionDetection ? " (via unicode/leetspeak evasion detection)" : "";
        plugin.getLogger().info("[WordFilter] " + triggeringPlayer.getName() + " triggered the word filter" + logNote + ": " + originalMessage);
    }
}
