package dev.stonechat.plugin.chatgame.games;

import dev.stonechat.plugin.chatgame.ChatGame;
import dev.stonechat.plugin.chatgame.GameRound;
import dev.stonechat.plugin.chatgame.GameType;
import dev.stonechat.plugin.chatgame.RewardConfig;

import java.util.List;
import java.util.Random;

/**
 * Math Solver: "Solve: 45 + 17". The number range and allowed operators are
 * fully configurable. Division falls back to multiplication whenever it
 * wouldn't produce a whole number, so the answer is always a simple integer.
 */
public class MathGame extends ChatGame {

    private final int minNumber;
    private final int maxNumber;
    private final List<String> operators;

    public MathGame(String id, boolean enabled, int durationSeconds, String permission, boolean caseSensitive,
                     int minPlayersOnline, boolean hintEnabled, String notificationTypeOverride, RewardConfig reward,
                     int minNumber, int maxNumber, List<String> operators) {
        super(id, GameType.MATH, enabled, durationSeconds, permission, caseSensitive, minPlayersOnline,
                hintEnabled, notificationTypeOverride, reward);
        this.minNumber = minNumber;
        this.maxNumber = Math.max(minNumber, maxNumber);
        this.operators = (operators == null || operators.isEmpty()) ? List.of("+", "-", "*") : operators;
    }

    @Override
    public GameRound generateRound(Random random) {
        int a = minNumber + random.nextInt(maxNumber - minNumber + 1);
        int b = minNumber + random.nextInt(maxNumber - minNumber + 1);
        String op = operators.get(random.nextInt(operators.size()));

        int result = switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "/" -> (b != 0 && a % b == 0) ? a / b : a * b; // avoid division by zero / non-integer results
            default -> a * b;
        };

        String question = "Solve: " + a + " " + op + " " + b;
        return new GameRound(question, List.of(String.valueOf(result)));
    }

    public int getMinNumber() {
        return minNumber;
    }

    public int getMaxNumber() {
        return maxNumber;
    }

    public List<String> getOperators() {
        return operators;
    }
}
