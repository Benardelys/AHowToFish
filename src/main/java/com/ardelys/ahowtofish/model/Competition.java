package com.ardelys.ahowtofish.model;

import com.ardelys.ahowtofish.competition.CompetitionType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Competition {
    private final CompetitionType type;
    private final int totalDurationSeconds;
    private int remainingSeconds;
    private final long startTimeMillis;
    private boolean active;
    private final Map<UUID, Double> scores = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerNames = new ConcurrentHashMap<>();

    public Competition(CompetitionType type, int totalDurationSeconds) {
        this.type = type;
        this.totalDurationSeconds = totalDurationSeconds;
        this.remainingSeconds = totalDurationSeconds;
        this.startTimeMillis = System.currentTimeMillis();
        this.active = true;
    }

    public CompetitionType getType() {
        return type;
    }

    public int getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void decrementSecond() {
        if (remainingSeconds > 0) {
            remainingSeconds--;
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void recordScore(UUID uuid, String name, double value) {
        playerNames.put(uuid, name);
        if (type == CompetitionType.LARGEST_FISH) {
            scores.merge(uuid, value, Math::max);
        } else {
            scores.merge(uuid, value, Double::sum);
        }
    }

    public double getScore(UUID uuid) {
        return scores.getOrDefault(uuid, 0.0);
    }

    public String getPlayerName(UUID uuid) {
        return playerNames.getOrDefault(uuid, "Unknown");
    }

    public List<Map.Entry<UUID, Double>> getLeaderboard() {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
    }

    public int getRank(UUID uuid) {
        List<Map.Entry<UUID, Double>> lb = getLeaderboard();
        for (int i = 0; i < lb.size(); i++) {
            if (lb.get(i).getKey().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
    }

    public String getLeaderName() {
        List<Map.Entry<UUID, Double>> lb = getLeaderboard();
        if (lb.isEmpty()) return "None";
        return getPlayerName(lb.get(0).getKey());
    }

    public double getLeaderScore() {
        List<Map.Entry<UUID, Double>> lb = getLeaderboard();
        if (lb.isEmpty()) return 0.0;
        return lb.get(0).getValue();
    }

    public int getParticipantCount() {
        return scores.size();
    }

    public enum State {
        STARTING,
        ACTIVE,
        ENDING,
        FINISHED
    }

    private State state = State.ACTIVE;

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
