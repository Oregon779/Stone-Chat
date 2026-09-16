package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.StoneChat;
import dev.stonechat.plugin.util.ColorUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AnvilInputManager implements Listener {

    private final StoneChat plugin;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public AnvilInputManager(StoneChat plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openTextInput(Player player, String title, String currentValue, Consumer<String> onSubmit) {
        player.closeInventory();
        pending.put(player.getUniqueId(), onSubmit);

        player.sendMessage(ColorUtil.parse("&8&m                                                "));
        player.sendMessage(ColorUtil.parse("&e&l" + title));
        player.sendMessage(ColorUtil.parse("&7Type the new value in chat now."));
        if (currentValue != null && !currentValue.isEmpty()) {
            player.sendMessage(ColorUtil.parse("&7Current value: &f" + currentValue));
        }
        player.sendMessage(ColorUtil.parse("&7Type &ccancel &7to abort."));
        player.sendMessage(ColorUtil.parse("&8&m                                                "));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!pending.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Consumer<String> consumer = pending.remove(player.getUniqueId());
        if (consumer == null) return;

        if (text.equalsIgnoreCase("cancel")) {
            player.sendMessage(ColorUtil.parse("&cCancelled."));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> consumer.accept(text));
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public Consumer<String> consumePending(UUID uuid) {
        return pending.remove(uuid);
    }

    public void cancelPending(UUID uuid) {
        pending.remove(uuid);
    }
}
