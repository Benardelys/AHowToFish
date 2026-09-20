package com.ardelys.ahowtofish.model;

public record FishRarity(
        String id,
        String displayName,
        String color,
        double chanceMultiplier,
        double xpMultiplier,
        double valueMultiplier,
        boolean broadcast,
        String sound,
        String particle,
        String permission
) {
    public boolean hasPermission(org.bukkit.entity.Player player) {
        if (permission == null || permission.isBlank()) return true;
        return player.hasPermission(permission) || player.hasPermission("ahowtofish.admin");
    }
}
