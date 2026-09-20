package com.ardelys.ahowtofish.model;

import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.PdcKeys;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record FishingRod(
        String id,
        String displayName,
        Material material,
        Integer customModelData,
        List<String> lore,
        int durability,
        double luckBonus,
        double xpMultiplier,
        double moneyMultiplier,
        double rareChanceBonus,
        double legendaryChanceBonus,
        double doubleCatchChance,
        int requiredLevel,
        String permission
) {
    public boolean canUse(Player player, int playerLevel) {
        if (playerLevel < requiredLevel) return false;
        if (permission != null && !permission.isBlank() && !player.hasPermission(permission) && !player.hasPermission("ahowtofish.admin")) {
            return false;
        }
        return true;
    }

    public ItemStack createItemStack() {
        ItemBuilder builder = ItemBuilder.from(material != null ? material : Material.FISHING_ROD)
                .name(displayName)
                .customModelData(customModelData)
                .setStringPdc(PdcKeys.ROD_ID, id)
                .unbreakable(durability <= 0);

        List<String> list = new ArrayList<>();
        if (lore != null) {
            list.addAll(lore);
        }
        list.add("&8----------------------------");
        if (luckBonus > 0) list.add("&7Luck: &a+" + TextUtil.formatDecimal(luckBonus));
        if (xpMultiplier > 1.0) list.add("&7XP Boost: &b" + TextUtil.formatDecimal(xpMultiplier) + "x");
        if (moneyMultiplier > 1.0) list.add("&7Money Boost: &6" + TextUtil.formatDecimal(moneyMultiplier) + "x");
        if (rareChanceBonus > 0) list.add("&7Rare Chance: &d+" + TextUtil.formatDecimal(rareChanceBonus) + "%");
        if (legendaryChanceBonus > 0) list.add("&7Legendary Chance: &6+" + TextUtil.formatDecimal(legendaryChanceBonus) + "%");
        if (doubleCatchChance > 0) list.add("&7Double Catch: &e+" + TextUtil.formatDecimal(doubleCatchChance) + "%");
        if (requiredLevel > 0) list.add("&7Req. Level: &c" + requiredLevel);
        list.add("&8----------------------------");

        builder.lore(list);
        return builder.build();
    }
}
