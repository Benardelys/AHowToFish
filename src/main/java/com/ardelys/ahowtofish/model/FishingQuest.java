package com.ardelys.ahowtofish.model;

import java.util.Collections;
import java.util.List;

public record FishingQuest(
        String id,
        String name,
        List<String> description,
        QuestCategory category,
        QuestObjectiveType objectiveType,
        double requiredAmount,
        String targetRequirement, 
        int timeLimitSeconds,     
        int retryCooldownSeconds, 
        List<String> prerequisites,
        String nextQuestId,
        long xpReward,
        double moneyReward,
        String rewardRod,
        String rewardBait,
        List<String> commandRewards,
        String soundReward,
        String dialogueOffer,
        String dialogueProgress,
        String dialogueComplete,
        String dialogueExpire
) {
    public enum QuestState {
        LOCKED,
        AVAILABLE,
        ACTIVE,
        COMPLETED,
        CLAIMED,
        EXPIRED
    }

    public enum QuestCategory {
        STARTER,
        TUTORIAL,
        DAILY,
        WEEKLY,
        PERMANENT
    }

    public enum QuestObjectiveType {
        RECEIVE_ITEM,
        CAST,
        CATCH_TOTAL,
        CATCH_RARITY,
        CATCH_WEIGHT,
        TALK_NPC,
        SELL_FISH,
        SELL_VALUE,
        REACH_LEVEL,
        EARN_XP,
        EARN_COINS,
        VISIT_ZONE,
        PURCHASE_ROD,
        PURCHASE_BAIT,
        USE_BAIT,
        UPGRADE,
        COMPLETE_COMPETITION
    }

    public FishingQuest(
            String id,
            String name,
            String description,
            QuestType legacyType,
            double requiredAmount,
            String targetRequirement,
            QuestRecurrence legacyRecurrence,
            long xpReward,
            double moneyReward,
            List<String> commandRewards,
            String soundReward
    ) {
        this(
                id,
                name,
                List.of(description),
                legacyRecurrence == QuestRecurrence.DAILY ? QuestCategory.DAILY :
                        (legacyRecurrence == QuestRecurrence.WEEKLY ? QuestCategory.WEEKLY : QuestCategory.PERMANENT),
                mapLegacyType(legacyType),
                requiredAmount,
                targetRequirement,
                0,
                60,
                Collections.emptyList(),
                null,
                xpReward,
                moneyReward,
                null,
                null,
                commandRewards != null ? commandRewards : Collections.emptyList(),
                soundReward != null ? soundReward : "UI_TOAST_CHALLENGE_COMPLETE",
                "",
                "",
                "",
                ""
        );
    }

    private static QuestObjectiveType mapLegacyType(QuestType lt) {
        if (lt == null) return QuestObjectiveType.CATCH_TOTAL;
        return switch (lt) {
            case CATCH_TOTAL -> QuestObjectiveType.CATCH_TOTAL;
            case CATCH_RARITY -> QuestObjectiveType.CATCH_RARITY;
            case CATCH_WEIGHT -> QuestObjectiveType.CATCH_WEIGHT;
            case EARN_MONEY -> QuestObjectiveType.EARN_COINS;
            case FISH_ZONE -> QuestObjectiveType.VISIT_ZONE;
        };
    }

    public enum QuestType {
        CATCH_TOTAL,
        CATCH_RARITY,
        CATCH_WEIGHT,
        EARN_MONEY,
        FISH_ZONE
    }

    public enum QuestRecurrence {
        DAILY,
        WEEKLY,
        PERMANENT
    }

    public QuestType type() {
        return switch (objectiveType) {
            case CATCH_TOTAL -> QuestType.CATCH_TOTAL;
            case CATCH_RARITY -> QuestType.CATCH_RARITY;
            case CATCH_WEIGHT -> QuestType.CATCH_WEIGHT;
            case EARN_COINS -> QuestType.EARN_MONEY;
            case VISIT_ZONE -> QuestType.FISH_ZONE;
            default -> QuestType.CATCH_TOTAL;
        };
    }

    public QuestRecurrence recurrence() {
        if (category == QuestCategory.DAILY) return QuestRecurrence.DAILY;
        if (category == QuestCategory.WEEKLY) return QuestRecurrence.WEEKLY;
        return QuestRecurrence.PERMANENT;
    }
}