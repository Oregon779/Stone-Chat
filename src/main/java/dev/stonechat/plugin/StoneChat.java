package dev.stonechat.plugin;

import dev.stonechat.plugin.command.BroadcastCommand;
import dev.stonechat.plugin.command.ChatClearCommand;
import dev.stonechat.plugin.command.ChatGameCommand;
import dev.stonechat.plugin.command.ChatLogCommand;
import dev.stonechat.plugin.command.ChatMuteCommand;
import dev.stonechat.plugin.command.IgnoreCommand;
import dev.stonechat.plugin.command.MessageCommand;
import dev.stonechat.plugin.command.ChatCommand;
import dev.stonechat.plugin.command.ReplyCommand;
import dev.stonechat.plugin.command.StoneChatCommand;
import dev.stonechat.plugin.listener.BroadcastChatInputListener;
import dev.stonechat.plugin.listener.ChatColorGuiListener;
import dev.stonechat.plugin.listener.EditorGuiListener;
import dev.stonechat.plugin.listener.PlayerChatListener;
import dev.stonechat.plugin.listener.PlayerCommandListener;
import dev.stonechat.plugin.listener.PlayerJoinListener;
import dev.stonechat.plugin.listener.PlayerQuitListener;
import dev.stonechat.plugin.manager.AnvilInputManager;
import dev.stonechat.plugin.manager.AutoMessageManager;
import dev.stonechat.plugin.manager.BroadcastManager;
import dev.stonechat.plugin.manager.CapsManager;
import dev.stonechat.plugin.manager.ChatColorGuiManager;
import dev.stonechat.plugin.manager.ChatFormatManager;
import dev.stonechat.plugin.manager.ChatLogManager;
import dev.stonechat.plugin.manager.GuiConfigManager;
import dev.stonechat.plugin.chatgame.ChatGameManager;
import dev.stonechat.plugin.manager.ConfigManager;
import dev.stonechat.plugin.manager.CooldownManager;
import dev.stonechat.plugin.manager.IgnoreManager;
import dev.stonechat.plugin.manager.JoinDelayManager;
import dev.stonechat.plugin.manager.LanguageManager;
import dev.stonechat.plugin.manager.LinkFilterManager;
import dev.stonechat.plugin.manager.MuteManager;
import dev.stonechat.plugin.manager.NotificationManager;
import dev.stonechat.plugin.manager.PingManager;
import dev.stonechat.plugin.manager.PlayerColorManager;
import dev.stonechat.plugin.manager.PlayerSettingsGuiManager;
import dev.stonechat.plugin.manager.PendingBroadcastManager;
import dev.stonechat.plugin.manager.PrivateMessageManager;
import dev.stonechat.plugin.manager.PunishmentManager;
import dev.stonechat.plugin.manager.SettingsEditorManager;
import dev.stonechat.plugin.manager.SoundManager;
import dev.stonechat.plugin.manager.UpdateChecker;
import dev.stonechat.plugin.manager.WordFilterManager;
import dev.stonechat.plugin.util.DataFiles;
import org.bukkit.plugin.java.JavaPlugin;

public class StoneChat extends JavaPlugin {

    private DataFiles dataFiles;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private WordFilterManager wordFilterManager;
    private MuteManager muteManager;
    private LinkFilterManager linkFilterManager;
    private PingManager pingManager;
    private CooldownManager cooldownManager;
    private CapsManager capsManager;
    private JoinDelayManager joinDelayManager;
    private ChatFormatManager chatFormatManager;
    private BroadcastManager broadcastManager;
    private PendingBroadcastManager pendingBroadcastManager;
    private ChatGameManager chatGameManager;
    private NotificationManager notificationManager;
    private SoundManager soundManager;
    private PunishmentManager punishmentManager;
    private PlayerColorManager playerColorManager;
    private PlayerSettingsGuiManager playerSettingsGuiManager;
    private ChatColorGuiManager chatColorGuiManager;
    private GuiConfigManager guiConfigManager;
    private PrivateMessageManager privateMessageManager;
    private IgnoreManager ignoreManager;
    private AnvilInputManager anvilInputManager;
    private SettingsEditorManager settingsEditorManager;
    private UpdateChecker updateChecker;
    private ChatLogManager chatLogManager;
    private AutoMessageManager autoMessageManager;

    @Override
    public void onEnable() {
        this.dataFiles = new DataFiles(getLogger());
        getLogger().info("Loading configuration...");
        this.configManager = new ConfigManager(this);
        this.languageManager = new LanguageManager(this);
        this.languageManager.load(configManager.getLanguage());
        getLogger().info("Loaded config.yml and messages.yml (language: " + languageManager.getLanguage() + ").");

        getLogger().info("Initializing managers...");
        this.wordFilterManager = new WordFilterManager(this);
        this.muteManager = new MuteManager();
        this.linkFilterManager = new LinkFilterManager(this);
        this.notificationManager = new NotificationManager(this);
        this.soundManager = new SoundManager(this);
        this.punishmentManager = new PunishmentManager(this);
        this.pingManager = new PingManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.capsManager = new CapsManager(this);
        this.joinDelayManager = new JoinDelayManager(this);
        this.chatFormatManager = new ChatFormatManager(this);
        this.broadcastManager = new BroadcastManager(this);
        this.pendingBroadcastManager = new PendingBroadcastManager();
        this.chatGameManager = new ChatGameManager(this);
        this.chatGameManager.load();
        this.chatGameManager.startAutoScheduler();
        this.playerColorManager = new PlayerColorManager(this);
        this.guiConfigManager = new GuiConfigManager(this);
        this.guiConfigManager.load();
        this.chatColorGuiManager = new ChatColorGuiManager(this);
        this.playerSettingsGuiManager = new PlayerSettingsGuiManager(this);
        this.privateMessageManager = new PrivateMessageManager(this);
        this.ignoreManager = new IgnoreManager(this);
        this.anvilInputManager = new AnvilInputManager(this);
        this.settingsEditorManager = new SettingsEditorManager(this);
        this.updateChecker = new UpdateChecker(this);
        this.chatLogManager = new ChatLogManager(this);
        this.autoMessageManager = new AutoMessageManager(this);
        this.autoMessageManager.start();
        getLogger().info("Managers initialized (word filter, mute, links, notifications, sounds, ping, cooldown, anti-caps, join delay, chat format, broadcast, chat games, chat colors, chat log, auto messages).");

        getLogger().info("Registering listeners...");
        registerListeners();

        getLogger().info("Registering commands...");
        registerCommands();

        getLogger().info("Checking soft dependencies...");
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI found - placeholders in chat format will be resolved.");
        } else {
            getLogger().info("PlaceholderAPI not found - only built-in placeholders will be available.");
        }

        getLogger().info("Starting update checker...");
        updateChecker.start();

        getLogger().info("Stone Chat has been enabled - chat is now managed by this plugin.");
    }

    @Override
    public void onDisable() {
        if (chatGameManager != null) {
            chatGameManager.stopAutoScheduler();
        }
        if (updateChecker != null) {
            updateChecker.shutdown();
        }
        if (notificationManager != null) {
            notificationManager.hideAllBossbars();
        }
        if (autoMessageManager != null) {
            autoMessageManager.stop();
        }
        if (chatLogManager != null) {
            chatLogManager.shutdown();
        }
        if (dataFiles != null) {
            dataFiles.shutdown();
        }
        getLogger().info("Stone Chat has been disabled.");
    }

    public void reload() {
        configManager.load();
        languageManager.load(configManager.getLanguage());
        chatGameManager.load();
        chatGameManager.startAutoScheduler();
        guiConfigManager.load();

        wordFilterManager.reload();
        pingManager.reload();
        linkFilterManager.reload();
        soundManager.reload();
        chatFormatManager.reload();
        chatColorGuiManager.reload();
        updateChecker.start();
        autoMessageManager.start();
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerChatListener(this), this);
        pm.registerEvents(new BroadcastChatInputListener(this), this);
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
        pm.registerEvents(new PlayerCommandListener(this), this);
        pm.registerEvents(new ChatColorGuiListener(this), this);
        pm.registerEvents(new EditorGuiListener(this), this);
        pm.registerEvents(updateChecker, this);
        getLogger().info("Listeners registered (chat, join, quit, command preprocess, chat color GUI, settings editor GUI, update checker).");
    }

    private void registerCommands() {
        StoneChatCommand stoneChatCommand = new StoneChatCommand(this);
        getCommand("stonechat").setExecutor(stoneChatCommand);
        getCommand("stonechat").setTabCompleter(stoneChatCommand);

        getCommand("chatmute").setExecutor(new ChatMuteCommand(this));

        ChatGameCommand chatGameCommand = new ChatGameCommand(this);
        getCommand("chatgame").setExecutor(chatGameCommand);
        getCommand("chatgame").setTabCompleter(chatGameCommand);

        BroadcastCommand broadcastCommand = new BroadcastCommand(this);
        getCommand("broadcast").setExecutor(broadcastCommand);
        getCommand("broadcast").setTabCompleter(broadcastCommand);

        MessageCommand messageCommand = new MessageCommand(this);
        getCommand("msg").setExecutor(messageCommand);
        getCommand("msg").setTabCompleter(messageCommand);

        getCommand("r").setExecutor(new ReplyCommand(this));

        getCommand("chatclear").setExecutor(new ChatClearCommand(this));

        IgnoreCommand ignoreCommand = new IgnoreCommand(this);
        getCommand("ignore").setExecutor(ignoreCommand);
        getCommand("ignore").setTabCompleter(ignoreCommand);

        ChatCommand chatCommand = new ChatCommand(this);
        getCommand("chat").setExecutor(chatCommand);
        getCommand("chat").setTabCompleter(chatCommand);

        ChatLogCommand chatLogCommand = new ChatLogCommand(this);
        getCommand("chatlog").setExecutor(chatLogCommand);
        getCommand("chatlog").setTabCompleter(chatLogCommand);

        getLogger().info("Commands registered (/stonechat, /chat, /msg, /r, /ignore, /chatmute, /chatclear, /broadcast, /chatgame, /chatlog).");
    }

    public DataFiles getDataFiles() {
        return dataFiles;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public WordFilterManager getWordFilterManager() {
        return wordFilterManager;
    }

    public MuteManager getMuteManager() {
        return muteManager;
    }

    public LinkFilterManager getLinkFilterManager() {
        return linkFilterManager;
    }

    public PingManager getPingManager() {
        return pingManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public CapsManager getCapsManager() {
        return capsManager;
    }

    public JoinDelayManager getJoinDelayManager() {
        return joinDelayManager;
    }

    public ChatFormatManager getChatFormatManager() {
        return chatFormatManager;
    }

    public BroadcastManager getBroadcastManager() {
        return broadcastManager;
    }

    public PendingBroadcastManager getPendingBroadcastManager() {
        return pendingBroadcastManager;
    }

    public ChatGameManager getChatGameManager() {
        return chatGameManager;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public PlayerColorManager getPlayerColorManager() {
        return playerColorManager;
    }

    public ChatColorGuiManager getChatColorGuiManager() {
        return chatColorGuiManager;
    }

    public GuiConfigManager getGuiConfigManager() {
        return guiConfigManager;
    }

    public PlayerSettingsGuiManager getPlayerSettingsGuiManager() {
        return playerSettingsGuiManager;
    }

    public PrivateMessageManager getPrivateMessageManager() {
        return privateMessageManager;
    }

    public IgnoreManager getIgnoreManager() {
        return ignoreManager;
    }

    public AnvilInputManager getAnvilInputManager() {
        return anvilInputManager;
    }

    public SettingsEditorManager getSettingsEditorManager() {
        return settingsEditorManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public ChatLogManager getChatLogManager() {
        return chatLogManager;
    }

    public AutoMessageManager getAutoMessageManager() {
        return autoMessageManager;
    }
}
