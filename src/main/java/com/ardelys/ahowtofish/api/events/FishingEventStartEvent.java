package com.ardelys.ahowtofish.api.events;

import com.ardelys.ahowtofish.model.SpecialEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class FishingEventStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final SpecialEvent event;

    public FishingEventStartEvent(SpecialEvent event) {
        this.event = event;
    }

    public SpecialEvent getEvent() { return event; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
