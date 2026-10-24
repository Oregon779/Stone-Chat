package dev.stonechat.plugin.chatgame;

import dev.stonechat.plugin.PluginTestBase;
import dev.stonechat.plugin.chatgame.games.ReverseGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The Reverse and Reaction chat games. */
class NewGameTypesTest extends PluginTestBase {

    private PlayerMock steve;
    private PlayerMock alex;

    @BeforeEach
    void players() {
        steve = server.addPlayer("Steve");
        alex = server.addPlayer("Alex");
    }

    private List<Component> components(PlayerMock player) {
        List<Component> received = new ArrayList<>();
        Component next;
        while ((next = player.nextComponentMessage()) != null) received.add(next);
        return received;
    }

    private static String clickCommand(Component component) {
        if (component.clickEvent() != null && component.clickEvent().action() == ClickEvent.Action.RUN_COMMAND
                && component.clickEvent().payload() instanceof ClickEvent.Payload.Text text) {
            return text.value();
        }
        for (Component child : component.children()) {
            String found = clickCommand(child);
            if (found != null) return found;
        }
        return null;
    }

    /** Starts the shipped reaction game and returns the command its button runs. */
    private String startReaction() {
        ChatGameManager games = plugin.getChatGameManager();
        assertTrue(games.start(games.getGame("reaction")));
        String command = components(alex).stream().map(NewGameTypesTest::clickCommand)
                .filter(c -> c != null).findFirst().orElseThrow(() -> new AssertionError("no button in chat"));
        inbox(steve);
        inbox(alex);
        return command;
    }

    @Test
    void reverseShowsTheWordBackwardsAndSkipsPalindromes() {
        ReverseGame game = new ReverseGame("r", true, 60, "", false, 0, true, "DEFAULT", RewardConfig.none(),
                List.of("level", "diamond", "Racecar"), "Backwards: %word%");

        for (int i = 0; i < 20; i++) {
            GameRound round = game.generateRound(new Random(i));
            assertEquals("Backwards: dnomaid", round.question());
            assertTrue(round.matches(" Diamond ", false));
        }
        assertNull(new ReverseGame("p", true, 60, "", false, 0, true, "DEFAULT", RewardConfig.none(),
                List.of("level"), "%word%").generateRound(new Random()), "only palindromes: no round");
    }

    @Test
    void theShippedReverseGameIsPlayableInChat() throws Exception {
        ChatGameManager games = plugin.getChatGameManager();
        assertTrue(games.start(games.getGame("reverse_word")));
        String question = inbox(steve).stream().filter(m -> m.contains("Read it backwards: ")).findFirst().orElseThrow();
        String backwards = question.substring(question.indexOf("Read it backwards: ") + "Read it backwards: ".length()).trim();

        assertTrue(chat(steve, new StringBuilder(backwards).reverse().toString()).isCancelled());
        server.getScheduler().performOneTick();
        assertTrue(inbox(alex).stream().anyMatch(m -> m.contains("Steve won the chat game")));
    }

    @Test
    void theFirstClickWinsAndLaterClicksAreTooLate() {
        String button = startReaction();
        assertTrue(button.startsWith("/stonechat react "), button);

        steve.performCommand(button.substring(1));
        alex.performCommand(button.substring(1));

        assertFalse(plugin.getChatGameManager().isRunning());
        assertTrue(inbox(steve).stream().anyMatch(m -> m.matches(".*Steve was the fastest and clicked after \\d+\\.\\ds!.*")));
        assertTrue(inbox(alex).stream().anyMatch(m -> m.contains("Too late")));
    }

    @Test
    void anOldButtonDoesNotWinTheNextRound() {
        String oldButton = startReaction();
        plugin.getChatGameManager().stop("Admin");
        String newButton = startReaction();
        assertNotEquals(oldButton, newButton, "every round has its own code");

        steve.performCommand(oldButton.substring(1));

        assertTrue(plugin.getChatGameManager().isRunning());
        assertTrue(inbox(steve).stream().anyMatch(m -> m.contains("Too late")));
    }

    @Test
    void reactionRoundsAlwaysShowInChatAndHaveNoHint() {
        plugin.getChatGameManager().set("default-games.reaction.notification-type", "TITLE");
        plugin.getChatGameManager().set("default-games.reaction.hint-enabled", true);
        plugin.getChatGameManager().set("chat-games.hint.after-seconds", 3);
        plugin.reload();

        startReaction();
        server.getScheduler().performTicks(14 * 20L);

        assertTrue(plugin.getChatGameManager().isRunning());
        assertTrue(inbox(steve).stream().noneMatch(m -> m.contains("Hint")));
    }

    @Test
    void anyGameCanHaveItsOwnWinMessageWithTheTimeItTook() throws Exception {
        plugin.getChatGameManager().addCustomGameEntry("quick", "2+2?", List.of("4"));
        plugin.getChatGameManager().set("custom-games.quick.win-message", "&a%player% solved it in %time%s");
        plugin.reload();
        plugin.getChatGameManager().start(plugin.getChatGameManager().getGame("quick"));

        chat(steve, "4");
        server.getScheduler().performOneTick();

        assertTrue(inbox(alex).stream().anyMatch(m -> m.matches(".*Steve solved it in \\d+\\.\\ds.*")));
    }
}
