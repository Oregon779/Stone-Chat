package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.util.DataFiles;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class IgnoreManager {

    private final StoneChat plugin;
    private final Map<UUID, Set<UUID>> ignoredPlayers = new ConcurrentHashMap<>();
    private File file;

    public IgnoreManager(StoneChat plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "ignorelist.yml");

        ignoredPlayers.clear();
        YamlConfiguration data = DataFiles.loadYaml(plugin.getLogger(), file);
        for (String key : data.getKeys(false)) {
            UUID owner;
            try {
                owner = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            Set<UUID> set = ConcurrentHashMap.newKeySet();
            for (String entry : data.getStringList(key)) {
                try {
                    set.add(UUID.fromString(entry));
                } catch (IllegalArgumentException ignored) {

                }
            }
            if (!set.isEmpty()) {
                ignoredPlayers.put(owner, set);
            }
        }
    }

    public boolean isIgnoring(UUID owner, UUID target) {
        Set<UUID> set = ignoredPlayers.get(owner);
        return set != null && set.contains(target);
    }

    public Set<UUID> getIgnored(UUID owner) {
        return ignoredPlayers.getOrDefault(owner, Set.of());
    }

    public boolean toggleIgnore(UUID owner, UUID target) {
        Set<UUID> set = ignoredPlayers.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet());
        boolean nowIgnoring;
        if (set.contains(target)) {
            set.remove(target);
            nowIgnoring = false;
        } else {
            set.add(target);
            nowIgnoring = true;
        }
        plugin.getDataFiles().writeLater(file.toPath(), this::serialize);
        return nowIgnoring;
    }

    private String serialize() {
        YamlConfiguration data = new YamlConfiguration();
        for (var entry : ignoredPlayers.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            List<String> list = entry.getValue().stream().map(UUID::toString).sorted().collect(Collectors.toList());
            data.set(entry.getKey().toString(), list);
        }
        return data.saveToString();
    }
}
