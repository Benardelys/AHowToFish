package com.ardelys.ahowtofish.integration;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.competition.CompetitionManager;
import com.ardelys.ahowtofish.database.dao.ProfileDao;
import com.ardelys.ahowtofish.model.Competition;
import com.ardelys.ahowtofish.model.FishingZone;
import com.ardelys.ahowtofish.model.SpecialEvent;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.player.PlayerStats;
import com.ardelys.ahowtofish.player.ProfileManager;
import com.ardelys.ahowtofish.progression.ProgressionManager;
import com.ardelys.ahowtofish.utility.TextUtil;
import com.ardelys.ahowtofish.zone.ZoneManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderHook extends PlaceholderExpansion {
    private final AHowToFishPlugin plugin;
    private final ProfileManager profileManager;
    private final ProgressionManager progressionManager;
    private final ZoneManager zoneManager;
    private final CompetitionManager competitionManager;
    private final ProfileDao profileDao;

    private final Pattern TOP_PATTERN = Pattern.compile("top_([0-9]+)_(name|value)");
    private final Map<String, List<ProfileDao.LeaderboardEntry>> cachedLeaderboards = new ConcurrentHashMap<>();
    private final AtomicBoolean updatingLeaderboard = new AtomicBoolean(false);
    private long lastLeaderboardUpdate = 0;

    public PlaceholderHook(AHowToFishPlugin plugin,
                           ProfileManager profileManager,
                           ProgressionManager progressionManager,
                           ZoneManager zoneManager,
                           CompetitionManager competitionManager,
                           ProfileDao profileDao) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.progressionManager = progressionManager;
        this.zoneManager = zoneManager;
        this.competitionManager = competitionManager;
        this.profileDao = profileDao;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ahowtofish";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Ardelys";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) return "";

        FishingProfile profile = profileManager.getProfile(offlinePlayer.getUniqueId());
        if (profile == null) {
            return "";
        }

        Player onlinePlayer = offlinePlayer.getPlayer();
        PlayerStats stats = profile.getStats();

        switch (params.toLowerCase()) {
            case "level" -> { return String.valueOf(profile.getLevel()); }
            case "xp" -> { return TextUtil.formatInteger(profile.getXp()); }
            case "xp_required" -> {
                long req = progressionManager.getRequiredXpForLevel(profile.getLevel() + 1);
                return TextUtil.formatInteger(req);
            }
            case "xp_progress" -> {
                return TextUtil.formatDecimal(progressionManager.getLevelProgressPercent(profile)) + "%";
            }
            case "fish_caught" -> { return TextUtil.formatInteger(stats.getTotalFishCaught()); }
            case "total_weight" -> { return TextUtil.formatDecimal(stats.getTotalFishWeight()) + " kg"; }
            case "largest_fish" -> { return TextUtil.formatDecimal(stats.getLargestFishWeight()) + " kg"; }
            case "largest_fish_weight" -> { return TextUtil.formatDecimal(stats.getLargestFishWeight()); }
            case "money_earned" -> { return "$" + TextUtil.formatDecimal(stats.getTotalMoneyEarned()); }
            case "rare_fish" -> { return TextUtil.formatInteger(stats.getRareCaught()); }
            case "legendary_fish" -> { return TextUtil.formatInteger(stats.getLegendaryCaught()); }
            case "current_zone" -> {
                if (onlinePlayer != null) {
                    FishingZone zone = zoneManager.getZoneAt(onlinePlayer.getLocation());
                    return zone != null ? zone.displayName() : "Wilderness";
                }
                return "None";
            }
            case "fishing_streak" -> { return String.valueOf(stats.getCurrentStreak()); }
            case "best_streak" -> { return String.valueOf(stats.getBestStreak()); }
            case "competition_rank" -> {
                if (competitionManager != null && competitionManager.isCompetitionActive()) {
                    int rank = competitionManager.getActiveCompetition().getRank(offlinePlayer.getUniqueId());
                    return rank > 0 ? String.valueOf(rank) : "Unranked";
                }
                return "None";
            }
            case "coins" -> {
                if (onlinePlayer != null && plugin.getVaultHook() != null && plugin.getVaultHook().isAvailable()) {
                    return TextUtil.formatDecimal(plugin.getVaultHook().getBalance(onlinePlayer));
                }
                return TextUtil.formatDecimal(stats.getTotalMoneyEarned());
            }
            case "rod" -> {
                if (onlinePlayer != null) {
                    var rod = plugin.getRodManager().getRodFromItem(onlinePlayer.getInventory().getItemInMainHand());
                    return rod != null ? rod.displayName() : "Standard Rod";
                }
                return "Standard Rod";
            }
            case "bait" -> {
                if (onlinePlayer != null) {
                    var bait = plugin.getBaitManager().getActiveBait(onlinePlayer);
                    return bait != null ? bait.displayName() : "None";
                }
                return "None";
            }
            case "competition_name" -> {
                Competition comp = competitionManager != null ? competitionManager.getActiveCompetition() : null;
                return (comp != null && comp.isActive()) ? comp.getType().getDisplayName() : "None";
            }
            case "competition_time" -> {
                Competition comp = competitionManager != null ? competitionManager.getActiveCompetition() : null;
                return (comp != null && comp.isActive()) ? TextUtil.formatTime(comp.getRemainingSeconds()) : "00:00";
            }
            case "competition_score" -> {
                Competition comp = competitionManager != null ? competitionManager.getActiveCompetition() : null;
                return (comp != null && comp.isActive()) ? TextUtil.formatDecimal(comp.getScore(offlinePlayer.getUniqueId())) : "0";
            }
            case "competition_position" -> {
                Competition comp = competitionManager != null ? competitionManager.getActiveCompetition() : null;
                if (comp != null && comp.isActive()) {
                    int rank = comp.getRank(offlinePlayer.getUniqueId());
                    return rank > 0 ? String.valueOf(rank) : "-";
                }
                return "-";
            }
            case "competition_leader" -> {
                Competition comp = competitionManager != null ? competitionManager.getActiveCompetition() : null;
                return (comp != null && comp.isActive()) ? comp.getLeaderName() : "None";
            }
            case "competition_leader_score" -> {
                Competition comp = competitionManager != null ? competitionManager.getActiveCompetition() : null;
                return (comp != null && comp.isActive()) ? TextUtil.formatDecimal(comp.getLeaderScore()) : "0";
            }
            case "event_name" -> {
                SpecialEvent ev = plugin.getEventManager() != null ? plugin.getEventManager().getActiveEvent() : null;
                return ev != null ? ev.name() : "None";
            }
            case "event_time" -> {
                if (plugin.getEventManager() != null && plugin.getEventManager().isEventActive()) {
                    return TextUtil.formatTime(plugin.getEventManager().getRemainingSeconds());
                }
                return "00:00";
            }
            case "event_multiplier" -> {
                SpecialEvent ev = plugin.getEventManager() != null ? plugin.getEventManager().getActiveEvent() : null;
                return ev != null ? String.valueOf(ev.xpMultiplier()) : "1.0";
            }
            case "sellable_fish" -> {
                if (onlinePlayer != null && plugin.getFishSellService() != null) {
                    var summary = plugin.getFishSellService().getSellableFishSummary(onlinePlayer);
                    return String.valueOf(summary.totalCount());
                }
                return "0";
            }
            case "sellable_weight" -> {
                if (onlinePlayer != null && plugin.getFishSellService() != null) {
                    var summary = plugin.getFishSellService().getSellableFishSummary(onlinePlayer);
                    return TextUtil.formatDecimal(summary.totalWeight());
                }
                return "0.00";
            }
            case "sellable_value" -> {
                if (onlinePlayer != null && plugin.getFishSellService() != null) {
                    var summary = plugin.getFishSellService().getSellableFishSummary(onlinePlayer);
                    return TextUtil.formatDecimal(summary.totalValue());
                }
                return "0.00";
            }
            case "active_quest" -> {
                String qId = profile.getActiveQuestId();
                if (qId != null && plugin.getQuestManager() != null) {
                    var q = plugin.getQuestManager().getQuest(qId);
                    if (q != null) return q.name();
                }
                return "None";
            }
            case "quest_progress" -> {
                String qId = profile.getActiveQuestId();
                if (qId != null) {
                    var qp = profile.getPlayerQuestProgress(qId);
                    var q = plugin.getQuestManager() != null ? plugin.getQuestManager().getQuest(qId) : null;
                    if (qp != null && q != null) {
                        return String.valueOf((int) Math.min(qp.getProgress(), q.requiredAmount()));
                    }
                }
                return "0";
            }
            case "quest_required" -> {
                String qId = profile.getActiveQuestId();
                if (qId != null && plugin.getQuestManager() != null) {
                    var q = plugin.getQuestManager().getQuest(qId);
                    if (q != null) return String.valueOf((int) q.requiredAmount());
                }
                return "0";
            }
            case "quest_time" -> {
                String qId = profile.getActiveQuestId();
                if (qId != null) {
                    var qp = profile.getPlayerQuestProgress(qId);
                    if (qp != null && qp.getExpiresAt() > 0) {
                        return TextUtil.formatTime(qp.getRemainingSeconds());
                    }
                }
                return "--:--";
            }
            case "quest_status" -> {
                String qId = profile.getActiveQuestId();
                if (qId != null) {
                    var qp = profile.getPlayerQuestProgress(qId);
                    if (qp != null) return qp.getState().name();
                }
                return "NONE";
            }
            case "next_quest" -> {
                String qId = profile.getActiveQuestId();
                if (qId != null && plugin.getQuestManager() != null) {
                    var q = plugin.getQuestManager().getQuest(qId);
                    if (q != null && q.nextQuestId() != null) {
                        var nextQ = plugin.getQuestManager().getQuest(q.nextQuestId());
                        return nextQ != null ? nextQ.name() : q.nextQuestId();
                    }
                }
                return "None";
            }
            case "quests_completed" -> {
                return String.valueOf(profile.getCompletedQuests().size());
            }
        }

        Matcher matcher = TOP_PATTERN.matcher(params.toLowerCase());
        if (matcher.matches()) {
            int rank = Integer.parseInt(matcher.group(1));
            String type = matcher.group(2);

            refreshLeaderboardCache();
            List<ProfileDao.LeaderboardEntry> list = cachedLeaderboards.get("LEVEL");
            if (list != null && rank >= 1 && rank <= list.size()) {
                ProfileDao.LeaderboardEntry entry = list.get(rank - 1);
                if (type.equals("name")) return entry.name();
                if (type.equals("value")) return TextUtil.formatDecimal(entry.value());
            }
            return "-";
        }

        return null;
    }

    private void refreshLeaderboardCache() {
        if (System.currentTimeMillis() - lastLeaderboardUpdate > 30000) { 
            if (updatingLeaderboard.compareAndSet(false, true)) {
                lastLeaderboardUpdate = System.currentTimeMillis();
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        List<ProfileDao.LeaderboardEntry> entries = profileDao.getLeaderboard("LEVEL", 10);
                        if (entries != null) {
                            cachedLeaderboards.put("LEVEL", entries);
                        }
                    } finally {
                        updatingLeaderboard.set(false);
                    }
                });
            }
        }
    }
}