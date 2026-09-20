package com.ardelys.ahowtofish.fishing;

import com.ardelys.ahowtofish.event.EventManager;
import com.ardelys.ahowtofish.model.*;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.zone.ZoneManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FishingEngine {
    private final FishManager fishManager;
    private final RodManager rodManager;
    private final BaitManager baitManager;
    private final ZoneManager zoneManager;
    private final AbilityManager abilityManager;
    private final EventManager eventManager;

    public FishingEngine(FishManager fishManager,
                         RodManager rodManager,
                         BaitManager baitManager,
                         ZoneManager zoneManager,
                         AbilityManager abilityManager,
                         EventManager eventManager) {
        this.fishManager = fishManager;
        this.rodManager = rodManager;
        this.baitManager = baitManager;
        this.zoneManager = zoneManager;
        this.abilityManager = abilityManager;
        this.eventManager = eventManager;
    }

    public FishCatchResult rollFish(Player player, FishingProfile profile, Location hookLocation) {
        int playerLevel = profile.getLevel();
        FishingZone currentZone = zoneManager.getZoneAt(hookLocation);
        FishingRod rod = rodManager.getRodFromItem(player.getInventory().getItemInMainHand());
        FishingBait bait = baitManager.getActiveBait(player);
        SpecialEvent activeEvent = eventManager.getActiveEvent();

        String currentZoneId = currentZone != null ? currentZone.id() : null;
        String currentBaitId = bait != null ? bait.id() : null;

        List<Fish> candidates = new ArrayList<>();
        for (Fish f : fishManager.getAllFish()) {
            if (f.canCatch(playerLevel, currentZoneId, currentBaitId)) {
                candidates.add(f);
            }
        }

        if (candidates.isEmpty()) {
            
            for (Fish f : fishManager.getAllFish()) {
                if (f.minLevel() <= playerLevel) {
                    candidates.add(f);
                }
            }
            if (candidates.isEmpty() && !fishManager.getAllFish().isEmpty()) {
                candidates.addAll(fishManager.getAllFish());
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        double luckBonus = (rod != null ? rod.luckBonus() : 0.0) + abilityManager.getPlayerUpgradeEffect(profile, "luck");
        double rareBonus = (rod != null ? rod.rareChanceBonus() : 0.0) + abilityManager.getPlayerUpgradeEffect(profile, "rare_chance");
        double legBonus = (rod != null ? rod.legendaryChanceBonus() : 0.0) + abilityManager.getPlayerUpgradeEffect(profile, "legendary_chance");

        double baitRareMul = bait != null ? bait.rareChanceMultiplier() : 1.0;
        double baitLegMul = bait != null ? bait.legendaryChanceMultiplier() : 1.0;
        double eventRareMul = activeEvent != null ? activeEvent.rareChanceMultiplier() : 1.0;
        double eventLegMul = activeEvent != null ? activeEvent.legendaryChanceMultiplier() : 1.0;

        Map<Fish, Double> weightedMap = new HashMap<>();
        double totalWeight = 0.0;

        for (Fish f : candidates) {
            FishRarity rarity = fishManager.getRarity(f.rarityId());
            if (rarity != null && !rarity.hasPermission(player)) {
                continue;
            }

            double weight = f.chance();

            if (currentZone != null && currentZone.fishChanceMultipliers().containsKey(f.id())) {
                weight *= currentZone.fishChanceMultipliers().get(f.id());
            }

            if (rarity != null) {
                weight *= rarity.chanceMultiplier();
                String rId = rarity.id();
                if (rId.contains("RARE")) {
                    weight *= (1.0 + (rareBonus / 100.0)) * baitRareMul * eventRareMul;
                } else if (rId.contains("LEGENDARY") || rId.contains("MYTHIC")) {
                    weight *= (1.0 + (legBonus / 100.0)) * baitLegMul * eventLegMul;
                }
            }

            weight *= (1.0 + (luckBonus * 0.05));
            if (weight <= 0.0) weight = 0.1;

            weightedMap.put(f, weight);
            totalWeight += weight;
        }

        double randomRoll = ThreadLocalRandom.current().nextDouble(0, totalWeight);
        double cumulative = 0.0;
        Fish chosenFish = candidates.get(0);

        for (Map.Entry<Fish, Double> entry : weightedMap.entrySet()) {
            cumulative += entry.getValue();
            if (randomRoll <= cumulative) {
                chosenFish = entry.getKey();
                break;
            }
        }

        FishRarity rarity = fishManager.getRarity(chosenFish.rarityId());
        if (rarity == null) {
            rarity = new FishRarity("COMMON", "Common", "&f", 1.0, 1.0, 1.0, false, "ENTITY_ITEM_PICKUP", "WATER_SPLASH", "");
        }

        double minW = Math.max(0.1, chosenFish.minWeight());
        double maxW = Math.max(minW, chosenFish.maxWeight());
        double rolledWeight = minW + (maxW - minW) * ThreadLocalRandom.current().nextDouble();

        double weightBonusPercent = com.ardelys.ahowtofish.security.MultiplierService.clampMultiplier(
                abilityManager.getPlayerUpgradeEffect(profile, "fish_weight"), 0.0, 5.0, 0.0
        );
        if (weightBonusPercent > 0) {
            rolledWeight *= (1.0 + weightBonusPercent);
        }
        rolledWeight = com.ardelys.ahowtofish.security.MultiplierService.sanitizeWeight(
                Math.round(rolledWeight * 100.0) / 100.0
        );

        double avgW = Math.max(0.1, (minW + maxW) / 2.0);
        double weightRatio = Math.max(0.1, Math.min(10.0, rolledWeight / avgW));

        double rawMoneyMul = (currentZone != null ? currentZone.moneyMultiplier() : 1.0)
                * (rod != null ? rod.moneyMultiplier() : 1.0)
                * (bait != null ? bait.moneyMultiplier() : 1.0)
                * (activeEvent != null ? activeEvent.moneyMultiplier() : 1.0)
                * (1.0 + abilityManager.getPlayerUpgradeEffect(profile, "money_boost"));
        double moneyMul = com.ardelys.ahowtofish.security.MultiplierService.clampMultiplier(rawMoneyMul);

        double rawPrice = chosenFish.sellPrice() * weightRatio * rarity.valueMultiplier() * moneyMul;
        double finalPrice = com.ardelys.ahowtofish.security.MultiplierService.sanitizeMoney(
                Math.round(rawPrice * 100.0) / 100.0
        );

        double rawXpMul = (currentZone != null ? currentZone.xpMultiplier() : 1.0)
                * (rod != null ? rod.xpMultiplier() : 1.0)
                * (bait != null ? bait.xpMultiplier() : 1.0)
                * (activeEvent != null ? activeEvent.xpMultiplier() : 1.0)
                * (1.0 + abilityManager.getPlayerUpgradeEffect(profile, "xp_boost"));
        double xpMul = com.ardelys.ahowtofish.security.MultiplierService.clampMultiplier(rawXpMul);

        long rawXp = (long) (chosenFish.xpReward() * weightRatio * rarity.xpMultiplier() * xpMul);
        long finalXp = com.ardelys.ahowtofish.security.MultiplierService.sanitizeXp(Math.max(1, rawXp));

        return new FishCatchResult(
                chosenFish,
                rarity,
                rolledWeight,
                finalPrice,
                finalXp,
                player.getUniqueId(),
                player.getName(),
                System.currentTimeMillis()
        );
    }

    public boolean shouldDoubleCatch(Player player, FishingProfile profile) {
        FishingRod rod = rodManager.getRodFromItem(player.getInventory().getItemInMainHand());
        double chance = (rod != null ? rod.doubleCatchChance() : 0.0) + (abilityManager.getPlayerUpgradeEffect(profile, "double_catch") * 100.0);
        return ThreadLocalRandom.current().nextDouble(0, 100) < chance;
    }
}
