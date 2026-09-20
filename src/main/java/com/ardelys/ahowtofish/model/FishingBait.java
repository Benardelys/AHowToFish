package com.ardelys.ahowtofish.model;

import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.PdcKeys;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record FishingBait(
        String id,
        String displayName,
        Material material,
        Integer customModelData,
        List<String> lore,
        int maxUses,
        double fishChanceMultiplier,
        double rareChanceMultiplier,
        double legendaryChanceMultiplier,
        double xpMultiplier,
        double moneyMultiplier,
        String specialEffect,
        double cost
) {
    public ItemStack createItemStack(int amount) {
        ItemBuilder builder = ItemBuilder.from(material != null ? material : Material.WHEAT_SEEDS, amount)
                .name(displayName)
                .customModelData(customModelData)
                .setStringPdc(PdcKeys.BAIT_ID, id)
                .setIntPdc(PdcKeys.BAIT_USES, maxUses);

        List<String> list = new ArrayList<>();
        if (lore != null) {
            list.addAll(lore);
        }
        list.add("&8----------------------------");
        list.add("&7Uses Remaining: &a" + maxUses + "/" + maxUses);
        if (fishChanceMultiplier > 1.0) list.add("&7Fish Speed: &a" + TextUtil.formatDecimal(fishChanceMultiplier) + "x");
        if (rareChanceMultiplier > 1.0) list.add("&7Rare Chance: &d" + TextUtil.formatDecimal(rareChanceMultiplier) + "x");
        if (legendaryChanceMultiplier > 1.0) list.add("&7Legendary Chance: &6" + TextUtil.formatDecimal(legendaryChanceMultiplier) + "x");
        if (xpMultiplier > 1.0) list.add("&7XP Multiplier: &b" + TextUtil.formatDecimal(xpMultiplier) + "x");
        if (moneyMultiplier > 1.0) list.add("&7Money Multiplier: &6" + TextUtil.formatDecimal(moneyMultiplier) + "x");
        if (specialEffect != null && !specialEffect.isBlank() && !specialEffect.equalsIgnoreCase("none")) {
            list.add("&7Special Effect: &e" + specialEffect);
        }
        list.add("&8----------------------------");
        list.add("&ePut in offhand while fishing to use!");

        builder.lore(list);
        return builder.build();
    }
}
