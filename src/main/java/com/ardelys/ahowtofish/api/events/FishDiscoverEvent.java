package com.ardelys.ahowtofish.api.events;

import com.ardelys.ahowtofish.model.Fish;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class FishDiscoverEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Fish fish;
    private final double weight;

    public FishDiscoverEvent(Player player, Fish fish, double weight) {
        this.player = player;
        this.fish = fish;
        this.weight = weight;
    }

    public Player getPlayer() { return player; }
    public Fish getFish() { return fish; }
    public double getWeight() { return weight; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
