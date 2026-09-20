package com.ardelys.ahowtofish.model;

import java.util.List;

public record FishingAchievement(
        String id,
        String name,
        String description,
        AchievementType type,
        double requiredValue,
        String targetRequirement,
        long xpReward,
        double moneyReward,
        List<String> commandRewards,
        String titleReward
) {
    public enum AchievementType {
        TOTAL_FISH_CAUGHT,
        TOTAL_WEIGHT_CAUGHT,
        REACH_LEVEL,
        DISCOVER_FISH_COUNT,
        CATCH_RARITY,
        MAX_WEIGHT_CAUGHT,
        TOTAL_MONEY_EARNED
    }
}
