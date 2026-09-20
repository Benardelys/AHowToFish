package com.ardelys.ahowtofish.utility;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class PdcKeys {
    public static NamespacedKey FISH_ID;
    public static NamespacedKey FISH_RARITY;
    public static NamespacedKey FISH_WEIGHT;
    public static NamespacedKey FISH_PRICE;
    public static NamespacedKey FISH_CATCH_TIME;
    public static NamespacedKey FISH_CATCHER;
    
    public static NamespacedKey ROD_ID;
    public static NamespacedKey BAIT_ID;
    public static NamespacedKey BAIT_USES;

    private PdcKeys() {}

    public static void init(Plugin plugin) {
        FISH_ID = new NamespacedKey(plugin, "fish_id");
        FISH_RARITY = new NamespacedKey(plugin, "fish_rarity");
        FISH_WEIGHT = new NamespacedKey(plugin, "fish_weight");
        FISH_PRICE = new NamespacedKey(plugin, "fish_price");
        FISH_CATCH_TIME = new NamespacedKey(plugin, "fish_catch_time");
        FISH_CATCHER = new NamespacedKey(plugin, "fish_catcher");

        ROD_ID = new NamespacedKey(plugin, "rod_id");
        BAIT_ID = new NamespacedKey(plugin, "bait_id");
        BAIT_USES = new NamespacedKey(plugin, "bait_uses");

        ITEM_TYPE = new NamespacedKey(plugin, "item_type");
        ITEM_VERSION = new NamespacedKey(plugin, "item_version");
        ITEM_SIGNATURE = new NamespacedKey(plugin, "item_signature");
        TRANSACTION_ID = new NamespacedKey(plugin, "transaction_id");
        NPC_ID = new NamespacedKey(plugin, "npc_id");
    }

    public static NamespacedKey ITEM_TYPE;
    public static NamespacedKey ITEM_VERSION;
    public static NamespacedKey ITEM_SIGNATURE;
    public static NamespacedKey TRANSACTION_ID;
    public static NamespacedKey NPC_ID;
}
