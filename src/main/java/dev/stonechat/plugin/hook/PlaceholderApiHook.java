package dev.stonechat.plugin.hook;

import dev.stonechat.plugin.StoneChat;

/**
 * The only door to PlaceholderAPI classes. Stone Chat calls this only after checking that PlaceholderAPI is
 * installed, so the plugin still loads without it: no other class mentions a PlaceholderAPI type.
 */
public final class PlaceholderApiHook {

    private PlaceholderApiHook() {
    }

    /** Registers the %stonechat_...% placeholders and returns how to remove them again. */
    public static Runnable register(StoneChat plugin) {
        StoneChatExpansion expansion = new StoneChatExpansion(plugin);
        if (!expansion.register()) {
            plugin.getLogger().warning("PlaceholderAPI refused the %stonechat_...% placeholders.");
            return () -> {
            };
        }
        return expansion::unregister;
    }
}
