package com.ardelys.ahowtofish.performance;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public class PerformanceManager {

    private final AHowToFishPlugin plugin;

    private boolean enabled = true;
    private int cleanupIntervalMinutes = 10;
    private long transactionMaxAgeHours = 168; 
    private long rateLimiterStaleMinutes = 30;

    private BukkitTask cleanupTask = null;

    public PerformanceManager(AHowToFishPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        stop();

        FileConfiguration config = plugin.getConfig();
        if (config != null) {
            this.enabled = config.getBoolean("performance.enabled", true);
            this.cleanupIntervalMinutes = Math.max(1, config.getInt("performance.cleanup-interval-minutes", 10));
            this.transactionMaxAgeHours = Math.max(1, config.getLong("performance.transaction-max-age-hours", 168));
            this.rateLimiterStaleMinutes = Math.max(1, config.getLong("performance.rate-limiter-stale-minutes", 30));
        }

        if (enabled) {
            startCleanupTask();
        }
    }

    private void startCleanupTask() {
        long ticks = cleanupIntervalMinutes * 60L * 20L;
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                performMaintenance();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[AHowToFish-Performance] Error during periodic maintenance: " + e.getMessage());
            }
        }, ticks, ticks);
    }

    public void performMaintenance() {
        
        if (plugin.getGuiSessionManager() != null) {
            plugin.getGuiSessionManager().cleanExpiredSessions();
        }

        if (plugin.getFishingRateLimiter() != null) {
            plugin.getFishingRateLimiter().cleanupStaleEntries(rateLimiterStaleMinutes * 60L * 1000L);
        }

        if (plugin.getSecurityManager() != null && plugin.getSecurityManager().getRateLimiter() != null) {
            plugin.getSecurityManager().getRateLimiter().cleanupStaleEntries(rateLimiterStaleMinutes * 60L * 1000L);
        }

        if (plugin.getTransactionManager() != null) {
            plugin.getTransactionManager().cleanupOldTransactions(transactionMaxAgeHours * 3600L * 1000L);
        }

        TextUtil.clearComponentCache();
    }

    public void stop() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    public void sendDiagnostics(CommandSender sender) {
        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory() / (1024 * 1024);
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long freeMem = runtime.freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;

        long totalQueries = plugin.getDatabaseManager() != null ? plugin.getDatabaseManager().getTotalQueries() : 0;
        double avgLatency = plugin.getDatabaseManager() != null ? plugin.getDatabaseManager().getAverageLatencyMs() : 0.0;
        int dirtyProfiles = plugin.getProfileManager() != null ? plugin.getProfileManager().getDirtyProfileCount() : 0;
        int cachedTransactions = plugin.getTransactionManager() != null ? plugin.getTransactionManager().getRecentTransactionCount() : 0;
        int trackedPlayersRateLimit = plugin.getFishingRateLimiter() != null ? plugin.getFishingRateLimiter().getTrackedPlayerCount() : 0;
        int textCacheSize = TextUtil.getComponentCacheSize();

        sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
        sender.sendMessage(TextUtil.colorize("&6&l✦ AHowToFish Performance Diagnostics ✦"));
        sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
        sender.sendMessage(TextUtil.colorize("&7JVM Memory: &e" + usedMem + "MB &7used / &e" + totalMem + "MB &7allocated (&e" + maxMem + "MB &7max)"));
        sender.sendMessage(TextUtil.colorize("&7Database Queries: &e" + totalQueries + " &7total | Avg Latency: &e" + String.format(java.util.Locale.US, "%.2f", avgLatency) + "ms"));
        sender.sendMessage(TextUtil.colorize("&7Dirty Profiles Queued: &e" + dirtyProfiles));
        sender.sendMessage(TextUtil.colorize("&7Recent Transactions Cached: &e" + cachedTransactions));
        sender.sendMessage(TextUtil.colorize("&7Rate Limiter Tracked: &e" + trackedPlayersRateLimit));
        sender.sendMessage(TextUtil.colorize("&7Text Component Cache: &e" + textCacheSize + "/512 entries"));
        sender.sendMessage(TextUtil.colorize("&7Periodic Maintenance: &aEvery " + cleanupIntervalMinutes + "m &8(Active)"));
        sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
    }
}
