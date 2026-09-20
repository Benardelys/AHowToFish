package com.ardelys.ahowtofish.competition;

import com.ardelys.ahowtofish.api.events.FishingCompetitionEndEvent;
import com.ardelys.ahowtofish.api.events.FishingCompetitionStartEvent;
import com.ardelys.ahowtofish.integration.VaultHook;
import com.ardelys.ahowtofish.language.LanguageManager;
import com.ardelys.ahowtofish.language.MessageKey;
import com.ardelys.ahowtofish.model.Competition;
import com.ardelys.ahowtofish.model.FishCatchResult;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.player.ProfileManager;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class CompetitionManager {
    private final JavaPlugin plugin;
    private final LanguageManager languageManager;
    private final VaultHook vaultHook;
    private final ProfileManager profileManager;

    private Competition activeCompetition = null;
    private BukkitTask competitionTask = null;
    private BossBar bossBar = null;

    private final Map<Integer, Double> rewardMoney = new HashMap<>();
    private final Map<Integer, List<String>> rewardCommands = new HashMap<>();

    public CompetitionManager(JavaPlugin plugin, LanguageManager languageManager, VaultHook vaultHook, ProfileManager profileManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.vaultHook = vaultHook;
        this.profileManager = profileManager;
    }

    public void load() {
        rewardMoney.clear();
        rewardCommands.clear();

        File file = new File(plugin.getDataFolder(), "competitions.yml");
        if (!file.exists()) {
            plugin.saveResource("competitions.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection rewardsSec = config.getConfigurationSection("competitions.rewards");

        if (rewardsSec != null) {
            for (String key : rewardsSec.getKeys(false)) {
                try {
                    int rank = Integer.parseInt(key);
                    double money = rewardsSec.getDouble(key + ".money", 0.0);
                    List<String> cmds = rewardsSec.getStringList(key + ".commands");
                    if (money > 0) rewardMoney.put(rank, money);
                    if (!cmds.isEmpty()) rewardCommands.put(rank, cmds);
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public boolean isCompetitionActive() {
        return activeCompetition != null && activeCompetition.isActive();
    }

    public Competition getActiveCompetition() {
        return activeCompetition;
    }

    public boolean startCompetition(CompetitionType type, int durationSeconds) {
        if (isCompetitionActive()) {
            return false;
        }

        activeCompetition = new Competition(type, durationSeconds);
        bossBar = BossBar.bossBar(
                Component.text(TextUtil.colorize("&b&lTournament: &e" + type.getDisplayName() + " &7(" + TextUtil.formatTime(durationSeconds) + ")")),
                1.0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bossBar);
            SoundParticleUtil.playSound(p, "UI_TOAST_CHALLENGE_COMPLETE", 1.0f, 1.0f);
        }

        Bukkit.getPluginManager().callEvent(new FishingCompetitionStartEvent(activeCompetition));
        Bukkit.broadcast(TextUtil.toComponent(languageManager.getMessage(
                Bukkit.getConsoleSender(),
                MessageKey.COMPETITION_START,
                "type", type.getDisplayName(),
                "duration", TextUtil.formatTime(durationSeconds)
        )));

        startTicker();
        return true;
    }

    private void startTicker() {
        if (competitionTask != null) {
            competitionTask.cancel();
        }

        competitionTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isCompetitionActive()) {
                stopTicker();
                return;
            }

            activeCompetition.decrementSecond();
            int remaining = activeCompetition.getRemainingSeconds();

            if (remaining <= 0) {
                endCompetition();
                return;
            }

            if (bossBar != null) {
                float progress = Math.max(0.0f, Math.min(1.0f, (float) remaining / activeCompetition.getTotalDurationSeconds()));
                bossBar.progress(progress);

                if (progress > 0.5f) {
                    bossBar.color(BossBar.Color.BLUE);
                } else if (progress > 0.2f) {
                    bossBar.color(BossBar.Color.YELLOW);
                } else {
                    bossBar.color(BossBar.Color.RED);
                }

                List<Map.Entry<UUID, Double>> top = activeCompetition.getLeaderboard();
                String leaderStr = "None";
                if (!top.isEmpty()) {
                    Map.Entry<UUID, Double> first = top.get(0);
                    leaderStr = activeCompetition.getPlayerName(first.getKey()) + " (" + TextUtil.formatDecimal(first.getValue()) + " " + activeCompetition.getType().getUnit() + ")";
                }

                bossBar.name(TextUtil.toComponent("&b&l" + activeCompetition.getType().getDisplayName() + " &8| &e1st: " + leaderStr + " &8| &f" + TextUtil.formatTime(remaining)));

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.showBossBar(bossBar);
                }
            }

            if (remaining == 60 || remaining == 30 || remaining == 10) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendActionBar(TextUtil.toComponent("&c&l⏱ Tournament Ending Soon: &e" + remaining + "s left!"));
                    SoundParticleUtil.playSound(p, "BLOCK_NOTE_BLOCK_PLING", 0.8f, 1.2f);
                }
            } else if (remaining <= 5) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendActionBar(TextUtil.toComponent("&c&l⏱ Tournament Ending in " + remaining + "..."));
                    SoundParticleUtil.playSound(p, "BLOCK_NOTE_BLOCK_HAT", 1.0f, 1.5f);
                }
            }
        }, 20L, 20L);
    }

    public void stopCompetition() {
        if (activeCompetition != null) {
            activeCompetition.setActive(false);
            stopTicker();
            if (bossBar != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.hideBossBar(bossBar);
                }
                bossBar = null;
            }
            activeCompetition = null;
        }
    }

    public void endCompetition() {
        if (!isCompetitionActive()) return;

        Competition comp = activeCompetition;
        comp.setActive(false);
        stopTicker();

        if (bossBar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bossBar);
            }
            bossBar = null;
        }

        List<Map.Entry<UUID, Double>> leaderboard = comp.getLeaderboard();
        Bukkit.getPluginManager().callEvent(new FishingCompetitionEndEvent(comp, leaderboard));

        Bukkit.broadcast(TextUtil.toComponent("&8&m----------------------------------------"));
        Bukkit.broadcast(TextUtil.toComponent("&6&l✦ FISHING TOURNAMENT RESULTS ✦"));
        Bukkit.broadcast(TextUtil.toComponent("&7Category: &e" + comp.getType().getDisplayName()));

        if (leaderboard.isEmpty()) {
            Bukkit.broadcast(TextUtil.toComponent("&cNo catches were recorded during this tournament!"));
        } else {
            int max = Math.min(3, leaderboard.size());
            for (int i = 0; i < max; i++) {
                int rank = i + 1;
                Map.Entry<UUID, Double> entry = leaderboard.get(i);
                String name = comp.getPlayerName(entry.getKey());
                double score = entry.getValue();

                String medal = switch (rank) {
                    case 1 -> "&e➊ 1st Place";
                    case 2 -> "&f➋ 2nd Place";
                    case 3 -> "&6➌ 3rd Place";
                    default -> "&7" + rank + "th";
                };

                Bukkit.broadcast(TextUtil.toComponent(medal + ": &f" + name + " &7- &a" + TextUtil.formatDecimal(score) + " " + comp.getType().getUnit()));

                Player winnerPlayer = Bukkit.getPlayer(entry.getKey());
                if (winnerPlayer != null) {
                    String winTitle = switch (rank) {
                        case 1 -> "&e&l🏆 1ST PLACE WINNER! 🏆";
                        case 2 -> "&f&l🥈 2ND PLACE! 🥈";
                        case 3 -> "&6&l🥉 3RD PLACE! 🥉";
                        default -> "&aTournament Finished!";
                    };
                    TextUtil.sendTitle(winnerPlayer, winTitle, "&7Prize awarded! Check chat for rewards.", 10, 60, 20);
                    SoundParticleUtil.playSound(winnerPlayer, "UI_TOAST_CHALLENGE_COMPLETE", 1.0f, 1.2f);

                    if (rank == 1) {
                        FishingProfile prof = profileManager.getProfile(winnerPlayer);
                        if (prof != null) prof.getStats().incrementCompetitionsWon();
                    }

                    if (rewardMoney.containsKey(rank) && vaultHook != null && vaultHook.isAvailable()) {
                        vaultHook.deposit(winnerPlayer, rewardMoney.get(rank));
                        winnerPlayer.sendMessage(TextUtil.colorize("&aTournament Prize: You received &e$" + TextUtil.formatDecimal(rewardMoney.get(rank)) + "&a!"));
                    }
                    if (rewardCommands.containsKey(rank)) {
                        for (String cmd : rewardCommands.get(rank)) {
                            if (plugin instanceof com.ardelys.ahowtofish.AHowToFishPlugin ahf && ahf.getSecurityManager() != null) {
                                ahf.getSecurityManager().executeConsoleReward(winnerPlayer, cmd);
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", winnerPlayer.getName()));
                            }
                        }
                    }
                }
            }
        }
        Bukkit.broadcast(TextUtil.toComponent("&8&m----------------------------------------"));

        activeCompetition = null;
    }

    private void stopTicker() {
        if (competitionTask != null) {
            competitionTask.cancel();
            competitionTask = null;
        }
    }

    public void onFishCatch(Player player, FishCatchResult result) {
        if (!isCompetitionActive()) return;

        double score = switch (activeCompetition.getType()) {
            case MOST_FISH -> 1.0;
            case LARGEST_FISH -> result.weight();
            case TOTAL_WEIGHT -> result.weight();
            case HIGHEST_VALUE -> result.finalPrice();
            case MOST_RARE -> {
                String r = result.rarity().id();
                yield (r.contains("RARE") || r.contains("LEGENDARY") || r.contains("MYTHIC") || r.contains("SECRET")) ? 1.0 : 0.0;
            }
        };

        if (score > 0) {
            activeCompetition.recordScore(player.getUniqueId(), player.getName(), score);
        }
    }

    public Map<Integer, Double> getRewardMoney() {
        return Collections.unmodifiableMap(rewardMoney);
    }

    public Map<Integer, List<String>> getRewardCommands() {
        return Collections.unmodifiableMap(rewardCommands);
    }
}
