package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.progression.ProgressionManager;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class LevelProgressionMenu extends CustomGui {
    private final AHowToFishPlugin plugin;
    private final Player player;

    public LevelProgressionMenu(AHowToFishPlugin plugin, Player player) {
        super(45, plugin.getLanguageManager().getMessage(player, "gui.progression.title", "&1&l✦ Fishing Level Progression"), "PROGRESSION_MENU");
        this.plugin = plugin;
        this.player = player;

        initialize();
    }

    private void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);
        FishingProfile profile = plugin.getProfileManager().getProfile(player);
        ProgressionManager pm = plugin.getProgressionManager();

        int level = profile.getLevel();
        long currentXp = profile.getXp();
        long nextXp = pm.getRequiredXpForLevel(level + 1);
        double progress = pm.getLevelProgressPercent(profile);

        setItem(13, ItemBuilder.from(Material.NETHER_STAR)
                .name(plugin.getLanguageManager().getMessage(player, "gui.progression.current_rank", "&6&lCurrent Rank &8| &eLevel {level}").replace("{level}", String.valueOf(level)))
                .lore(
                        plugin.getLanguageManager().getMessage(player, "gui.progression.total_xp", "&7Total XP: &b{xp}").replace("{xp}", TextUtil.formatInteger(currentXp)),
                        plugin.getLanguageManager().getMessage(player, "gui.progression.next_level_xp", "&7XP Needed for Next Level: &f{xp}").replace("{xp}", TextUtil.formatInteger(nextXp)),
                        plugin.getLanguageManager().getMessage(player, "gui.progression.progress", "&7Progress: &a{percent}%").replace("{percent}", TextUtil.formatDecimal(progress))
                ).build());

        int[] milestones = {5, 10, 20, 35, 50, 75, 100};
        int[] slots = {20, 21, 22, 23, 24, 25, 26};

        for (int i = 0; i < milestones.length; i++) {
            int mLevel = milestones[i];
            int slot = slots[i];
            boolean reached = level >= mLevel;

            Material mat = reached ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            String status = reached ?
                    plugin.getLanguageManager().getMessage(player, "gui.progression.unlocked", "&a✔ Unlocked") :
                    plugin.getLanguageManager().getMessage(player, "gui.progression.locked", "&c✘ Locked (Level {level})").replace("{level}", String.valueOf(mLevel));

            setItem(slot, ItemBuilder.from(mat)
                    .name(plugin.getLanguageManager().getMessage(player, "gui.progression.milestone_title", "&e&lMilestone: Level {level}").replace("{level}", String.valueOf(mLevel)))
                    .lore(
                            plugin.getLanguageManager().getMessage(player, "gui.progression.milestone_req_level", "&7Required Level: &f{level}").replace("{level}", String.valueOf(mLevel)),
                            plugin.getLanguageManager().getMessage(player, "gui.progression.milestone_req_xp", "&7Required XP: &b{xp}").replace("{xp}", TextUtil.formatInteger(pm.getRequiredXpForLevel(mLevel))),
                            "",
                            status
                    ).build());
        }

        setItem(40, ItemBuilder.from(Material.DARK_OAK_DOOR)
                .name(plugin.getLanguageManager().getMessage(player, "gui.common.back", "&cBack to Main Menu"))
                .build(), e -> new MainFishingMenu(plugin, player).open(player));
    }
}
