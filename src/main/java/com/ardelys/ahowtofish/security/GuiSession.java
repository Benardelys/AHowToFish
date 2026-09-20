package com.ardelys.ahowtofish.security;

import java.util.UUID;

public record GuiSession(
        UUID playerUuid,
        String guiType,
        UUID sessionId,
        long createdAt,
        long expiresAt
) {
    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    public boolean isExpired(long now) {
        return now >= expiresAt;
    }

    public boolean isValid(UUID checkingPlayerUuid, String checkingGuiType) {
        if (!playerUuid.equals(checkingPlayerUuid)) return false;
        if (guiType != null && checkingGuiType != null && !guiType.equalsIgnoreCase(checkingGuiType)) return false;
        return !isExpired();
    }
}