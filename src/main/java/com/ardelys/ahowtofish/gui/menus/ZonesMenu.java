package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.model.FishingZone;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ZonesMenu extends CustomGui {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    private final AHowToFishPlugin plugin;
    private final Player player;
    private final FishingProfile profile;

    public ZonesMenu(AHowToFishPlugin plugin, Player player) {
        super(45, plugin.getLanguageManager().getMessage(player, "gui.zones.title", "&1&l✦ Fishing Habitats & Zones"), "ZONES_MENU");
        this.plugin = plugin;
        this.player = player;
        this.profile = plugin.getProfileManager().getProfile(player);

        initialize();
    }

    private void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        List<FishingZone> zones = new ArrayList<>(plugin.getZoneManager().getAllZones());
        for (int i = 0; i < zones.size() && i < SLOTS.length; i++) {
            FishingZone zone = zones.get(i);
            int slot = SLOTS[i];
            boolean eligible = profile != null && profile.getLevel() >= zone.requiredLevel();

            Material mat = eligible ? Material.COMPASS : Material.RECOVERY_COMPASS;
            String status = eligible ?
                    plugin.getLanguageManager().getMessage(player, "gui.zones.available", "&a✔ Available") :
                    plugin.getLanguageManager().getMessage(player, "gui.zones.locked", "&c✘ Requires Level {level}").replace("{level}", String.valueOf(zone.requiredLevel()));

            List<String> lore = new ArrayList<>();
            lore.add(plugin.getLanguageManager().getMessage(player, "gui.zones.world", "&7World: &f{world}").replace("{world}", zone.worldName()));
            lore.add(plugin.getLanguageManager().getMessage(player, "gui.zones.req_level", "&7Required Level: &e{level}").replace("{level}", String.valueOf(zone.requiredLevel())));
            lore.add(plugin.getLanguageManager().getMessage(player, "gui.zones.xp_boost", "&7XP Boost: &b{multiplier}x").replace("{multiplier}", TextUtil.formatDecimal(zone.xpMultiplier())));
            lore.add(plugin.getLanguageManager().getMessage(player, "gui.zones.money_boost", "&7Money Boost: &6{multiplier}x").replace("{multiplier}", TextUtil.formatDecimal(zone.moneyMultiplier())));
            lore.add("");
            lore.add(status);

            setItem(slot, ItemBuilder.from(mat)
                    .name("&3&l" + zone.displayName())
                    .lore(lore)
                    .build());
        }

        setItem(40, ItemBuilder.from(Material.DARK_OAK_DOOR)
                .name(plugin.getLanguageManager().getMessage(player, "gui.common.back", "&cBack to Main Menu"))
                .build(), e -> new MainFishingMenu(plugin, player).open(player));
    }
}
