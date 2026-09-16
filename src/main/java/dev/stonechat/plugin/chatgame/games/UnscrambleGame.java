package dev.stonechat.plugin.chatgame.games;

import dev.stonechat.plugin.chatgame.ChatGame;
import dev.stonechat.plugin.chatgame.GameRound;
import dev.stonechat.plugin.chatgame.GameType;
import dev.stonechat.plugin.chatgame.RewardConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Unscramble Word: "Unscramble: e-l-p-p-a" -> apple. Picks a random word
 * from the configured list and shuffles its letters (with a hyphen between
 * each letter, guaranteed to never equal the original arrangement).
 */
public class UnscrambleGame extends ChatGame {

    private final List<String> words;

    public UnscrambleGame(String id, boolean enabled, int durationSeconds, String permission, boolean caseSensitive,
                           int minPlayersOnline, boolean hintEnabled, String notificationTypeOverride, RewardConfig reward,
                           List<String> words) {
        super(id, GameType.UNSCRAMBLE, enabled, durationSeconds, permission, caseSensitive, minPlayersOnline,
                hintEnabled, notificationTypeOverride, reward);
        this.words = words == null ? List.of() : words;
    }

    @Override
    public GameRound generateRound(Random random) {
        if (words.isEmpty()) return null;
        String word = words.get(random.nextInt(words.size()));

        List<Character> letters = new ArrayList<>();
        for (char c : word.toCharArray()) letters.add(c);

        String scrambledJoined = word;
        for (int attempt = 0; attempt < 10 && scrambledJoined.equalsIgnoreCase(word); attempt++) {
            Collections.shuffle(letters, random);
            StringBuilder sb = new StringBuilder();
            for (char c : letters) sb.append(c);
            scrambledJoined = sb.toString();
        }

        StringBuilder display = new StringBuilder();
        for (int i = 0; i < scrambledJoined.length(); i++) {
            if (i > 0) display.append('-');
            display.append(scrambledJoined.charAt(i));
        }

        return new GameRound("Unscramble: " + display, List.of(word));
    }

    public List<String> getWords() {
        return words;
    }
}
