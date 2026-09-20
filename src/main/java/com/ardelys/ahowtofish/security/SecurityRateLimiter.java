package com.ardelys.ahowtofish.security;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SecurityRateLimiter {
    private final Map<RateLimitCategory, Integer> limits = new EnumMap<>(RateLimitCategory.class);
    private final Map<UUID, Map<RateLimitCategory, Deque<Long>>> userActivity = new ConcurrentHashMap<>();
    private static final long WINDOW_MS = 1000L;

    public SecurityRateLimiter() {
        for (RateLimitCategory cat : RateLimitCategory.values()) {
            limits.put(cat, cat.getDefaultLimitPerSecond());
        }
    }

    public void setLimit(RateLimitCategory category, int limitPerSecond) {
        if (category != null && limitPerSecond > 0) {
            limits.put(category, limitPerSecond);
        }
    }

    public int getLimit(RateLimitCategory category) {
        return limits.getOrDefault(category, category != null ? category.getDefaultLimitPerSecond() : 10);
    }

    public boolean tryAcquire(UUID playerUuid, RateLimitCategory category) {
        if (playerUuid == null || category == null) return true;

        int maxAllowed = getLimit(category);
        long now = System.currentTimeMillis();

        Map<RateLimitCategory, Deque<Long>> catMap = userActivity.computeIfAbsent(playerUuid, u -> new ConcurrentHashMap<>());
        Deque<Long> timestamps = catMap.computeIfAbsent(category, c -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            long cutoff = now - WINDOW_MS;
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxAllowed) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }

    public void clear(UUID playerUuid) {
        if (playerUuid != null) {
            userActivity.remove(playerUuid);
        }
    }

    public void cleanupExpired() {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        for (Iterator<Map.Entry<UUID, Map<RateLimitCategory, Deque<Long>>>> it = userActivity.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Map<RateLimitCategory, Deque<Long>>> entry = it.next();
            Map<RateLimitCategory, Deque<Long>> catMap = entry.getValue();
            catMap.values().removeIf(timestamps -> {
                synchronized (timestamps) {
                    while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                        timestamps.pollFirst();
                    }
                    return timestamps.isEmpty();
                }
            });
            if (catMap.isEmpty()) {
                it.remove();
            }
        }
    }

    public void cleanupStaleEntries(long maxAgeMs) {
        cleanupExpired();
    }

    public int getTrackedPlayerCount() {
        return userActivity.size();
    }
}
