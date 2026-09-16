package dev.stonechat.plugin.chatgame;

import java.util.List;

/**
 * Describes what a winner receives. Any part can be empty/disabled - an
 * admin might only want a sound and no money, or only a command and no
 * particle, etc. A game with no reward override falls back to the global
 * default reward configured under chat-games.default-reward in chatgames.yml.
 */
public record RewardConfig(
        List<String> consoleCommands,
        double vaultMoney,
        boolean soundEnabled,
        String soundName,
        float soundVolume,
        float soundPitch,
        boolean particleEnabled,
        String particleName,
        int particleCount
) {

    public static RewardConfig none() {
        return new RewardConfig(List.of(), 0, false, "ENTITY_PLAYER_LEVELUP", 1f, 1f, false, "TOTEM", 20);
    }
}
