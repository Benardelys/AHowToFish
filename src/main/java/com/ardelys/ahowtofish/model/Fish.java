package com.ardelys.ahowtofish.model;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public record Fish(
        String id,
        String displayName,
        Material material,
        Integer customModelData,
        List<String> lore,
        String rarityId,
        double minWeight,
        double maxWeight,
        double sellPrice,
        double baseValue,
        long xpReward,
        int minLevel,
        int maxLevel,
        String requiredZone,
        String requiredBait,
        double chance,
        String sound,
        String particle,
        List<String> commands,
        Map<String, Object> specialProperties
) {
    public boolean canCatch(int playerLevel, String currentZoneId, String currentBaitId) {
        if (playerLevel < minLevel) return false;
        if (maxLevel > 0 && playerLevel > maxLevel) return false;

        if (requiredZone != null && !requiredZone.isBlank() && !requiredZone.equalsIgnoreCase("none")) {
            if (currentZoneId == null || !requiredZone.equalsIgnoreCase(currentZoneId)) {
                return false;
            }
        }

        if (requiredBait != null && !requiredBait.isBlank() && !requiredBait.equalsIgnoreCase("none")) {
            if (currentBaitId == null || !requiredBait.equalsIgnoreCase(currentBaitId)) {
                return false;
            }
        }

        return true;
    }
}
