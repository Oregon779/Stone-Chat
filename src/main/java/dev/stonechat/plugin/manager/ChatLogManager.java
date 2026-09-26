package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Stores every public chat message per player in chatlogs/&lt;uuid&gt;.log, one
 * tab-separated line per message. All file access runs on a single background
 * thread, so chat never waits on disk and reads always see completed writes.
 */
public class ChatLogManager {

    public enum Status {
        SENT("sent", false),
        CENSORED("censored", false),
        CHAT_GAME("chat-game", false),
        BLOCKED_WORD_FILTER("word-filter", true),
        BLOCKED_LINK("link", true),
        BLOCKED_CAPS("caps", true),
        BLOCKED_MUTE("mute", true),
        BLOCKED_COOLDOWN("cooldown", true),
        BLOCKED_JOIN_DELAY("join-delay", true),
        BLOCKED_LENGTH("length", true);

        private final String messageKey;
        private final boolean blocked;

        Status(String messageKey, boolean blocked) {
            this.messageKey = messageKey;
            this.blocked = blocked;
        }

        public String messageKey() {
            return messageKey;
        }
    }

    public record Entry(long timestamp, Status status, String playerName, String message) {
    }

    public record Page(List<Entry> entries, int page, int totalPages, int totalEntries) {
    }

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private final StoneChat plugin;
    private final Path folder;
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "StoneChat-ChatLog");
        thread.setDaemon(true);
        return thread;
    });
    // Only ever touched from the io thread.
    private final Map<UUID, Integer> lineCounts = new HashMap<>();

    public ChatLogManager(StoneChat plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "chatlogs").toPath();
        submit(this::pruneAll);
    }

    public void record(Player player, String message, Status status) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isChatLogEnabled()) return;
        if (status.blocked && !config.isChatLogBlockedMessages()) return;

        UUID uuid = player.getUniqueId();
        String line = System.currentTimeMillis() + "\t" + status.name() + "\t" + player.getName() + "\t"
                + message.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
        submit(() -> append(uuid, line));
    }

    /** Page 1 holds the newest entries; entries within a page are oldest-first, like chat. */
    public void readPage(UUID uuid, int page, int pageSize, Consumer<Page> callback) {
        submit(() -> {
            List<Entry> entries = readEntries(logFile(uuid));
            int total = entries.size();
            int size = Math.max(1, pageSize);
            int totalPages = Math.max(1, (total + size - 1) / size);
            int current = Math.max(1, Math.min(page, totalPages));

            int end = total - (current - 1) * size;
            int start = Math.max(0, end - size);
            List<Entry> slice = total == 0 ? List.of() : List.copyOf(entries.subList(start, end));

            Page result = new Page(slice, current, totalPages, total);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    public void forget(UUID uuid) {
        submit(() -> lineCounts.remove(uuid));
    }

    public void shutdown() {
        io.shutdown();
        try {
            if (!io.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Chat log writer did not finish in time, some entries may be missing.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void submit(Runnable task) {
        try {
            io.execute(task);
        } catch (RejectedExecutionException ignored) {
            // Plugin is shutting down.
        }
    }

    private Path logFile(UUID uuid) {
        return folder.resolve(uuid + ".log");
    }

    private void append(UUID uuid, String line) {
        Path file = logFile(uuid);
        try {
            Files.createDirectories(folder);
            Files.writeString(file, line + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not write chat log for " + uuid, e);
            return;
        }

        Integer known = lineCounts.get(uuid);
        int count = known == null ? readLines(file).size() : known + 1;
        int max = plugin.getConfigManager().getChatLogMaxEntries();
        // Compact with some headroom so a full log isn't rewritten on every single message.
        if (max > 0 && count > max + Math.max(10, max / 10)) {
            count = compact(file);
        }
        lineCounts.put(uuid, count);
    }

    private void pruneAll() {
        if (!Files.isDirectory(folder)) return;
        try (var files = Files.list(folder)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".log")).toList()) {
                compact(file);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not clean up old chat logs", e);
        }
    }

    /** Drops entries past the retention period and beyond the per-player limit. Returns the remaining entry count. */
    private int compact(Path file) {
        List<String> lines = readLines(file);
        long cutoff = retentionCutoff();
        List<String> kept = new ArrayList<>(lines.size());
        for (String line : lines) {
            Entry entry = parse(line);
            if (entry != null && entry.timestamp() >= cutoff) {
                kept.add(line);
            }
        }

        int max = plugin.getConfigManager().getChatLogMaxEntries();
        if (max > 0 && kept.size() > max) {
            kept = kept.subList(kept.size() - max, kept.size());
        }
        if (kept.size() == lines.size()) {
            return kept.size();
        }

        try {
            if (kept.isEmpty()) {
                Files.deleteIfExists(file);
            } else {
                Path temp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.write(temp, kept, StandardCharsets.UTF_8);
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not compact chat log " + file.getFileName(), e);
            return lines.size();
        }
        return kept.size();
    }

    private List<Entry> readEntries(Path file) {
        long cutoff = retentionCutoff();
        List<Entry> entries = new ArrayList<>();
        for (String line : readLines(file)) {
            Entry entry = parse(line);
            if (entry != null && entry.timestamp() >= cutoff) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private List<String> readLines(Path file) {
        if (!Files.exists(file)) return List.of();
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read chat log " + file.getFileName(), e);
            return List.of();
        }
    }

    private long retentionCutoff() {
        int days = plugin.getConfigManager().getChatLogRetentionDays();
        return days <= 0 ? Long.MIN_VALUE : System.currentTimeMillis() - days * DAY_MILLIS;
    }

    private Entry parse(String line) {
        String[] parts = line.split("\t", 4);
        if (parts.length < 4) return null;
        try {
            return new Entry(Long.parseLong(parts[0]), Status.valueOf(parts[1]), parts[2], parts[3]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
