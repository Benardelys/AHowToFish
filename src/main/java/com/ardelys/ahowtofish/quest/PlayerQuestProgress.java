package com.ardelys.ahowtofish.quest;

import com.ardelys.ahowtofish.model.FishingQuest.QuestState;

public class PlayerQuestProgress {
    private final String questId;
    private QuestState state;
    private double progress;
    private long startedAt;
    private long expiresAt;
    private long completedAt;
    private long cooldownUntil;

    public PlayerQuestProgress(String questId) {
        this(questId, QuestState.LOCKED, 0.0, 0L, 0L, 0L, 0L);
    }

    public PlayerQuestProgress(String questId, QuestState state, double progress,
                               long startedAt, long expiresAt, long completedAt, long cooldownUntil) {
        this.questId = questId.toLowerCase();
        this.state = state != null ? state : QuestState.LOCKED;
        this.progress = Math.max(0.0, progress);
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.completedAt = completedAt;
        this.cooldownUntil = cooldownUntil;
    }

    public String getQuestId() { return questId; }

    public QuestState getState() {
        if (state == QuestState.ACTIVE && expiresAt > 0 && System.currentTimeMillis() >= expiresAt) {
            this.state = QuestState.EXPIRED;
        }
        return state;
    }

    public void setState(QuestState state) { this.state = state; }

    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = Math.max(0.0, progress); }
    public void addProgress(double amount) {
        if (amount > 0) {
            this.progress += amount;
        }
    }

    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public long getCooldownUntil() { return cooldownUntil; }
    public void setCooldownUntil(long cooldownUntil) { this.cooldownUntil = cooldownUntil; }

    public boolean isExpired() {
        return getState() == QuestState.EXPIRED;
    }

    public long getRemainingSeconds() {
        if (expiresAt <= 0) return 0L;
        long diff = expiresAt - System.currentTimeMillis();
        return Math.max(0L, diff / 1000L);
    }

    public boolean isCooldownActive() {
        return cooldownUntil > System.currentTimeMillis();
    }

    public long getRemainingCooldownSeconds() {
        if (cooldownUntil <= 0) return 0L;
        long diff = cooldownUntil - System.currentTimeMillis();
        return Math.max(0L, diff / 1000L);
    }

    public boolean isClaimable() {
        return getState() == QuestState.COMPLETED;
    }

    public boolean isActive() {
        return getState() == QuestState.ACTIVE;
    }
}