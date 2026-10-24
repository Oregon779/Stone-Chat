package dev.stonechat.plugin.chatgame.games;

import dev.stonechat.plugin.chatgame.ChatGame;
import dev.stonechat.plugin.chatgame.GameRound;
import dev.stonechat.plugin.chatgame.GameType;
import dev.stonechat.plugin.chatgame.RewardConfig;

import java.util.List;
import java.util.Random;

/**
 * Reverse: "Read it backwards: dnomaid" -> diamond. Picks a random word or phrase from the configured list
 * and shows it written backwards. Palindromes are skipped, their reversed form would give the answer away.
 */
public class ReverseGame extends ChatGame {

    private final List<String> words;
    private final String questionFormat;

    public ReverseGame(String id, boolean enabled, int durationSeconds, String permission, boolean caseSensitive,
                       int minPlayersOnline, boolean hintEnabled, String notificationTypeOverride, RewardConfig reward,
                       List<String> words, String questionFormat) {
        super(id, GameType.REVERSE, enabled, durationSeconds, permission, caseSensitive, minPlayersOnline,
                hintEnabled, notificationTypeOverride, reward);
        this.words = words == null ? List.of() : words.stream()
                .filter(word -> word != null && !word.isBlank() && !isPalindrome(word))
                .toList();
        this.questionFormat = questionFormat;
    }

    private static boolean isPalindrome(String word) {
        String letters = word.trim().toLowerCase();
        return new StringBuilder(letters).reverse().toString().equals(letters);
    }

    @Override
    public GameRound generateRound(Random random) {
        if (words.isEmpty()) return null;
        String word = words.get(random.nextInt(words.size())).trim();
        String reversed = new StringBuilder(word).reverse().toString();
        return new GameRound(questionFormat.replace("%word%", reversed), List.of(word));
    }

    public List<String> getWords() {
        return words;
    }
}
