package dev.stonechat.plugin.chatgame.games;

import dev.stonechat.plugin.chatgame.ChatGame;
import dev.stonechat.plugin.chatgame.GameRound;
import dev.stonechat.plugin.chatgame.GameType;
import dev.stonechat.plugin.chatgame.RewardConfig;

import java.util.List;
import java.util.Random;

/**
 * A fully custom game created by an admin: one or more question/answer
 * pairs (each with any number of accepted answers), one picked at random
 * per round - functionally identical to {@link TriviaGame} but kept as its
 * own type so custom and default games are clearly separated everywhere
 * (the editor GUI, the config file, and /chatgame commands).
 */
public class CustomGame extends ChatGame {

    public record Entry(String question, List<String> answers) {
    }

    private final List<Entry> entries;

    public CustomGame(String id, boolean enabled, int durationSeconds, String permission, boolean caseSensitive,
                       int minPlayersOnline, boolean hintEnabled, String notificationTypeOverride, RewardConfig reward,
                       List<Entry> entries) {
        super(id, GameType.CUSTOM, enabled, durationSeconds, permission, caseSensitive, minPlayersOnline,
                hintEnabled, notificationTypeOverride, reward);
        this.entries = entries == null ? List.of() : entries;
    }

    @Override
    public GameRound generateRound(Random random) {
        if (entries.isEmpty()) return null;
        Entry entry = entries.get(random.nextInt(entries.size()));
        return new GameRound(entry.question(), entry.answers());
    }

    public List<Entry> getEntries() {
        return entries;
    }
}
