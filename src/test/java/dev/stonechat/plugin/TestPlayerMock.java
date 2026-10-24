package dev.stonechat.plugin;

import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** MockBukkit leaves Paper's custom chat completions unimplemented; this player records them like a client would. */
public class TestPlayerMock extends PlayerMock {

    private final Set<String> customChatCompletions = new LinkedHashSet<>();

    public TestPlayerMock(ServerMock server, String name) {
        super(server, name, UUID.randomUUID());
    }

    public Set<String> customChatCompletions() {
        return Set.copyOf(customChatCompletions);
    }

    @Override
    public void addCustomChatCompletions(Collection<String> completions) {
        customChatCompletions.addAll(completions);
    }

    @Override
    public void removeCustomChatCompletions(Collection<String> completions) {
        customChatCompletions.removeAll(completions);
    }

    @Override
    public void setCustomChatCompletions(Collection<String> completions) {
        customChatCompletions.clear();
        customChatCompletions.addAll(completions);
    }
}
