package com.ardelys.ahowtofish.security;

public final class MultiplierService {
    public static final double MIN_MULTIPLIER = 0.0;
    public static final double MAX_MULTIPLIER = 50.0;
    public static final double MAX_MONEY_PER_FISH = 1_000_000.0;
    public static final long MAX_XP_PER_FISH = 500_000L;
    public static final double MAX_WEIGHT_KG = 10_000.0;

    private MultiplierService() {}

    public static double clampMultiplier(double val, double min, double max, double fallback) {
        if (Double.isNaN(val) || Double.isInfinite(val)) {
            return fallback;
        }
        if (val < min) return min;
        if (val > max) return max;
        return val;
    }

    public static double clampMultiplier(double val) {
        return clampMultiplier(val, MIN_MULTIPLIER, MAX_MULTIPLIER, 1.0);
    }

    public static double sanitizeMoney(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0.0) {
            return 0.0;
        }
        return Math.min(amount, MAX_MONEY_PER_FISH);
    }

    public static long sanitizeXp(long amount) {
        if (amount <= 0L) {
            return 0L;
        }
        return Math.min(amount, MAX_XP_PER_FISH);
    }

    public static double sanitizeWeight(double weight) {
        if (Double.isNaN(weight) || Double.isInfinite(weight) || weight <= 0.0) {
            return 0.1;
        }
        return Math.min(weight, MAX_WEIGHT_KG);
    }
}