package dev.stonechat.plugin.chatgame.games;

import dev.stonechat.plugin.chatgame.ChatGame;
import dev.stonechat.plugin.chatgame.GameRound;
import dev.stonechat.plugin.chatgame.GameType;
import dev.stonechat.plugin.chatgame.RewardConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fill in the Blanks: "M_ne_ra_t" -> Minecraft. Picks a random word and
 * blanks out a configurable percentage of its letters (always keeping the
 * first and last letter visible, so the word stays recognizable in shape).
 */
public class FillBlanksGame extends ChatGame {

    private final List<String> words;
    private final int blankPercentage;

    public FillBlanksGame(String id, boolean enabled, int durationSeconds, String permission, boolean caseSensitive,
                           int minPlayersOnline, boolean hintEnabled, String notificationTypeOverride, RewardConfig reward,
                           List<String> words, int blankPercentage) {
        super(id, GameType.FILL_BLANKS, enabled, durationSeconds, permission, caseSensitive, minPlayersOnline,
                hintEnabled, notificationTypeOverride, reward);
        this.words = words == null ? List.of() : words;
        this.blankPercentage = Math.max(10, Math.min(90, blankPercentage));
    }

    @Override
    public GameRound generateRound(Random random) {
        if (words.isEmpty()) return null;
        String word = words.get(random.nextInt(words.size()));

        char[] chars = word.toCharArray();
        List<Integer> blankableIndexes = new ArrayList<>();
        // Keep the first and last letter visible so the word's length/shape stays recognizable.
        for (int i = 1; i < chars.length - 1; i++) {
            blankableIndexes.add(i);
        }
        java.util.Collections.shuffle(blankableIndexes, random);

        int blanksToPlace = Math.max(1, (chars.length * blankPercentage) / 100);
        for (int i = 0; i < Math.min(blanksToPlace, blankableIndexes.size()); i++) {
            chars[blankableIndexes.get(i)] = '_';
        }

        String display = new String(chars);
        return new GameRound("Fill in the blanks: " + display, List.of(word));
    }

    public List<String> getWords() {
        return words;
    }

    public int getBlankPercentage() {
        return blankPercentage;
    }
}
