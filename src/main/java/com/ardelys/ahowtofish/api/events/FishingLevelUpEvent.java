package com.ardelys.ahowtofish.api.events;

import com.ardelys.ahowtofish.player.FishingProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class FishingLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final FishingProfile profile;
    private final int oldLevel;
    private final int newLevel;

    public FishingLevelUpEvent(Player player, FishingProfile profile, int oldLevel, int newLevel) {
        this.player = player;
        this.profile = profile;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public Player getPlayer() { return player; }
    public FishingProfile getProfile() { return profile; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
