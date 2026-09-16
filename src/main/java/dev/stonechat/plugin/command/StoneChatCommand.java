package dev.stonechat.plugin.command;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StoneChatCommand implements CommandExecutor, TabCompleter {

    private final StoneChat plugin;

    public StoneChatCommand(StoneChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "help" -> sendHelp(sender);
            case "checkupdate" -> handleCheckUpdate(sender);
            case "togglepings" -> handleTogglePings(sender);
            case "edit", "editor" -> handleEdit(sender);
            case "settings", "menu" -> handleSettings(sender);
            default -> sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.unknown-command"));
        }
        return true;
    }

    private void handleSettings(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.player-only"));
            return;
        }
        plugin.getPlayerSettingsGuiManager().open(player);
    }

    private void handleEdit(CommandSender sender) {
        if (!sender.hasPermission("stonechat.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.player-only"));
            return;
        }
        plugin.getSettingsEditorManager().openMainMenu(player);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("stonechat.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.no-permission"));
            return;
        }
        plugin.reload();
        sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.reloaded"));
    }

    private void handleCheckUpdate(CommandSender sender) {
        if (!sender.hasPermission("stonechat.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.no-permission"));
            return;
        }
        sender.sendMessage(plugin.getLanguageManager().getPrefixed("update.check-triggered"));
        plugin.getUpdateChecker().checkNow();
    }

    private void handleTogglePings(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("general.player-only"));
            return;
        }
        boolean nowIgnoring = plugin.getPingManager().toggleIgnorePings(player);
        String path = nowIgnoring ? "ping.toggled-on" : "ping.toggled-off";
        player.sendMessage(plugin.getLanguageManager().getPrefixed(path));
    }

    private void sendHelp(CommandSender sender) {
        var lm = plugin.getLanguageManager();

        if (sender.hasPermission("stonechat.admin")) {
            sender.sendMessage(lm.get("help.header"));
            sender.sendMessage(lm.get("help.reload"));
            sender.sendMessage(lm.get("help.checkupdate"));
            sender.sendMessage(lm.get("help.edit"));
            sender.sendMessage(lm.get("help.settings"));
            sender.sendMessage(lm.get("help.help"));
            sender.sendMessage(lm.get("help.togglepings"));
            if (sender.hasPermission("stonechat.mute")) {
                sender.sendMessage(lm.get("help.chatmute"));
            }
            if (sender.hasPermission("stonechat.chatgame")) {
                sender.sendMessage(lm.get("help.chatgame"));
            }
            if (sender.hasPermission("stonechat.broadcast")) {
                sender.sendMessage(lm.get("help.broadcast"));
            }
            if (canUseChatColor(sender)) {
                sender.sendMessage(lm.get("help.chatcolor"));
            }
            sender.sendMessage(lm.get("help.footer"));
            return;
        }

        sender.sendMessage(lm.get("help.header"));
        sender.sendMessage(lm.get("help.settings"));
        sender.sendMessage(lm.get("help.togglepings"));
        if (canUseChatColor(sender)) {
            sender.sendMessage(lm.get("help.chatcolor"));
        }
        sender.sendMessage(lm.get("help.footer"));
    }

    private boolean canUseChatColor(CommandSender sender) {
        if (!plugin.getConfigManager().isChatColorGuiEnabled()) return false;
        String usePermission = plugin.getConfigManager().getChatColorGuiUsePermission();
        return usePermission.isBlank() || sender.hasPermission(usePermission);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("help", "togglepings", "settings"));
            if (sender.hasPermission("stonechat.admin")) {
                options.add("reload");
                options.add("checkupdate");
                options.add("editor");
            }
            return options.stream()
                    .filter(o -> o.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
