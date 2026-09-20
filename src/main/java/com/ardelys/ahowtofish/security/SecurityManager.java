package com.ardelys.ahowtofish.security;

import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class SecurityManager {
    private final JavaPlugin plugin;
    private boolean enabled = true;
    private boolean productionMode = true;
    private boolean transactionProtection = true;
    private boolean duplicateRewardProtection = true;
    private boolean inventoryProtection = true;
    private boolean itemValidation = true;
    private boolean guiSessionValidation = true;
    private boolean commandRateLimit = true;
    private boolean sellRateLimit = true;
    private boolean fishingRateLimit = true;
    private boolean suspiciousActionLogging = true;
    private boolean databaseIntegrityCheck = true;

    private long guiSessionTimeoutMs = 120_000L;
    private final SecurityRateLimiter rateLimiter = new SecurityRateLimiter();
    private final Map<SecurityAlertLevel, SecurityActionResponse> actionResponses = new EnumMap<>(SecurityAlertLevel.class);

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public record SecurityLogEntry(long timestamp, SecurityAlertLevel level, String component, String message, UUID playerUuid) {}
    private final Deque<SecurityLogEntry> alertHistory = new ConcurrentLinkedDeque<>();
    private static final int MAX_ALERT_HISTORY = 100;

    private final ExecutorService logExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AHowToFish-SecurityLogger");
        t.setDaemon(true);
        return t;
    });

    public SecurityManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initDefaultActionResponses();
    }

    private void initDefaultActionResponses() {
        actionResponses.put(SecurityAlertLevel.CRITICAL, SecurityActionResponse.LOG_BLOCK_AND_ALERT);
        actionResponses.put(SecurityAlertLevel.HIGH, SecurityActionResponse.LOG_BLOCK_AND_ALERT);
        actionResponses.put(SecurityAlertLevel.WARNING, SecurityActionResponse.LOG_AND_BLOCK);
        actionResponses.put(SecurityAlertLevel.MEDIUM, SecurityActionResponse.LOG_AND_BLOCK);
        actionResponses.put(SecurityAlertLevel.LOW, SecurityActionResponse.LOG);
        actionResponses.put(SecurityAlertLevel.INFO, SecurityActionResponse.LOG);
    }

    public void load(FileConfiguration config) {
        if (config == null) return;
        this.enabled = config.getBoolean("security.enabled", true);
        this.productionMode = config.getBoolean("environment.production", true);
        this.transactionProtection = config.getBoolean("security.transaction-protection", true);
        this.duplicateRewardProtection = config.getBoolean("security.duplicate-reward-protection", true);
        this.inventoryProtection = config.getBoolean("security.inventory-protection", true);
        this.itemValidation = config.getBoolean("security.item-validation", true);
        this.guiSessionValidation = config.getBoolean("security.gui-session-validation", true);
        this.commandRateLimit = config.getBoolean("security.command-rate-limit", true);
        this.sellRateLimit = config.getBoolean("security.sell-rate-limit", true);
        this.fishingRateLimit = config.getBoolean("security.fishing-rate-limit", true);
        this.suspiciousActionLogging = config.getBoolean("security.suspicious-action-logging", true);
        this.databaseIntegrityCheck = config.getBoolean("security.database-integrity-check", true);

        long timeoutSec = config.getLong("security.gui.session-timeout", 120L);
        this.guiSessionTimeoutMs = Math.max(10L, timeoutSec) * 1000L;

        ConfigurationSection rlSec = config.getConfigurationSection("security.rate-limits");
        if (rlSec != null) {
            for (RateLimitCategory cat : RateLimitCategory.values()) {
                String key = cat.name().toLowerCase(Locale.ROOT).replace('_', '-');
                if (rlSec.contains(key)) {
                    rateLimiter.setLimit(cat, rlSec.getInt(key));
                }
            }
        }

        ConfigurationSection actSec = config.getConfigurationSection("security.actions");
        if (actSec != null) {
            for (SecurityAlertLevel lvl : SecurityAlertLevel.values()) {
                String actStr = actSec.getString(lvl.name());
                if (actStr != null) {
                    try {
                        actionResponses.put(lvl, SecurityActionResponse.valueOf(actStr.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    public SecurityActionResponse getActionResponse(SecurityAlertLevel level) {
        if (level == null) return SecurityActionResponse.LOG;
        return actionResponses.getOrDefault(level, switch (level) {
            case CRITICAL, HIGH -> SecurityActionResponse.LOG_BLOCK_AND_ALERT;
            case WARNING, MEDIUM -> SecurityActionResponse.LOG_AND_BLOCK;
            case LOW, INFO -> SecurityActionResponse.LOG;
        });
    }

    public SecurityActionResponse handleViolation(SecurityAlertLevel level, RateLimitCategory category, String component, String message, UUID playerUuid) {
        SecurityActionResponse response = getActionResponse(level);
        String categoryTag = category != null ? "[" + category.name() + "] " : "";
        logAlert(level, component, categoryTag + message, playerUuid);
        return response;
    }

    public void logAlert(SecurityAlertLevel level, String component, String message, UUID playerUuid) {
        SecurityLogEntry entry = new SecurityLogEntry(System.currentTimeMillis(), level, component, message, playerUuid);
        alertHistory.addFirst(entry);
        while (alertHistory.size() > MAX_ALERT_HISTORY) {
            alertHistory.removeLast();
        }

        SecurityActionResponse response = getActionResponse(level);
        if (response.shouldLog()) {
            String logMsg = String.format("[AHowToFish-Security] [%s] [%s] %s%s",
                    level.name(),
                    component,
                    message,
                    (playerUuid != null ? " (Player: " + playerUuid + ")" : "")
            );

            if (suspiciousActionLogging && plugin != null && plugin.getLogger() != null) {
                switch (level) {
                    case CRITICAL, HIGH -> plugin.getLogger().log(Level.SEVERE, logMsg);
                    case WARNING, MEDIUM -> plugin.getLogger().log(Level.WARNING, logMsg);
                    default -> plugin.getLogger().log(Level.INFO, logMsg);
                }
            }

            writeLogToFileAsync(level, component, message, playerUuid);
        }

        if (response.shouldAlert()) {
            broadcastAdminAlert(level, component, message, playerUuid);
        }
    }

    private void writeLogToFileAsync(SecurityAlertLevel level, String component, String message, UUID playerUuid) {
        if (plugin == null || plugin.getDataFolder() == null) return;
        long now = System.currentTimeMillis();
        String formattedDate = LOG_DATE_FORMAT.format(Instant.ofEpochMilli(now));
        String targetIdentifier = playerUuid != null ? playerUuid.toString() : "CONSOLE/SYSTEM";
        String line = String.format("[%s] [%s] [%s] [%s] %s%n",
                formattedDate,
                level.name(),
                component,
                targetIdentifier,
                message
        );

        logExecutor.submit(() -> {
            try {
                File logDir = new File(plugin.getDataFolder(), "logs");
                if (!logDir.exists()) {
                    logDir.mkdirs();
                }
                File logFile = new File(logDir, "security.log");
                Files.writeString(logFile.toPath(), line, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                if (plugin.getLogger() != null) {
                    plugin.getLogger().warning("[AHowToFish-Security] Failed to write to security.log: " + e.getMessage());
                }
            }
        });
    }

    public void broadcastAdminAlert(SecurityAlertLevel level, String component, String message, UUID playerUuid) {
        try {
            if (Bukkit.getServer() == null) return;
            String alertPrefix = switch (level) {
                case CRITICAL -> "<red><bold>[CRITICAL SECURITY]</bold></red>";
                case HIGH -> "<red>[SECURITY ALERT]</red>";
                case WARNING -> "<gold>[SECURITY WARNING]</gold>";
                case MEDIUM -> "<yellow>[SECURITY NOTICE]</yellow>";
                case LOW -> "<gray>[SECURITY INFO]</gray>";
                default -> "<gray>[SECURITY]</gray>";
            };

            String formatted = alertPrefix + " <white>[" + component + "] " + message +
                    (playerUuid != null ? " <gray>(" + playerUuid + ")</gray>" : "") + "</white>";
            String legacy = TextUtil.colorize(formatted);

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("ahowtofish.admin.alerts") || player.hasPermission("ahowtofish.admin")) {
                    player.sendMessage(legacy);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isValidPlayerName(String name) {
        return name != null && USERNAME_PATTERN.matcher(name).matches();
    }

    public void executeConsoleReward(Player player, String commandTemplate) {
        if (player == null || commandTemplate == null || commandTemplate.isBlank()) return;
        String playerName = player.getName();
        if (!isValidPlayerName(playerName)) {
            logAlert(SecurityAlertLevel.CRITICAL, "CommandSanitizer",
                    "Blocked command dispatch with illegal player name: " + playerName, player.getUniqueId());
            return;
        }

        String cmd = commandTemplate.replace("{player}", playerName);
        if (cmd.contains("\n") || cmd.contains("\r")) {
            logAlert(SecurityAlertLevel.CRITICAL, "CommandSanitizer",
                    "Blocked command dispatch containing newline injection: " + cmd, player.getUniqueId());
            return;
        }

        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } catch (Throwable t) {
            logAlert(SecurityAlertLevel.HIGH, "CommandSanitizer",
                    "Error executing console reward command '" + cmd + "': " + t.getMessage(), player.getUniqueId());
        }
    }

    public void shutdown() {
        try {
            logExecutor.shutdown();
            if (!logExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                logExecutor.shutdownNow();
            }
        } catch (Exception ignored) {
            logExecutor.shutdownNow();
        }
    }

    public List<SecurityLogEntry> getRecentAlerts(int limit) {
        List<SecurityLogEntry> list = new ArrayList<>();
        int count = 0;
        for (SecurityLogEntry entry : alertHistory) {
            if (count++ >= limit) break;
            list.add(entry);
        }
        return list;
    }

    public boolean isEnabled() { return enabled; }
    public boolean isProductionMode() { return productionMode; }
    public boolean isTransactionProtection() { return enabled && transactionProtection; }
    public boolean isDuplicateRewardProtection() { return enabled && duplicateRewardProtection; }
    public boolean isInventoryProtection() { return enabled && inventoryProtection; }
    public boolean isItemValidation() { return enabled && itemValidation; }
    public boolean isGuiSessionValidation() { return enabled && guiSessionValidation; }
    public boolean isCommandRateLimit() { return enabled && commandRateLimit; }
    public boolean isSellRateLimit() { return enabled && sellRateLimit; }
    public boolean isFishingRateLimit() { return enabled && fishingRateLimit; }
    public SecurityRateLimiter getRateLimiter() { return rateLimiter; }
    public long getGuiSessionTimeoutMs() { return guiSessionTimeoutMs; }
}