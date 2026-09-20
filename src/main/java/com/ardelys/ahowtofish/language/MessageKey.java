package com.ardelys.ahowtofish.language;

public enum MessageKey {
    PREFIX("prefix"),
    NO_PERMISSION("general.no-permission"),
    ONLY_PLAYERS("general.only-players"),
    RELOAD_SUCCESS("general.reload-success"),
    PLAYER_NOT_FOUND("general.player-not-found"),
    INVALID_ARGS("general.invalid-arguments"),
    UNKNOWN_COMMAND("general.unknown-command"),
    LANGUAGE_CHANGED("general.language-changed"),
    PROFILE_LOADING("general.profile-loading"),
    TRANSACTION_BUSY("general.transaction-busy"),
    PROFILE_NOT_READY("general.profile-not-ready"),
    DEPOSIT_FAILED("general.deposit-failed"),
    FISH_NOT_FOUND("general.fish-not-found"),
    ROD_NOT_FOUND("general.rod-not-found"),
    BAIT_NOT_FOUND("general.bait-not-found"),
    ZONE_NOT_FOUND("general.zone-not-found"),
    ZONE_SAME_WORLD("general.zone-same-world"),
    ZONE_SET_POS_FIRST("general.zone-set-pos-first"),

    FISH_CAUGHT("fishing.caught"),
    FISH_CAUGHT_BROADCAST("fishing.caught-broadcast"),
    FISH_ESCAPED("fishing.escaped"),
    TREASURE_CAUGHT("fishing.treasure-caught"),
    DOUBLE_CATCH("fishing.double-catch"),
    LUCKY_CATCH("fishing.lucky-catch"),
    BAIT_CONSUMED("fishing.bait-consumed"),
    BAIT_DEPLETED("fishing.bait-depleted"),
    ROD_LEVEL_REQUIRED("fishing.rod-level-required"),
    ZONE_LEVEL_REQUIRED("fishing.zone-level-required"),
    INVENTORY_FULL("fishing.inventory-full"),

    LEVEL_UP_TITLE("progression.level-up-title"),
    LEVEL_UP_SUBTITLE("progression.level-up-subtitle"),
    LEVEL_UP_MESSAGE("progression.level-up-message"),

    FISH_SOLD("shop.fish-sold"),
    FISH_SOLD_ALL("shop.fish-sold-all"),
    NO_FISH_TO_SELL("shop.no-fish-to-sell"),
    BUY_SUCCESS("shop.buy-success"),
    NOT_ENOUGH_MONEY("shop.not-enough-money"),

    QUEST_COMPLETED("quest.completed"),
    ACHIEVEMENT_UNLOCKED("achievement.unlocked"),
    UPGRADE_PURCHASED("upgrade.purchased"),
    UPGRADE_MAX_LEVEL("upgrade.max-level"),

    COMPETITION_START("competition.started"),
    COMPETITION_END("competition.ended"),
    COMPETITION_STANDINGS("competition.standings"),
    COMPETITION_NO_ACTIVE("competition.no-active"),

    EVENT_START("event.started"),
    EVENT_END("event.ended"),
    EVENT_NO_ACTIVE("event.no-active"),

    ADMIN_LEVEL_SET("admin.level-set"),
    ADMIN_XP_SET("admin.xp-set"),
    ADMIN_XP_ADDED("admin.xp-added"),
    ADMIN_RESET("admin.profile-reset"),
    ADMIN_GIVE_FISH("admin.fish-given"),
    ADMIN_GIVE_ROD("admin.rod-given"),
    ADMIN_GIVE_BAIT("admin.bait-given"),
    ADMIN_ZONE_CREATED("admin.zone-created"),
    ADMIN_ZONE_DELETED("admin.zone-deleted"),
    ADMIN_ZONE_POS1("admin.zone-pos1"),
    ADMIN_ZONE_POS2("admin.zone-pos2");

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
