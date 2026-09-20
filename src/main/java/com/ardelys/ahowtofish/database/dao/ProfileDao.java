package com.ardelys.ahowtofish.database.dao;

import com.ardelys.ahowtofish.database.DatabaseManager;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.player.PlayerCollection;
import com.ardelys.ahowtofish.player.PlayerStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProfileDao {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public record LeaderboardEntry(UUID uuid, String name, double value) {}

    public ProfileDao(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    public FishingProfile loadProfile(UUID uuid, String playerName) {
        FishingProfile profile = new FishingProfile(uuid, playerName);

        try (Connection conn = databaseManager.getConnection()) {
            
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM ahf_profiles WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        profile.setPlayerName(rs.getString("player_name"));
                        profile.setLevel(rs.getInt("level"));
                        profile.setXp(rs.getLong("xp"));
                        profile.setLanguage(rs.getString("language"));
                        try {
                            profile.setStarterReceived(rs.getInt("starter_received") == 1);
                            profile.setActiveQuestId(rs.getString("active_quest"));
                        } catch (SQLException ignored) {}
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM ahf_stats WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        PlayerStats stats = profile.getStats();
                        stats.setTotalCasts(rs.getLong("casts"));
                        stats.setSuccessfulCatches(rs.getLong("successful"));
                        stats.setFailedCatches(rs.getLong("failed"));
                        stats.setTotalFishCaught(rs.getLong("total_fish"));
                        stats.setTotalXpEarned(rs.getLong("total_xp"));
                        stats.setLargestFishWeight(rs.getDouble("largest_weight"));
                        stats.setTotalFishWeight(rs.getDouble("total_weight"));
                        stats.setTotalMoneyEarned(rs.getDouble("total_money"));
                        stats.setCommonCaught(rs.getLong("common_caught"));
                        stats.setRareCaught(rs.getLong("rare_caught"));
                        stats.setLegendaryCaught(rs.getLong("legendary_caught"));
                        stats.setCurrentStreak(rs.getInt("current_streak"));
                        stats.setBestStreak(rs.getInt("best_streak"));
                        stats.setEventsWon(rs.getInt("events_won"));
                        stats.setCompetitionsWon(rs.getInt("competitions_won"));
                        stats.setFishingTimeSeconds(rs.getLong("fishing_time"));
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM ahf_collection WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        profile.getCollection().putRecord(new PlayerCollection.Record(
                                rs.getString("fish_id"),
                                rs.getLong("total_catches"),
                                rs.getDouble("largest_weight"),
                                rs.getDouble("total_weight"),
                                rs.getDouble("total_value"),
                                rs.getLong("first_catch")
                        ));
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM ahf_quests WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String qId = rs.getString("quest_id");
                        profile.setQuestProgress(qId, rs.getDouble("progress"));
                        if (rs.getInt("completed") == 1) {
                            profile.markQuestCompleted(qId);
                        }
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM ahf_player_quests WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String qId = rs.getString("quest_id");
                        String stateStr = rs.getString("state");
                        com.ardelys.ahowtofish.model.FishingQuest.QuestState state = com.ardelys.ahowtofish.model.FishingQuest.QuestState.LOCKED;
                        try {
                            if (stateStr != null) state = com.ardelys.ahowtofish.model.FishingQuest.QuestState.valueOf(stateStr);
                        } catch (Exception ignored) {}
                        double prog = rs.getDouble("progress");
                        long start = rs.getLong("started_at");
                        long exp = rs.getLong("expires_at");
                        long comp = rs.getLong("completed_at");
                        long cd = rs.getLong("cooldown_until");

                        com.ardelys.ahowtofish.quest.PlayerQuestProgress qp =
                                new com.ardelys.ahowtofish.quest.PlayerQuestProgress(qId, state, prog, start, exp, comp, cd);
                        profile.setPlayerQuestProgress(qId, qp);
                    }
                }
            } catch (SQLException ignored) {
                
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM ahf_achievements WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        profile.unlockAchievement(rs.getString("achievement_id"));
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM ahf_upgrades WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        profile.setUpgradeLevel(rs.getString("upgrade_id"), rs.getInt("level"));
                    }
                }
            }

            profile.clearDirty();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[AHowToFish] Failed to load fishing profile for " + playerName, e);
        }

        return profile;
    }

    public void saveProfile(FishingProfile profile) {
        if (profile == null) return;
        long startTime = System.currentTimeMillis();
        boolean isMySql = databaseManager.isMySql();

        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                
                String saveProfileQuery = isMySql ? """
                    INSERT INTO ahf_profiles (uuid, player_name, level, xp, language, starter_received, active_quest, last_seen)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        player_name = VALUES(player_name),
                        level = VALUES(level),
                        xp = VALUES(xp),
                        language = VALUES(language),
                        starter_received = VALUES(starter_received),
                        active_quest = VALUES(active_quest),
                        last_seen = VALUES(last_seen);
                """ : """
                    INSERT INTO ahf_profiles (uuid, player_name, level, xp, language, starter_received, active_quest, last_seen)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        player_name = excluded.player_name,
                        level = excluded.level,
                        xp = excluded.xp,
                        language = excluded.language,
                        starter_received = excluded.starter_received,
                        active_quest = excluded.active_quest,
                        last_seen = excluded.last_seen;
                """;

                try (PreparedStatement ps = conn.prepareStatement(saveProfileQuery)) {
                    ps.setString(1, profile.getUuid().toString());
                    ps.setString(2, profile.getPlayerName());
                    ps.setInt(3, profile.getLevel());
                    ps.setLong(4, profile.getXp());
                    ps.setString(5, profile.getLanguage());
                    ps.setInt(6, profile.isStarterReceived() ? 1 : 0);
                    ps.setString(7, profile.getActiveQuestId());
                    ps.setLong(8, System.currentTimeMillis());
                    ps.executeUpdate();
                }

                PlayerStats stats = profile.getStats();
                String saveStatsQuery = isMySql ? """
                    INSERT INTO ahf_stats (uuid, casts, successful, failed, total_fish, total_xp, largest_weight,
                        total_weight, total_money, common_caught, rare_caught, legendary_caught, current_streak,
                        best_streak, events_won, competitions_won, fishing_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        casts = VALUES(casts),
                        successful = VALUES(successful),
                        failed = VALUES(failed),
                        total_fish = VALUES(total_fish),
                        total_xp = VALUES(total_xp),
                        largest_weight = VALUES(largest_weight),
                        total_weight = VALUES(total_weight),
                        total_money = VALUES(total_money),
                        common_caught = VALUES(common_caught),
                        rare_caught = VALUES(rare_caught),
                        legendary_caught = VALUES(legendary_caught),
                        current_streak = VALUES(current_streak),
                        best_streak = VALUES(best_streak),
                        events_won = VALUES(events_won),
                        competitions_won = VALUES(competitions_won),
                        fishing_time = VALUES(fishing_time);
                """ : """
                    INSERT INTO ahf_stats (uuid, casts, successful, failed, total_fish, total_xp, largest_weight,
                        total_weight, total_money, common_caught, rare_caught, legendary_caught, current_streak,
                        best_streak, events_won, competitions_won, fishing_time)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        casts = excluded.casts,
                        successful = excluded.successful,
                        failed = excluded.failed,
                        total_fish = excluded.total_fish,
                        total_xp = excluded.total_xp,
                        largest_weight = excluded.largest_weight,
                        total_weight = excluded.total_weight,
                        total_money = excluded.total_money,
                        common_caught = excluded.common_caught,
                        rare_caught = excluded.rare_caught,
                        legendary_caught = excluded.legendary_caught,
                        current_streak = excluded.current_streak,
                        best_streak = excluded.best_streak,
                        events_won = excluded.events_won,
                        competitions_won = excluded.competitions_won,
                        fishing_time = excluded.fishing_time;
                """;

                try (PreparedStatement ps = conn.prepareStatement(saveStatsQuery)) {
                    bindStats(ps, profile.getUuid(), stats);
                    ps.executeUpdate();
                }

                var colRecords = profile.getCollection().getAllRecords().values();
                if (!colRecords.isEmpty()) {
                    String colQuery = isMySql ? """
                        INSERT INTO ahf_collection (uuid, fish_id, total_catches, largest_weight, total_weight, total_value, first_catch)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            total_catches = VALUES(total_catches),
                            largest_weight = VALUES(largest_weight),
                            total_weight = VALUES(total_weight),
                            total_value = VALUES(total_value);
                    """ : """
                        INSERT INTO ahf_collection (uuid, fish_id, total_catches, largest_weight, total_weight, total_value, first_catch)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(uuid, fish_id) DO UPDATE SET
                            total_catches = excluded.total_catches,
                            largest_weight = excluded.largest_weight,
                            total_weight = excluded.total_weight,
                            total_value = excluded.total_value;
                    """;
                    try (PreparedStatement ps = conn.prepareStatement(colQuery)) {
                        for (PlayerCollection.Record rec : colRecords) {
                            ps.setString(1, profile.getUuid().toString());
                            ps.setString(2, rec.fishId());
                            ps.setLong(3, rec.totalCatches());
                            ps.setDouble(4, rec.largestWeight());
                            ps.setDouble(5, rec.totalWeight());
                            ps.setDouble(6, rec.totalValue());
                            ps.setLong(7, rec.firstCatchTimestamp());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                var questEntries = profile.getQuestProgress().entrySet();
                if (!questEntries.isEmpty()) {
                    String qQuery = isMySql ? """
                        INSERT INTO ahf_quests (uuid, quest_id, progress, completed)
                        VALUES (?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            progress = VALUES(progress),
                            completed = VALUES(completed);
                    """ : """
                        INSERT INTO ahf_quests (uuid, quest_id, progress, completed)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT(uuid, quest_id) DO UPDATE SET
                            progress = excluded.progress,
                            completed = excluded.completed;
                    """;
                    try (PreparedStatement ps = conn.prepareStatement(qQuery)) {
                        for (Map.Entry<String, Double> entry : questEntries) {
                            ps.setString(1, profile.getUuid().toString());
                            ps.setString(2, entry.getKey());
                            ps.setDouble(3, entry.getValue());
                            ps.setInt(4, profile.isQuestCompleted(entry.getKey()) ? 1 : 0);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                var pQuests = profile.getPlayerQuests().values();
                if (!pQuests.isEmpty()) {
                    String pqQuery = isMySql ? """
                        INSERT INTO ahf_player_quests (uuid, quest_id, state, progress, started_at, expires_at, completed_at, cooldown_until)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            state = VALUES(state),
                            progress = VALUES(progress),
                            started_at = VALUES(started_at),
                            expires_at = VALUES(expires_at),
                            completed_at = VALUES(completed_at),
                            cooldown_until = VALUES(cooldown_until);
                    """ : """
                        INSERT INTO ahf_player_quests (uuid, quest_id, state, progress, started_at, expires_at, completed_at, cooldown_until)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(uuid, quest_id) DO UPDATE SET
                            state = excluded.state,
                            progress = excluded.progress,
                            started_at = excluded.started_at,
                            expires_at = excluded.expires_at,
                            completed_at = excluded.completed_at,
                            cooldown_until = excluded.cooldown_until;
                    """;
                    try (PreparedStatement ps = conn.prepareStatement(pqQuery)) {
                        for (com.ardelys.ahowtofish.quest.PlayerQuestProgress qp : pQuests) {
                            ps.setString(1, profile.getUuid().toString());
                            ps.setString(2, qp.getQuestId());
                            ps.setString(3, qp.getState().name());
                            ps.setDouble(4, qp.getProgress());
                            ps.setLong(5, qp.getStartedAt());
                            ps.setLong(6, qp.getExpiresAt());
                            ps.setLong(7, qp.getCompletedAt());
                            ps.setLong(8, qp.getCooldownUntil());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                var achievements = profile.getUnlockedAchievements();
                if (!achievements.isEmpty()) {
                    String achSql = isMySql ?
                            "INSERT IGNORE INTO ahf_achievements (uuid, achievement_id, unlocked_at) VALUES (?, ?, ?)" :
                            "INSERT OR IGNORE INTO ahf_achievements (uuid, achievement_id, unlocked_at) VALUES (?, ?, ?)";
                    long now = System.currentTimeMillis();
                    try (PreparedStatement ps = conn.prepareStatement(achSql)) {
                        for (String achId : achievements) {
                            ps.setString(1, profile.getUuid().toString());
                            ps.setString(2, achId);
                            ps.setLong(3, now);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                var upgrades = profile.getUpgrades().entrySet();
                if (!upgrades.isEmpty()) {
                    String upgQuery = isMySql ? """
                        INSERT INTO ahf_upgrades (uuid, upgrade_id, level)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            level = VALUES(level);
                    """ : """
                        INSERT INTO ahf_upgrades (uuid, upgrade_id, level)
                        VALUES (?, ?, ?)
                        ON CONFLICT(uuid, upgrade_id) DO UPDATE SET
                            level = excluded.level;
                    """;
                    try (PreparedStatement ps = conn.prepareStatement(upgQuery)) {
                        for (Map.Entry<String, Integer> entry : upgrades) {
                            ps.setString(1, profile.getUuid().toString());
                            ps.setString(2, entry.getKey());
                            ps.setInt(3, entry.getValue());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                conn.commit();
                profile.clearDirty();
                databaseManager.recordQueryTime(System.currentTimeMillis() - startTime);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[AHowToFish] Failed to save fishing profile for " + profile.getPlayerName(), e);
        }
    }

    private void bindStats(PreparedStatement ps, UUID uuid, PlayerStats stats) throws SQLException {
        ps.setString(1, uuid.toString());
        ps.setLong(2, stats.getTotalCasts());
        ps.setLong(3, stats.getSuccessfulCatches());
        ps.setLong(4, stats.getFailedCatches());
        ps.setLong(5, stats.getTotalFishCaught());
        ps.setLong(6, stats.getTotalXpEarned());
        ps.setDouble(7, stats.getLargestFishWeight());
        ps.setDouble(8, stats.getTotalFishWeight());
        ps.setDouble(9, stats.getTotalMoneyEarned());
        ps.setLong(10, stats.getCommonCaught());
        ps.setLong(11, stats.getRareCaught());
        ps.setLong(12, stats.getLegendaryCaught());
        ps.setInt(13, stats.getCurrentStreak());
        ps.setInt(14, stats.getBestStreak());
        ps.setInt(15, stats.getEventsWon());
        ps.setInt(16, stats.getCompetitionsWon());
        ps.setLong(17, stats.getFishingTimeSeconds());
    }

    public List<LeaderboardEntry> getLeaderboard(String category, int limit) {
        List<LeaderboardEntry> list = new ArrayList<>();
        String query;

        switch (category.toUpperCase()) {
            case "LEVEL" -> query = """
                SELECT p.uuid, p.player_name, p.level as score
                FROM ahf_profiles p
                ORDER BY p.level DESC, p.xp DESC
                LIMIT ?
            """;
            case "FISH_CAUGHT" -> query = """
                SELECT p.uuid, p.player_name, s.total_fish as score
                FROM ahf_stats s
                JOIN ahf_profiles p ON s.uuid = p.uuid
                ORDER BY s.total_fish DESC
                LIMIT ?
            """;
            case "LARGEST_FISH" -> query = """
                SELECT p.uuid, p.player_name, s.largest_weight as score
                FROM ahf_stats s
                JOIN ahf_profiles p ON s.uuid = p.uuid
                ORDER BY s.largest_weight DESC
                LIMIT ?
            """;
            case "TOTAL_WEIGHT" -> query = """
                SELECT p.uuid, p.player_name, s.total_weight as score
                FROM ahf_stats s
                JOIN ahf_profiles p ON s.uuid = p.uuid
                ORDER BY s.total_weight DESC
                LIMIT ?
            """;
            case "MONEY_EARNED" -> query = """
                SELECT p.uuid, p.player_name, s.total_money as score
                FROM ahf_stats s
                JOIN ahf_profiles p ON s.uuid = p.uuid
                ORDER BY s.total_money DESC
                LIMIT ?
            """;
            case "RARE_FISH" -> query = """
                SELECT p.uuid, p.player_name, s.rare_caught as score
                FROM ahf_stats s
                JOIN ahf_profiles p ON s.uuid = p.uuid
                ORDER BY s.rare_caught DESC
                LIMIT ?
            """;
            case "LEGENDARY_FISH" -> query = """
                SELECT p.uuid, p.player_name, s.legendary_caught as score
                FROM ahf_stats s
                JOIN ahf_profiles p ON s.uuid = p.uuid
                ORDER BY s.legendary_caught DESC
                LIMIT ?
            """;
            default -> query = """
                SELECT p.uuid, p.player_name, s.total_xp as score
                FROM ahf_stats s
                JOIN ahf_profiles p ON s.uuid = p.uuid
                ORDER BY s.total_xp DESC
                LIMIT ?
            """;
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, Math.max(1, Math.min(100, limit)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new LeaderboardEntry(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("player_name"),
                            rs.getDouble("score")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[AHowToFish] Error retrieving leaderboard for " + category, e);
        }

        return list;
    }
}
