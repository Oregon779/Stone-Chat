package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.PluginTestBase;
import dev.stonechat.plugin.TestPlayerMock;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Typing "@St" + Tab completes "@Steve": the client gets "@Name" chat suggestions for everyone it can ping. */
class PingCompletionTest extends PluginTestBase {

    private TestPlayerMock join(String name) {
        return (TestPlayerMock) server.addPlayer(name);
    }

    @Test
    void playersGetSuggestionsForEachOtherButNotForThemselves() {
        TestPlayerMock steve = join("Steve");
        TestPlayerMock alex = join("Alex");

        assertEquals(Set.of("@Alex"), steve.customChatCompletions());
        assertEquals(Set.of("@Steve"), alex.customChatCompletions());
    }

    @Test
    void aPlayerWhoLeavesIsNoLongerSuggested() {
        TestPlayerMock steve = join("Steve");
        TestPlayerMock alex = join("Alex");
        join("Notch");

        alex.disconnect();

        assertEquals(Set.of("@Notch"), steve.customChatCompletions());
    }

    @Test
    void immunePlayersAreNotSuggested() {
        TestPlayerMock steve = join("Steve");
        TestPlayerMock staff = join("Staff");
        staff.addAttachment(plugin, "stonechat.ping.immune", true);

        plugin.reload();

        assertEquals(Set.of(), steve.customChatCompletions());
    }

    @Test
    void aNewTriggerSymbolReplacesTheOldSuggestions() {
        TestPlayerMock steve = join("Steve");
        join("Alex");

        plugin.getConfigManager().set("ping.trigger-symbol", "!");
        plugin.reload();

        assertEquals(Set.of("!Alex"), steve.customChatCompletions());
    }

    @Test
    void canBeTurnedOffAndSelfPingAddsTheOwnName() {
        TestPlayerMock steve = join("Steve");
        join("Alex");

        plugin.getConfigManager().set("ping.tab-complete", false);
        plugin.reload();
        assertEquals(Set.of(), steve.customChatCompletions());

        plugin.getConfigManager().set("ping.tab-complete", true);
        plugin.getConfigManager().set("ping.allow-self-ping", true);
        plugin.reload();
        assertEquals(Set.of("@Alex", "@Steve"), steve.customChatCompletions());
    }

    @Test
    void suggestionsAreTakenBackWhenThePluginStops() {
        TestPlayerMock steve = join("Steve");
        join("Alex");

        server.getPluginManager().disablePlugin(plugin);

        assertEquals(Set.of(), steve.customChatCompletions());
    }
}
