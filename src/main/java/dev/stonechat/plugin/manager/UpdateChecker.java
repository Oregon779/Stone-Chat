package dev.stonechat.plugin.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.stonechat.plugin.StoneChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;

public class UpdateChecker implements Listener {

    private static final String MODRINTH_PROJECT_SLUG = "stone-chat";

    private final StoneChat plugin;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private BukkitTask task;
    private Boolean scheduledEnabled;
    private Integer scheduledIntervalMinutes;
    private volatile String latestKnownVersion = null;

    public String getLatestKnownVersion() {
        return latestKnownVersion;
    }
    private volatile int versionsBehind = -1;

    public UpdateChecker(StoneChat plugin) {
        this.plugin = plugin;
    }

    /** (Re)schedules the periodic check. A no-op if the settings didn't change, so plugin reloads don't re-query Modrinth. */
    public void start() {
        boolean enabled = plugin.getConfigManager().isUpdateCheckerEnabled();
        int intervalMinutes = plugin.getConfigManager().getUpdateCheckerIntervalMinutes();
        if (Boolean.valueOf(enabled).equals(scheduledEnabled) && Integer.valueOf(intervalMinutes).equals(scheduledIntervalMinutes)) {
            return;
        }
        cancelTask();
        scheduledEnabled = enabled;
        scheduledIntervalMinutes = intervalMinutes;
        if (!enabled) {
            plugin.getLogger().info("Update checker is turned off (update-checker.enabled: false in config.yml) - this is not an error, just a status note.");
            return;
        }
        long intervalTicks = Math.max(5, intervalMinutes) * 60L * 20L;
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::check, 100L, intervalTicks);
    }

    public void checkNow() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::check);
    }

    public void stop() {
        cancelTask();
        scheduledEnabled = null;
        scheduledIntervalMinutes = null;
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void shutdown() {
        stop();
        httpClient.shutdownNow();
    }

    private void check() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT_SLUG + "/version"))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "StonePlugins/StoneChat update-checker")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                plugin.getLogger().warning("Update checker: Modrinth responded with status " + response.statusCode() + " for project '" + MODRINTH_PROJECT_SLUG + "'.");
                return;
            }

            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            if (versions.isEmpty()) {
                plugin.getLogger().info("Update checker: project '" + MODRINTH_PROJECT_SLUG + "' found, but no version uploaded yet.");
                return;
            }

            String newest = versions.get(0).getAsJsonObject().get("version_number").getAsString();
            String current = plugin.getPluginMeta().getVersion();

            if (isNewer(newest, current)) {
                latestKnownVersion = newest;
                versionsBehind = countVersionsBehind(versions, current);
                logToConsole(newest, current);
                Bukkit.getScheduler().runTask(plugin, () -> notifyOnlineEligiblePlayers(newest, current));
            } else {
                latestKnownVersion = null;
                versionsBehind = -1;
                plugin.getLogger().info("No new version available (running " + current + ").");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Update checker: check failed (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
        }
    }

    private void logToConsole(String newest, String current) {
        String behind = versionsBehind < 0 ? "an unknown number of versions" : versionsBehind + " version(s)";
        plugin.getLogger().info("A new version of Stone Chat is available: " + newest + " (you're on " + current + ", " + behind
                + " behind). Get it at https://modrinth.com/project/" + MODRINTH_PROJECT_SLUG);
    }

    private int countVersionsBehind(JsonArray versions, String current) {
        for (int i = 0; i < versions.size(); i++) {
            String versionNumber = versions.get(i).getAsJsonObject().get("version_number").getAsString();
            if (versionNumber.equals(current)) {
                return i;
            }
        }
        return -1;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String newest = latestKnownVersion;
        if (newest == null || !isEligible(player)) {
            return;
        }
        notifyPlayer(player, newest, plugin.getPluginMeta().getVersion());
    }

    private boolean isEligible(Player player) {
        return player.isOp() || player.hasPermission("stonechat.admin");
    }

    private void notifyOnlineEligiblePlayers(String newest, String current) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isEligible(player)) {
                notifyPlayer(player, newest, current);
            }
        }
    }

    private void notifyPlayer(Player player, String newest, String current) {
        LanguageManager lm = plugin.getLanguageManager();
        HashMap<String, String> behindPlaceholder = new HashMap<>();
        behindPlaceholder.put("%count%", String.valueOf(versionsBehind));
        String behindText = versionsBehind < 0
                ? lm.getRaw("update.versions-behind-unknown")
                : lm.getRaw("update.versions-behind", behindPlaceholder);

        HashMap<String, String> placeholders = new HashMap<>();
        placeholders.put("%version%", newest);
        placeholders.put("%current%", current);
        placeholders.put("%behind%", behindText);
        player.sendMessage(lm.getPrefixed("update.available", placeholders));
    }

    private boolean isNewer(String remote, String current) {
        try {
            String[] remoteParts = remote.split("\\.");
            String[] currentParts = current.split("\\.");
            int length = Math.max(remoteParts.length, currentParts.length);
            for (int i = 0; i < length; i++) {
                int r = i < remoteParts.length ? parsePart(remoteParts[i]) : 0;
                int c = i < currentParts.length ? parsePart(currentParts[i]) : 0;
                if (r != c) {
                    return r > c;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private int parsePart(String part) {
        StringBuilder digits = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (!Character.isDigit(c)) {
                break;
            }
            digits.append(c);
        }
        return digits.isEmpty() ? 0 : Integer.parseInt(digits.toString());
    }
}
