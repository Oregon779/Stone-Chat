package dev.stonechat.plugin.command;

import dev.stonechat.plugin.PluginTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** /stonechat preview shows a chat line in the current (or a not yet saved) format to the admin only. */
class PreviewTest extends PluginTestBase {

    private PlayerMock admin;
    private PlayerMock steve;

    @BeforeEach
    void players() {
        admin = server.addPlayer("Admin");
        admin.setOp(true);
        steve = server.addPlayer("Steve");
        inbox(admin);
        inbox(steve);
    }

    private List<String> adminSees(String command) {
        admin.performCommand(command);
        return inbox(admin).stream().map(m -> m.replace("​", "")).toList();
    }

    @Test
    void showsTheCurrentFormatToTheAdminOnly() {
        List<String> seen = adminSees("stonechat preview hi @Steve");

        assertTrue(seen.contains("Admin » hi @Steve"), seen.toString());
        assertTrue(seen.stream().anyMatch(m -> m.contains("only you see this")));
        assertTrue(inbox(steve).isEmpty(), "nobody else sees it and Steve is not pinged");
    }

    @Test
    void withoutTextASampleMessageIsUsed() {
        assertTrue(adminSees("stonechat preview").contains("Admin » Hello! This is how chat looks."));
    }

    @Test
    void anUnsavedFormatCanBeTriedWithoutChangingTheConfig() {
        String saved = plugin.getChatFormatManager().getFormat();

        List<String> seen = adminSees("stonechat preview format <gold>[%player_name%]</gold> %message%");

        assertTrue(seen.contains("[Admin] Hello! This is how chat looks."), seen.toString());
        assertTrue(seen.stream().anyMatch(m -> m.contains("not saved")));
        assertEquals(saved, plugin.getChatFormatManager().getFormat());
        assertTrue(adminSees("stonechat preview format").stream().anyMatch(m -> m.contains("Usage")));
    }

    @Test
    void needsAdminRights() {
        steve.performCommand("stonechat preview");

        assertTrue(inbox(steve).stream().anyMatch(m -> m.contains("do not have permission")));
    }
}
