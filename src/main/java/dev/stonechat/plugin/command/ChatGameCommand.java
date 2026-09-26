package dev.stonechat.plugin.command;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.chatgame.ChatGame;
import dev.stonechat.plugin.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /chatgame start [id] - starts a specific game, or a random enabled one if no id is given.
 * /chatgame stop - force-stops the currently running round.
 * /chatgame list - lists every configured game and whether it's enabled.
 * <p>
 * Creating games, editing questions/word lists, and configuring rewards
 * all happen in the settings editor (/sc editor -> Chat Games) now - this
 * command is intentionally just a quick way to trigger/stop/inspect games
 * in-game without opening a menu.
 */
public class ChatGameCommand implements CommandExecutor, TabCompleter {

    private final StoneChat plugin;

    public ChatGameCommand(StoneChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ColorUtil.parse("&cUsage: /chatgame <start|stop|list> [id]"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender);
            case "list" -> handleList(sender);
            default -> sender.sendMessage(ColorUtil.parse("&cUsage: /chatgame <start|stop|list> [id]"));
        }
        return true;
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            ChatGame game = plugin.getChatGameManager().getGame(args[1]);
            if (game == null) {
                sender.sendMessage(ColorUtil.parse("&cNo chat game found with id '" + args[1] + "'."));
                return;
            }
            if (!mayStart(sender, game)) {
                sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.no-permission"));
                return;
            }
            boolean started = plugin.getChatGameManager().start(game);
            sender.sendMessage(ColorUtil.parse(started
                    ? "&aStarted chat game '" + game.getId() + "'."
                    : "&cCould not start that game right now (disabled, already running, on cooldown, or not enough players)."));
            return;
        }

        List<ChatGame> enabled = plugin.getChatGameManager().getGames().values().stream()
                .filter(ChatGame::isEnabled)
                .filter(game -> mayStart(sender, game))
                .collect(Collectors.toList());
        if (enabled.isEmpty()) {
            sender.sendMessage(ColorUtil.parse("&cNo chat games are currently enabled."));
            return;
        }
        ChatGame random = enabled.get(new java.util.Random().nextInt(enabled.size()));
        boolean started = plugin.getChatGameManager().start(random);
        sender.sendMessage(ColorUtil.parse(started
                ? "&aStarted chat game '" + random.getId() + "'."
                : "&cCould not start a chat game right now (already running or on cooldown)."));
    }

    /** A game's optional extra permission is required on top of stonechat.chatgame to start it by hand. */
    private boolean mayStart(CommandSender sender, ChatGame game) {
        return game.getPermission().isBlank() || sender.hasPermission(game.getPermission());
    }

    private void handleStop(CommandSender sender) {
        boolean stopped = plugin.getChatGameManager().stop(sender.getName());
        sender.sendMessage(ColorUtil.parse(stopped ? "&aStopped the running chat game." : "&cNo chat game is currently running."));
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse("&e&lChat Games:"));
        for (ChatGame game : plugin.getChatGameManager().getGames().values()) {
            String status = game.isEnabled() ? "&aEnabled" : "&7Disabled";
            String kind = game.isDefaultGame() ? "&7(built-in " + game.getType().name() + ")" : "&7(custom)";
            sender.sendMessage(ColorUtil.parse("  &f" + game.getId() + " &8- " + status + " &8" + kind));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("start", "stop", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            List<String> ids = new ArrayList<>();
            for (ChatGame game : plugin.getChatGameManager().getGames().values()) {
                if (game.getId().toLowerCase().startsWith(args[1].toLowerCase())) ids.add(game.getId());
            }
            return ids;
        }
        return List.of();
    }
}
