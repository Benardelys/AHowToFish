package com.ardelys.ahowtofish.model;

import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.PdcKeys;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record FishCatchResult(
        Fish fish,
        FishRarity rarity,
        double weight,
        double finalPrice,
        long finalXp,
        UUID catcherUuid,
        String catcherName,
        long catchTimestamp
) {
    public ItemStack buildItemStack() {
        ItemBuilder builder = ItemBuilder.from(fish.material() != null ? fish.material() : org.bukkit.Material.COD)
                .name(rarity.color() + fish.displayName())
                .customModelData(fish.customModelData())
                .setStringPdc(PdcKeys.FISH_ID, fish.id())
                .setStringPdc(PdcKeys.FISH_RARITY, rarity.id())
                .setDoublePdc(PdcKeys.FISH_WEIGHT, weight)
                .setDoublePdc(PdcKeys.FISH_PRICE, finalPrice)
                .setLongPdc(PdcKeys.FISH_CATCH_TIME, catchTimestamp)
                .setStringPdc(PdcKeys.FISH_CATCHER, catcherUuid.toString());

        List<String> formattedLore = new ArrayList<>();
        if (fish.lore() != null && !fish.lore().isEmpty()) {
            for (String line : fish.lore()) {
                formattedLore.add(line);
            }
        }

        formattedLore.add("&8----------------------------");
        formattedLore.add("&7Rarity: " + rarity.color() + rarity.displayName());
        formattedLore.add("&7Weight: &f" + TextUtil.formatDecimal(weight) + " kg");
        formattedLore.add("&7Value: &a$" + TextUtil.formatDecimal(finalPrice));
        formattedLore.add("&7XP: &b+" + TextUtil.formatInteger(finalXp));
        formattedLore.add("&7Caught by: &e" + catcherName);
        formattedLore.add("&8----------------------------");

        builder.lore(formattedLore);
        ItemStack item = builder.build();

        if (com.ardelys.ahowtofish.AHowToFishPlugin.getInstance() != null &&
            com.ardelys.ahowtofish.AHowToFishPlugin.getInstance().getItemSecurityManager() != null) {
            com.ardelys.ahowtofish.AHowToFishPlugin.getInstance().getItemSecurityManager().stampSignature(item, this);
        }

        return item;
    }
}
