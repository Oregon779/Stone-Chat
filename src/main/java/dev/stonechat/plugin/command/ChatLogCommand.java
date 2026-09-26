package dev.stonechat.plugin.command;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.manager.ChatLogManager;
import dev.stonechat.plugin.manager.LanguageManager;
import dev.stonechat.plugin.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatLogCommand implements CommandExecutor, TabCompleter {

    private static final String DEFAULT_DATE_FORMAT = "dd.MM.yyyy HH:mm:ss";

    private final StoneChat plugin;

    public ChatLogCommand(StoneChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lm = plugin.getLanguageManager();
        if (!sender.hasPermission("stonechat.chatlog")) {
            sender.sendMessage(lm.getPrefixed("general.no-permission"));
            return true;
        }
        if (!plugin.getConfigManager().isChatLogEnabled()) {
            sender.sendMessage(lm.getPrefixed("chat-log.disabled"));
            return true;
        }
        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(lm.getPrefixed("chat-log.usage"));
            return true;
        }

        int page = 1;
        if (args.length == 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(lm.getPrefixed("chat-log.usage"));
                return true;
            }
        }

        OfflinePlayer target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            target = Bukkit.getOfflinePlayerIfCached(args[0]);
        }
        if (target == null) {
            sender.sendMessage(lm.getPrefixed("chat-log.unknown-player", LanguageManager.placeholders("%player%", args[0])));
            return true;
        }

        String name = target.getName() != null ? target.getName() : args[0];
        plugin.getChatLogManager().readPage(target.getUniqueId(), page, plugin.getConfigManager().getChatLogEntriesPerPage(),
                result -> show(sender, name, result));
        return true;
    }

    private void show(CommandSender sender, String name, ChatLogManager.Page page) {
        LanguageManager lm = plugin.getLanguageManager();
        if (page.totalEntries() == 0) {
            sender.sendMessage(lm.getPrefixed("chat-log.empty", LanguageManager.placeholders("%player%", name)));
            return;
        }

        sender.sendMessage(lm.getPrefixed("chat-log.header", LanguageManager.placeholders(
                "%player%", name,
                "%page%", String.valueOf(page.page()),
                "%pages%", String.valueOf(page.totalPages()),
                "%total%", String.valueOf(page.totalEntries()))));

        DateTimeFormatter formatter = dateFormatter();
        for (ChatLogManager.Entry entry : page.entries()) {
            String time = formatter.format(Instant.ofEpochMilli(entry.timestamp()).atZone(ZoneId.systemDefault()));
            String status = lm.getRaw("chat-log.status." + entry.status().messageKey());
            String template = ColorUtil.toMiniMessageString(lm.getRaw("chat-log.entry",
                            LanguageManager.placeholders("%time%", time, "%status%", status)))
                    .replace("%message%", "<chatlog_message>");
            sender.sendMessage(MiniMessage.miniMessage().deserialize(template,
                    Placeholder.unparsed("chatlog_message", entry.message())));
        }

        if (sender instanceof Player && page.totalPages() > 1) {
            sender.sendMessage(navigation(name, page));
        }
    }

    private Component navigation(String name, ChatLogManager.Page page) {
        LanguageManager lm = plugin.getLanguageManager();
        boolean hasOlder = page.page() < page.totalPages();
        boolean hasNewer = page.page() > 1;

        Component nav = Component.empty();
        if (hasOlder) {
            nav = nav.append(navButton(lm.getRaw("chat-log.older"), name, page.page() + 1));
        }
        if (hasOlder && hasNewer) {
            nav = nav.append(ColorUtil.parse(lm.getRaw("chat-log.separator")));
        }
        if (hasNewer) {
            nav = nav.append(navButton(lm.getRaw("chat-log.newer"), name, page.page() - 1));
        }
        return nav;
    }

    private Component navButton(String label, String name, int targetPage) {
        Component hover = ColorUtil.parse(plugin.getLanguageManager().getRaw("chat-log.nav-hover",
                LanguageManager.placeholders("%page%", String.valueOf(targetPage))));
        return ColorUtil.parse(label)
                .clickEvent(ClickEvent.runCommand("/chatlog " + name + " " + targetPage))
                .hoverEvent(HoverEvent.showText(hover));
    }

    private DateTimeFormatter dateFormatter() {
        try {
            return DateTimeFormatter.ofPattern(plugin.getConfigManager().getChatLogDateFormat());
        } catch (IllegalArgumentException e) {
            return DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !sender.hasPermission("stonechat.chatlog")) {
            return List.of();
        }
        String prefix = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .toList();
    }
}
