package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.config.ConfigUpdater;
import dev.stonechat.plugin.model.MessageDisplayType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

/**
 * Owns config.yml. Every value is read from the YAML tree exactly once, in
 * {@link #load()}, and cached into a plain field from then on - the getters
 * below are simple field reads, not repeated {@code YamlConfiguration}
 * lookups (which internally split the dotted path and walk nested
 * {@code ConfigurationSection}s on every single call).
 * <p>
 * This matters because these getters sit on the chat hot path: a single
 * chat message runs through word-filter, link-blocker, cooldown, anti-caps,
 * ping and chat-format checks, each reading several of these values - at a
 * couple hundred concurrent players producing a steady stream of messages,
 * that's thousands of avoidable YAML traversals per second for values that
 * only ever change when an admin edits config.yml or hits /stonechat reload
 * (both of which call {@link #load()} again, refreshing every cached field).
 */
public class ConfigManager {

    private final StoneChat plugin;
    private volatile YamlConfiguration config;
    private File file;

    private volatile String language;

    private volatile boolean wordFilterEnabled;
    private volatile List<String> filteredWords;
    private volatile boolean wordFilterCensorMode;
    private volatile boolean wordFilterNotifyAdmins;
    private volatile boolean evasionDetectionEnabled;
    private volatile boolean leetspeakDetectionEnabled;
    private volatile MessageDisplayType wordFilterNotificationType;

    private volatile String muteBypassPermission;
    private volatile boolean muteBlockingCommands;
    private volatile MessageDisplayType muteNotificationType;

    private volatile boolean linkBlockerEnabled;
    private volatile String linkBypassPermission;
    private volatile List<String> linkPatterns;
    private volatile MessageDisplayType linkBlockerNotificationType;

    private volatile boolean pingEnabled;
    private volatile String pingTriggerSymbol;
    private volatile String pingHighlightTemplate;
    private volatile boolean allowSelfPing;
    private volatile String pingImmunePermission;
    private volatile boolean pingIgnorable;
    private volatile MessageDisplayType pingNotificationType;

    private volatile boolean cooldownEnabled;
    private volatile int chatCooldownSeconds;
    private volatile int commandCooldownSeconds;
    private volatile String cooldownBypassPermission;
    private volatile List<String> cooldownCommands;
    private volatile MessageDisplayType cooldownNotificationType;

    private volatile boolean antiCapsEnabled;
    private volatile int antiCapsMaxPercentage;
    private volatile int antiCapsMinLength;
    private volatile boolean antiCapsAutoCorrect;
    private volatile String antiCapsBypassPermission;
    private volatile MessageDisplayType antiCapsNotificationType;

    private volatile boolean joinDelayEnabled;
    private volatile int joinDelaySeconds;
    private volatile String joinDelayBypassPermission;
    private volatile MessageDisplayType joinDelayNotificationType;

    private volatile boolean maxLengthEnabled;
    private volatile int maxLength;
    private volatile MessageDisplayType maxLengthNotificationType;

    private volatile String bossbarColor;
    private volatile String bossbarStyle;
    private volatile int bossbarDurationSeconds;
    private volatile boolean broadcastUsePrefix;
    private volatile String broadcastDefaultType;

    private volatile int titleFadeIn;
    private volatile int titleStay;
    private volatile int titleFadeOut;

    private volatile boolean updateCheckerEnabled;
    private volatile int updateCheckerIntervalMinutes;

    private volatile boolean privateMessagesEnabled;

    private volatile int chatClearLines;
    private volatile MessageDisplayType chatClearNotificationType;

    private volatile boolean chatColorGuiEnabled;
    private volatile String chatColorGuiUsePermission;

    private volatile boolean chatLogEnabled;
    private volatile boolean chatLogBlockedMessages;
    private volatile int chatLogRetentionDays;
    private volatile int chatLogMaxEntries;
    private volatile int chatLogEntriesPerPage;
    private volatile String chatLogDateFormat;

    private volatile boolean autoMessagesEnabled;
    private volatile int autoMessagesIntervalSeconds;
    private volatile boolean autoMessagesRandomOrder;
    private volatile int autoMessagesMinPlayers;
    private volatile BroadcastManager.Type autoMessagesDisplayType;
    private volatile String autoMessagesPrefix;
    private volatile List<List<String>> autoMessages;

    public ConfigManager(StoneChat plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "config.yml");
        this.config = ConfigUpdater.updateFile(plugin, file, "config.yml");
        refreshCache();
    }

    private void refreshCache() {
        language = config.getString("general.default-language", "en");

        wordFilterEnabled = config.getBoolean("word-filter.enabled", true);
        filteredWords = List.copyOf(config.getStringList("word-filter.filtered-words"));
        wordFilterCensorMode = "censor".equalsIgnoreCase(config.getString("word-filter.action", "block"));
        wordFilterNotifyAdmins = config.getBoolean("word-filter.notify-admins", true);
        evasionDetectionEnabled = config.getBoolean("word-filter.evasion-detection.enabled", true);
        leetspeakDetectionEnabled = config.getBoolean("word-filter.evasion-detection.normalize-leetspeak", false);
        wordFilterNotificationType = readNotificationType("word-filter.notification-type");

        muteBypassPermission = config.getString("chat-mute.bypass-permission", "stonechat.mute.bypass");
        muteBlockingCommands = config.getBoolean("chat-mute.block-commands", true);
        muteNotificationType = readNotificationType("chat-mute.notification-type");

        linkBlockerEnabled = config.getBoolean("link-blocker.enabled", true);
        linkBypassPermission = config.getString("link-blocker.bypass-permission", "stonechat.links.bypass");
        linkPatterns = List.copyOf(config.getStringList("link-blocker.patterns"));
        linkBlockerNotificationType = readNotificationType("link-blocker.notification-type");

        pingEnabled = config.getBoolean("ping.enabled", true);
        pingTriggerSymbol = config.getString("ping.trigger-symbol", "@");
        pingHighlightTemplate = config.getString("ping.highlight-template", "<yellow><bold>%player%</bold></yellow>");
        allowSelfPing = config.getBoolean("ping.allow-self-ping", false);
        pingImmunePermission = config.getString("ping.immune-permission", "stonechat.ping.immune");
        pingIgnorable = config.getBoolean("ping.ignorable", true);
        pingNotificationType = readNotificationType("ping.notification-type");

        cooldownEnabled = config.getBoolean("cooldown.enabled", true);
        chatCooldownSeconds = config.getInt("cooldown.chat-seconds", 5);
        commandCooldownSeconds = config.getInt("cooldown.command-seconds", 3);
        cooldownBypassPermission = config.getString("cooldown.bypass-permission", "stonechat.cooldown.bypass");
        cooldownCommands = List.copyOf(config.getStringList("cooldown.cooldown-commands"));
        cooldownNotificationType = readNotificationType("cooldown.notification-type");

        antiCapsEnabled = config.getBoolean("anti-caps.enabled", true);
        antiCapsMaxPercentage = config.getInt("anti-caps.max-percentage", 60);
        antiCapsMinLength = config.getInt("anti-caps.min-length", 5);
        antiCapsAutoCorrect = "autocorrect".equalsIgnoreCase(config.getString("anti-caps.mode", "block"));
        antiCapsBypassPermission = config.getString("anti-caps.bypass-permission", "stonechat.caps.bypass");
        antiCapsNotificationType = readNotificationType("anti-caps.notification-type");

        joinDelayEnabled = config.getBoolean("join-delay.enabled", true);
        joinDelaySeconds = config.getInt("join-delay.seconds", 5);
        joinDelayBypassPermission = config.getString("join-delay.bypass-permission", "stonechat.joindelay.bypass");
        joinDelayNotificationType = readNotificationType("join-delay.notification-type");

        maxLengthEnabled = config.getBoolean("max-message-length.enabled", true);
        maxLength = config.getInt("max-message-length.max-length", 100);
        maxLengthNotificationType = readNotificationType("max-message-length.notification-type");

        bossbarColor = config.getString("broadcast.bossbar.color", "BLUE");
        bossbarStyle = config.getString("broadcast.bossbar.style", "SOLID");
        bossbarDurationSeconds = config.getInt("broadcast.bossbar.duration-seconds", 10);
        broadcastUsePrefix = config.getBoolean("broadcast.use-prefix", true);
        broadcastDefaultType = config.getString("broadcast.default-type", "CHAT");

        titleFadeIn = config.getInt("title-timing.fade-in-ticks", 10);
        titleStay = config.getInt("title-timing.stay-ticks", 60);
        titleFadeOut = config.getInt("title-timing.fade-out-ticks", 10);

        updateCheckerEnabled = config.getBoolean("update-checker.enabled", true);
        updateCheckerIntervalMinutes = config.getInt("update-checker.check-interval-minutes", 60);

        privateMessagesEnabled = config.getBoolean("private-messages.enabled", true);

        chatClearLines = config.getInt("chat-clear.lines", 100);
        chatClearNotificationType = readNotificationType("chat-clear.notification-type");

        chatColorGuiEnabled = config.getBoolean("chat-color-gui.enabled", true);
        chatColorGuiUsePermission = config.getString("chat-color-gui.use-permission", "");

        chatLogEnabled = config.getBoolean("chat-log.enabled", true);
        chatLogBlockedMessages = config.getBoolean("chat-log.log-blocked-messages", true);
        chatLogRetentionDays = config.getInt("chat-log.retention-days", 30);
        chatLogMaxEntries = config.getInt("chat-log.max-entries-per-player", 1000);
        chatLogEntriesPerPage = Math.max(1, config.getInt("chat-log.entries-per-page", 10));
        chatLogDateFormat = config.getString("chat-log.date-format", "dd.MM.yyyy HH:mm:ss");

        autoMessagesEnabled = config.getBoolean("auto-messages.enabled", false);
        autoMessagesIntervalSeconds = config.getInt("auto-messages.interval-seconds", 60);
        autoMessagesRandomOrder = "RANDOM".equalsIgnoreCase(config.getString("auto-messages.order", "SEQUENTIAL"));
        autoMessagesMinPlayers = config.getInt("auto-messages.min-players-online", 1);
        autoMessagesDisplayType = readBroadcastType("auto-messages.display-type");
        autoMessagesPrefix = config.getString("auto-messages.prefix", "");
        autoMessages = readAutoMessages();
    }

    private MessageDisplayType readNotificationType(String path) {
        return MessageDisplayType.fromConfig(config.getString(path, "CHAT"), MessageDisplayType.CHAT);
    }

    private BroadcastManager.Type readBroadcastType(String path) {
        try {
            return BroadcastManager.Type.valueOf(config.getString(path, "CHAT").trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BroadcastManager.Type.CHAT;
        }
    }

    /** Each entry is a block with a "lines" list; a plain string entry is accepted as a one-line message too. */
    private List<List<String>> readAutoMessages() {
        List<List<String>> messages = new java.util.ArrayList<>();
        for (Object item : config.getList("auto-messages.messages", List.of())) {
            List<String> lines = new java.util.ArrayList<>();
            if (item instanceof String text) {
                lines.add(text);
            } else if (item instanceof java.util.Map<?, ?> map && map.get("lines") instanceof List<?> rawLines) {
                for (Object line : rawLines) {
                    if (line != null) lines.add(String.valueOf(line));
                }
            }
            if (!lines.isEmpty()) {
                messages.add(List.copyOf(lines));
            }
        }
        return List.copyOf(messages);
    }

    /** Writes a single value and re-caches everything - used by the in-game settings editor, which edits one field at a time. */
    public void set(String path, Object value) {
        config.set(path, value);
        try {
            config.save(file);
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Could not save config.yml: " + e.getMessage());
        }
        refreshCache();
    }

    public YamlConfiguration raw() {
        return config;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isWordFilterEnabled() {
        return wordFilterEnabled;
    }

    public List<String> getFilteredWords() {
        return filteredWords;
    }

    public boolean isWordFilterCensorMode() {
        return wordFilterCensorMode;
    }

    public boolean isWordFilterNotifyAdmins() {
        return wordFilterNotifyAdmins;
    }

    public boolean isEvasionDetectionEnabled() {
        return evasionDetectionEnabled;
    }

    public boolean isLeetspeakDetectionEnabled() {
        return leetspeakDetectionEnabled;
    }

    public String getMuteBypassPermission() {
        return muteBypassPermission;
    }

    public boolean isMuteBlockingCommands() {
        return muteBlockingCommands;
    }

    public boolean isLinkBlockerEnabled() {
        return linkBlockerEnabled;
    }

    public String getLinkBypassPermission() {
        return linkBypassPermission;
    }

    public List<String> getLinkPatterns() {
        return linkPatterns;
    }

    public boolean isPingEnabled() {
        return pingEnabled;
    }

    public String getPingTriggerSymbol() {
        return pingTriggerSymbol;
    }

    public String getPingHighlightTemplate() {
        return pingHighlightTemplate;
    }

    public boolean isAllowSelfPing() {
        return allowSelfPing;
    }

    public String getPingImmunePermission() {
        return pingImmunePermission;
    }

    public boolean isPingIgnorable() {
        return pingIgnorable;
    }

    public boolean isCooldownEnabled() {
        return cooldownEnabled;
    }

    public int getChatCooldownSeconds() {
        return chatCooldownSeconds;
    }

    public int getCommandCooldownSeconds() {
        return commandCooldownSeconds;
    }

    public String getCooldownBypassPermission() {
        return cooldownBypassPermission;
    }

    public List<String> getCooldownCommands() {
        return cooldownCommands;
    }

    public boolean isAntiCapsEnabled() {
        return antiCapsEnabled;
    }

    public int getAntiCapsMaxPercentage() {
        return antiCapsMaxPercentage;
    }

    public int getAntiCapsMinLength() {
        return antiCapsMinLength;
    }

    public boolean isAntiCapsAutoCorrect() {
        return antiCapsAutoCorrect;
    }

    public String getAntiCapsBypassPermission() {
        return antiCapsBypassPermission;
    }

    public boolean isJoinDelayEnabled() {
        return joinDelayEnabled;
    }

    public int getJoinDelaySeconds() {
        return joinDelaySeconds;
    }

    public String getJoinDelayBypassPermission() {
        return joinDelayBypassPermission;
    }

    public boolean isMaxLengthEnabled() {
        return maxLengthEnabled;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public String getBossbarColor() {
        return bossbarColor;
    }

    public boolean isBroadcastUsePrefix() {
        return broadcastUsePrefix;
    }

    public String getBroadcastDefaultType() {
        return broadcastDefaultType;
    }

    public String getBossbarStyle() {
        return bossbarStyle;
    }

    public int getBossbarDurationSeconds() {
        return bossbarDurationSeconds;
    }

    public int getTitleFadeIn() {
        return titleFadeIn;
    }

    public int getTitleStay() {
        return titleStay;
    }

    public int getTitleFadeOut() {
        return titleFadeOut;
    }

    /** Not cached: used only for arbitrary, admin-driven lookups (e.g. enumerating chat colors in the editor GUI), never on the chat hot path. */
    public ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }

    public MessageDisplayType getMuteNotificationType() {
        return muteNotificationType;
    }

    public MessageDisplayType getPingNotificationType() {
        return pingNotificationType;
    }

    public MessageDisplayType getWordFilterNotificationType() {
        return wordFilterNotificationType;
    }

    public MessageDisplayType getLinkBlockerNotificationType() {
        return linkBlockerNotificationType;
    }

    public MessageDisplayType getCooldownNotificationType() {
        return cooldownNotificationType;
    }

    public MessageDisplayType getAntiCapsNotificationType() {
        return antiCapsNotificationType;
    }

    public MessageDisplayType getJoinDelayNotificationType() {
        return joinDelayNotificationType;
    }

    public MessageDisplayType getMaxLengthNotificationType() {
        return maxLengthNotificationType;
    }

    public MessageDisplayType getChatClearNotificationType() {
        return chatClearNotificationType;
    }

    public boolean isUpdateCheckerEnabled() {
        return updateCheckerEnabled;
    }

    public int getUpdateCheckerIntervalMinutes() {
        return updateCheckerIntervalMinutes;
    }

    public boolean isPrivateMessagesEnabled() {
        return privateMessagesEnabled;
    }

    public int getChatClearLines() {
        return chatClearLines;
    }

    public boolean isChatColorGuiEnabled() {
        return chatColorGuiEnabled;
    }

    public String getChatColorGuiUsePermission() {
        return chatColorGuiUsePermission;
    }

    public boolean isChatLogEnabled() {
        return chatLogEnabled;
    }

    public boolean isChatLogBlockedMessages() {
        return chatLogBlockedMessages;
    }

    public int getChatLogRetentionDays() {
        return chatLogRetentionDays;
    }

    public int getChatLogMaxEntries() {
        return chatLogMaxEntries;
    }

    public int getChatLogEntriesPerPage() {
        return chatLogEntriesPerPage;
    }

    public String getChatLogDateFormat() {
        return chatLogDateFormat;
    }

    public boolean isAutoMessagesEnabled() {
        return autoMessagesEnabled;
    }

    public int getAutoMessagesIntervalSeconds() {
        return autoMessagesIntervalSeconds;
    }

    public boolean isAutoMessagesRandomOrder() {
        return autoMessagesRandomOrder;
    }

    public int getAutoMessagesMinPlayers() {
        return autoMessagesMinPlayers;
    }

    public BroadcastManager.Type getAutoMessagesDisplayType() {
        return autoMessagesDisplayType;
    }

    public String getAutoMessagesPrefix() {
        return autoMessagesPrefix;
    }

    public List<List<String>> getAutoMessages() {
        return autoMessages;
    }
}
