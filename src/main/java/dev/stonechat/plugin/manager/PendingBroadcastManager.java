package dev.stonechat.plugin.manager;

import dev.stonechat.plugin.manager.BroadcastManager.Type;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PendingBroadcastManager {

    public enum Stage { AWAITING_MESSAGE, AWAITING_CONFIRM }

    public record PendingBroadcast(Type type, Integer durationSeconds, Stage stage, String message) {

        PendingBroadcast withMessageAwaitingConfirm(String message) {
            return new PendingBroadcast(type, durationSeconds, Stage.AWAITING_CONFIRM, message);
        }
    }

    private final Map<UUID, PendingBroadcast> pending = new ConcurrentHashMap<>();

    public void request(UUID uuid, Type type, Integer durationSeconds) {
        pending.put(uuid, new PendingBroadcast(type, durationSeconds, Stage.AWAITING_MESSAGE, null));
    }

    public void advanceToConfirm(UUID uuid, String message) {
        PendingBroadcast current = pending.get(uuid);
        if (current == null) return;
        pending.put(uuid, current.withMessageAwaitingConfirm(message));
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public PendingBroadcast peek(UUID uuid) {
        return pending.get(uuid);
    }

    public PendingBroadcast consume(UUID uuid) {
        return pending.remove(uuid);
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }
}
