package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.config.ConfigUpdater;
import dev.stonechat.plugin.util.ColorUtil;
import dev.stonechat.plugin.util.LuckPermsUtil;
import dev.stonechat.plugin.util.PlaceholderUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatFormatManager {

    private final StoneChat plugin;
    private volatile YamlConfiguration config;
    private volatile DateTimeFormatter dateFormatter;
    private volatile DateTimeFormatter timeFormatter;
    private File file;

    public ChatFormatManager(StoneChat plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "chatformat.yml");
        this.config = ConfigUpdater.updateFile(plugin, file, "chatformat.yml");
        this.dateFormatter = safeFormatter(config.getString("date-format", "dd.MM.yyyy"), "dd.MM.yyyy");
        this.timeFormatter = safeFormatter(config.getString("time-format", "HH:mm"), "HH:mm");
    }

    public void reload() {
        load();
    }

    public YamlConfiguration rawConfig() {
        return config;
    }

    public void set(String path, Object value) {
        config.set(path, value);
        try {
            config.save(file);
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Could not save chatformat.yml: " + e.getMessage());
        }
    }

    private DateTimeFormatter safeFormatter(String pattern, String fallbackPattern) {
        try {
            return DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid date/time format pattern '" + pattern + "', falling back to '" + fallbackPattern + "'.");
            return DateTimeFormatter.ofPattern(fallbackPattern);
        }
    }

    public String getFormat() {
        return config.getString("format", "%player_name%: %message%");
    }

    public boolean isClickToMessageEnabled() {
        return config.getBoolean("name-interactions.click-to-message", true);
    }

    public boolean isHoverStatsEnabled() {
        return config.getBoolean("name-interactions.hover-stats", true);
    }

    public Component buildMessage(Player player, String processedMessage) {
        String format = getFormat();

        if (format.contains("%player_name%")) {
            format = format.replace("%player_name%", buildPlayerNameSnippet(player));
        }

        String withPlayerPlaceholders = PlaceholderUtil.apply(player, format);

        if (withPlayerPlaceholders.contains("%date%")) {
            withPlayerPlaceholders = withPlayerPlaceholders.replace("%date%", LocalDateTime.now().format(dateFormatter));
        }
        if (withPlayerPlaceholders.contains("%time%")) {
            withPlayerPlaceholders = withPlayerPlaceholders.replace("%time%", LocalDateTime.now().format(timeFormatter));
        }

        String withMessage = withPlayerPlaceholders.replace("%message%", coloredMessage(player, processedMessage));

        return ColorUtil.parse(withMessage);
    }

    public Component buildMessageContent(Player player, String processedMessage) {
        return ColorUtil.parse(coloredMessage(player, processedMessage));
    }

    private String coloredMessage(Player player, String processedMessage) {
        String colorCode = plugin.getPlayerColorManager().getColorCode(player.getUniqueId());
        return colorCode != null ? colorCode + processedMessage : processedMessage;
    }

    private String buildPlayerNameSnippet(Player player) {
        String name = player.getName();
        String snippet = name;

        if (isHoverStatsEnabled()) {
            snippet = "<hover:show_text:'" + buildHoverText(player) + "'>" + snippet + "</hover>";
        }
        if (isClickToMessageEnabled()) {
            snippet = "<click:suggest_command:'/msg " + name + " '>" + snippet + "</click>";
        }
        return snippet;
    }

    private String buildHoverText(Player player) {
        var lm = plugin.getLanguageManager();

        String rank = LuckPermsUtil.getPrimaryGroup(player);
        String rankDisplay = (rank != null && !rank.isBlank()) ? rank : lm.getRaw("chat-format.hover-no-rank");
        String playtime = formatPlaytime(player);
        long deaths = player.getStatistic(Statistic.DEATHS);
        long kills = player.getStatistic(Statistic.PLAYER_KILLS);

        var placeholders = LanguageManager.placeholders(
                "%rank%", rankDisplay,
                "%playtime%", playtime,
                "%deaths%", String.valueOf(deaths),
                "%kills%", String.valueOf(kills)
        );

        String combined = String.join("<newline>",
                lm.getRaw("chat-format.hover-rank", placeholders),
                lm.getRaw("chat-format.hover-playtime", placeholders),
                lm.getRaw("chat-format.hover-deaths", placeholders),
                lm.getRaw("chat-format.hover-kills", placeholders)
        );

        return combined.replace("'", "");
    }

    private String formatPlaytime(Player player) {

        int ticksPlayed = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long totalMinutes = ticksPlayed / (20L * 60L);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }
}
