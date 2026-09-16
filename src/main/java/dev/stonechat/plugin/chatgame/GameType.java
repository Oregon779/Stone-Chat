package dev.stonechat.plugin.chatgame;

/**
 * The category a {@link ChatGame} belongs to. The five built-in types each
 * have a dedicated subclass that knows how to generate a round for that
 * type of game; CUSTOM games are fully admin-defined question/answer sets.
 */
public enum GameType {
    MATH,
    UNSCRAMBLE,
    FAST_TYPING,
    TRIVIA,
    FILL_BLANKS,
    CUSTOM
}
