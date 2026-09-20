package com.ardelys.ahowtofish.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SellLockManager {
    private final Map<UUID, Long> activeSellLocks = new ConcurrentHashMap<>();
    private static final long LOCK_EXPIRATION_MS = 5000; 

    public synchronized boolean acquireLock(UUID playerUuid) {
        if (playerUuid == null) return false;
        long now = System.currentTimeMillis();

        Long existingTime = activeSellLocks.get(playerUuid);
        if (existingTime != null && (now - existingTime) < LOCK_EXPIRATION_MS) {
            return false; 
        }

        activeSellLocks.put(playerUuid, now);
        return true;
    }

    public synchronized void releaseLock(UUID playerUuid) {
        if (playerUuid != null) {
            activeSellLocks.remove(playerUuid);
        }
    }

    public boolean isLocked(UUID playerUuid) {
        if (playerUuid == null) return false;
        Long time = activeSellLocks.get(playerUuid);
        return time != null && (System.currentTimeMillis() - time) < LOCK_EXPIRATION_MS;
    }

    public void clearAll() {
        activeSellLocks.clear();
    }
}