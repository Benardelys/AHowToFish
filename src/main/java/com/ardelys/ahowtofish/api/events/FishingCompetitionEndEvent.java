package com.ardelys.ahowtofish.api.events;

import com.ardelys.ahowtofish.model.Competition;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FishingCompetitionEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Competition competition;
    private final List<Map.Entry<UUID, Double>> winners;

    public FishingCompetitionEndEvent(Competition competition, List<Map.Entry<UUID, Double>> winners) {
        this.competition = competition;
        this.winners = winners;
    }

    public Competition getCompetition() { return competition; }
    public List<Map.Entry<UUID, Double>> getWinners() { return winners; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
