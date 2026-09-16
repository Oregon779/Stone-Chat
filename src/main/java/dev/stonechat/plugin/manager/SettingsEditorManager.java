package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.model.EditorGuiHolder;
import dev.stonechat.plugin.util.ColorUtil;
import dev.stonechat.plugin.util.SmallCaps;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class SettingsEditorManager {

    private final StoneChat plugin;

    public SettingsEditorManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    private record ConfigTarget(Supplier<YamlConfiguration> reader, BiConsumer<String, Object> writer) {
        boolean getBoolean(String path, boolean def) {
            return reader.get().getBoolean(path, def);
        }

        int getInt(String path, int def) {
            return reader.get().getInt(path, def);
        }

        String getString(String path, String def) {
            return reader.get().getString(path, def);
        }

        java.util.List<String> getStringList(String path) {
            return reader.get().getStringList(path);
        }
    }

    private ConfigTarget mainConfig() {
        return new ConfigTarget(() -> plugin.getConfigManager().raw(), plugin.getConfigManager()::set);
    }

    private ConfigTarget chatFormatConfig() {
        return new ConfigTarget(() -> plugin.getChatFormatManager().rawConfig(), plugin.getChatFormatManager()::set);
    }

    private ConfigTarget chatGamesConfig() {
        return new ConfigTarget(() -> plugin.getChatGameManager().rawConfig(), plugin.getChatGameManager()::set);
    }

    private ConfigTarget messagesConfig() {
        return new ConfigTarget(() -> plugin.getLanguageManager().rawMessages(),
                (path, value) -> plugin.getLanguageManager().set(path, String.valueOf(value)));
    }

    private enum FieldType { TOGGLE, NUMBER, CYCLE, TEXT, MESSAGE, ACTION, STRINGLIST }

    private record Field(String path, String label, Material material, FieldType type,
                          int min, int max, int step, String[] options, Runnable action) {

        static Field toggle(String path, String label, Material material) {
            return new Field(path, label, material, FieldType.TOGGLE, 0, 0, 0, null, null);
        }

        static Field toggle(String path, String label, Material material, boolean defaultValue) {
            return new Field(path, label, material, FieldType.TOGGLE, defaultValue ? 1 : 0, 0, 0, null, null);
        }

        static Field number(String path, String label, Material material, int min, int max, int step) {
            return new Field(path, label, material, FieldType.NUMBER, min, max, step, null, null);
        }

        static Field cycle(String path, String label, Material material, String[] options) {
            return new Field(path, label, material, FieldType.CYCLE, 0, 0, 0, options, null);
        }

        static Field text(String path, String label, Material material) {
            return new Field(path, label, material, FieldType.TEXT, 0, 0, 0, null, null);
        }

        static Field stringList(String path, String label, Material material) {
            return new Field(path, label, material, FieldType.STRINGLIST, 0, 0, 0, null, null);
        }

        static Field message(String path, String label, Material material) {
            return new Field(path, label, material, FieldType.MESSAGE, 0, 0, 0, null, null);
        }

        static Field action(String label, Material material, Runnable action) {
            return new Field(null, label, material, FieldType.ACTION, 0, 0, 0, null, action);
        }

        static Field action(String label, String description, Material material, Runnable action) {
            return new Field(description, label, material, FieldType.ACTION, 0, 0, 0, null, action);
        }
    }

    @FunctionalInterface
    private interface IndexedGridConsumer {
        void accept(int index, int row, int col);
    }

    private static final int[] GRID_COLUMNS = {1, 2, 3, 5, 6, 7};

    private void placeGridCentered(int count, int startRow, IndexedGridConsumer placer) {
        int index = 0;
        int row = startRow;
        while (index < count) {
            int remaining = count - index;
            int itemsThisRow = Math.min(GRID_COLUMNS.length, remaining);

            if (itemsThisRow == GRID_COLUMNS.length) {

                for (int i = 0; i < itemsThisRow; i++) {
                    placer.accept(index, row, GRID_COLUMNS[i]);
                    index++;
                }
            } else {

                int startCol = 1 + (7 - itemsThisRow) / 2;
                for (int i = 0; i < itemsThisRow; i++) {
                    placer.accept(index, row, startCol + i);
                    index++;
                }
            }
            row++;
        }
    }

    public void openMainMenu(Player player) {
        EditorGuiHolder holder = new EditorGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 45,
                ColorUtil.parse("<gradient:#55A8FE:#00C9FF><bold>" + SmallCaps.of("Stone Chat") + "</bold></gradient>"
                        + " <dark_gray>| <gray>" + SmallCaps.of("Admin Editor")));
        holder.setInventory(inv);

        ItemStack headerItem = buildItem(Material.NETHER_STAR,
                "<gradient:#55A8FE:#00C9FF><bold>" + SmallCaps.of("Stone Chat Editor") + "</bold></gradient>",
                List.of("&7Click a category below to configure it.", "&7Changes save and apply instantly."));
        inv.setItem(4, headerItem);

        String[][] categories = {
                {"BOOK", "&eWord Filter", "&7Blocked words, censoring, evasion detection"},
                {"BARRIER", "&eChat Mute", "&7Server-wide chat mute toggle and its sound/notification"},
                {"CHAIN", "&eLink Blocker", "&7Blocks links in chat"},
                {"BELL", "&ePing", "&7@mentions: trigger symbol, sound, who's immune"},
                {"CLOCK", "&eCooldown", "&7Chat and command spam cooldown"},
                {"WRITABLE_BOOK", "&eAnti-Caps", "&7Excessive CAPS LOCK blocking/auto-correct"},
                {"OAK_DOOR", "&eJoin Delay", "&7How long after joining before a player can chat"},
                {"PAPER", "&eMax Length", "&7Maximum chat message length"},
                {"NAME_TAG", "&eChat Format", "&7How public chat messages are displayed"},
                {"DIAMOND_SWORD", "&eChat Games", "&7Trivia/math/custom games, create & edit"},
                {"BEACON", "&eBroadcast", "&7Send announcements via chat/actionbar/title/bossbar"},
                {"FIREWORK_STAR", "&eChat Color GUI", "&7The /chatcolor picker's settings"},
                {"OAK_SIGN", "&ePrivate Messages", "&7/msg and /r formatting and sound"},
                {"TNT", "&eChat Clear", "&7/chatclear behaviour"},
                {"COMPASS", "&eUpdate Checker", "&7Automatic Modrinth update notifications"}
        };
        Runnable[] actions = {
                () -> openWordFilterMenu(player), () -> openChatMuteMenu(player), () -> openLinkBlockerMenu(player),
                () -> openPingMenu(player), () -> openCooldownMenu(player), () -> openAntiCapsMenu(player),
                () -> openJoinDelayMenu(player), () -> openMaxLengthMenu(player), () -> openChatFormatMenu(player),
                () -> openChatGamesMenu(player), () -> openBroadcastMenu(player), () -> openChatColorGuiMenu(player),
                () -> openPrivateMessagesMenu(player), () -> openChatClearMenu(player), () -> openUpdateCheckerMenu(player)
        };

        placeGridCentered(categories.length, 1, (i, row, col) -> {
            int slot = row * 9 + col;
            Material material = Material.matchMaterial(categories[i][0]);
            if (material == null) material = Material.BOOK;
            addNavButton(inv, holder, slot, material, categories[i][1], categories[i][2], actions[i]);
        });

        fillEmptySlots(inv, 45, Material.BLACK_STAINED_GLASS_PANE);
        player.openInventory(inv);
    }

    private void openWordFilterMenu(Player player) {
        renderMenu(player, menuTitle("Word Filter"), mainConfig(), () -> openWordFilterMenu(player), List.of(
                Field.toggle("word-filter.enabled", "Enabled", Material.LIME_DYE),
                Field.cycle("word-filter.action", "Action", Material.HOPPER, new String[]{"block", "censor"}),
                Field.toggle("word-filter.evasion-detection.enabled", "Evasion Detection", Material.ENDER_EYE),
                Field.toggle("word-filter.evasion-detection.normalize-leetspeak", "Leetspeak Detection", Material.REDSTONE),
                Field.toggle("word-filter.notify-admins", "Notify Admins", Material.BELL),
                Field.cycle("word-filter.notification-type", "Notification Type", Material.OAK_SIGN, notificationOptions()),
                Field.toggle("word-filter.sound.enabled", "Sound Enabled", Material.NOTE_BLOCK),
                Field.text("word-filter.sound.sound-name", "Sound Name", Material.JUKEBOX),
                Field.text("word-filter.punish-command", "Punish Command", Material.COMMAND_BLOCK),
                Field.message("word-filter.blocked.chat", "Message: Blocked (Chat)", Material.WRITTEN_BOOK),
                Field.message("word-filter.admin-notify", "Message: Admin Notify", Material.WRITTEN_BOOK)
        ), Material.RED_STAINED_GLASS_PANE);
    }

    private void openChatMuteMenu(Player player) {
        renderMenu(player, menuTitle("Chat Mute"), mainConfig(), () -> openChatMuteMenu(player), List.of(
                Field.toggle("chat-mute.block-commands", "Also Block Commands", Material.COMMAND_BLOCK, true),
                Field.cycle("chat-mute.notification-type", "Notification Type", Material.OAK_SIGN, notificationOptions()),
                Field.toggle("chat-mute.sound-toggle.enabled", "Toggle Sound Enabled", Material.NOTE_BLOCK),
                Field.toggle("chat-mute.sound-blocked.enabled", "Blocked Sound Enabled", Material.NOTE_BLOCK),
                Field.message("chat-mute.now-muted.chat", "Message: Now Muted (Chat)", Material.WRITTEN_BOOK),
                Field.message("chat-mute.now-unmuted.chat", "Message: Now Unmuted (Chat)", Material.WRITTEN_BOOK),
                Field.message("chat-mute.chat-blocked.chat", "Message: Chat Blocked (Chat)", Material.WRITTEN_BOOK)
        ), Material.ORANGE_STAINED_GLASS_PANE);
    }

    private void openLinkBlockerMenu(Player player) {
        renderMenu(player, menuTitle("Link Blocker"), mainConfig(), () -> openLinkBlockerMenu(player), List.of(
                Field.toggle("link-blocker.enabled", "Enabled", Material.LIME_DYE),
                Field.cycle("link-blocker.notification-type", "Notification Type", Material.OAK_SIGN, notificationOptions()),
                Field.toggle("link-blocker.sound.enabled", "Sound Enabled", Material.NOTE_BLOCK),
                Field.text("link-blocker.punish-command", "Punish Command", Material.COMMAND_BLOCK),
                Field.message("link-blocker.blocked.chat", "Message: Blocked (Chat)", Material.WRITTEN_BOOK)
        ), Material.CYAN_STAINED_GLASS_PANE);
    }

    private void openPingMenu(Player player) {
        renderMenu(player, menuTitle("Ping"), mainConfig(), () -> openPingMenu(player), List.of(
                Field.toggle("ping.enabled", "Enabled", Material.LIME_DYE),
                Field.text("ping.trigger-symbol", "Trigger Symbol", Material.NAME_TAG),
                Field.toggle("ping.allow-self-ping", "Allow Self-Ping", Material.PLAYER_HEAD),
                Field.toggle("ping.ignorable", "Ignorable", Material.BARRIER),
                Field.cycle("ping.notification-type", "Notification Type", Material.OAK_SIGN, notificationOptions()),
                Field.toggle("ping.sound.enabled", "Sound Enabled", Material.NOTE_BLOCK),
                Field.text("ping.sound.sound-name", "Sound Name", Material.JUKEBOX),
                Field.message("ping.sound-notify.chat", "Message: Mentioned (Chat)", Material.WRITTEN_BOOK)
        ), Material.YELLOW_STAINED_GLASS_PANE);
    }

    private void openCooldownMenu(Player player) {
        renderMenu(player, menuTitle("Cooldown"), mainConfig(), () -> openCooldownMenu(player), List.of(
                Field.toggle("cooldown.enabled", "Enabled", Material.LIME_DYE),
                Field.number("cooldown.chat-seconds", "Chat Cooldown (s)", Material.CLOCK, 0, 3600, 1),
                Field.number("cooldown.command-seconds", "Command Cooldown (s)", Material.CLOCK, 0, 3600, 1),
                Field.stringList("cooldown.cooldown-commands", "Cooldown Applies To", Material.COMMAND_BLOCK),
                Field.cycle("cooldown.notification-type", "Notification Type", Material.OAK_SIGN, notificationOptions()),
                Field.toggle("cooldown.sound.enabled", "Sound Enabled", Material.NOTE_BLOCK),
                Field.message("cooldown.chat-wait.chat", "Message: Chat Wait (Chat)", Material.WRITTEN_BOOK)
        ), Material.LIGHT_BLUE_STAINED_GLASS_PANE);
    }

    private void openAntiCapsMenu(Player player) {
        renderMenu(player, menuTitle("Anti-Caps"), mainConfig(), () -> openAntiCapsMenu(player), List.of(
                Field.toggle("anti-caps.enabled", "Enabled", Material.LIME_DYE),
                Field.number("anti-caps.max-percentage", "Max Percentage", Material.HOPPER, 0, 100, 5),
                Field.number("anti-caps.min-length", "Min Length", Material.HOPPER, 0, 100, 1),
                Field.cycle("anti-caps.mode", "Mode", Material.PISTON, new String[]{"block", "autocorrect"}),
                Field.cycle("anti-caps.notification-type", "Notification Type", Material.OAK_SIGN, notificationOptions()),
                Field.toggle("anti-caps.sound.enabled", "Sound Enabled", Material.NOTE_BLOCK),
                Field.text("anti-caps.punish-command", "Punish Command", Material.COMMAND_BLOCK),
                Field.message("anti-caps.blocked.chat", "Message: Blocked (Chat)", Material.WRITTEN_BOOK)
        ), Material.LIME_STAINED_GLASS_PANE);
    }

    private void openJoinDelayMenu(Player player) {
        renderMenu(player, menuTitle("Join Delay"), mainConfig(), () -> openJoinDelayMenu(player), List.of(
                Field.toggle("join-delay.enabled", "Enabled", Material.LIME_DYE),
                Field.number("join-delay.seconds", "Seconds", Material.CLOCK, 0, 600, 1),
                Field.cycle("join-delay.notification-type", "Notification Type", Material.OAK_SIGN, notificationOptions()),
                Field.toggle("join-delay.sound.enabled", "Sound Enabled", Material.NOTE_BLOCK),
                Field.message("join-delay.wait.chat", "Message: Wait (Chat)", Material.WRITTEN_BOOK)
        ), Material.BROWN_STAINED_GLASS_PANE);
    }

    private void openMaxLengthMenu(Player player) {
        renderMenu(player, menuTitle("Max Length"), mainConfig(), () -> openMaxLengthMenu(player), List.of(
                Field.toggle("max-message-length.enabled", "Enabled", Material.LIME_DYE),
                Field.number("max-message-length.max-length", "Max Length", Material.HOPPER, 1, 1000, 10),
                Field.cycle("max-message-length.notification-type", "Notification Type", Material.OAK_SIGN, notificationOptions()),
                Field.toggle("max-message-length.sound.enabled", "Sound Enabled", Material.NOTE_BLOCK),
                Field.message("max-length.blocked.chat", "Message: Blocked (Chat)", Material.WRITTEN_BOOK)
        ), Material.WHITE_STAINED_GLASS_PANE);
    }

    private void openChatFormatMenu(Player player) {
        renderMenu(player, menuTitle("Chat Format"), chatFormatConfig(), () -> openChatFormatMenu(player), List.of(
                Field.toggle("name-interactions.click-to-message", "Click to Message", Material.PAPER),
                Field.toggle("name-interactions.hover-stats", "Hover Stats", Material.BOOK),
                Field.text("date-format", "Date Format", Material.CLOCK),
                Field.text("time-format", "Time Format", Material.CLOCK),
                Field.toggle("use-placeholderapi", "Use PlaceholderAPI", Material.STRUCTURE_VOID),
                Field.message("chat-format.hover-rank", "Message: Hover Rank", Material.WRITTEN_BOOK),
                Field.message("chat-format.hover-playtime", "Message: Hover Playtime", Material.WRITTEN_BOOK),
                Field.message("chat-format.hover-deaths", "Message: Hover Deaths", Material.WRITTEN_BOOK),
                Field.message("chat-format.hover-kills", "Message: Hover Kills", Material.WRITTEN_BOOK)
        ), Material.MAGENTA_STAINED_GLASS_PANE);

    }

    private void openBroadcastMenu(Player player) {
        ConfigTarget target = mainConfig();
        String currentType = target.getString("broadcast.default-type", "CHAT").toUpperCase();
        final BroadcastManager.Type type = resolveBroadcastType(currentType);

        List<Field> fields = new ArrayList<>();
        fields.add(Field.action("Send Broadcast Now", "&7Type your message in chat, sent through the channel selected below.", Material.WRITABLE_BOOK, () -> {
            player.closeInventory();
            plugin.getPendingBroadcastManager().request(player.getUniqueId(), type, null);
            player.sendMessage(ColorUtil.parse("&8&m                                                "));
            player.sendMessage(ColorUtil.parse("&6&lStone Chat Broadcast"));
            player.sendMessage(ColorUtil.parse("&7Type your broadcast message in chat now &7(channel: &e" + prettyType(currentType) + "&7)."));
            player.sendMessage(ColorUtil.parse("&7Type &ccancel &7to abort."));
            player.sendMessage(ColorUtil.parse("&8&m                                                "));
        }));
        fields.add(Field.cycle("broadcast.default-type", "Display Type", Material.BEACON, notificationOptions()));
        fields.add(Field.toggle("broadcast.sound.enabled", "Sound Enabled", Material.NOTE_BLOCK));

        switch (currentType) {
            case "CHAT" -> {
                fields.add(Field.toggle("broadcast.use-prefix", "Use Broadcast Prefix", Material.NAME_TAG));
                fields.add(Field.message("broadcast.prefix", "Message: Broadcast Prefix", Material.WRITTEN_BOOK));
            }
            case "ACTIONBAR" -> {

            }
            case "TITLE" -> {
                fields.add(Field.number("title-timing.fade-in-ticks", "Title Fade In (ticks)", Material.CLOCK, 0, 200, 5));
                fields.add(Field.number("title-timing.stay-ticks", "Title Stay (ticks)", Material.CLOCK, 0, 1200, 10));
                fields.add(Field.number("title-timing.fade-out-ticks", "Title Fade Out (ticks)", Material.CLOCK, 0, 200, 5));
            }
            case "BOSSBAR" -> {
                fields.add(Field.cycle("broadcast.bossbar.color", "Bossbar Color", Material.ORANGE_DYE,
                        new String[]{"PINK", "BLUE", "RED", "GREEN", "YELLOW", "PURPLE", "WHITE"}));
                fields.add(Field.cycle("broadcast.bossbar.style", "Bossbar Style", Material.HOPPER,
                        new String[]{"PROGRESS", "NOTCHED_6", "NOTCHED_10", "NOTCHED_12", "NOTCHED_20"}));
                fields.add(Field.number("broadcast.bossbar.duration-seconds", "Bossbar Duration (s)", Material.CLOCK, 1, 600, 5));
            }
        }

        renderMenu(player, "<dark_gray>- <gradient:#55A8FE:#00C9FF><bold>" + SmallCaps.of("Broadcast") + "</bold></gradient> <dark_gray>-</dark_gray> " + prettyType(currentType),
                target, () -> openBroadcastMenu(player), fields, Material.ORANGE_STAINED_GLASS_PANE);
    }

    private String prettyType(String type) {
        return switch (type) {
            case "ACTIONBAR" -> "Actionbar";
            case "TITLE" -> "Title";
            case "BOSSBAR" -> "Bossbar";
            default -> "Chat";
        };
    }

    private BroadcastManager.Type resolveBroadcastType(String raw) {
        try {
            return BroadcastManager.Type.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return BroadcastManager.Type.CHAT;
        }
    }

    private int parseIntOrDefault(String raw, int def) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private String menuTitle(String label) {
        return "<dark_gray>─ <gradient:#55A8FE:#00C9FF><bold>" + SmallCaps.of(label) + "</bold></gradient> <dark_gray>─";
    }

    private void openChatColorGuiMenu(Player player) {
        renderMenu(player, menuTitle("Chat Color GUI"), mainConfig(), () -> openChatColorGuiMenu(player), List.of(
                Field.toggle("chat-color-gui.enabled", "Enabled", Material.LIME_DYE),
                Field.number("chat-color-gui.size", "Inventory Size", Material.CHEST, 9, 54, 9),
                Field.text("chat-color-gui.title", "Menu Title", Material.NAME_TAG),
                Field.toggle("chat-color-gui.sound-select.enabled", "Select Sound Enabled", Material.NOTE_BLOCK),
                Field.toggle("chat-color-gui.sound-denied.enabled", "Denied Sound Enabled", Material.NOTE_BLOCK),
                Field.message("chat-color.selected", "Message: Color Selected", Material.WRITTEN_BOOK),
                Field.message("chat-color.reset", "Message: Color Reset", Material.WRITTEN_BOOK)
        ), Material.PINK_STAINED_GLASS_PANE);

    }

    private void openPrivateMessagesMenu(Player player) {
        renderMenu(player, menuTitle("Private Messages"), mainConfig(), () -> openPrivateMessagesMenu(player), List.of(
                Field.toggle("private-messages.enabled", "Enabled", Material.LIME_DYE),
                Field.toggle("private-messages.sound.enabled", "Sound Enabled", Material.NOTE_BLOCK),
                Field.text("private-messages.sound.sound-name", "Sound Name", Material.JUKEBOX),
                Field.message("private-message.sender-format", "Message: Sender Format", Material.WRITTEN_BOOK),
                Field.message("private-message.receiver-format", "Message: Receiver Format", Material.WRITTEN_BOOK)
        ), Material.PURPLE_STAINED_GLASS_PANE);
    }

    private void openChatClearMenu(Player player) {
        renderMenu(player, menuTitle("Chat Clear"), mainConfig(), () -> openChatClearMenu(player), List.of(
                Field.number("chat-clear.lines", "Blank Lines", Material.PAPER, 1, 500, 10),
                Field.cycle("chat-clear.notification-type", "Notification Type", Material.OAK_SIGN, notificationOptions()),
                Field.toggle("chat-clear.sound.enabled", "Sound Enabled", Material.NOTE_BLOCK),
                Field.message("chat-clear.cleared.chat", "Message: Cleared (Chat)", Material.WRITTEN_BOOK)
        ), Material.GRAY_STAINED_GLASS_PANE);
    }

    private void openUpdateCheckerMenu(Player player) {
        String currentVersion = plugin.getPluginMeta().getVersion();
        String latest = plugin.getUpdateChecker().getLatestKnownVersion();
        boolean upToDate = latest == null || latest.equalsIgnoreCase(currentVersion);

        String statusLabel = upToDate
                ? "<gradient:#00c853:#64dd17><bold>✔ Up To Date</bold></gradient>"
                : "<gradient:#ff9800:#ff5722><bold>⚠ Update Available!</bold></gradient>";

        List<Field> fields = new ArrayList<>();
        fields.add(Field.action(statusLabel,
                "&7Running: &f" + currentVersion + (latest != null ? " &8| &7Latest: &f" + latest : " &8| &7Latest: &7(unknown yet)"),
                upToDate ? Material.EMERALD_BLOCK : Material.NETHER_STAR, () -> openUpdateCheckerMenu(player)));
        fields.add(Field.action("&b&l⟳ Check Now", "&7Immediately asks Modrinth for the latest version.", Material.COMPASS, () -> {
            player.sendMessage(ColorUtil.parse(plugin.getLanguageManager().getRaw("update.check-triggered")));
            plugin.getUpdateChecker().checkNow();
            Bukkit.getScheduler().runTaskLater(plugin, () -> openUpdateCheckerMenu(player), 40L);
        }));
        fields.add(Field.toggle("update-checker.enabled", "Auto-Check Enabled", Material.LIME_DYE));
        fields.add(Field.number("update-checker.check-interval-minutes", "Check Interval (min)", Material.CLOCK, 5, 1440, 5));

        renderMenu(player, "<dark_gray>- <gradient:#55A8FE:#00C9FF><bold>" + SmallCaps.of("Update Checker") + "</bold></gradient> <dark_gray>-",
                mainConfig(), () -> openUpdateCheckerMenu(player), fields, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
    }

    // =====================================================================
    // Chat Games - new architecture: Overview -> Settings & Timers /
    // Default Games Management / Custom Games Management -> per-game detail.
    // =====================================================================

    private String gameBasePath(dev.stonechat.plugin.chatgame.ChatGame game) {
        return (game.isDefaultGame() ? "default-games." : "custom-games.") + game.getId();
    }

    /** Main overview: global toggle, settings & timers, default games, custom games. */
    private void openChatGamesMenu(Player player) {
        EditorGuiHolder holder = new EditorGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27,
                ColorUtil.parse("<dark_gray>- <gradient:#55A8FE:#00C9FF><bold>" + SmallCaps.of("Chat Games") + "</bold></gradient> <dark_gray>-"));
        holder.setInventory(inv);

        boolean enabled = plugin.getChatGameManager().isModuleEnabled();
        ItemStack toggleItem = buildItem(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                (enabled ? "&a" : "&7") + "Chat Games Module",
                List.of(enabled ? "&a✔ Enabled" : "&7✖ Disabled",
                        "&7Master switch for the entire module.", "&7Click to toggle"));
        inv.setItem(10, toggleItem);
        holder.setHandler(10, (p, click) -> {
            plugin.getChatGameManager().set("chat-games.enabled", !enabled);
            plugin.reload();
            openChatGamesMenu(p);
        });

        ItemStack settingsItem = buildItem(Material.CLOCK, "&e&lSettings & Timers",
                List.of("&7Interval, answer time, cooldown,", "&7broadcast messages and the default reward.", "&7Click to open"));
        inv.setItem(12, settingsItem);
        holder.setHandler(12, (p, click) -> openChatGamesSettingsMenu(p));

        ItemStack defaultItem = buildItem(Material.DIAMOND_SWORD, "&b&lDefault Games Management",
                List.of("&7Math Solver, Unscramble Word, Fast Typing,", "&7Trivia / Quiz, Fill in the Blanks.", "&7Click to open"));
        inv.setItem(14, defaultItem);
        holder.setHandler(14, (p, click) -> openDefaultGamesMenu(p));

        ItemStack customItem = buildItem(Material.WRITABLE_BOOK, "&d&lCustom Games Management",
                List.of("&7Create, edit, enable/disable or delete", "&7your own question/answer games.", "&7Click to open"));
        inv.setItem(16, customItem);
        holder.setHandler(16, (p, click) -> openCustomGamesMenu(p));

        addNavButton(inv, holder, 26, Material.ARROW, "&7« Back to Main Menu", () -> openMainMenu(player));
        fillEmptySlots(inv, 27, Material.YELLOW_STAINED_GLASS_PANE);
        player.openInventory(inv);
    }

    /** Timers, broadcast messages, and the global default reward. */
    private void openChatGamesSettingsMenu(Player player) {
        List<Field> fields = new ArrayList<>();
        fields.add(Field.number("chat-games.interval-seconds", "Auto-Start Interval (s)", Material.CLOCK, 30, 86400, 30));
        fields.add(Field.number("chat-games.answer-time-seconds", "Default Answer Time (s)", Material.CLOCK, 5, 600, 5));
        fields.add(Field.number("chat-games.cooldown-after-game-seconds", "Cooldown After Game (s)", Material.CLOCK, 0, 3600, 5));
        fields.add(Field.number("chat-games.min-players-online", "Min Players Online (auto-start)", Material.PLAYER_HEAD, 0, 100, 1));
        fields.add(Field.toggle("chat-games.hint.enabled", "Hints Enabled", Material.BOOK, true));
        fields.add(Field.number("chat-games.hint.after-seconds", "Hint After (s)", Material.BOOK, 1, 600, 5));
        fields.add(Field.cycle("chat-games.notification-type", "Notification Type", Material.OAK_SIGN, notificationOptions()));
        fields.add(Field.text("chat-games.broadcast.prefix", "Message: Chat Prefix", Material.WRITTEN_BOOK));
        fields.add(Field.text("chat-games.broadcast.start-message", "Message: Game Started", Material.WRITTEN_BOOK));
        fields.add(Field.text("chat-games.broadcast.win-message", "Message: Winner", Material.WRITTEN_BOOK));
        fields.add(Field.text("chat-games.broadcast.timeout-message", "Message: Timeout", Material.WRITTEN_BOOK));
        fields.add(Field.text("chat-games.broadcast.hint-message", "Message: Hint", Material.WRITTEN_BOOK));
        fields.addAll(buildRewardFields("chat-games.default-reward", false));

        renderMenuWithCustomBack(player, "<dark_gray>- <gradient:#55A8FE:#00C9FF><bold>" + SmallCaps.of("Chat Games Settings") + "</bold></gradient> <dark_gray>-",
                chatGamesConfig(), () -> openChatGamesSettingsMenu(player), fields, Material.YELLOW_STAINED_GLASS_PANE,
                "&7« Back to Chat Games", () -> openChatGamesMenu(player));
    }

    /** Reusable reward field block, used for both the global default reward and any per-game override. */
    private List<Field> buildRewardFields(String basePath, boolean withOverrideToggle) {
        List<Field> fields = new ArrayList<>();
        if (withOverrideToggle) {
            fields.add(Field.toggle(basePath + ".override", "Override Default Reward", Material.NETHER_STAR));
        }
        fields.add(Field.stringList(basePath + ".console-commands", "Reward: Console Commands", Material.COMMAND_BLOCK));
        fields.add(Field.number(basePath + ".vault-money", "Reward: Vault Money", Material.EMERALD, 0, 1000000, 10));
        fields.add(Field.toggle(basePath + ".sound.enabled", "Reward: Sound Enabled", Material.NOTE_BLOCK, true));
        fields.add(Field.text(basePath + ".sound.name", "Reward: Sound Name", Material.JUKEBOX));
        fields.add(Field.toggle(basePath + ".particle.enabled", "Reward: Particle Enabled", Material.FIREWORK_STAR));
        fields.add(Field.text(basePath + ".particle.name", "Reward: Particle Name", Material.BLAZE_POWDER));
        fields.add(Field.number(basePath + ".particle.count", "Reward: Particle Count", Material.BLAZE_POWDER, 1, 200, 5));
        return fields;
    }

    /** Lists the five built-in games; click one to edit it. Default games can be disabled but never deleted. */
    private void openDefaultGamesMenu(Player player) {
        List<dev.stonechat.plugin.chatgame.ChatGame> games = plugin.getChatGameManager().getDefaultGames();

        EditorGuiHolder holder = new EditorGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27,
                ColorUtil.parse("<dark_gray>- <gradient:#55A8FE:#00C9FF><bold>" + SmallCaps.of("Default Games") + "</bold></gradient> <dark_gray>-"));
        holder.setInventory(inv);

        placeGridCentered(games.size(), 1, (i, row, col) -> {
            dev.stonechat.plugin.chatgame.ChatGame game = games.get(i);
            int slot = row * 9 + col;
            inv.setItem(slot, buildGameListItem(game));
            holder.setHandler(slot, (p, click) -> openGameDetailMenu(p, game.getId()));
        });

        addNavButton(inv, holder, 26, Material.ARROW, "&7« Back to Chat Games", () -> openChatGamesMenu(player));
        fillEmptySlots(inv, 27, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        player.openInventory(inv);
    }

    /** Lists custom games plus a "Create New" button. */
    private void openCustomGamesMenu(Player player) {
        List<dev.stonechat.plugin.chatgame.ChatGame> games = plugin.getChatGameManager().getCustomGames();

        EditorGuiHolder holder = new EditorGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 45,
                ColorUtil.parse("<dark_gray>- <gradient:#55A8FE:#00C9FF><bold>" + SmallCaps.of("Custom Games") + "</bold></gradient> <dark_gray>-"));
        holder.setInventory(inv);

        if (games.isEmpty()) {
            inv.setItem(13, buildItem(Material.BARRIER, "&7No Custom Games Yet",
                    List.of("&7Click \"+ Create New\" below to make one.")));
        } else {
            placeGridCentered(games.size(), 1, (i, row, col) -> {
                dev.stonechat.plugin.chatgame.ChatGame game = games.get(i);
                int slot = row * 9 + col;
                inv.setItem(slot, buildGameListItem(game));
                holder.setHandler(slot, (p, click) -> openGameDetailMenu(p, game.getId()));
            });
        }

        addNavButton(inv, holder, 31, Material.EMERALD, "&a&l+ Create New Custom Game",
                "&7Walks you through an id, a question and its answers.", () -> startCreateCustomGameWizard(player));
        addNavButton(inv, holder, 44, Material.ARROW, "&7« Back to Chat Games", () -> openChatGamesMenu(player));
        fillEmptySlots(inv, 45, Material.PINK_STAINED_GLASS_PANE);
        player.openInventory(inv);
    }

    private ItemStack buildGameListItem(dev.stonechat.plugin.chatgame.ChatGame game) {
        Material icon = switch (game.getType()) {
            case MATH -> Material.REDSTONE;
            case UNSCRAMBLE -> Material.PAPER;
            case FAST_TYPING -> Material.FEATHER;
            case TRIVIA -> Material.BOOK;
            case FILL_BLANKS -> Material.NAME_TAG;
            case CUSTOM -> Material.WRITABLE_BOOK;
        };
        return buildItem(icon, (game.isEnabled() ? "&a" : "&7") + game.getId(),
                List.of("&7Type: &f" + game.getType().name(),
                        game.isEnabled() ? "&a✔ Enabled" : "&7✖ Disabled",
                        "&7Click to edit"));
    }

    /** Anvil-chat wizard for creating a brand-new custom game (id, then its first question/answers). */
    private void startCreateCustomGameWizard(Player player) {
        plugin.getAnvilInputManager().openTextInput(player, "New Custom Game: ID", "", rawId -> {
            String id = rawId.trim().toLowerCase().replace(' ', '_');
            if (id.isBlank()) {
                player.sendMessage(ColorUtil.parse("&cCancelled - id cannot be empty."));
                openCustomGamesMenu(player);
                return;
            }
            if (!id.matches("[a-z0-9_-]+")) {
                player.sendMessage(ColorUtil.parse("&cInvalid id - only letters, numbers, '_' and '-' are allowed."));
                openCustomGamesMenu(player);
                return;
            }
            if (plugin.getChatGameManager().getGame(id) != null) {
                player.sendMessage(ColorUtil.parse("&cA game with id '" + id + "' already exists."));
                openCustomGamesMenu(player);
                return;
            }
            startAddQuestionWizard(player, id, true);
        });
    }

    /** Anvil-chat wizard: question, then its accepted answers, appended to a TRIVIA or CUSTOM game's question bank. */
    private void startAddQuestionWizard(Player player, String gameId, boolean isNewGame) {
        plugin.getAnvilInputManager().openTextInput(player, "Question", "", question -> {
            if (question.isBlank()) {
                player.sendMessage(ColorUtil.parse("&cCancelled - question cannot be empty."));
                openChatGamesMenu(player);
                return;
            }
            plugin.getAnvilInputManager().openTextInput(player, "Accepted Answers (comma-separated)", "", answersRaw -> {
                List<String> answers = new ArrayList<>();
                for (String part : answersRaw.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) answers.add(trimmed);
                }
                if (answers.isEmpty()) {
                    player.sendMessage(ColorUtil.parse("&cCancelled - at least one answer is required."));
                    openChatGamesMenu(player);
                    return;
                }
                plugin.getChatGameManager().addCustomGameEntry(gameId, question, answers);
                plugin.reload();
                player.sendMessage(ColorUtil.parse(isNewGame
                        ? "&aCustom game '" + gameId + "' created!"
                        : "&aQuestion added to '" + gameId + "'."));
                openGameDetailMenu(player, gameId);
            });
        });
    }

    /** Full per-game detail menu: common settings, type-specific settings, reward override, and (custom only) delete. */
    private void openGameDetailMenu(Player player, String gameId) {
        dev.stonechat.plugin.chatgame.ChatGame game = plugin.getChatGameManager().getGame(gameId);
        if (game == null) {
            openChatGamesMenu(player);
            return;
        }

        String base = gameBasePath(game);
        List<Field> fields = new ArrayList<>();
        fields.add(Field.toggle(base + ".enabled", "Enabled", Material.LIME_DYE));
        fields.add(Field.number(base + ".duration-seconds", "Duration (s, 0 = default)", Material.CLOCK, 0, 3600, 5));
        fields.add(Field.text(base + ".permission", "Extra Permission Required", Material.NAME_TAG));
        fields.add(Field.toggle(base + ".case-sensitive", "Case Sensitive Answers", Material.WRITABLE_BOOK));
        fields.add(Field.number(base + ".min-players-online", "Min Players Online", Material.PLAYER_HEAD, 0, 100, 1));
        fields.add(Field.toggle(base + ".hint-enabled", "Hints Enabled", Material.BOOK, true));
        fields.add(Field.cycle(base + ".notification-type", "Notification Type",
                Material.OAK_SIGN, new String[]{"DEFAULT", "CHAT", "ACTIONBAR", "TITLE", "BOSSBAR"}));

        switch (game.getType()) {
            case MATH -> {
                fields.add(Field.number(base + ".min-number", "Min Number", Material.SLIME_BALL, 0, 1000000, 5));
                fields.add(Field.number(base + ".max-number", "Max Number", Material.SLIME_BALL, 1, 1000000, 5));
                fields.add(Field.stringList(base + ".operators", "Allowed Operators (+ - * /)", Material.REDSTONE));
            }
            case UNSCRAMBLE -> fields.add(Field.stringList(base + ".words", "Word List", Material.PAPER));
            case FAST_TYPING -> fields.add(Field.stringList(base + ".phrases", "Word / Phrase List", Material.FEATHER));
            case FILL_BLANKS -> {
                fields.add(Field.number(base + ".blank-percentage", "Blank Percentage", Material.NAME_TAG, 10, 90, 5));
                fields.add(Field.stringList(base + ".words", "Word List", Material.PAPER));
            }
            case TRIVIA, CUSTOM -> {
                int questionCount = game.getType() == dev.stonechat.plugin.chatgame.GameType.TRIVIA
                        ? ((dev.stonechat.plugin.chatgame.games.TriviaGame) game).getEntries().size()
                        : ((dev.stonechat.plugin.chatgame.games.CustomGame) game).getEntries().size();
                fields.add(Field.action("+ Add Question", "&7Add another question/answer set (typed in chat).",
                        Material.PAPER, () -> startAddQuestionWizard(player, gameId, false)));
                fields.add(Field.action("Questions: " + questionCount,
                        "&7How many question/answer sets this game has - one is picked at random each round.",
                        Material.BOOKSHELF, () -> {
                }));
                if (questionCount > 1) {
                    fields.add(Field.action("- Remove Last Question", "&7Deletes the most recently added question.",
                            Material.PAPER, () -> {
                        plugin.getChatGameManager().removeLastQuestionEntry(gameId);
                        plugin.reload();
                        openGameDetailMenu(player, gameId);
                    }));
                }
            }
        }

        fields.addAll(buildRewardFields(base + ".reward", true));

        if (!game.isDefaultGame()) {
            fields.add(Field.action("&c&l✖ Delete Custom Game",
                    "&7Permanently removes this game. Asks you to type DELETE to confirm.", Material.BARRIER, () -> {
                plugin.getAnvilInputManager().openTextInput(player, "Type DELETE to confirm", "", confirm -> {
                    if (confirm.trim().equalsIgnoreCase("DELETE")) {
                        plugin.getChatGameManager().deleteCustomGame(gameId);
                        plugin.reload();
                        player.sendMessage(ColorUtil.parse("&cCustom game '" + gameId + "' deleted."));
                        openCustomGamesMenu(player);
                    } else {
                        player.sendMessage(ColorUtil.parse("&7Deletion cancelled."));
                        openGameDetailMenu(player, gameId);
                    }
                });
            }));
        }

        Runnable backAction = game.isDefaultGame() ? () -> openDefaultGamesMenu(player) : () -> openCustomGamesMenu(player);
        renderMenuWithCustomBack(player, "<dark_gray>- <gradient:#55A8FE:#00C9FF><bold>" + SmallCaps.of(gameId) + "</bold></gradient> <dark_gray>-",
                chatGamesConfig(), () -> openGameDetailMenu(player, gameId), fields, Material.YELLOW_STAINED_GLASS_PANE, backAction);
    }


    private void renderMenu(Player player, String title, ConfigTarget target, Runnable self, List<Field> fields, Material filler) {
        renderMenuWithCustomBack(player, title, target, self, fields, filler, "&7« Back to Main Menu", () -> openMainMenu(player));
    }

    /** Same as {@link #renderMenu}, but the back button runs {@code backAction} instead of always returning to the main menu - used for screens nested more than one level deep (e.g. a game's detail menu, which should go back to its list, not straight to the main menu). */
    private void renderMenuWithCustomBack(Player player, String title, ConfigTarget target, Runnable self,
                                           List<Field> fields, Material filler, Runnable backAction) {
        renderMenuWithCustomBack(player, title, target, self, fields, filler, "&7« Back", backAction);
    }

    private void renderMenuWithCustomBack(Player player, String title, ConfigTarget target, Runnable self,
                                           List<Field> fields, Material filler, String backLabel, Runnable backAction) {
        EditorGuiHolder holder = new EditorGuiHolder();

        int contentRows = Math.max(1, (int) Math.ceil(fields.size() / 6.0));
        int totalRows = Math.min(6, contentRows + 3);
        int size = totalRows * 9;

        Inventory inv = Bukkit.createInventory(holder, size, ColorUtil.parse(title));
        holder.setInventory(inv);

        placeGridCentered(fields.size(), 1, (i, row, col) -> {
            int slot = row * 9 + col;
            placeField(inv, holder, slot, target, fields.get(i), self);
        });

        addNavButton(inv, holder, size - 1, Material.ARROW, backLabel, backAction);
        fillEmptySlots(inv, size, filler);
        player.openInventory(inv);
    }

    private void fillEmptySlots(Inventory inv, int size, Material filler) {
        ItemStack fillerItem = new ItemStack(filler);
        ItemMeta meta = fillerItem.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse(" "));
            fillerItem.setItemMeta(meta);
        }
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, fillerItem);
            }
        }
    }

    private void placeField(Inventory inv, EditorGuiHolder holder, int slot, ConfigTarget target, Field field, Runnable refresh) {
        switch (field.type()) {
            case TOGGLE -> {
                boolean defaultValue = field.min() == 1;
                boolean value = target.getBoolean(field.path(), defaultValue);
                Material toggleMaterial = value ? Material.LIME_DYE : Material.GRAY_DYE;
                inv.setItem(slot, buildItem(toggleMaterial, (value ? "&a" : "&7") + field.label(),
                        List.of("<dark_gray>┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈",
                                value ? "&a✔ Enabled" : "&7✖ Disabled",
                                "&8Default: &7" + (defaultValue ? "Enabled" : "Disabled"),
                                "<dark_gray>┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈",
                                "&e▶ &7Left-Click &8- &7Toggle")));
                holder.setHandler(slot, (player, click) -> {
                    target.writer().accept(field.path(), !target.getBoolean(field.path(), defaultValue));
                    plugin.reload();
                    refresh.run();
                });
            }
            case NUMBER -> {
                int value = target.getInt(field.path(), field.min());
                inv.setItem(slot, buildItem(field.material(), "&e" + field.label(),
                        List.of("<dark_gray>┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈",
                                "&fCurrent: <gradient:#ffd200:#f7971e>" + value + "</gradient>",
                                "&8Range: &7" + field.min() + " - " + field.max(),
                                "<dark_gray>┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈",
                                "&e▶ &7Left/Right-Click &8- &c-" + field.step() + " &7/ &a+" + field.step(),
                                "&e▶ &7Shift+Left/Right &8- &c-" + (field.step() * 10) + " &7/ &a+" + (field.step() * 10))));
                holder.setHandler(slot, (player, click) -> {
                    int current = target.getInt(field.path(), field.min());
                    int delta = field.step() * (click.isShiftClick() ? 10 : 1);
                    int updated = click.isRightClick() ? current + delta : current - delta;
                    updated = Math.max(field.min(), Math.min(field.max(), updated));
                    target.writer().accept(field.path(), updated);
                    plugin.reload();
                    refresh.run();
                });
            }
            case CYCLE -> {
                String value = target.getString(field.path(), field.options()[0]);
                inv.setItem(slot, buildItem(field.material(), "&e" + field.label(),
                        List.of("<dark_gray>┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈",
                                "&fCurrent: <gradient:#ffd200:#f7971e>" + value + "</gradient>",
                                "&8Options: &7" + String.join(", ", field.options()),
                                "<dark_gray>┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈",
                                "&e▶ &7Left-Click &8- &7Cycle to next")));
                holder.setHandler(slot, (player, click) -> {
                    String current = target.getString(field.path(), field.options()[0]);
                    String[] options = field.options();
                    int index = 0;
                    for (int i = 0; i < options.length; i++) {
                        if (options[i].equalsIgnoreCase(current)) {
                            index = i;
                            break;
                        }
                    }
                    String next = options[(index + 1) % options.length];
                    target.writer().accept(field.path(), next);
                    plugin.reload();
                    refresh.run();
                });
            }
            case TEXT -> {
                String value = target.getString(field.path(), "");
                inv.setItem(slot, buildItem(field.material(), "&e" + field.label(),
                        List.of("<dark_gray>┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈",
                                "&fCurrent: <gradient:#ffd200:#f7971e>" + value + "</gradient>",
                                "<dark_gray>┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈",
                                "&e▶ &7Left-Click &8- &7Edit in chat")));
                holder.setHandler(slot, (player, click) -> plugin.getAnvilInputManager().openTextInput(
                        player, field.label(), value, newValue -> {
                            target.writer().accept(field.path(), newValue);
                            plugin.reload();
                            refresh.run();
                        }));
            }
            case STRINGLIST -> {
                java.util.List<String> values = target.getStringList(field.path());
                String display = values.isEmpty() ? "&8(empty = applies to everything)" : String.join(", ", values);
                inv.setItem(slot, buildItem(field.material(), "&e" + field.label(),
                        List.of("&fCurrent: &e" + display, "&7Comma-separated list.", "&7Click to edit in chat")));
                holder.setHandler(slot, (player, click) -> plugin.getAnvilInputManager().openTextInput(
                        player, field.label() + " (comma-separated, empty = all)", String.join(", ", values), newValue -> {
                            java.util.List<String> parsed = new ArrayList<>();
                            if (!newValue.isBlank()) {
                                for (String part : newValue.split(",")) {
                                    String trimmed = part.trim();
                                    if (!trimmed.isEmpty()) parsed.add(trimmed);
                                }
                            }
                            target.writer().accept(field.path(), parsed);
                            plugin.reload();
                            refresh.run();
                        }));
            }
            case MESSAGE -> {
                ConfigTarget messages = messagesConfig();
                String value = messages.getString(field.path(), "");
                inv.setItem(slot, buildItem(field.material(), "&d" + field.label(),
                        List.of("&fCurrent: &e" + value, "&7Edits messages.yml", "&7Click to edit in chat")));
                holder.setHandler(slot, (player, click) -> plugin.getAnvilInputManager().openTextInput(
                        player, field.label(), value, newValue -> {
                            messages.writer().accept(field.path(), newValue);
                            plugin.reload();
                            refresh.run();
                        }));
            }
            case ACTION -> {
                String styledLabel = field.label().startsWith("&") || field.label().startsWith("<")
                        ? field.label() : "&b&l" + field.label();
                String description = field.path() != null ? field.path() : "&7Click to run";
                inv.setItem(slot, buildItem(field.material(), styledLabel, List.of(description)));
                holder.setHandler(slot, (player, click) -> field.action().run());
            }
        }
    }

    private String[] notificationOptions() {
        return new String[]{"CHAT", "ACTIONBAR", "TITLE", "BOSSBAR"};
    }

    private void addNavButton(Inventory inv, EditorGuiHolder holder, int slot, Material material, String label, Runnable action) {
        addNavButton(inv, holder, slot, material, label, "&7Click to open", action);
    }

    private void addNavButton(Inventory inv, EditorGuiHolder holder, int slot, Material material, String label, String description, Runnable action) {
        inv.setItem(slot, buildItem(material, label, List.of(description)));
        holder.setHandler(slot, (player, click) -> action.run());
    }

    private ItemStack buildItem(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse(name).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(ColorUtil.parse(line).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
