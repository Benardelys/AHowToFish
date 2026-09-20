package com.ardelys.ahowtofish.scoreboard;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.competition.CompetitionManager;
import com.ardelys.ahowtofish.event.EventManager;
import com.ardelys.ahowtofish.model.Competition;
import com.ardelys.ahowtofish.model.FishingQuest;
import com.ardelys.ahowtofish.model.FishingZone;
import com.ardelys.ahowtofish.model.SpecialEvent;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.quest.PlayerQuestProgress;
import com.ardelys.ahowtofish.utility.TextUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {

    private final AHowToFishPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    private boolean enabled = true;
    private long updateInterval = 20L;

    private String normalTitle = "&6&l✦ AHowToFish ✦";
    private List<String> normalLines = new ArrayList<>();

    private String competitionTitle = "&6&l✦ TOURNAMENT ✦";
    private List<String> competitionLines = new ArrayList<>();

    private String eventTitle = "&d&l✦ SPECIAL EVENT ✦";
    private List<String> eventLines = new ArrayList<>();

    private String questTitle = "&6&l✦ QUEST ACTIVE ✦";
    private List<String> questLines = new ArrayList<>();

    private final Map<UUID, PlayerBoard> boards = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    private static final String[] COLOR_ENTRIES = new String[16];

    static {
        char[] codes = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        for (int i = 0; i < 16; i++) {
            COLOR_ENTRIES[i] = "§" + codes[i] + "§r";
        }
    }

    public ScoreboardManager(AHowToFishPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        this.configFile = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!configFile.exists()) {
            plugin.saveResource("scoreboard.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        this.enabled = config.getBoolean("scoreboard.enabled", true);
        this.updateInterval = Math.max(5L, config.getLong("scoreboard.update-interval", 20L));

        this.normalTitle = config.getString("scoreboard.normal.title", "&6&l✦ AHowToFish ✦");
        this.normalLines = config.getStringList("scoreboard.normal.lines");

        this.competitionTitle = config.getString("scoreboard.competition.title", "&6&l✦ TOURNAMENT ✦");
        this.competitionLines = config.getStringList("scoreboard.competition.lines");

        this.eventTitle = config.getString("scoreboard.event.title", "&d&l✦ SPECIAL EVENT ✦");
        this.eventLines = config.getStringList("scoreboard.event.lines");

        this.questTitle = config.getString("scoreboard.quest.title", "&6&l✦ QUEST ACTIVE ✦");
        this.questLines = config.getStringList("scoreboard.quest.lines");

        if (enabled) {
            startTask();
        } else {
            cleanupAllBoards();
        }
    }

    private void startTask() {
        this.updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayer(player);
            }
        }, 10L, updateInterval);
    }

    public void updatePlayer(Player player) {
        if (!enabled || player == null || !player.isOnline()) return;

        PlayerBoard board = boards.computeIfAbsent(player.getUniqueId(), u -> new PlayerBoard(player));

        String title;
        List<String> rawLines;

        CompetitionManager cm = plugin.getCompetitionManager();
        EventManager em = plugin.getEventManager();

        if (cm != null && cm.isCompetitionActive()) {
            title = competitionTitle;
            rawLines = competitionLines;
        } else if (em != null && em.isEventActive()) {
            title = eventTitle;
            rawLines = eventLines;
        } else if (hasActiveQuest(player)) {
            title = questTitle;
            rawLines = (questLines != null && !questLines.isEmpty()) ? questLines : normalLines;
        } else {
            title = normalTitle;
            rawLines = normalLines;
        }

        String formattedTitle = resolvePlaceholders(player, title);
        List<String> formattedLines = new ArrayList<>();
        for (String raw : rawLines) {
            formattedLines.add(resolvePlaceholders(player, raw));
        }

        board.update(formattedTitle, formattedLines);
    }

    public void remove(UUID uuid) {
        PlayerBoard board = boards.remove(uuid);
        if (board != null) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                org.bukkit.scoreboard.ScoreboardManager sm = Bukkit.getScoreboardManager();
                if (sm != null) {
                    p.setScoreboard(sm.getMainScoreboard());
                }
            }
        }
    }

    public void cleanupAllBoards() {
        for (UUID uuid : new ArrayList<>(boards.keySet())) {
            remove(uuid);
        }
        boards.clear();
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getUpdateInterval() {
        return updateInterval;
    }

    private String resolvePlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) return "";

        String noneStr = plugin.getLanguageManager().getMessage(player, "scoreboard_labels.none", "None");
        String unrankedStr = plugin.getLanguageManager().getMessage(player, "scoreboard_labels.unranked", "Unranked");
        String wildWaters = plugin.getLanguageManager().getMessage(player, "scoreboard_labels.wild_waters", "Wild Waters");
        String stdRod = plugin.getLanguageManager().getMessage(player, "command.status.standard_rod", "Standard Rod");

        if (text.contains("AHowToFish")) {
            text = text.replace("&6&l✦ AHowToFish ✦", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.title_normal", "&6&l✦ AHowToFish ✦"));
        }
        if (text.contains("TOURNAMENT") || text.contains("TURNUVA")) {
            text = plugin.getLanguageManager().getMessage(player, "scoreboard_labels.title_tournament", "&6&l✦ TOURNAMENT ✦");
        }
        if (text.contains("SPECIAL EVENT") || text.contains("ÖZEL ETKİNLİK")) {
            text = plugin.getLanguageManager().getMessage(player, "scoreboard_labels.title_event", "&d&l✦ SPECIAL EVENT ✦");
        }
        if (text.contains("QUEST ACTIVE") || text.contains("GÖREV AKTİF")) {
            text = plugin.getLanguageManager().getMessage(player, "scoreboard_labels.title_quest", "&6&l✦ QUEST ACTIVE ✦");
        }

        text = text.replace("&e⚓ Level:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.level", "&e⚓ Level:"))
                   .replace("&e✦ XP:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.xp", "&e✦ XP:"))
                   .replace("&bFish Caught:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.fish_caught", "&bFish Caught:"))
                   .replace("&bLargest Fish:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.largest_fish", "&bLargest Fish:"))
                   .replace("&6Coins:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.coins", "&6Coins:"))
                   .replace("&dZone:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.zone", "&dZone:"))
                   .replace("&dActive Rod:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.rod", "&dActive Rod:"))
                   .replace("&eTournament:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.tournament", "&eTournament:"))
                   .replace("&eTime Left:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.time_left", "&eTime Left:"))
                   .replace("&cTime Left:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.time_left", "&cTime Left:"))
                   .replace("&bYour Score:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.your_score", "&bYour Score:"))
                   .replace("&bYour Rank:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.your_rank", "&bYour Rank:"))
                   .replace("&6Leader:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.leader", "&6Leader:"))
                   .replace("&6Top Score:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.top_score", "&6Top Score:"))
                   .replace("&aKeep fishing to win!", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.win_hint", "&aKeep fishing to win!"))
                   .replace("&dEvent:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.event", "&dEvent:"))
                   .replace("&aBonus Mul:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.bonus_mul", "&aBonus Mul:"))
                   .replace("&eQuest:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.quest", "&eQuest:"))
                   .replace("&bProgress:", plugin.getLanguageManager().getMessage(player, "scoreboard_labels.progress", "&bProgress:"));

        FishingProfile profile = plugin.getProfileManager().getProfile(player);
        int level = profile != null ? profile.getLevel() : 1;
        long xp = profile != null ? profile.getXp() : 0;
        long reqXp = plugin.getProgressionManager().getRequiredXpForLevel(level + 1);

        long fishCaught = profile != null ? profile.getStats().getTotalFishCaught() : 0;
        double largestFish = profile != null ? profile.getStats().getLargestFishWeight() : 0.0;

        double coins = 0.0;
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isAvailable()) {
            coins = plugin.getVaultHook().getBalance(player);
        } else if (profile != null) {
            coins = profile.getStats().getTotalMoneyEarned();
        }

        FishingZone zone = plugin.getZoneManager().getZoneAt(player.getLocation());
        String zoneName = zone != null ? zone.displayName() : wildWaters;

        var rod = plugin.getRodManager().getRodFromItem(player.getInventory().getItemInMainHand());
        String rodName = rod != null ? rod.displayName() : stdRod;

        var bait = plugin.getBaitManager().getActiveBait(player);
        String baitName = bait != null ? bait.displayName() : noneStr;

        String res = text
                .replace("%ahowtofish_level%", String.valueOf(level))
                .replace("%ahowtofish_xp%", TextUtil.formatInteger(xp))
                .replace("%ahowtofish_required_xp%", TextUtil.formatInteger(reqXp))
                .replace("%ahowtofish_fish_caught%", TextUtil.formatInteger(fishCaught))
                .replace("%ahowtofish_largest_fish%", TextUtil.formatDecimal(largestFish) + " kg")
                .replace("%ahowtofish_coins%", TextUtil.formatDecimal(coins))
                .replace("%ahowtofish_zone%", zoneName)
                .replace("%ahowtofish_rod%", rodName)
                .replace("%ahowtofish_bait%", baitName);

        Competition comp = plugin.getCompetitionManager() != null ? plugin.getCompetitionManager().getActiveCompetition() : null;
        if (comp != null && comp.isActive()) {
            int rank = comp.getRank(player.getUniqueId());
            res = res
                    .replace("%ahowtofish_competition_name%", comp.getType().getDisplayName())
                    .replace("%ahowtofish_competition_time%", TextUtil.formatTime(comp.getRemainingSeconds()))
                    .replace("%ahowtofish_competition_score%", String.valueOf(comp.getScore(player.getUniqueId())))
                    .replace("%ahowtofish_competition_position%", rank > 0 ? String.valueOf(rank) : unrankedStr)
                    .replace("%ahowtofish_competition_leader%", comp.getLeaderName())
                    .replace("%ahowtofish_competition_leader_score%", String.valueOf(comp.getLeaderScore()));
        } else {
            res = res
                    .replace("%ahowtofish_competition_name%", noneStr)
                    .replace("%ahowtofish_competition_time%", "00:00")
                    .replace("%ahowtofish_competition_score%", "0")
                    .replace("%ahowtofish_competition_position%", "-")
                    .replace("%ahowtofish_competition_leader%", noneStr)
                    .replace("%ahowtofish_competition_leader_score%", "0");
        }

        SpecialEvent ev = plugin.getEventManager() != null ? plugin.getEventManager().getActiveEvent() : null;
        if (ev != null) {
            int remSec = plugin.getEventManager().getRemainingSeconds();
            res = res
                    .replace("%ahowtofish_event_name%", ev.name())
                    .replace("%ahowtofish_event_time%", TextUtil.formatTime(remSec))
                    .replace("%ahowtofish_event_multiplier%", String.valueOf(ev.xpMultiplier()));
        } else {
            res = res
                    .replace("%ahowtofish_event_name%", noneStr)
                    .replace("%ahowtofish_event_time%", "00:00")
                    .replace("%ahowtofish_event_multiplier%", "1.0");
        }

        String activeQId = profile != null ? profile.getActiveQuestId() : null;
        FishingQuest activeQ = (activeQId != null && plugin.getQuestManager() != null) ? plugin.getQuestManager().getQuest(activeQId) : null;
        com.ardelys.ahowtofish.quest.PlayerQuestProgress qp = (profile != null && activeQ != null) ? profile.getPlayerQuestProgress(activeQ.id()) : null;

        if (activeQ != null && qp != null) {
            int cur = (int) Math.min(qp.getProgress(), activeQ.requiredAmount());
            int req = (int) activeQ.requiredAmount();
            String qName = plugin.getQuestManager() != null ? plugin.getQuestManager().getLocalizedQuestName(player, activeQ) : activeQ.name();
            res = res
                    .replace("%ahowtofish_active_quest%", qName)
                    .replace("%ahowtofish_quest_progress%", String.valueOf(cur))
                    .replace("%ahowtofish_quest_required%", String.valueOf(req))
                    .replace("%ahowtofish_quest_time%", qp.getExpiresAt() > 0 ? TextUtil.formatTime(qp.getRemainingSeconds()) : "--:--")
                    .replace("%ahowtofish_quest_status%", qp.getState().name());
        } else {
            res = res
                    .replace("%ahowtofish_active_quest%", noneStr)
                    .replace("%ahowtofish_quest_progress%", "0")
                    .replace("%ahowtofish_quest_required%", "0")
                    .replace("%ahowtofish_quest_time%", "00:00")
                    .replace("%ahowtofish_quest_status%", "NONE");
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            res = PlaceholderAPI.setPlaceholders(player, res);
        }

        return TextUtil.colorize(res);
    }

    private boolean hasActiveQuest(Player player) {
        if (player == null) return false;
        FishingProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null) return false;
        String qId = profile.getActiveQuestId();
        if (qId == null) return false;
        com.ardelys.ahowtofish.quest.PlayerQuestProgress qp = profile.getPlayerQuestProgress(qId);
        return qp != null && qp.getState() == com.ardelys.ahowtofish.model.FishingQuest.QuestState.ACTIVE;
    }

    private static class PlayerBoard {
        private final Scoreboard scoreboard;
        private final Objective objective;
        private final List<Team> lines = new ArrayList<>();
        private String lastTitle = "";
        private final String[] lastLines = new String[15];
        private int lastTotal = -1;

        public PlayerBoard(Player player) {
            org.bukkit.scoreboard.ScoreboardManager sm = Bukkit.getScoreboardManager();
            this.scoreboard = sm != null ? sm.getNewScoreboard() : Bukkit.getServer().getScoreboardManager().getNewScoreboard();

            Objective obj = scoreboard.getObjective("ahowtofish");
            if (obj != null) {
                obj.unregister();
            }
            this.objective = scoreboard.registerNewObjective("ahowtofish", Criteria.DUMMY, Component.text("AHowToFish"));
            this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            for (int i = 0; i < 15; i++) {
                Team team = scoreboard.registerNewTeam("line_" + i);
                String entry = COLOR_ENTRIES[i];
                team.addEntry(entry);
                lines.add(team);
                lastLines[i] = "";
            }

            player.setScoreboard(this.scoreboard);
        }

        public void update(String title, List<String> newLines) {
            if (!Objects.equals(title, lastTitle)) {
                objective.displayName(TextUtil.toComponent(title));
                this.lastTitle = title;
            }

            int total = Math.min(15, newLines.size());

            if (lastTotal > total) {
                for (int i = total; i < lastTotal; i++) {
                    String entry = COLOR_ENTRIES[i];
                    scoreboard.resetScores(entry);
                    lastLines[i] = "";
                }
            }

            for (int i = 0; i < total; i++) {
                String line = newLines.get(i);
                String oldLine = lastLines[i];
                int targetScore = total - i;

                if (!Objects.equals(line, oldLine)) {
                    Team team = lines.get(i);
                    team.prefix(TextUtil.toComponent(line));
                    lastLines[i] = line;
                }

                if (lastTotal != total) {
                    String entry = COLOR_ENTRIES[i];
                    objective.getScore(entry).setScore(targetScore);
                }
            }

            this.lastTotal = total;
        }
    }
}