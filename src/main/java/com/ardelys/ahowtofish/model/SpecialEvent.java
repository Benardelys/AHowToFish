package com.ardelys.ahowtofish.model;

public record SpecialEvent(
        String id,
        String name,
        String description,
        int durationSeconds,
        double xpMultiplier,
        double moneyMultiplier,
        double rareChanceMultiplier,
        double legendaryChanceMultiplier,
        String broadcastStart,
        String broadcastEnd,
        String soundStart,
        String soundEnd,
        String particle
) {}
