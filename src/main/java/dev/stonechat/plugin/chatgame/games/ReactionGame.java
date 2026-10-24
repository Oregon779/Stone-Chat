package dev.stonechat.plugin.chatgame.games;

import dev.stonechat.plugin.chatgame.ChatGame;
import dev.stonechat.plugin.chatgame.GameRound;
import dev.stonechat.plugin.chatgame.GameType;
import dev.stonechat.plugin.chatgame.RewardConfig;
import dev.stonechat.plugin.util.ColorUtil;

import java.util.HexFormat;
import java.util.List;
import java.util.Random;

/**
 * Reaction: a button appears in chat and whoever clicks it first wins. The click runs
 * "/stonechat react &lt;code&gt;" with a fresh random code per round, so nobody can answer
 * before the button exists. Rounds always show in chat, the only place a button can be clicked.
 */
public class ReactionGame extends ChatGame {

    public static final String COMMAND = "/stonechat react ";

    private final String button;
    private final String buttonHover;

    public ReactionGame(String id, boolean enabled, int durationSeconds, String permission, int minPlayersOnline,
                        String notificationTypeOverride, RewardConfig reward, String button, String buttonHover) {
        super(id, GameType.REACTION, enabled, durationSeconds, permission, true, minPlayersOnline,
                false, notificationTypeOverride, reward);
        this.button = button;
        this.buttonHover = buttonHover;
    }

    @Override
    public GameRound generateRound(Random random) {
        byte[] bytes = new byte[6];
        random.nextBytes(bytes);
        String code = HexFormat.of().formatHex(bytes);
        String question = "<click:run_command:'" + COMMAND + code + "'><hover:show_text:'"
                + buttonHover.replace("'", "") + "'>" + button + "</hover></click>";
        return new GameRound(question, List.of(code));
    }

    @Override
    public boolean supportsHints() {
        return false;
    }

    @Override
    public boolean requiresChat() {
        return true;
    }

    @Override
    public String shownAnswer(String typedAnswer) {
        return ColorUtil.stripToPlain(button);
    }

    public String getButton() {
        return button;
    }
}
