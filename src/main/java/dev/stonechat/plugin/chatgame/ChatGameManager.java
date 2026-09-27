package dev.stonechat.plugin.chatgame;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.chatgame.games.CustomGame;
import dev.stonechat.plugin.chatgame.games.FastTypingGame;
import dev.stonechat.plugin.chatgame.games.FillBlanksGame;
import dev.stonechat.plugin.chatgame.games.MathGame;
import dev.stonechat.plugin.chatgame.games.TriviaGame;
import dev.stonechat.plugin.chatgame.games.UnscrambleGame;
import dev.stonechat.plugin.config.ConfigUpdater;
import dev.stonechat.plugin.model.MessageDisplayType;
import dev.stonechat.plugin.util.ColorUtil;
import dev.stonechat.plugin.util.VaultEconomyUtil;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Owns chatgames.yml end to end and runs the currently active round, if
 * any. Every game (the five built-ins and any custom ones) is parsed into
 * a typed {@link ChatGame} subclass on {@link #load()}; the settings
 * editor writes directly to the underlying YAML paths and triggers a full
 * plugin reload, which calls {@link #load()} again and rebuilds the
 * registry from disk.
 */
public class ChatGameManager {

    private final StoneChat plugin;
    private final Map<String, ChatGame> games = new LinkedHashMap<>();
    private final Random random = new Random();
    private final Object roundLock = new Object();

    private File file;
    private volatile YamlConfiguration config;
    private volatile BukkitTask autoStartTask;
    private volatile long lastGameEndedAtMillis = 0L;
    private volatile ActiveRound activeRound;

    public ChatGameManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    public static class ActiveRound {
        public final ChatGame game;
        public final GameRound round;
        public BukkitTask timeoutTask;
        public BukkitTask hintTask;

        public ActiveRound(ChatGame game, GameRound round) {
            this.game = game;
            this.round = round;
        }
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "chatgames.yml");
        this.config = ConfigUpdater.updateFile(plugin, file, "chatgames.yml", java.util.Set.of("custom-games"));

        games.clear();
        loadSection("default-games");
        loadSection("custom-games");
    }

    private void loadSection(String sectionPath) {
        ConfigurationSection section = config.getConfigurationSection(sectionPath);
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection gameSection = section.getConfigurationSection(id);
            if (gameSection == null) continue;
            ChatGame game = parseGame(id, gameSection);
            if (game != null) games.put(id.toLowerCase(), game);
        }
    }

    private ChatGame parseGame(String id, ConfigurationSection section) {
        GameType type;
        try {
            type = GameType.valueOf(section.getString("type", "CUSTOM").toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown chat game type for '" + id + "', skipping.");
            return null;
        }

        boolean enabled = section.getBoolean("enabled", true);
        int duration = section.getInt("duration-seconds", 0);
        if (duration <= 0) duration = getAnswerTimeSeconds();
        String permission = section.getString("permission", "");
        boolean caseSensitive = section.getBoolean("case-sensitive", false);
        int minPlayers = section.getInt("min-players-online", 0);
        boolean hintEnabled = section.getBoolean("hint-enabled", true);
        String notificationOverride = section.getString("notification-type", "DEFAULT");
        RewardConfig reward = parseReward(section.getConfigurationSection("reward"));

        return switch (type) {
            case MATH -> new MathGame(id, enabled, duration, permission, caseSensitive, minPlayers, hintEnabled,
                    notificationOverride, reward,
                    section.getInt("min-number", 1), section.getInt("max-number", 50), section.getStringList("operators"));

            case UNSCRAMBLE -> new UnscrambleGame(id, enabled, duration, permission, caseSensitive, minPlayers,
                    hintEnabled, notificationOverride, reward, section.getStringList("words"));

            case FAST_TYPING -> new FastTypingGame(id, enabled, duration, permission, caseSensitive, minPlayers,
                    hintEnabled, notificationOverride, reward, section.getStringList("phrases"));

            case FILL_BLANKS -> new FillBlanksGame(id, enabled, duration, permission, caseSensitive, minPlayers,
                    hintEnabled, notificationOverride, reward, section.getStringList("words"),
                    section.getInt("blank-percentage", 40));

            case TRIVIA -> new TriviaGame(id, enabled, duration, permission, caseSensitive, minPlayers, hintEnabled,
                    notificationOverride, reward,
                    parseQuestionEntries(section.getMapList("questions")).stream()
                            .map(e -> new TriviaGame.TriviaEntry(e.question(), e.answers())).toList());

            case CUSTOM -> new CustomGame(id, enabled, duration, permission, caseSensitive, minPlayers, hintEnabled,
                    notificationOverride, reward,
                    parseQuestionEntries(section.getMapList("questions")).stream()
                            .map(e -> new CustomGame.Entry(e.question(), e.answers())).toList());
        };
    }

    private record RawQuestion(String question, List<String> answers) {
    }

    private List<RawQuestion> parseQuestionEntries(List<Map<?, ?>> raw) {
        List<RawQuestion> result = new ArrayList<>();
        for (Map<?, ?> entry : raw) {
            Object q = entry.get("question");
            if (q == null) continue;
            String question = q.toString().trim();
            if (question.isEmpty()) continue;

            List<String> answers = new ArrayList<>();
            Object answersObj = entry.get("answers");
            if (answersObj instanceof List<?> list) {
                for (Object a : list) {
                    if (a != null && !a.toString().isBlank()) answers.add(a.toString().trim());
                }
            }
            if (answers.isEmpty()) continue;
            result.add(new RawQuestion(question, answers));
        }
        return result;
    }

    private RewardConfig parseReward(ConfigurationSection section) {
        RewardConfig globalDefault = parseDefaultReward();
        if (section == null || !section.getBoolean("override", false)) {
            return globalDefault;
        }
        return new RewardConfig(
                section.getStringList("console-commands"),
                section.getDouble("vault-money", 0),
                section.getBoolean("sound.enabled", true),
                section.getString("sound.name", "ENTITY_PLAYER_LEVELUP"),
                (float) section.getDouble("sound.volume", 1.0),
                (float) section.getDouble("sound.pitch", 1.2),
                section.getBoolean("particle.enabled", false),
                section.getString("particle.name", "TOTEM_OF_UNDYING"),
                section.getInt("particle.count", 20)
        );
    }

    private RewardConfig parseDefaultReward() {
        ConfigurationSection section = config.getConfigurationSection("chat-games.default-reward");
        if (section == null) return RewardConfig.none();
        return new RewardConfig(
                section.getStringList("console-commands"),
                section.getDouble("vault-money", 0),
                section.getBoolean("sound.enabled", true),
                section.getString("sound.name", "ENTITY_PLAYER_LEVELUP"),
                (float) section.getDouble("sound.volume", 1.0),
                (float) section.getDouble("sound.pitch", 1.2),
                section.getBoolean("particle.enabled", false),
                section.getString("particle.name", "TOTEM_OF_UNDYING"),
                section.getInt("particle.count", 20)
        );
    }

    public boolean isModuleEnabled() {
        return config.getBoolean("chat-games.enabled", true);
    }

    public int getIntervalSeconds() {
        return config.getInt("chat-games.interval-seconds", 300);
    }

    public int getAnswerTimeSeconds() {
        return Math.max(5, config.getInt("chat-games.answer-time-seconds", 60));
    }

    public int getGlobalMinPlayersOnline() {
        return config.getInt("chat-games.min-players-online", 1);
    }

    private MessageDisplayType getGlobalNotificationType() {
        return MessageDisplayType.fromConfig(config.getString("chat-games.notification-type", "CHAT"), MessageDisplayType.CHAT);
    }

    private MessageDisplayType resolveNotificationType(ChatGame game) {
        String override = game.getNotificationTypeOverride();
        if (override == null || override.equalsIgnoreCase("DEFAULT")) {
            return getGlobalNotificationType();
        }
        return MessageDisplayType.fromConfig(override, getGlobalNotificationType());
    }

    public YamlConfiguration rawConfig() {
        return config;
    }

    public void set(String path, Object value) {
        config.set(path, value);
        save();
    }

    private void save() {
        try {
            dev.stonechat.plugin.util.DataFiles.writeAtomically(file.toPath(), config.saveToString());
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save chatgames.yml: " + e.getMessage());
        }
    }

    public ChatGame getGame(String id) {
        return games.get(id.toLowerCase());
    }

    public Map<String, ChatGame> getGames() {
        return games;
    }

    public List<ChatGame> getDefaultGames() {
        return games.values().stream().filter(ChatGame::isDefaultGame).toList();
    }

    public List<ChatGame> getCustomGames() {
        return games.values().stream().filter(g -> !g.isDefaultGame()).toList();
    }

    public boolean deleteCustomGame(String id) {
        ChatGame game = getGame(id);
        if (game == null || game.isDefaultGame()) return false;
        games.remove(id.toLowerCase());
        config.set("custom-games." + id, null);
        save();
        return true;
    }

    public void addCustomGameEntry(String id, String question, List<String> answers) {
        String base = "custom-games." + id;
        if (!config.contains(base)) {
            config.set(base + ".type", "CUSTOM");
            config.set(base + ".enabled", true);
            config.set(base + ".duration-seconds", 0);
            config.set(base + ".permission", "");
            config.set(base + ".case-sensitive", false);
            config.set(base + ".min-players-online", 0);
            config.set(base + ".hint-enabled", true);
            config.set(base + ".notification-type", "DEFAULT");
            config.set(base + ".reward.override", false);
        }
        List<Map<String, Object>> questions = new ArrayList<>();
        for (Map<?, ?> raw : config.getMapList(base + ".questions")) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : raw.entrySet()) {
                copy.put(String.valueOf(e.getKey()), e.getValue());
            }
            questions.add(copy);
        }
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("question", question);
        entry.put("answers", answers);
        questions.add(entry);
        config.set(base + ".questions", questions);
        save();
    }

    public boolean removeLastQuestionEntry(String id) {
        ChatGame existing = getGame(id);
        String path = (existing != null && existing.isDefaultGame() ? "default-games." : "custom-games.") + id + ".questions";
        List<Map<?, ?>> questions = new ArrayList<>(config.getMapList(path));
        if (questions.size() <= 1) return false;
        questions.remove(questions.size() - 1);
        config.set(path, questions);
        save();
        return true;
    }

    public boolean isRunning() {
        return activeRound != null;
    }

    public ActiveRound getActiveRound() {
        return activeRound;
    }

    public int getRemainingCooldownSeconds() {
        long cooldownMillis = Math.max(0, config.getInt("chat-games.cooldown-after-game-seconds", 0)) * 1000L;
        if (cooldownMillis == 0) return 0;
        long elapsed = System.currentTimeMillis() - lastGameEndedAtMillis;
        if (elapsed >= cooldownMillis) return 0;
        return (int) Math.ceil((cooldownMillis - elapsed) / 1000.0);
    }

    public boolean start(ChatGame game) {
        if (!isModuleEnabled() || !game.isEnabled()) return false;
        if (getRemainingCooldownSeconds() > 0) return false;
        if (game.getMinPlayersOnline() > 0 && Bukkit.getOnlinePlayers().size() < game.getMinPlayersOnline()) return false;

        GameRound round = game.generateRound(random);
        if (round == null) return false;

        MessageDisplayType notificationType = resolveNotificationType(game);

        synchronized (roundLock) {
            if (activeRound != null) return false;
            ActiveRound current = new ActiveRound(game, round);
            activeRound = current;

            current.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                synchronized (roundLock) {
                    if (activeRound != current) return;
                    if (current.hintTask != null) current.hintTask.cancel();
                    activeRound = null;
                    lastGameEndedAtMillis = System.currentTimeMillis();
                }
                broadcast(notificationType, getMessage("timeout-message"));
            }, game.getDurationSeconds() * 20L);

            boolean hintsGloballyEnabled = config.getBoolean("chat-games.hint.enabled", true);
            if (hintsGloballyEnabled && game.isHintEnabled()) {
                int afterSeconds = Math.max(1, config.getInt("chat-games.hint.after-seconds", 20));
                if (afterSeconds < game.getDurationSeconds()) {
                    String firstAnswer = round.acceptedAnswers().isEmpty() ? "" : round.acceptedAnswers().get(0);
                    current.hintTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (activeRound != current) return;
                        String hint = getMessage("hint-message")
                                .replace("%firstletter%", firstAnswer.isEmpty() ? "?" : String.valueOf(firstAnswer.charAt(0)))
                                .replace("%length%", String.valueOf(firstAnswer.length()));
                        broadcast(notificationType, hint);
                    }, afterSeconds * 20L);
                }
            }
        }

        String startMessage = getMessage("start-message").replace("%question%", round.question());
        broadcast(notificationType, startMessage);
        return true;
    }

    /** Called from the async chat thread with the text exactly as the player typed it. */
    public boolean checkAnswer(Player player, String typedMessage) {
        RewardConfig reward;
        MessageDisplayType notificationType;

        synchronized (roundLock) {
            if (activeRound == null) return false;
            if (!activeRound.round.matches(typedMessage, activeRound.game.isCaseSensitive())) return false;

            reward = activeRound.game.getReward();
            notificationType = resolveNotificationType(activeRound.game);

            if (activeRound.timeoutTask != null) activeRound.timeoutTask.cancel();
            if (activeRound.hintTask != null) activeRound.hintTask.cancel();
            activeRound = null;
            lastGameEndedAtMillis = System.currentTimeMillis();
        }

        String winMessage = getMessage("win-message")
                .replace("%player%", player.getName())
                .replace("%answer%", ColorUtil.sanitizeUserInput(typedMessage.trim()));
        // Economy deposits, particles and console commands must not run on the async chat thread.
        runOnMainThread(() -> {
            broadcast(notificationType, winMessage);
            giveReward(player, reward);
        });
        return true;
    }

    private void runOnMainThread(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public boolean stop(String stoppedBy) {
        synchronized (roundLock) {
            if (activeRound == null) return false;
            if (activeRound.timeoutTask != null) activeRound.timeoutTask.cancel();
            if (activeRound.hintTask != null) activeRound.hintTask.cancel();
            activeRound = null;
            lastGameEndedAtMillis = System.currentTimeMillis();
        }
        broadcast(getGlobalNotificationType(), getMessage("stop-message").replace("%player%", stoppedBy));
        return true;
    }

    private String getMessage(String key) {
        return config.getString("chat-games.broadcast." + key, "");
    }

    private void broadcast(MessageDisplayType type, String rawMessage) {
        String withPrefix = (type == MessageDisplayType.CHAT)
                ? config.getString("chat-games.broadcast.prefix", "") + rawMessage
                : rawMessage;

        switch (type) {
            case CHAT -> Bukkit.broadcast(ColorUtil.parse(withPrefix));
            case ACTIONBAR -> {
                for (Player p : Bukkit.getOnlinePlayers()) p.sendActionBar(ColorUtil.parse(withPrefix));
            }
            case TITLE -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.showTitle(net.kyori.adventure.title.Title.title(ColorUtil.parse(withPrefix), net.kyori.adventure.text.Component.empty()));
                }
            }
            case BOSSBAR -> {
                net.kyori.adventure.bossbar.BossBar bar = net.kyori.adventure.bossbar.BossBar.bossBar(
                        ColorUtil.parse(withPrefix), 1.0f,
                        net.kyori.adventure.bossbar.BossBar.Color.YELLOW, net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS);
                for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(bar);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bar);
                }, 100L); // visible for 5 seconds
            }
        }
    }

    private void giveReward(Player winner, RewardConfig reward) {
        if (reward == null) return;

        for (String command : reward.consoleCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", winner.getName()));
        }

        if (reward.vaultMoney() > 0) {
            VaultEconomyUtil.deposit(winner, reward.vaultMoney());
        }

        if (!winner.isOnline()) return;

        if (reward.soundEnabled()) {
            try {
                Sound sound = Sound.valueOf(reward.soundName().toUpperCase());
                winner.playSound(winner.getLocation(), sound, reward.soundVolume(), reward.soundPitch());
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (reward.particleEnabled()) {
            Particle particle = resolveParticle(reward.particleName());
            if (particle == null) {
                plugin.getLogger().warning("Invalid chat game reward particle '" + reward.particleName() + "' - no particle is shown.");
                return;
            }
            try {
                winner.getWorld().spawnParticle(particle, winner.getLocation().add(0, 1, 0), Math.max(1, reward.particleCount()));
            } catch (IllegalArgumentException ignored) {
                // Particles that need extra data (DUST, ITEM, BLOCK, ...) can't be spawned from a name alone.
            }
        }
    }

    /** "TOTEM" is the pre-1.20.5 name that older chatgames.yml files still contain. */
    static Particle resolveParticle(String name) {
        if (name == null || name.isBlank()) return null;
        String key = name.trim().toUpperCase();
        if (key.equals("TOTEM")) key = "TOTEM_OF_UNDYING";
        try {
            return Particle.valueOf(key);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void startAutoScheduler() {
        stopAutoScheduler();
        if (!isModuleEnabled()) return;

        long intervalTicks = Math.max(20L, getIntervalSeconds() * 20L);
        autoStartTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (isRunning() || getRemainingCooldownSeconds() > 0) return;
            if (Bukkit.getOnlinePlayers().size() < getGlobalMinPlayersOnline()) return;

            ChatGame game = pickRandomEnabledGame();
            if (game != null) start(game);
        }, intervalTicks, intervalTicks);
    }

    public void stopAutoScheduler() {
        if (autoStartTask != null) {
            autoStartTask.cancel();
            autoStartTask = null;
        }
    }

    private ChatGame pickRandomEnabledGame() {
        List<ChatGame> enabled = games.values().stream().filter(ChatGame::isEnabled).toList();
        if (enabled.isEmpty()) return null;
        return enabled.get(random.nextInt(enabled.size()));
    }
}
