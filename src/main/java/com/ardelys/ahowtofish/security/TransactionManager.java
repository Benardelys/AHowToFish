package com.ardelys.ahowtofish.security;

import com.ardelys.ahowtofish.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class TransactionManager {
    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final SecurityManager securityManager;
    private static final int MAX_CACHE_SIZE = 5000;
    private final Map<String, Boolean> recentTransactions = Collections.synchronizedMap(
            new LinkedHashMap<>(MAX_CACHE_SIZE + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED
    }

    public TransactionManager(JavaPlugin plugin, DatabaseManager databaseManager, SecurityManager securityManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.securityManager = securityManager;
        initTable();
    }

    private void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS ahf_transactions (
                tx_id VARCHAR(64) PRIMARY KEY,
                player_uuid VARCHAR(36) NOT NULL,
                status VARCHAR(16) NOT NULL,
                details TEXT,
                created_at BIGINT NOT NULL
            );
        """;
        try (Connection conn = databaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[AHowToFish-Security] Could not initialize ahf_transactions table!", e);
        }
    }

    public String generateTransactionId(UUID playerUuid) {
        long timestamp = System.currentTimeMillis();
        String randomHex = UUID.randomUUID().toString().substring(0, 8);
        return String.format("FISH-%s-%d-%s", playerUuid.toString().substring(0, 8), timestamp, randomHex);
    }

    public boolean isTransactionProcessed(String txId) {
        if (txId == null) return false;
        Boolean cached = recentTransactions.get(txId);
        return cached != null && cached;
    }

    public int getRecentTransactionCount() {
        return recentTransactions.size();
    }

    public void cleanupOldTransactions(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM ahf_transactions WHERE created_at < ?")) {
                ps.setLong(1, cutoff);
                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    plugin.getLogger().fine("[AHowToFish] Purged " + deleted + " old transaction records.");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[AHowToFish] Error during transaction table cleanup", e);
            }
        });
    }

    public boolean recordTransaction(String txId, UUID playerUuid, TransactionStatus status, String details) {
        if (txId == null) return false;

        recentTransactions.put(txId, status == TransactionStatus.COMPLETED);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String insertSqlite = """
                INSERT INTO ahf_transactions (tx_id, player_uuid, status, details, created_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(tx_id) DO UPDATE SET status = excluded.status;
            """;
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSqlite)) {
                ps.setString(1, txId);
                ps.setString(2, playerUuid.toString());
                ps.setString(3, status.name());
                ps.setString(4, details != null ? details : "");
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException ex) {
                String insertMysql = """
                    INSERT INTO ahf_transactions (tx_id, player_uuid, status, details, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE status = VALUES(status);
                """;
                try (Connection conn2 = databaseManager.getConnection();
                     PreparedStatement ps2 = conn2.prepareStatement(insertMysql)) {
                    ps2.setString(1, txId);
                    ps2.setString(2, playerUuid.toString());
                    ps2.setString(3, status.name());
                    ps2.setString(4, details != null ? details : "");
                    ps2.setLong(5, System.currentTimeMillis());
                    ps2.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "[AHowToFish-Security] Failed to persist transaction " + txId, e);
                }
            }
        });

        return true;
    }
}