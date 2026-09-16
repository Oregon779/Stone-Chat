package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import org.bukkit.entity.Player;

public class CapsManager {

    private final StoneChat plugin;

    public CapsManager(StoneChat plugin) {
        this.plugin = plugin;
    }

    public enum Result {
        ALLOWED, BLOCKED, CORRECTED
    }

    public static class CapsCheck {
        public final Result result;
        public final String correctedMessage;

        public CapsCheck(Result result, String correctedMessage) {
            this.result = result;
            this.correctedMessage = correctedMessage;
        }
    }

    public CapsCheck check(Player player, String message) {
        if (!plugin.getConfigManager().isAntiCapsEnabled()) {
            return new CapsCheck(Result.ALLOWED, message);
        }
        if (player.hasPermission(plugin.getConfigManager().getAntiCapsBypassPermission())) {
            return new CapsCheck(Result.ALLOWED, message);
        }
        if (message.length() < plugin.getConfigManager().getAntiCapsMinLength()) {
            return new CapsCheck(Result.ALLOWED, message);
        }

        int letters = 0;
        int upper = 0;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    upper++;
                }
            }
        }
        if (letters == 0) {
            return new CapsCheck(Result.ALLOWED, message);
        }
        int percentage = (int) Math.round((upper * 100.0) / letters);

        if (percentage <= plugin.getConfigManager().getAntiCapsMaxPercentage()) {
            return new CapsCheck(Result.ALLOWED, message);
        }

        if (plugin.getConfigManager().isAntiCapsAutoCorrect()) {
            return new CapsCheck(Result.CORRECTED, message.toLowerCase());
        }
        return new CapsCheck(Result.BLOCKED, message);
    }
}
