package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.util.ColorUtil;
import dev.stonechat.plugin.util.DataFiles;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /socialspy: staff with stonechat.socialspy can see a copy of every private message. Who turned it on is kept
 * in socialspy.yml, so it stays on after a restart.
 */
public class SocialSpyManager {

    public static final String PERMISSION = "stonechat.socialspy";

    private final StoneChat plugin;
    private final File file;
    private final Set<UUID> spies = ConcurrentHashMap.newKeySet();

    public SocialSpyManager(StoneChat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "socialspy.yml");
        for (String entry : DataFiles.loadYaml(plugin.getLogger(), file).getStringList("spying")) {
            try {
                spies.add(UUID.fromString(entry));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public boolean isSpying(Player player) {
        return isSpying(player.getUniqueId());
    }

    public boolean isSpying(UUID uuid) {
        return spies.contains(uuid);
    }

    /** @return true if social spy is on now */
    public boolean toggle(Player player) {
        boolean nowSpying = !spies.remove(player.getUniqueId());
        if (nowSpying) {
            spies.add(player.getUniqueId());
        }
        plugin.getDataFiles().writeLater(file.toPath(), () -> {
            YamlConfiguration data = new YamlConfiguration();
            data.set("spying", spies.stream().map(UUID::toString).sorted().toList());
            return data.saveToString();
        });
        return nowSpying;
    }

    /** Sends a copy of a delivered private message to every spy except the two players involved. */
    public void relay(Player sender, Player receiver, String deliveredMessage) {
        if (!plugin.getConfigManager().isSocialSpyEnabled() || spies.isEmpty()) return;

        String template = ColorUtil.toMiniMessageString(plugin.getLanguageManager().getRaw("private-message.spy-format",
                        LanguageManager.placeholders("%sender%", sender.getName(), "%receiver%", receiver.getName())))
                .replace("%message%", "<spy_message>");
        var copy = MiniMessage.miniMessage().deserialize(template, Placeholder.unparsed("spy_message", deliveredMessage));

        for (Player spy : Bukkit.getOnlinePlayers()) {
            if (spy.equals(sender) || spy.equals(receiver)) continue;
            if (spies.contains(spy.getUniqueId()) && spy.hasPermission(PERMISSION)) {
                spy.sendMessage(copy);
            }
        }
    }
}
