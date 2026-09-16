package dev.stonechat.plugin.chatgame.games;

import dev.stonechat.plugin.chatgame.ChatGame;
import dev.stonechat.plugin.chatgame.GameRound;
import dev.stonechat.plugin.chatgame.GameType;
import dev.stonechat.plugin.chatgame.RewardConfig;

import java.util.List;
import java.util.Random;

/**
 * Trivia / Quiz: asks a random question from the configured bank. Each
 * entry can define multiple acceptable answers (synonyms, alternate
 * spellings, singular/plural, ...).
 */
public class TriviaGame extends ChatGame {

    /** One trivia entry: a question and every answer that counts as correct. */
    public record TriviaEntry(String question, List<String> answers) {
    }

    private final List<TriviaEntry> entries;

    public TriviaGame(String id, boolean enabled, int durationSeconds, String permission, boolean caseSensitive,
                       int minPlayersOnline, boolean hintEnabled, String notificationTypeOverride, RewardConfig reward,
                       List<TriviaEntry> entries) {
        super(id, GameType.TRIVIA, enabled, durationSeconds, permission, caseSensitive, minPlayersOnline,
                hintEnabled, notificationTypeOverride, reward);
        this.entries = entries == null ? List.of() : entries;
    }

    @Override
    public GameRound generateRound(Random random) {
        if (entries.isEmpty()) return null;
        TriviaEntry entry = entries.get(random.nextInt(entries.size()));
        return new GameRound(entry.question(), entry.answers());
    }

    public List<TriviaEntry> getEntries() {
        return entries;
    }
}
