package com.ardelys.ahowtofish.player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FishingProfile {
    private final UUID uuid;
    private String playerName;
    private int level = 1;
    private long xp = 0;
    private String language = null; 
    private final PlayerStats stats = new PlayerStats();
    private final PlayerCollection collection = new PlayerCollection();
    private boolean starterReceived = false;
    private String activeQuestId = null;
    private final Map<String, Double> questProgress = new ConcurrentHashMap<>();
    private final Map<String, com.ardelys.ahowtofish.quest.PlayerQuestProgress> playerQuests = new ConcurrentHashMap<>();
    private final Set<String> completedQuests = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> unlockedAchievements = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<String, Integer> upgrades = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;
    private volatile ProfileState state = ProfileState.UNLOADED;

    public FishingProfile(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
    }

    public ProfileState getState() { return state; }
    public void setState(ProfileState state) { this.state = state; }
    public boolean isReady() { return state == ProfileState.ACTIVE; }

    public UUID getUuid() { return uuid; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; markDirty(); }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, level); markDirty(); }

    public long getXp() { return xp; }
    public void setXp(long xp) { this.xp = Math.max(0, xp); markDirty(); }
    public void addXp(long amount) {
        if (amount > 0) {
            this.xp += amount;
            this.stats.addXp(amount);
            markDirty();
        }
    }

    public boolean hasExplicitLanguage() {
        return language != null && !language.isBlank();
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        if (language == null || language.isBlank() || language.equalsIgnoreCase("default") || language.equalsIgnoreCase("auto") || language.equalsIgnoreCase("null")) {
            this.language = null;
        } else {
            this.language = language.trim().toLowerCase(Locale.ROOT);
        }
        markDirty();
    }

    public PlayerStats getStats() { return stats; }
    public PlayerCollection getCollection() { return collection; }

    public boolean isStarterReceived() { return starterReceived; }
    public void setStarterReceived(boolean starterReceived) { this.starterReceived = starterReceived; markDirty(); }

    public String getActiveQuestId() { return activeQuestId; }
    public void setActiveQuestId(String activeQuestId) {
        this.activeQuestId = (activeQuestId != null && !activeQuestId.isBlank()) ? activeQuestId.toLowerCase(Locale.ROOT) : null;
        markDirty();
    }

    public Map<String, com.ardelys.ahowtofish.quest.PlayerQuestProgress> getPlayerQuests() { return playerQuests; }
    public com.ardelys.ahowtofish.quest.PlayerQuestProgress getPlayerQuestProgress(String questId) {
        if (questId == null) return null;
        return playerQuests.get(questId.toLowerCase(Locale.ROOT));
    }
    public void setPlayerQuestProgress(String questId, com.ardelys.ahowtofish.quest.PlayerQuestProgress progress) {
        if (questId == null || progress == null) return;
        playerQuests.put(questId.toLowerCase(Locale.ROOT), progress);
        questProgress.put(questId.toLowerCase(Locale.ROOT), progress.getProgress());
        if (progress.getState() == com.ardelys.ahowtofish.model.FishingQuest.QuestState.COMPLETED ||
            progress.getState() == com.ardelys.ahowtofish.model.FishingQuest.QuestState.CLAIMED) {
            completedQuests.add(questId.toLowerCase(Locale.ROOT));
        }
        markDirty();
    }

    public Map<String, Double> getQuestProgress() { return questProgress; }
    public double getQuestProgress(String questId) {
        return questProgress.getOrDefault(questId.toLowerCase(), 0.0);
    }
    public void setQuestProgress(String questId, double value) {
        questProgress.put(questId.toLowerCase(), value);
        markDirty();
    }
    public void addQuestProgress(String questId, double value) {
        questProgress.merge(questId.toLowerCase(), value, Double::sum);
        markDirty();
    }

    public Set<String> getCompletedQuests() { return completedQuests; }
    public boolean isQuestCompleted(String questId) {
        return completedQuests.contains(questId.toLowerCase());
    }
    public void markQuestCompleted(String questId) {
        completedQuests.add(questId.toLowerCase());
        markDirty();
    }

    public Set<String> getUnlockedAchievements() { return unlockedAchievements; }
    public boolean hasAchievement(String achievementId) {
        return unlockedAchievements.contains(achievementId.toLowerCase());
    }
    public void unlockAchievement(String achievementId) {
        unlockedAchievements.add(achievementId.toLowerCase());
        markDirty();
    }

    public Map<String, Integer> getUpgrades() { return upgrades; }
    public int getUpgradeLevel(String upgradeId) {
        return upgrades.getOrDefault(upgradeId.toLowerCase(), 0);
    }
    public void setUpgradeLevel(String upgradeId, int level) {
        upgrades.put(upgradeId.toLowerCase(), level);
        markDirty();
    }
    public void incrementUpgradeLevel(String upgradeId) {
        upgrades.merge(upgradeId.toLowerCase(), 1, Integer::sum);
        markDirty();
    }

    public boolean isDirty() { return dirty; }
    public void markDirty() { this.dirty = true; }
    public void clearDirty() { this.dirty = false; }
}
