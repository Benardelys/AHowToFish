package com.ardelys.ahowtofish.player;

public class PlayerStats {
    private long totalCasts = 0;
    private long successfulCatches = 0;
    private long failedCatches = 0;
    private long totalFishCaught = 0;
    private long totalXpEarned = 0;
    private double largestFishWeight = 0.0;
    private double totalFishWeight = 0.0;
    private double totalMoneyEarned = 0.0;
    private long commonCaught = 0;
    private long rareCaught = 0;
    private long legendaryCaught = 0;
    private int currentStreak = 0;
    private int bestStreak = 0;
    private int eventsWon = 0;
    private int competitionsWon = 0;
    private long fishingTimeSeconds = 0;

    public PlayerStats() {}

    public long getTotalCasts() { return totalCasts; }
    public void setTotalCasts(long totalCasts) { this.totalCasts = totalCasts; }
    public void incrementCasts() { this.totalCasts++; }

    public long getSuccessfulCatches() { return successfulCatches; }
    public void setSuccessfulCatches(long successfulCatches) { this.successfulCatches = successfulCatches; }
    public void incrementSuccessfulCatches() {
        this.successfulCatches++;
        this.currentStreak++;
        if (this.currentStreak > this.bestStreak) {
            this.bestStreak = this.currentStreak;
        }
    }

    public long getFailedCatches() { return failedCatches; }
    public void setFailedCatches(long failedCatches) { this.failedCatches = failedCatches; }
    public void incrementFailedCatches() {
        this.failedCatches++;
        this.currentStreak = 0;
    }

    public long getTotalFishCaught() { return totalFishCaught; }
    public void setTotalFishCaught(long totalFishCaught) { this.totalFishCaught = totalFishCaught; }
    public void incrementFishCaught() { this.totalFishCaught++; }

    public long getTotalXpEarned() { return totalXpEarned; }
    public void setTotalXpEarned(long totalXpEarned) { this.totalXpEarned = totalXpEarned; }
    public void addXp(long xp) { this.totalXpEarned += xp; }

    public double getLargestFishWeight() { return largestFishWeight; }
    public void setLargestFishWeight(double largestFishWeight) { this.largestFishWeight = largestFishWeight; }
    public void recordWeight(double weight) {
        if (weight > this.largestFishWeight) {
            this.largestFishWeight = weight;
        }
        this.totalFishWeight += weight;
    }

    public double getTotalFishWeight() { return totalFishWeight; }
    public void setTotalFishWeight(double totalFishWeight) { this.totalFishWeight = totalFishWeight; }

    public double getTotalMoneyEarned() { return totalMoneyEarned; }
    public void setTotalMoneyEarned(double totalMoneyEarned) { this.totalMoneyEarned = totalMoneyEarned; }
    public void addMoney(double money) { this.totalMoneyEarned += money; }

    public long getCommonCaught() { return commonCaught; }
    public void setCommonCaught(long commonCaught) { this.commonCaught = commonCaught; }
    public void incrementCommon() { this.commonCaught++; }

    public long getRareCaught() { return rareCaught; }
    public void setRareCaught(long rareCaught) { this.rareCaught = rareCaught; }
    public void incrementRare() { this.rareCaught++; }

    public long getLegendaryCaught() { return legendaryCaught; }
    public void setLegendaryCaught(long legendaryCaught) { this.legendaryCaught = legendaryCaught; }
    public void incrementLegendary() { this.legendaryCaught++; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getBestStreak() { return bestStreak; }
    public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }

    public int getEventsWon() { return eventsWon; }
    public void setEventsWon(int eventsWon) { this.eventsWon = eventsWon; }
    public void incrementEventsWon() { this.eventsWon++; }

    public int getCompetitionsWon() { return competitionsWon; }
    public void setCompetitionsWon(int competitionsWon) { this.competitionsWon = competitionsWon; }
    public void incrementCompetitionsWon() { this.competitionsWon++; }

    public long getFishingTimeSeconds() { return fishingTimeSeconds; }
    public void setFishingTimeSeconds(long fishingTimeSeconds) { this.fishingTimeSeconds = fishingTimeSeconds; }
    public void addFishingTime(long seconds) { this.fishingTimeSeconds += seconds; }
}
