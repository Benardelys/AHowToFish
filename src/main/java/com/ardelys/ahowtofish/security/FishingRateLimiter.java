package com.ardelys.ahowtofish.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FishingRateLimiter {
    private final SecurityManager securityManager;
    private final Map<UUID, Long> castTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCatchTimestamps = new ConcurrentHashMap<>();

    private long minCastDurationMs = 800; 
    private long catchCooldownMs = 1200;  

    public FishingRateLimiter(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setMinCastDurationMs(long ms) {
        this.minCastDurationMs = Math.max(200, ms);
    }

    public void setCatchCooldownMs(long ms) {
        this.catchCooldownMs = Math.max(500, ms);
    }

    public void recordCast(UUID playerUuid) {
        if (playerUuid != null) {
            castTimestamps.put(playerUuid, System.currentTimeMillis());
        }
    }

    public boolean validateAndRecordCatch(UUID playerUuid) {
        if (playerUuid == null) return false;
        if (!securityManager.isFishingRateLimit()) return true;

        long now = System.currentTimeMillis();

        Long castTime = castTimestamps.remove(playerUuid);
        if (castTime == null || (now - castTime) < minCastDurationMs) {
            securityManager.logAlert(
                    SecurityAlertLevel.HIGH,
                    "FishingRateLimiter",
                    "Impossible rapid catch detected! (Duration: " + (castTime != null ? (now - castTime) : "no-cast") + "ms)",
                    playerUuid
            );
            return false;
        }

        Long lastCatch = lastCatchTimestamps.get(playerUuid);
        if (lastCatch != null && (now - lastCatch) < catchCooldownMs) {
            securityManager.logAlert(
                    SecurityAlertLevel.WARNING,
                    "FishingRateLimiter",
                    "Catch rate limit exceeded! (Interval: " + (now - lastCatch) + "ms)",
                    playerUuid
            );
            return false;
        }

        lastCatchTimestamps.put(playerUuid, now);
        return true;
    }

    public void clear(UUID playerUuid) {
        if (playerUuid != null) {
            castTimestamps.remove(playerUuid);
            lastCatchTimestamps.remove(playerUuid);
        }
    }

    public int cleanupStaleEntries(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        int removed = 0;
        var it1 = castTimestamps.entrySet().iterator();
        while (it1.hasNext()) {
            if (it1.next().getValue() < cutoff) {
                it1.remove();
                removed++;
            }
        }
        var it2 = lastCatchTimestamps.entrySet().iterator();
        while (it2.hasNext()) {
            if (it2.next().getValue() < cutoff) {
                it2.remove();
                removed++;
            }
        }
        return removed;
    }

    public int getTrackedPlayerCount() {
        return Math.max(castTimestamps.size(), lastCatchTimestamps.size());
    }

    public void clearAll() {
        castTimestamps.clear();
        lastCatchTimestamps.clear();
    }
}