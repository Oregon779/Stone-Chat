package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PunishmentManager {

    private final StoneChat plugin;

    public PunishmentManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, String configPath) {
        String command = plugin.getConfigManager().raw().getString(configPath, "");
        if (command == null || command.isBlank()) return;

        String finalCommand = command.replace("%player%", player.getName());
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
    }
}
