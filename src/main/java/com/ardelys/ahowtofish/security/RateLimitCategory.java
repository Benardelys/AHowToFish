package com.ardelys.ahowtofish.security;

public enum RateLimitCategory {
    NPC_INTERACTION(3),
    GUI_ACTION(5),
    SELL(2),
    PURCHASE(2),
    REWARD_CLAIM(1),
    QUEST(2),
    COMMAND(5),
    PLACEHOLDER(10);

    private final int defaultLimitPerSecond;

    RateLimitCategory(int defaultLimitPerSecond) {
        this.defaultLimitPerSecond = defaultLimitPerSecond;
    }

    public int getDefaultLimitPerSecond() {
        return defaultLimitPerSecond;
    }
}
