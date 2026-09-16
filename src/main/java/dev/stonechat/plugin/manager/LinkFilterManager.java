package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;

import java.util.ArrayList;
import java.util.List;

public class LinkFilterManager {

    private final StoneChat plugin;
    private volatile List<String> lowerCasePatterns = List.of();

    public LinkFilterManager(StoneChat plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        List<String> patterns = plugin.getConfigManager().getLinkPatterns();
        List<String> lowered = new ArrayList<>(patterns.size());
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) continue;
            lowered.add(pattern.toLowerCase());
        }
        this.lowerCasePatterns = lowered;
    }

    public boolean containsLink(String message) {
        if (!plugin.getConfigManager().isLinkBlockerEnabled() || lowerCasePatterns.isEmpty()) return false;

        String lower = message.toLowerCase();
        for (String pattern : lowerCasePatterns) {
            if (lower.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
