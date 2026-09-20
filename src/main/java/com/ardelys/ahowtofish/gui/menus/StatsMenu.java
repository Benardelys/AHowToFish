package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.player.PlayerStats;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class StatsMenu extends CustomGui {
    private final AHowToFishPlugin plugin;
    private final Player player;

    public StatsMenu(AHowToFishPlugin plugin, Player player) {
        super(45, plugin.getLanguageManager().getMessage(player, "gui.stats.title", "&1&l✦ Fishing Statistics"), "STATS_MENU");
        this.plugin = plugin;
        this.player = player;

        initialize();
    }

    private void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);
        FishingProfile profile = plugin.getProfileManager().getProfile(player);
        PlayerStats stats = profile.getStats();

        setItem(11, ItemBuilder.from(Material.FISHING_ROD)
                .name(plugin.getLanguageManager().getMessage(player, "gui.stats.activity_title", "&e&lActivity & Catches"))
                .lore(
                        plugin.getLanguageManager().getMessage(player, "gui.stats.total_casts", "&7Total Casts: &f{casts}").replace("{casts}", TextUtil.formatInteger(stats.getTotalCasts())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.successful_catches", "&7Successful Catches: &a{count}").replace("{count}", TextUtil.formatInteger(stats.getSuccessfulCatches())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.failed_catches", "&7Missed Catches: &c{count}").replace("{count}", TextUtil.formatInteger(stats.getFailedCatches())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.total_fish_caught", "&7Total Fish Caught: &e{count}").replace("{count}", TextUtil.formatInteger(stats.getTotalFishCaught())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.discovered_species", "&7Discovered Species: &b{count}").replace("{count}", String.valueOf(profile.getCollection().getDiscoveredCount()))
                ).build());

        setItem(13, ItemBuilder.from(Material.ANVIL)
                .name(plugin.getLanguageManager().getMessage(player, "gui.stats.weight_title", "&6&lWeight & Records"))
                .lore(
                        plugin.getLanguageManager().getMessage(player, "gui.stats.largest_fish", "&7Largest Fish: &a{weight} kg").replace("{weight}", TextUtil.formatDecimal(stats.getLargestFishWeight())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.total_fish_weight", "&7Total Fish Weight: &f{weight} kg").replace("{weight}", TextUtil.formatDecimal(stats.getTotalFishWeight())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.current_streak", "&7Current Streak: &e{streak}").replace("{streak}", String.valueOf(stats.getCurrentStreak())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.best_streak", "&7Best Streak: &6{streak}").replace("{streak}", String.valueOf(stats.getBestStreak()))
                ).build());

        setItem(15, ItemBuilder.from(Material.GOLD_INGOT)
                .name(plugin.getLanguageManager().getMessage(player, "gui.stats.progression_title", "&a&lProgression & Earnings"))
                .lore(
                        plugin.getLanguageManager().getMessage(player, "gui.stats.current_level", "&7Current Level: &e{level}").replace("{level}", String.valueOf(profile.getLevel())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.total_xp_earned", "&7Total XP Earned: &b{xp}").replace("{xp}", TextUtil.formatInteger(stats.getTotalXpEarned())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.total_money_earned", "&7Total Money Earned: &a${money}").replace("{money}", TextUtil.formatDecimal(stats.getTotalMoneyEarned()))
                ).build());

        setItem(21, ItemBuilder.from(Material.AMETHYST_SHARD)
                .name(plugin.getLanguageManager().getMessage(player, "gui.stats.rarity_title", "&d&lRarity Breakdown"))
                .lore(
                        plugin.getLanguageManager().getMessage(player, "gui.stats.common_fish", "&7Common Fish: &f{count}").replace("{count}", TextUtil.formatInteger(stats.getCommonCaught())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.rare_fish", "&7Rare Fish: &9{count}").replace("{count}", TextUtil.formatInteger(stats.getRareCaught())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.legendary_fish", "&7Legendary Fish: &6{count}").replace("{count}", TextUtil.formatInteger(stats.getLegendaryCaught()))
                ).build());

        setItem(23, ItemBuilder.from(Material.GOLDEN_APPLE)
                .name(plugin.getLanguageManager().getMessage(player, "gui.stats.competitions_title", "&e&lTournaments & Events"))
                .lore(
                        plugin.getLanguageManager().getMessage(player, "gui.stats.competitions_won", "&7Competitions Won: &e{count}").replace("{count}", String.valueOf(stats.getCompetitionsWon())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.events_won", "&7Events Won: &6{count}").replace("{count}", String.valueOf(stats.getEventsWon())),
                        plugin.getLanguageManager().getMessage(player, "gui.stats.fishing_time", "&7Fishing Time: &f{time}").replace("{time}", TextUtil.formatTime(stats.getFishingTimeSeconds()))
                ).build());

        setItem(40, ItemBuilder.from(Material.DARK_OAK_DOOR)
                .name(plugin.getLanguageManager().getMessage(player, "gui.common.back", "&cBack to Main Menu"))
                .build(), e -> new MainFishingMenu(plugin, player).open(player));
    }
}
