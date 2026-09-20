package com.ardelys.ahowtofish.api.events;

import com.ardelys.ahowtofish.model.Competition;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class FishingCompetitionStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Competition competition;

    public FishingCompetitionStartEvent(Competition competition) {
        this.competition = competition;
    }

    public Competition getCompetition() { return competition; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
