package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.language.LanguageManager;
import com.ardelys.ahowtofish.model.FishingZone;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MainFishingMenu extends CustomGui {
    private final AHowToFishPlugin plugin;
    private final Player player;
    private final FishingProfile profile;
    private final LanguageManager lm;

    public MainFishingMenu(AHowToFishPlugin plugin, Player player) {
        super(54, plugin.getLanguageManager().getMessage(player, "gui.main.title", "&1&l✦ AHowToFish &8| &9Game Hub"), "MAIN_MENU");
        this.plugin = plugin;
        this.player = player;
        this.profile = plugin.getProfileManager().getProfile(player);
        this.lm = plugin.getLanguageManager();

        initialize();
    }

    private void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        String clickOpen = lm.getMessage(player, "gui.common.click_to_open", "&eClick to open!");
        String clickInspect = lm.getMessage(player, "gui.common.click_to_inspect", "&eClick to inspect!");

        int level = profile != null ? profile.getLevel() : 1;
        long xp = profile != null ? profile.getXp() : 0;
        double progress = profile != null ? plugin.getProgressionManager().getLevelProgressPercent(profile) : 0.0;
        long fishCaught = profile != null ? profile.getStats().getTotalFishCaught() : 0;
        double largestWeight = profile != null ? profile.getStats().getLargestFishWeight() : 0.0;
        int compsWon = profile != null ? profile.getStats().getCompetitionsWon() : 0;

        double balance = 0.0;
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isAvailable()) {
            balance = plugin.getVaultHook().getBalance(player);
        } else if (profile != null) {
            balance = profile.getStats().getTotalMoneyEarned();
        }

        FishingZone currentZone = plugin.getZoneManager().getZoneAt(player.getLocation());
        String wildZoneName = lm.getMessage(player, "command.status.wild_zone", "Open Waters");
        String zoneName = currentZone != null ? currentZone.displayName() : wildZoneName;

        List<String> profileLore = new ArrayList<>();
        profileLore.add(lm.getMessage(player, "gui.main.level", "&7Fishing Level: &e{level}").replace("{level}", String.valueOf(level)));
        profileLore.add(lm.getMessage(player, "gui.main.xp", "&7Fishing XP: &b{xp} &8(&a{progress}%&8)")
                .replace("{xp}", TextUtil.formatInteger(xp))
                .replace("{progress}", TextUtil.formatDecimal(progress)));
        profileLore.add(lm.getMessage(player, "gui.main.balance", "&7Coins Balance: &a${balance}").replace("{balance}", TextUtil.formatDecimal(balance)));
        profileLore.add(lm.getMessage(player, "gui.main.location", "&7Current Location: &f{location}").replace("{location}", zoneName));
        profileLore.add("");
        profileLore.add(lm.getMessage(player, "gui.main.fish_caught", "&7Total Fish Caught: &f{count}").replace("{count}", TextUtil.formatInteger(fishCaught)));
        profileLore.add(lm.getMessage(player, "gui.main.personal_record", "&7Personal Record: &a{weight} kg").replace("{weight}", TextUtil.formatDecimal(largestWeight)));
        profileLore.add(lm.getMessage(player, "gui.main.tournaments_won", "&7Tournaments Won: &6{count}").replace("{count}", String.valueOf(compsWon)));
        profileLore.add("");
        profileLore.add(lm.getMessage(player, "gui.main.profile_footer", "&b✦ Professional Angler Profile"));

        setItem(4, ItemBuilder.from(Material.PLAYER_HEAD)
                .name(lm.getMessage(player, "gui.main.profile_title", "&6&l✦ {player} &8| &eAngler Profile ✦").replace("{player}", player.getName()))
                .lore(profileLore)
                .build());

        setItem(20, ItemBuilder.from(Material.EMERALD)
                .name(lm.getMessage(player, "gui.main.shop_name", "&6&lFish Market & Bait Shop"))
                .lore(
                        lm.getMessage(player, "gui.main.shop_desc", "&7Sell freshly caught fish and purchase premium baits & gear."),
                        "",
                        clickOpen
                ).build(), e -> new ShopMenu(plugin, player).open(player));

        int discovered = profile != null ? profile.getCollection().getDiscoveredCount() : 0;
        int totalFish = plugin.getFishManager().getAllFish().size();
        setItem(21, ItemBuilder.from(Material.PUFFERFISH)
                .name(lm.getMessage(player, "gui.main.collection_name", "&b&lFish Collection"))
                .lore(
                        lm.getMessage(player, "gui.main.collection_desc", "&7Discover all unique species, rarities, and record catches."),
                        "",
                        lm.getMessage(player, "gui.main.discovered_counter", "&7Discovered: &a{discovered}&7/&f{total}")
                                .replace("{discovered}", String.valueOf(discovered))
                                .replace("{total}", String.valueOf(totalFish)),
                        "",
                        clickOpen
                ).build(), e -> new CollectionMenu(plugin, player, 0).open(player));

        setItem(22, ItemBuilder.from(Material.NETHER_STAR)
                .name(lm.getMessage(player, "gui.main.upgrades_name", "&c&lSkill Perks & Upgrades"))
                .lore(
                        lm.getMessage(player, "gui.main.upgrades_desc", "&7Unlock permanent passive abilities: Luck, Double Catch, XP Booster."),
                        "",
                        clickOpen
                ).build(), e -> new UpgradesMenu(plugin, player).open(player));

        setItem(23, ItemBuilder.from(Material.COMPASS)
                .name(lm.getMessage(player, "gui.main.zones_name", "&3&lFishing Habitats & Zones"))
                .lore(
                        lm.getMessage(player, "gui.main.zones_desc", "&7Explore unique fishing zones, requirements, and exotic local species."),
                        "",
                        clickOpen
                ).build(), e -> new ZonesMenu(plugin, player).open(player));

        setItem(24, ItemBuilder.from(Material.EXPERIENCE_BOTTLE)
                .name(lm.getMessage(player, "gui.main.progression_name", "&a&lFishing Progression"))
                .lore(
                        lm.getMessage(player, "gui.main.progression_desc", "&7Inspect your fishing level path, milestones, and rank progression."),
                        "",
                        lm.getMessage(player, "gui.main.progression_level", "&7Level: &e{level}").replace("{level}", String.valueOf(level)),
                        lm.getMessage(player, "gui.main.progression_pct", "&7Progress: &a{progress}%").replace("{progress}", TextUtil.formatDecimal(progress)),
                        "",
                        clickInspect
                ).build(), e -> new LevelProgressionMenu(plugin, player).open(player));

        setItem(29, ItemBuilder.from(Material.WRITABLE_BOOK)
                .name(lm.getMessage(player, "gui.main.quests_name", "&d&lFishing Quests & Missions"))
                .lore(
                        lm.getMessage(player, "gui.main.quests_desc", "&7Complete daily and weekly contracts to earn money, XP, and rare rewards."),
                        "",
                        clickOpen
                ).build(), e -> new QuestsMenu(plugin, player).open(player));

        setItem(30, ItemBuilder.from(Material.BOOK)
                .name(lm.getMessage(player, "gui.main.stats_name", "&e&lFishing Statistics"))
                .lore(
                        lm.getMessage(player, "gui.main.stats_desc", "&7View your complete fishing career, catches, streaks, records, and history."),
                        "",
                        clickOpen
                ).build(), e -> new StatsMenu(plugin, player).open(player));

        boolean compActive = plugin.getCompetitionManager() != null && plugin.getCompetitionManager().isCompetitionActive();
        String compStatus = compActive ?
                lm.getMessage(player, "gui.main.tournaments_active", "&aACTIVE TOURNAMENT") :
                lm.getMessage(player, "gui.main.tournaments_inactive", "&7No Active Tournament");

        setItem(31, ItemBuilder.from(Material.GOLDEN_SWORD)
                .name(lm.getMessage(player, "gui.main.tournaments_name", "&6&l✦ Fishing Tournaments ✦"))
                .lore(
                        lm.getMessage(player, "gui.main.tournaments_desc", "&7Compete against other anglers for massive coin prizes, fame, and leaderboard trophies."),
                        "",
                        "&7Status: " + compStatus,
                        "",
                        clickOpen
                ).build(), e -> new CompetitionLeaderboardMenu(plugin, player).open(player));

        setItem(32, ItemBuilder.from(Material.GOLDEN_HELMET)
                .name(lm.getMessage(player, "gui.main.leaderboard_name", "&e&lTop Anglers Leaderboard"))
                .lore(
                        lm.getMessage(player, "gui.main.leaderboard_desc", "&7View rankings for highest level, most fish, largest weight, and winnings."),
                        "",
                        clickOpen
                ).build(), e -> new LeaderboardMenu(plugin, player, "LEVEL").open(player));

        int completedAch = profile != null ? profile.getUnlockedAchievements().size() : 0;
        int totalAch = plugin.getAchievementManager().getAllAchievements().size();
        setItem(33, ItemBuilder.from(Material.DIAMOND)
                .name(lm.getMessage(player, "gui.main.achievements_name", "&5&lAchievements & Milestones"))
                .lore(
                        lm.getMessage(player, "gui.main.achievements_desc", "&7Track your lifelong fishing accomplishments and unlock prestige titles."),
                        "",
                        lm.getMessage(player, "gui.main.achievements_counter", "&7Completed: &a{completed}&7/&f{total}")
                                .replace("{completed}", String.valueOf(completedAch))
                                .replace("{total}", String.valueOf(totalAch)),
                        "",
                        clickOpen
                ).build(), e -> new AchievementsMenu(plugin, player).open(player));

        setItem(45, ItemBuilder.from(Material.HOPPER)
                .name(lm.getMessage(player, "gui.main.quick_sell_title", "&a&lQuick Sell All Fish"))
                .lore(
                        lm.getMessage(player, "gui.main.quick_sell_desc", "&7Instantly sell all caught fish in your inventory without opening the market."),
                        "",
                        lm.getMessage(player, "gui.main.quick_sell_click", "&a▶ Click to sell all")
                ).build(), e -> {
            if (plugin.getFishSellService() != null) {
                plugin.getFishSellService().sellAllFish(player);
                new MainFishingMenu(plugin, player).open(player);
            }
        });

        if (player.hasPermission("ahowtofish.admin")) {
            setItem(49, ItemBuilder.from(Material.COMMAND_BLOCK)
                    .name(lm.getMessage(player, "gui.main.admin_panel_title", "&4&lAdmin Control Center"))
                    .lore(
                            lm.getMessage(player, "gui.main.admin_panel_desc", "&7Access tournament controls, event triggers, item spawners, diagnostics, and hot-reload management."),
                            "",
                            lm.getMessage(player, "gui.main.admin_panel_click", "&c▶ Click to open admin panel")
                    ).build(), e -> new AdminPanelMenu(plugin, player).open(player));
        }

        setItem(53, ItemBuilder.from(Material.BARRIER)
                .name(lm.getMessage(player, "gui.common.close", "&cClose"))
                .build(), e -> player.closeInventory());
    }
}