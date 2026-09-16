package dev.stonechat.plugin.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

/**
 * Deposits Vault Economy money into a winner's account, if Vault (and an
 * economy plugin behind it) is installed. Reflection-based so the plugin
 * has no hard dependency on Vault - if it's absent, every method here just
 * quietly does nothing. Every reflective {@link Method} is resolved once
 * and cached, since chat game rewards are handed out on the chat hot path.
 */
public final class VaultEconomyUtil {

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;

    private static Object economy;
    private static Method depositPlayer;

    private VaultEconomyUtil() {
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(economyClass);
            if (registration == null) {
                available = false;
                return;
            }
            economy = registration.getProvider();
            depositPlayer = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            available = true;
        } catch (Throwable t) {
            available = false;
        }
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    /** Deposits {@code amount} into the player's Vault economy balance. No-op (returns false) if Vault isn't present or amount is not positive. */
    public static boolean deposit(OfflinePlayer player, double amount) {
        init();
        if (!available || amount <= 0) return false;
        try {
            depositPlayer.invoke(economy, player, amount);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
