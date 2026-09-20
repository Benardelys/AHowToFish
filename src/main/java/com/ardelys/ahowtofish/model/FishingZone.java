package com.ardelys.ahowtofish.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public record FishingZone(
        String id,
        String displayName,
        String worldName,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        int requiredLevel,
        String requiredPermission,
        double xpMultiplier,
        double moneyMultiplier,
        List<String> availableFish,
        Map<String, Double> fishChanceMultipliers,
        String worldGuardRegion
) {
    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (worldName != null && !worldName.isEmpty() && !loc.getWorld().getName().equalsIgnoreCase(worldName)) {
            return false;
        }

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        double x1 = Math.min(minX, maxX);
        double x2 = Math.max(minX, maxX);
        double y1 = Math.min(minY, maxY);
        double y2 = Math.max(minY, maxY);
        double z1 = Math.min(minZ, maxZ);
        double z2 = Math.max(minZ, maxZ);

        return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }

    public boolean canFish(Player player, int playerLevel) {
        if (playerLevel < requiredLevel) return false;
        if (requiredPermission != null && !requiredPermission.isBlank() && !player.hasPermission(requiredPermission) && !player.hasPermission("ahowtofish.admin")) {
            return false;
        }
        return true;
    }
}
