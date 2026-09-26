package dev.stonechat.plugin.util;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Crash-safe data file handling: files are replaced atomically (never left half-written), background writes to
 * the same file coalesce into one, and a corrupt file is moved aside instead of being silently overwritten.
 */
public final class DataFiles {

    private final Logger logger;
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "StoneChat-DataWriter");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Path, Supplier<String>> pending = new ConcurrentHashMap<>();

    public DataFiles(Logger logger) {
        this.logger = logger;
    }

    /** Writes the file in the background. {@code content} is evaluated on the writer thread, so it must be thread-safe. */
    public void writeLater(Path file, Supplier<String> content) {
        if (pending.put(file, content) != null) {
            return; // a write for this file is already queued and will pick up the newest content
        }
        try {
            io.execute(() -> flush(file));
        } catch (RejectedExecutionException e) {
            flush(file);
        }
    }

    /** Waits for queued writes, then writes anything still pending on the calling thread. */
    public void shutdown() {
        io.shutdown();
        try {
            io.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (Path file : List.copyOf(pending.keySet())) {
            flush(file);
        }
    }

    private void flush(Path file) {
        Supplier<String> content = pending.remove(file);
        if (content == null) return;
        try {
            writeAtomically(file, content.get());
        } catch (IOException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not save " + file.getFileName(), e);
        }
    }

    public static void writeAtomically(Path file, String content) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, content, StandardCharsets.UTF_8);
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Loads a data file; a corrupt one is renamed to *.corrupt-&lt;time&gt; (kept for recovery) and an empty config returned. */
    public static YamlConfiguration loadYaml(Logger logger, File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        if (!file.exists()) return yaml;
        try {
            yaml.load(file);
            return yaml;
        } catch (IOException | InvalidConfigurationException e) {
            Path backup = file.toPath().resolveSibling(file.getName() + ".corrupt-" + System.currentTimeMillis());
            try {
                Files.move(file.toPath(), backup);
                logger.severe(file.getName() + " was corrupt and has been moved to " + backup.getFileName()
                        + " - starting with an empty file. Error: " + e.getMessage());
            } catch (IOException moveError) {
                logger.log(Level.SEVERE, file.getName() + " is corrupt and could not be moved aside", moveError);
            }
            return new YamlConfiguration();
        }
    }
}
