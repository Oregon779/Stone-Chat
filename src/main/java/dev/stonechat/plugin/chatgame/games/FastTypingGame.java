package dev.stonechat.plugin.chatgame.games;

import dev.stonechat.plugin.chatgame.ChatGame;
import dev.stonechat.plugin.chatgame.GameRound;
import dev.stonechat.plugin.chatgame.GameType;
import dev.stonechat.plugin.chatgame.RewardConfig;

import java.util.List;
import java.util.Random;

/**
 * Fast Typing: displays a random word or phrase from the configured list;
 * whoever types it first (exactly as shown) wins. Simplest of the five
 * built-ins - no transformation of the source text at all, speed is the
 * whole point.
 */
public class FastTypingGame extends ChatGame {

    private final List<String> phrases;

    public FastTypingGame(String id, boolean enabled, int durationSeconds, String permission, boolean caseSensitive,
                           int minPlayersOnline, boolean hintEnabled, String notificationTypeOverride, RewardConfig reward,
                           List<String> phrases) {
        super(id, GameType.FAST_TYPING, enabled, durationSeconds, permission, caseSensitive, minPlayersOnline,
                hintEnabled, notificationTypeOverride, reward);
        this.phrases = phrases == null ? List.of() : phrases;
    }

    @Override
    public GameRound generateRound(Random random) {
        if (phrases.isEmpty()) return null;
        String phrase = phrases.get(random.nextInt(phrases.size()));
        return new GameRound("Type this as fast as you can: " + phrase, List.of(phrase));
    }

    public List<String> getPhrases() {
        return phrases;
    }
}
