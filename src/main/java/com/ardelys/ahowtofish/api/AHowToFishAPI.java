package com.ardelys.ahowtofish.api;

import com.ardelys.ahowtofish.model.*;
import com.ardelys.ahowtofish.player.FishingProfile;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface AHowToFishAPI {
    int getFishingLevel(UUID uuid);
    long getFishingXp(UUID uuid);
    void addFishingXp(Player player, long amount);
    void setFishingLevel(Player player, int level);
    FishingProfile getProfile(UUID uuid);
    FishingZone getCurrentZone(Player player);

    void registerCustomFish(Fish fish);
    void registerCustomRarity(FishRarity rarity);
    void registerCustomBait(FishingBait bait);
    void registerCustomRod(FishingRod rod);

    boolean giveFish(Player player, String fishId, int amount);
    boolean giveRod(Player player, String rodId);
    boolean giveBait(Player player, String baitId, int amount);
}
