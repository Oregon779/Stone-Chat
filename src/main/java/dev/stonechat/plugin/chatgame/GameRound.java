package dev.stonechat.plugin.chatgame;

import java.util.List;

/**
 * A single generated round: the question text broadcast to players, and
 * every string that counts as a correct answer. Having a list (rather than
 * a single answer) lets trivia/custom games accept synonyms, and lets fast
 * typing / fill-in-the-blanks accept minor case variants if configured.
 */
public record GameRound(String question, List<String> acceptedAnswers) {

    /** @return true if {@code input} matches any accepted answer, trimmed and optionally case-insensitive. */
    public boolean matches(String input, boolean caseSensitive) {
        String trimmed = input.trim();
        for (String answer : acceptedAnswers) {
            if (answer == null) continue;
            boolean matches = caseSensitive
                    ? trimmed.equals(answer.trim())
                    : trimmed.equalsIgnoreCase(answer.trim());
            if (matches) return true;
        }
        return false;
    }
}
