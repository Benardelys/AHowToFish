package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.language.LanguageManager;
import com.ardelys.ahowtofish.model.Competition;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CompetitionLeaderboardMenu extends CustomGui {

    private final AHowToFishPlugin plugin;
    private final Player player;
    private final LanguageManager lm;

    public CompetitionLeaderboardMenu(AHowToFishPlugin plugin, Player player) {
        super(54, plugin.getLanguageManager().getMessage(player, "gui.competition.title", "&6&l✦ Fishing Tournament ✦"), "COMPETITION_MENU");
        this.plugin = plugin;
        this.player = player;
        this.lm = plugin.getLanguageManager();

        initialize();
    }

    private void initialize() {
        fillBorder(Material.BLUE_STAINED_GLASS_PANE);

        Competition comp = plugin.getCompetitionManager() != null ? plugin.getCompetitionManager().getActiveCompetition() : null;
        boolean active = comp != null && comp.isActive();

        if (active) {
            List<String> infoLore = new ArrayList<>();
            infoLore.add(lm.getMessage(player, "gui.competition.category", "&7Category: &e{category}").replace("{category}", comp.getType().getDisplayName()));
            infoLore.add(lm.getMessage(player, "gui.competition.time_left", "&7Time Remaining: &a{time}").replace("{time}", TextUtil.formatTime(comp.getRemainingSeconds())));
            infoLore.add(lm.getMessage(player, "gui.competition.participants", "&7Participants: &b{count}").replace("{count}", String.valueOf(comp.getParticipantCount())));
            infoLore.add(lm.getMessage(player, "gui.competition.leader", "&7Current Leader: &6{player} &7({score} {unit})")
                    .replace("{player}", comp.getLeaderName())
                    .replace("{score}", TextUtil.formatDecimal(comp.getLeaderScore()))
                    .replace("{unit}", comp.getType().getUnit()));
            infoLore.add("");
            infoLore.add(lm.getMessage(player, "gui.competition.join_hint", "&eCast your rod in any fishing area to participate!"));

            setItem(4, ItemBuilder.from(Material.CLOCK)
                    .name(lm.getMessage(player, "gui.competition.active_title", "&6&l✦ ACTIVE TOURNAMENT ✦"))
                    .lore(infoLore)
                    .build());
        } else {
            setItem(4, ItemBuilder.from(Material.MINECART)
                    .name(lm.getMessage(player, "gui.competition.inactive_title", "&7&l✦ No Active Tournament ✦"))
                    .lore(
                            lm.getMessage(player, "gui.competition.inactive_desc1", "&7There is currently no ongoing tournament."),
                            lm.getMessage(player, "gui.competition.inactive_desc2", "&7Competitions start automatically at intervals"),
                            lm.getMessage(player, "gui.competition.inactive_desc3", "&7or can be launched by server administrators.")
                    ).build());
        }

        int[] displaySlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        if (active) {
            List<Map.Entry<UUID, Double>> lb = comp.getLeaderboard();
            for (int i = 0; i < displaySlots.length; i++) {
                int slot = displaySlots[i];
                int rank = i + 1;

                if (i < lb.size()) {
                    Map.Entry<UUID, Double> entry = lb.get(i);
                    String pName = comp.getPlayerName(entry.getKey());
                    double score = entry.getValue();

                    Material mat;
                    String rankDisplay;
                    if (rank == 1) {
                        mat = Material.GOLD_BLOCK;
                        rankDisplay = lm.getMessage(player, "gui.competition.first_place", "&e&l➊ 1st Place: &f{player}").replace("{player}", pName);
                    } else if (rank == 2) {
                        mat = Material.IRON_BLOCK;
                        rankDisplay = lm.getMessage(player, "gui.competition.second_place", "&f&l➋ 2nd Place: &f{player}").replace("{player}", pName);
                    } else if (rank == 3) {
                        mat = Material.COPPER_BLOCK;
                        rankDisplay = lm.getMessage(player, "gui.competition.third_place", "&6&l➌ 3rd Place: &f{player}").replace("{player}", pName);
                    } else {
                        mat = Material.PAPER;
                        rankDisplay = "&7#" + rank + " &f" + pName;
                    }

                    List<String> lore = new ArrayList<>();
                    lore.add(lm.getMessage(player, "gui.competition.score", "&7Score: &a{score} &7{unit}")
                            .replace("{score}", TextUtil.formatDecimal(score))
                            .replace("{unit}", comp.getType().getUnit()));

                    Double moneyPrize = plugin.getCompetitionManager().getRewardMoney().get(rank);
                    if (moneyPrize != null && moneyPrize > 0) {
                        lore.add(lm.getMessage(player, "gui.competition.prize", "&7Prize: &e+${prize}")
                                .replace("{prize}", TextUtil.formatDecimal(moneyPrize)));
                    }
                    if (entry.getKey().equals(player.getUniqueId())) {
                        lore.add("");
                        lore.add("&a✔ This is you!");
                    }

                    setItem(slot, ItemBuilder.from(mat)
                            .name(rankDisplay)
                            .lore(lore)
                            .build());
                } else {
                    setItem(slot, ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                            .name("&8#" + rank + " &7Vacant")
                            .lore("&8No angler has claimed this spot yet.")
                            .build());
                }
            }
        } else {
            for (int slot : displaySlots) {
                setItem(slot, ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                        .name("&8Standings Unavailable")
                        .build());
            }
        }

        if (active) {
            int myRank = comp.getRank(player.getUniqueId());
            double myScore = comp.getScore(player.getUniqueId());

            String unrankedStr = lm.getMessage(player, "scoreboard_labels.unranked", "Unranked");
            String rankStr = myRank > 0 ? "&e#" + myRank : "&7" + unrankedStr;

            List<String> myLore = new ArrayList<>();
            myLore.add(lm.getMessage(player, "gui.competition.my_rank", "&7Current Rank: {rank}").replace("{rank}", rankStr));
            myLore.add(lm.getMessage(player, "gui.competition.my_score", "&7Your Score: &a{score} &7{unit}")
                    .replace("{score}", TextUtil.formatDecimal(myScore))
                    .replace("{unit}", comp.getType().getUnit()));
            if (myRank > 0 && myRank <= 3) {
                myLore.add(lm.getMessage(player, "gui.competition.winning_pos", "&6★ You are in a winning position!"));
            } else if (myRank > 3) {
                myLore.add(lm.getMessage(player, "gui.competition.climb_ranks", "&7Catch more valuable fish to climb the ranks!"));
            } else {
                myLore.add(lm.getMessage(player, "gui.competition.not_participated", "&7You have not caught a qualifying fish yet."));
            }

            setItem(40, ItemBuilder.from(Material.PLAYER_HEAD)
                    .name(lm.getMessage(player, "gui.competition.my_status_title", "&b&lYour Tournament Status"))
                    .lore(myLore)
                    .build());
        }

        List<String> rewardLore = new ArrayList<>();
        Map<Integer, Double> rewards = plugin.getCompetitionManager().getRewardMoney();
        if (!rewards.isEmpty()) {
            rewards.forEach((rank, money) -> {
                String prefix = switch (rank) {
                    case 1 -> "&e➊ 1st:";
                    case 2 -> "&f➋ 2nd:";
                    case 3 -> "&6➌ 3rd:";
                    default -> "&7#" + rank + ":";
                };
                rewardLore.add(prefix + " &a$" + TextUtil.formatDecimal(money));
            });
        } else {
            rewardLore.add("&7Prizes are configured in &fcompetitions.yml");
        }

        setItem(48, ItemBuilder.from(Material.CHEST)
                .name(lm.getMessage(player, "gui.competition.prize_pool_title", "&a&lTournament Prize Pool"))
                .lore(rewardLore)
                .build());

        setItem(49, ItemBuilder.from(Material.SUNFLOWER)
                .name(lm.getMessage(player, "gui.competition.refresh_title", "&e&lRefresh Leaderboard"))
                .lore(lm.getMessage(player, "gui.competition.refresh_desc", "&7Click to refresh live standings."))
                .build(), e -> new CompetitionLeaderboardMenu(plugin, player).open(player));

        setItem(50, ItemBuilder.from(Material.ARROW)
                .name(lm.getMessage(player, "gui.common.back", "&cBack to Menu"))
                .build(), e -> new MainFishingMenu(plugin, player).open(player));
    }
}