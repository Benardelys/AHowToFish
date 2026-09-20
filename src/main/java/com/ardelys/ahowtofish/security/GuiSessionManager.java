package com.ardelys.ahowtofish.security;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuiSessionManager {
    private final SecurityManager securityManager;
    private final Map<UUID, GuiSession> activeSessions = new ConcurrentHashMap<>();
    private static final long DEFAULT_EXPIRATION_MS = 10 * 60 * 1000L; 

    public GuiSessionManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public GuiSession createSession(Player player, String guiType) {
        if (player == null) return null;
        return createSession(player.getUniqueId(), guiType, null);
    }

    public GuiSession createSession(UUID playerUuid, String guiType) {
        return createSession(playerUuid, guiType, null);
    }

    public GuiSession createSession(UUID playerUuid, String guiType, UUID sessionId) {
        if (playerUuid == null) return null;
        if (sessionId == null) sessionId = UUID.randomUUID();
        long now = System.currentTimeMillis();
        long timeoutMs = securityManager != null ? securityManager.getGuiSessionTimeoutMs() : DEFAULT_EXPIRATION_MS;
        GuiSession session = new GuiSession(playerUuid, guiType, sessionId, now, now + timeoutMs);
        activeSessions.put(playerUuid, session);
        return session;
    }

    public boolean validateSession(Player player, String guiType) {
        if (player == null) return false;
        return validateSession(player.getUniqueId(), guiType);
    }

    public boolean validateSession(UUID playerUuid, String guiType) {
        if (playerUuid == null) return false;
        if (securityManager != null && !securityManager.isGuiSessionValidation()) return true;

        GuiSession session = activeSessions.get(playerUuid);
        if (session == null || !session.isValid(playerUuid, guiType)) {
            if (securityManager != null) {
                securityManager.logAlert(
                        SecurityAlertLevel.HIGH,
                        "GuiSessionManager",
                        "Invalid or expired GUI session interaction rejected!",
                        playerUuid
                );
            }
            return false;
        }
        return true;
    }

    public GuiSession getSession(UUID playerUuid) {
        return activeSessions.get(playerUuid);
    }

    public void endSession(UUID playerUuid) {
        endSession(playerUuid, null);
    }

    public void endSession(UUID playerUuid, UUID sessionId) {
        if (playerUuid == null) return;
        if (sessionId == null) {
            activeSessions.remove(playerUuid);
        } else {
            GuiSession current = activeSessions.get(playerUuid);
            if (current != null && sessionId.equals(current.sessionId())) {
                activeSessions.remove(playerUuid);
            }
        }
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public int cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        int removed = 0;
        var it = activeSessions.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getValue().isExpired(now)) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public void clearAll() {
        activeSessions.clear();
    }
}