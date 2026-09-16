package dev.stonechat.plugin.manager;

import net.kyori.adventure.text.Component;
import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.config.ConfigUpdater;
import dev.stonechat.plugin.util.ColorUtil;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final StoneChat plugin;
    private volatile YamlConfiguration messages;
    private volatile String currentLanguage;
    private volatile File messagesFile;

    public LanguageManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    public void load(String language) {
        this.currentLanguage = (language == null || language.isBlank()) ? "en" : language;

        File langFolder = new File(plugin.getDataFolder(), "languages/" + currentLanguage);
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        File file = new File(langFolder, "messages.yml");
        String resourcePath = "languages/" + currentLanguage + "/messages.yml";

        if (!file.exists() && plugin.getResource(resourcePath) == null) {

            plugin.getLogger().warning("No messages.yml found for language '" + currentLanguage + "', falling back to English.");
            this.currentLanguage = "en";
            langFolder = new File(plugin.getDataFolder(), "languages/en");
            file = new File(langFolder, "messages.yml");
            resourcePath = "languages/en/messages.yml";
        }

        this.messagesFile = file;
        this.messages = ConfigUpdater.updateFile(plugin, file, resourcePath);
    }

    public String getLanguage() {
        return currentLanguage;
    }

    public YamlConfiguration rawMessages() {
        return messages;
    }

    public void set(String path, String value) {
        messages.set(path, value);
        try {
            messages.save(messagesFile);
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Could not save messages.yml: " + e.getMessage());
        }
    }

    public String getRaw(String path, Map<String, String> placeholders) {
        String raw = messages.getString(path, path);
        if (placeholders != null) {
            for (var entry : placeholders.entrySet()) {
                raw = raw.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return raw;
    }

    public String getRaw(String path) {
        return getRaw(path, Map.of());
    }

    public Component get(String path, Map<String, String> placeholders) {
        String raw = messages.getString(path, path);
        return ColorUtil.parse(raw, placeholders);
    }

    public Component get(String path) {
        return get(path, Map.of());
    }

    public Component getPrefixed(String path, Map<String, String> placeholders) {
        String prefix = messages.getString("general.prefix", "");
        String raw = prefix + messages.getString(path, path);
        return ColorUtil.parse(raw, placeholders);
    }

    public Component getPrefixed(String path) {
        return getPrefixed(path, Map.of());
    }

    public Component prefix() {
        return ColorUtil.parse(messages.getString("general.prefix", ""));
    }

    public static Map<String, String> placeholders(String... keyValuePairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            map.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }
}
