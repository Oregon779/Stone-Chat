package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Posts the messages from config.yml -> auto-messages one at a time, every interval-seconds. */
public class AutoMessageManager {

    private static final String MESSAGES_PATH = "auto-messages.messages";

    private final StoneChat plugin;
    private final Random random = new Random();
    private BukkitTask task;
    private int nextIndex;
    private int lastIndex = -1;

    public AutoMessageManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        nextIndex = 0;
        lastIndex = -1;

        ConfigManager config = plugin.getConfigManager();
        if (!config.isAutoMessagesEnabled() || config.getAutoMessages().isEmpty()) return;

        long intervalTicks = Math.max(1, config.getAutoMessagesIntervalSeconds()) * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Bukkit.getOnlinePlayers().size() < plugin.getConfigManager().getAutoMessagesMinPlayers()) return;
            sendNext();
        }, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void sendNext() {
        ConfigManager config = plugin.getConfigManager();
        List<List<String>> messages = config.getAutoMessages();
        if (messages.isEmpty()) return;

        List<String> lines = messages.get(pickIndex(messages.size(), config.isAutoMessagesRandomOrder()));
        BroadcastManager.Type type = config.getAutoMessagesDisplayType();
        if (type == BroadcastManager.Type.CHAT) {
            for (int i = 0; i < lines.size(); i++) {
                String line = i == 0 ? config.getAutoMessagesPrefix() + lines.get(i) : lines.get(i);
                plugin.getBroadcastManager().broadcastWithoutPrefix(type, line);
            }
        } else {
            plugin.getBroadcastManager().broadcastWithoutPrefix(type, lines.get(0));
        }
        plugin.getSoundManager().playToAll("auto-messages.sound");
    }

    private int pickIndex(int size, boolean randomOrder) {
        if (size == 1) return 0;
        if (randomOrder) {
            int index;
            do {
                index = random.nextInt(size);
            } while (index == lastIndex);
            lastIndex = index;
            return index;
        }
        int index = nextIndex % size;
        nextIndex = (index + 1) % size;
        return index;
    }

    /** Appends a one-line message, used by the in-game editor. Multi-line messages are written in config.yml. */
    public void addMessage(String line) {
        List<Object> messages = new ArrayList<>(rawMessages());
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("lines", List.of(line));
        messages.add(entry);
        plugin.getConfigManager().set(MESSAGES_PATH, messages);
    }

    public void removeLastMessage() {
        List<Object> messages = new ArrayList<>(rawMessages());
        if (messages.isEmpty()) return;
        messages.remove(messages.size() - 1);
        plugin.getConfigManager().set(MESSAGES_PATH, messages);
    }

    private List<?> rawMessages() {
        List<?> list = plugin.getConfigManager().raw().getList(MESSAGES_PATH);
        return list != null ? list : List.of();
    }
}
