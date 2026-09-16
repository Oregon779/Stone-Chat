package dev.stonechat.plugin.chatgame;

import java.util.Random;

/**
 * A configured chat game. Holds everything every game type has in common
 * (id, whether it's enabled, how long a round lasts, permissions, reward
 * override, ...) and leaves generating the actual question/answer pair to
 * {@link #generateRound(Random)}, implemented by each concrete subclass:
 * {@link dev.stonechat.plugin.chatgame.games.MathGame},
 * {@link dev.stonechat.plugin.chatgame.games.UnscrambleGame},
 * {@link dev.stonechat.plugin.chatgame.games.FastTypingGame},
 * {@link dev.stonechat.plugin.chatgame.games.TriviaGame},
 * {@link dev.stonechat.plugin.chatgame.games.FillBlanksGame} and
 * {@link dev.stonechat.plugin.chatgame.games.CustomGame}.
 * <p>
 * Instances are immutable - every "edit" in the settings editor produces a
 * new instance via one of the {@code with...} methods and replaces the old
 * one in {@code ChatGameManager}'s registry.
 */
public abstract class ChatGame {

    private final String id;
    private final GameType type;
    private final boolean enabled;
    private final int durationSeconds;
    private final String permission;
    private final boolean caseSensitive;
    private final int minPlayersOnline;
    private final boolean hintEnabled;
    private final String notificationTypeOverride;
    private final RewardConfig reward;

    protected ChatGame(String id, GameType type, boolean enabled, int durationSeconds, String permission,
                        boolean caseSensitive, int minPlayersOnline, boolean hintEnabled,
                        String notificationTypeOverride, RewardConfig reward) {
        this.id = id;
        this.type = type;
        this.enabled = enabled;
        this.durationSeconds = durationSeconds;
        this.permission = permission == null ? "" : permission;
        this.caseSensitive = caseSensitive;
        this.minPlayersOnline = minPlayersOnline;
        this.hintEnabled = hintEnabled;
        this.notificationTypeOverride = notificationTypeOverride == null ? "DEFAULT" : notificationTypeOverride;
        this.reward = reward;
    }

    /** Builds the question and accepted answers for one round of this game. */
    public abstract GameRound generateRound(Random random);

    /** @return true if this game type is one of the five built-ins that ship with the plugin (as opposed to admin-created CUSTOM games). */
    public boolean isDefaultGame() {
        return type != GameType.CUSTOM;
    }

    public String getId() {
        return id;
    }

    public GameType getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public int getMinPlayersOnline() {
        return minPlayersOnline;
    }

    public boolean isHintEnabled() {
        return hintEnabled;
    }

    public String getNotificationTypeOverride() {
        return notificationTypeOverride;
    }

    public RewardConfig getReward() {
        return reward;
    }
}
