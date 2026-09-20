package com.ardelys.ahowtofish.api.events;

import com.ardelys.ahowtofish.model.FishCatchResult;
import com.ardelys.ahowtofish.model.FishingBait;
import com.ardelys.ahowtofish.model.FishingRod;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class FishingCatchEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private FishCatchResult catchResult;
    private final FishingRod rod;
    private final FishingBait bait;

    public FishingCatchEvent(Player player, FishCatchResult catchResult, FishingRod rod, FishingBait bait) {
        this.player = player;
        this.catchResult = catchResult;
        this.rod = rod;
        this.bait = bait;
    }

    public Player getPlayer() { return player; }
    public FishCatchResult getCatchResult() { return catchResult; }
    public void setCatchResult(FishCatchResult catchResult) { this.catchResult = catchResult; }
    public FishingRod getRod() { return rod; }
    public FishingBait getBait() { return bait; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
