package com.ardelys.ahowtofish.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private DatabaseType databaseType;
    private HikariDataSource hikariDataSource;
    private File sqliteFile;

    private final java.util.concurrent.atomic.AtomicLong totalQueries = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalQueryTimeMs = new java.util.concurrent.atomic.AtomicLong(0);

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isMySql() {
        return databaseType == DatabaseType.MYSQL || databaseType == DatabaseType.MARIADB;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public void recordQueryTime(long ms) {
        totalQueries.incrementAndGet();
        totalQueryTimeMs.addAndGet(ms);
    }

    public long getTotalQueries() {
        return totalQueries.get();
    }

    public double getAverageLatencyMs() {
        long count = totalQueries.get();
        return count > 0 ? (double) totalQueryTimeMs.get() / count : 0.0;
    }

    public void init(FileConfiguration config) {
        String typeStr = config.getString("database.type", "SQLITE");
        this.databaseType = DatabaseType.fromString(typeStr);

        if (databaseType == DatabaseType.SQLITE) {
            initSQLite(config);
        } else {
            initHikari(config);
        }

        createTables();
    }

    private void initSQLite(FileConfiguration config) {
        try {
            Class.forName("org.sqlite.JDBC");
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            String fileName = config.getString("database.sqlite.file", "data.db");
            this.sqliteFile = new File(dataFolder, fileName);

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
            hikariConfig.setMaximumPoolSize(1); 
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
            hikariConfig.setPoolName("AHowToFish-SQLite-Pool");
            hikariConfig.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL; PRAGMA busy_timeout=5000;");

            this.hikariDataSource = new HikariDataSource(hikariConfig);
            plugin.getLogger().info("[AHowToFish] SQLite pooled database initialized at " + sqliteFile.getName() + " (WAL mode enabled)");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[AHowToFish] SQLite pool initialization failed!", e);
        }
    }

    private void initHikari(FileConfiguration config) {
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "ahowtofish");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "");
        boolean useSsl = config.getBoolean("database.mysql.useSSL", false);
        int poolSize = config.getInt("database.mysql.pool-size", 10);

        HikariConfig hikariConfig = new HikariConfig();
        String jdbcUrl = String.format("jdbc:%s://%s:%d/%s?useSSL=%b&characterEncoding=utf8",
                databaseType == DatabaseType.MARIADB ? "mariadb" : "mysql",
                host, port, database, useSsl);

        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setMinimumIdle(Math.max(2, poolSize / 2));
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setPoolName("AHowToFish-Pool");

        this.hikariDataSource = new HikariDataSource(hikariConfig);
        plugin.getLogger().info("[AHowToFish] Connected to " + databaseType + " database successfully.");
    }

    public Connection getConnection() throws SQLException {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            return hikariDataSource.getConnection();
        }
        throw new SQLException("Database connection pool is not initialized or has been closed!");
    }

    private void createTables() {
        String createProfiles = """
            CREATE TABLE IF NOT EXISTS ahf_profiles (
                uuid VARCHAR(36) PRIMARY KEY,
                player_name VARCHAR(32) NOT NULL,
                level INT DEFAULT 1,
                xp BIGINT DEFAULT 0,
                language VARCHAR(8) DEFAULT NULL,
                starter_received INT DEFAULT 0,
                active_quest VARCHAR(64) DEFAULT NULL,
                last_seen BIGINT DEFAULT 0
            );
        """;

        String createStats = """
            CREATE TABLE IF NOT EXISTS ahf_stats (
                uuid VARCHAR(36) PRIMARY KEY,
                casts BIGINT DEFAULT 0,
                successful BIGINT DEFAULT 0,
                failed BIGINT DEFAULT 0,
                total_fish BIGINT DEFAULT 0,
                total_xp BIGINT DEFAULT 0,
                largest_weight DOUBLE DEFAULT 0.0,
                total_weight DOUBLE DEFAULT 0.0,
                total_money DOUBLE DEFAULT 0.0,
                common_caught BIGINT DEFAULT 0,
                rare_caught BIGINT DEFAULT 0,
                legendary_caught BIGINT DEFAULT 0,
                current_streak INT DEFAULT 0,
                best_streak INT DEFAULT 0,
                events_won INT DEFAULT 0,
                competitions_won INT DEFAULT 0,
                fishing_time BIGINT DEFAULT 0
            );
        """;

        String createCollection = """
            CREATE TABLE IF NOT EXISTS ahf_collection (
                uuid VARCHAR(36),
                fish_id VARCHAR(64),
                total_catches BIGINT DEFAULT 0,
                largest_weight DOUBLE DEFAULT 0.0,
                total_weight DOUBLE DEFAULT 0.0,
                total_value DOUBLE DEFAULT 0.0,
                first_catch BIGINT DEFAULT 0,
                PRIMARY KEY (uuid, fish_id)
            );
        """;

        String createQuests = """
            CREATE TABLE IF NOT EXISTS ahf_quests (
                uuid VARCHAR(36),
                quest_id VARCHAR(64),
                progress DOUBLE DEFAULT 0.0,
                completed INT DEFAULT 0,
                PRIMARY KEY (uuid, quest_id)
            );
        """;

        String createPlayerQuests = """
            CREATE TABLE IF NOT EXISTS ahf_player_quests (
                uuid VARCHAR(36),
                quest_id VARCHAR(64),
                state VARCHAR(16) DEFAULT 'LOCKED',
                progress DOUBLE DEFAULT 0.0,
                started_at BIGINT DEFAULT 0,
                expires_at BIGINT DEFAULT 0,
                completed_at BIGINT DEFAULT 0,
                cooldown_until BIGINT DEFAULT 0,
                PRIMARY KEY (uuid, quest_id)
            );
        """;

        String createAchievements = """
            CREATE TABLE IF NOT EXISTS ahf_achievements (
                uuid VARCHAR(36),
                achievement_id VARCHAR(64),
                unlocked_at BIGINT DEFAULT 0,
                PRIMARY KEY (uuid, achievement_id)
            );
        """;

        String createUpgrades = """
            CREATE TABLE IF NOT EXISTS ahf_upgrades (
                uuid VARCHAR(36),
                upgrade_id VARCHAR(64),
                level INT DEFAULT 0,
                PRIMARY KEY (uuid, upgrade_id)
            );
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createProfiles);
            stmt.execute(createStats);
            stmt.execute(createCollection);
            stmt.execute(createQuests);
            stmt.execute(createPlayerQuests);
            stmt.execute(createAchievements);
            stmt.execute(createUpgrades);

            try { stmt.execute("ALTER TABLE ahf_profiles ADD COLUMN starter_received INT DEFAULT 0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE ahf_profiles ADD COLUMN active_quest VARCHAR(64) DEFAULT NULL"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[AHowToFish] Error initializing database tables!", e);
        }
    }

    public void close() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close();
        }
    }
}
