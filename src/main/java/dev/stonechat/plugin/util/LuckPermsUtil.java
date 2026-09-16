package dev.stonechat.plugin.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Talks to LuckPerms via reflection (no hard dependency, so the plugin still
 * works fine without LuckPerms installed). Every {@code Method} we need is
 * resolved exactly once via {@link #init()} and cached in a static field -
 * {@code getPrimaryGroup}/{@code getPrefix}/{@code getSuffix} can all be
 * called on the chat hot path (once per message, for the hover tooltip), so
 * repeating {@code Class.getMethod(...)} on every call - which walks the
 * class's method table each time - would be a real, avoidable cost at high
 * message throughput. Resolving once and invoking a cached {@link Method}
 * afterwards is a plain reflective call with no lookup overhead.
 */
public final class LuckPermsUtil {

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;

    private static Object luckPermsInstance;
    private static Method getUserManager;
    private static Method getUser;
    private static Method getPrimaryGroupMethod;
    private static Method getCachedData;
    private static Method getMetaData;
    private static Method getPrefixMethod;
    private static Method getSuffixMethod;

    private LuckPermsUtil() {
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> lpClass = Class.forName("net.luckperms.api.LuckPerms");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(lpClass);
            if (registration == null) {
                available = false;
                return;
            }
            luckPermsInstance = registration.getProvider();

            getUserManager = lpClass.getMethod("getUserManager");
            Class<?> userManagerClass = getUserManager.getReturnType();
            getUser = userManagerClass.getMethod("getUser", UUID.class);

            Class<?> userClass = getUser.getReturnType();
            getPrimaryGroupMethod = userClass.getMethod("getPrimaryGroup");
            getCachedData = userClass.getMethod("getCachedData");

            Class<?> cachedDataClass = getCachedData.getReturnType();
            getMetaData = cachedDataClass.getMethod("getMetaData");

            Class<?> metaDataClass = getMetaData.getReturnType();
            getPrefixMethod = metaDataClass.getMethod("getPrefix");
            getSuffixMethod = metaDataClass.getMethod("getSuffix");

            available = true;
        } catch (Throwable t) {
            available = false;
        }
    }

    private static Object fetchUser(Player player) throws Exception {
        Object userManager = getUserManager.invoke(luckPermsInstance);
        return getUser.invoke(userManager, player.getUniqueId());
    }

    public static String getPrimaryGroup(Player player) {
        init();
        if (!available) return null;
        try {
            Object user = fetchUser(player);
            if (user == null) return null;
            Object group = getPrimaryGroupMethod.invoke(user);
            return group != null ? group.toString() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getPrefix(Player player) {
        init();
        return getMetaValue(player, getPrefixMethod);
    }

    public static String getSuffix(Player player) {
        init();
        return getMetaValue(player, getSuffixMethod);
    }

    private static String getMetaValue(Player player, Method metaMethod) {
        if (!available) return null;
        try {
            Object user = fetchUser(player);
            if (user == null) return null;
            Object cachedData = getCachedData.invoke(user);
            Object metaData = getMetaData.invoke(cachedData);
            Object value = metaMethod.invoke(metaData);
            return value != null ? value.toString() : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
