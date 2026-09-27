package dev.stonechat.plugin.chatgame;

import dev.stonechat.plugin.PluginTestBase;
import dev.stonechat.plugin.chatgame.games.MathGame;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ChatGameTest extends PluginTestBase {

    private ChatGame customGame(String id, String question, String... answers) {
        plugin.getChatGameManager().addCustomGameEntry(id, question, List.of(answers));
        plugin.reload();
        return plugin.getChatGameManager().getGame(id);
    }

    private long countContaining(List<String> messages, String text) {
        return messages.stream().filter(m -> m.contains(text)).count();
    }

    @Test
    void winnerSideEffectsRunOnTheMainThreadNotTheChatThread() throws Exception {
        PlayerMock steve = server.addPlayer("Steve");
        assertTrue(plugin.getChatGameManager().start(customGame("quiz", "Best cartoon?", "Tom & Jerry")));
        inbox(steve);

        AsyncChatEvent answer = chat(steve, "tom & jerry");

        assertTrue(answer.isCancelled(), "a correct answer is not shown as normal chat");
        assertFalse(plugin.getChatGameManager().isRunning());
        assertEquals(0, countContaining(inbox(steve), "won the chat game"), "nothing may happen on the async chat thread");

        server.getScheduler().performOneTick();

        List<String> afterTick = inbox(steve).stream().map(m -> m.replace("\u200B", "")).toList();
        assertEquals(1, countContaining(afterTick, "Steve won the chat game! The answer was tom & jerry!"), afterTick.toString());
        steve.assertSoundHeard(Sound.ENTITY_PLAYER_LEVELUP);
    }

    @Test
    void onlyOneOfManySimultaneousCorrectAnswersWins() throws Exception {
        List<PlayerMock> players = new ArrayList<>();
        for (int i = 0; i < 20; i++) players.add(server.addPlayer("Racer" + i));
        plugin.getChatGameManager().start(customGame("race", "2+2?", "4"));

        List<CompletableFuture<AsyncChatEvent>> futures = new ArrayList<>();
        for (PlayerMock player : players) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return chat(player, "4");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        for (CompletableFuture<AsyncChatEvent> future : futures) future.get();
        server.getScheduler().performOneTick();

        assertEquals(1, countContaining(inbox(players.get(0)), "won the chat game"));
    }

    @Test
    void roundTimesOutAndOnlyItsOwnTimerEndsIt() {
        PlayerMock steve = server.addPlayer("Steve");
        ChatGame game = customGame("slow", "Name a color", "blue");
        assertTrue(plugin.getChatGameManager().start(game));
        server.getScheduler().performTicks(game.getDurationSeconds() * 20L + 1);

        assertFalse(plugin.getChatGameManager().isRunning());
        assertEquals(1, countContaining(inbox(steve), "Time's up"));
    }

    @Test
    void stopMessageComesFromChatgamesYml() {
        PlayerMock steve = server.addPlayer("Steve");
        plugin.getChatGameManager().set("chat-games.broadcast.stop-message", "&cRunde beendet von %player%.");
        plugin.reload();
        plugin.getChatGameManager().start(customGame("stoppable", "Q", "A"));
        inbox(steve);

        assertTrue(plugin.getChatGameManager().stop("Admin"));
        assertEquals(1, countContaining(inbox(steve), "Runde beendet von Admin."));
    }

    @Test
    void perGamePermissionIsRequiredToStartItByHand() {
        PlayerMock admin = server.addPlayer("Admin");
        admin.addAttachment(plugin, "stonechat.chatgame", true);
        customGame("vip", "Q", "A");
        plugin.getChatGameManager().set("custom-games.vip.permission", "stonechat.game.vip");
        plugin.reload();
        inbox(admin);

        admin.performCommand("chatgame start vip");
        assertFalse(plugin.getChatGameManager().isRunning());
        assertTrue(inbox(admin).stream().anyMatch(m -> m.contains("do not have permission")));

        admin.addAttachment(plugin, "stonechat.game.vip", true);
        admin.performCommand("chatgame start vip");
        assertTrue(plugin.getChatGameManager().isRunning());
    }

    @Test
    void gameIdsAreCaseInsensitive() throws Exception {
        File file = new File(plugin.getDataFolder(), "chatgames.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("custom-games.MyQuiz.type", "CUSTOM");
        yaml.set("custom-games.MyQuiz.questions", List.of(java.util.Map.of("question", "Q", "answers", List.of("A"))));
        yaml.save(file);
        plugin.reload();

        assertNotNull(plugin.getChatGameManager().getGame("myquiz"));
        assertNotNull(plugin.getChatGameManager().getGame("MyQuiz"));
    }

    private static final Pattern QUESTION = Pattern.compile("Solve: (-?\\d+) ([-+*/]) (-?\\d+)");

    @Test
    void mathQuestionAlwaysMatchesItsAnswer() {
        MathGame game = new MathGame("m", true, 60, "", false, 0, true, "DEFAULT", RewardConfig.none(),
                1, 50, List.of("/"));
        Random random = new Random(1);
        for (int i = 0; i < 2000; i++) {
            GameRound round = game.generateRound(random);
            Matcher m = QUESTION.matcher(round.question());
            assertTrue(m.matches(), round.question());
            long a = Long.parseLong(m.group(1)), b = Long.parseLong(m.group(3));
            long expected = switch (m.group(2)) {
                case "/" -> a / b;
                case "*" -> a * b;
                default -> throw new AssertionError("unexpected operator in " + round.question());
            };
            if (m.group(2).equals("/")) assertEquals(0, a % b, round.question());
            assertEquals(String.valueOf(expected), round.acceptedAnswers().get(0), round.question());
        }
    }

    @Test
    void mathAnswersDoNotOverflowWithLargeNumbers() {
        MathGame game = new MathGame("m", true, 60, "", false, 0, true, "DEFAULT", RewardConfig.none(),
                1_000_000, 1_000_000, List.of("*"));
        assertEquals("1000000000000", game.generateRound(new Random()).acceptedAnswers().get(0));
    }

    @Test
    void rewardParticleNamesResolveOnCurrentVersions() {
        assertEquals(Particle.TOTEM_OF_UNDYING, ChatGameManager.resolveParticle("TOTEM"), "name used by older configs");
        assertEquals(Particle.TOTEM_OF_UNDYING, ChatGameManager.resolveParticle(" totem_of_undying "));
        assertNull(ChatGameManager.resolveParticle("NOT_A_PARTICLE"));

        YamlConfiguration shipped = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "chatgames.yml"));
        assertNotNull(ChatGameManager.resolveParticle(shipped.getString("chat-games.default-reward.particle.name")));
    }
}
