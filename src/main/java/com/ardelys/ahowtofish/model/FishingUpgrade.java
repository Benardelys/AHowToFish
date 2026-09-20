package com.ardelys.ahowtofish.model;

import org.bukkit.Material;

public record FishingUpgrade(
        String id,
        String name,
        String description,
        int maxLevel,
        double baseCost,
        double costMultiplier,
        double effectPerLevel,
        Material iconMaterial,
        String permission
) {
    public double getCost(int currentLevel) {
        if (currentLevel >= maxLevel) return -1;
        return baseCost * Math.pow(costMultiplier, currentLevel);
    }

    public double getTotalEffect(int currentLevel) {
        return effectPerLevel * currentLevel;
    }
}
