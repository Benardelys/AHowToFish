package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.model.FishingAchievement;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AchievementsMenu extends CustomGui {
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final AHowToFishPlugin plugin;
    private final Player player;
    private final FishingProfile profile;

    public AchievementsMenu(AHowToFishPlugin plugin, Player player) {
        super(45, plugin.getLanguageManager().getMessage(player, "gui.achievements.title", "&1&l✦ Fishing Achievements"), "ACHIEVEMENTS_MENU");
        this.plugin = plugin;
        this.player = player;
        this.profile = plugin.getProfileManager().getProfile(player);

        initialize();
    }

    private void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        List<FishingAchievement> achievements = new ArrayList<>(plugin.getAchievementManager().getAllAchievements());
        for (int i = 0; i < achievements.size() && i < SLOTS.length; i++) {
            FishingAchievement ach = achievements.get(i);
            int slot = SLOTS[i];
            boolean unlocked = profile.hasAchievement(ach.id());

            Material mat = unlocked ? Material.DIAMOND : Material.COAL;
            String status = unlocked
                    ? plugin.getLanguageManager().getMessage(player, "gui.achievements.status_unlocked", "&a✔ Unlocked")
                    : plugin.getLanguageManager().getMessage(player, "gui.achievements.status_locked", "&c✘ Locked");

            String achName = plugin.getAchievementManager().getLocalizedAchievementName(player, ach);
            String achDesc = plugin.getAchievementManager().getLocalizedAchievementDesc(player, ach);
            String rewardsLabel = plugin.getLanguageManager().getMessage(player, "gui.achievements.rewards_label", "&7Rewards: &b+{xp} XP &8| &a+${money}")
                    .replace("{xp}", String.valueOf(ach.xpReward()))
                    .replace("{money}", TextUtil.formatDecimal(ach.moneyReward()));

            setItem(slot, ItemBuilder.from(mat)
                    .name("&b&l" + achName)
                    .lore(
                            "&7" + achDesc,
                            "",
                            rewardsLabel,
                            "",
                            status
                    ).build());
        }

        setItem(40, ItemBuilder.from(Material.DARK_OAK_DOOR)
                .name(plugin.getLanguageManager().getMessage(player, "gui.common.back", "&cBack to Main Menu"))
                .build(), e -> new MainFishingMenu(plugin, player).open(player));
    }
}
