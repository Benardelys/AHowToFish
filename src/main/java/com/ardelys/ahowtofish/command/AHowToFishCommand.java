package com.ardelys.ahowtofish.command;

import java.util.List;
import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.model.Competition;
import com.ardelys.ahowtofish.competition.CompetitionType;
import com.ardelys.ahowtofish.gui.menus.*;
import com.ardelys.ahowtofish.language.MessageKey;
import com.ardelys.ahowtofish.model.*;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.quest.PlayerQuestProgress;
import com.ardelys.ahowtofish.utility.PdcKeys;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class AHowToFishCommand implements CommandExecutor {
    private final AHowToFishPlugin plugin;

    public AHowToFishCommand(AHowToFishPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player && plugin.getSecurityManager() != null && plugin.getSecurityManager().isCommandRateLimit()) {
            if (!plugin.getSecurityManager().getRateLimiter().tryAcquire(player.getUniqueId(), com.ardelys.ahowtofish.security.RateLimitCategory.COMMAND)) {
                com.ardelys.ahowtofish.security.SecurityActionResponse resp = plugin.getSecurityManager().handleViolation(
                        com.ardelys.ahowtofish.security.SecurityAlertLevel.LOW,
                        com.ardelys.ahowtofish.security.RateLimitCategory.COMMAND,
                        "CommandExecutor",
                        "Command rate limit exceeded",
                        player.getUniqueId()
                );
                if (resp.shouldBlock()) {
                    sender.sendMessage(TextUtil.colorize("&cPlease slow down! You are sending commands too quickly."));
                    return true;
                }
            }
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("ahowtofish.menu") && !player.hasPermission("ahowtofish.use")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                new MainFishingMenu(plugin, player).open(player);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        switch (sub) {
            case "help" -> {
                sendHelp(sender);
                return true;
            }
            case "menu", "hub" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return true;
                }
                if (!player.hasPermission("ahowtofish.menu") && !player.hasPermission("ahowtofish.use")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                new MainFishingMenu(plugin, player).open(player);
                return true;
            }
            case "admin" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return true;
                }
                if (!player.hasPermission("ahowtofish.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                new AdminPanelMenu(plugin, player).open(player);
                return true;
            }
            case "status" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return true;
                }
                sendStatus(player);
                return true;
            }
            case "stats" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return true;
                }
                if (!player.hasPermission("ahowtofish.stats") && !player.hasPermission("ahowtofish.use")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                new StatsMenu(plugin, player).open(player);
                return true;
            }
            case "collection" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return true;
                }
                if (!player.hasPermission("ahowtofish.collection") && !player.hasPermission("ahowtofish.use")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                new CollectionMenu(plugin, player, 0).open(player);
                return true;
            }
            case "quests" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return true;
                }
                if (!player.hasPermission("ahowtofish.quests") && !player.hasPermission("ahowtofish.use")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                new QuestsMenu(plugin, player).open(player);
                return true;
            }
            case "quest" -> {
                handleQuestCommand(sender, args);
                return true;
            }
            case "achievements" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return true;
                }
                if (!player.hasPermission("ahowtofish.achievements") && !player.hasPermission("ahowtofish.use")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                new AchievementsMenu(plugin, player).open(player);
                return true;
            }
            case "leaderboard" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return true;
                }
                if (!player.hasPermission("ahowtofish.leaderboard") && !player.hasPermission("ahowtofish.use")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                new LeaderboardMenu(plugin, player, "LEVEL").open(player);
                return true;
            }
            case "shop" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return true;
                }
                if (!player.hasPermission("ahowtofish.shop") && !player.hasPermission("ahowtofish.use")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                new ShopMenu(plugin, player).open(player);
                return true;
            }
            case "sell" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return true;
                }
                if (!player.hasPermission("ahowtofish.sell") && !player.hasPermission("ahowtofish.use")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                sellHand(player);
                return true;
            }
            case "sellall" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return true;
                }
                if (!player.hasPermission("ahowtofish.sellall") && !player.hasPermission("ahowtofish.use")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                sellAll(player);
                return true;
            }
            case "language" -> {
                if (args.length >= 2 && (args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("debug"))) {
                    handleLanguageInfo(sender);
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return true;
                }
                if (args.length >= 2) {
                    String lang = args[1].toLowerCase(java.util.Locale.ROOT);
                    FishingProfile profile = plugin.getProfileManager().getProfile(player);
                    if (lang.equalsIgnoreCase("default") || lang.equalsIgnoreCase("auto") || lang.equalsIgnoreCase("reset")) {
                        profile.setLanguage(null);
                        plugin.getProfileManager().saveProfileAsync(profile);
                        plugin.getLanguageManager().sendMessage(player, MessageKey.LANGUAGE_CHANGED, "lang", plugin.getLanguageManager().getDefaultLanguage().toUpperCase(java.util.Locale.ROOT));
                    } else if (plugin.getLanguageManager().isLanguageLoaded(lang)) {
                        profile.setLanguage(lang);
                        plugin.getProfileManager().saveProfileAsync(profile);
                        plugin.getLanguageManager().sendMessage(player, MessageKey.LANGUAGE_CHANGED, "lang", lang.toUpperCase(java.util.Locale.ROOT));
                    } else {
                        plugin.getLanguageManager().sendMessage(player, MessageKey.INVALID_ARGS);
                    }
                } else {
                    new LanguageMenu(plugin, player).open(player);
                }
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("ahowtofish.admin.reload") && !sender.hasPermission("ahowtofish.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                plugin.reloadAllConfigurations();
                plugin.getLanguageManager().sendMessage(sender, MessageKey.RELOAD_SUCCESS);
                return true;
            }
            case "performance", "perf" -> {
                if (!sender.hasPermission("ahowtofish.admin.performance") && !sender.hasPermission("ahowtofish.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                if (plugin.getPerformanceManager() != null) {
                    plugin.getPerformanceManager().sendDiagnostics(sender);
                } else {
                    sender.sendMessage("§cPerformance manager is not available.");
                }
                return true;
            }
            case "give" -> {
                handleGiveFish(sender, args);
                return true;
            }
            case "giverod" -> {
                handleGiveRod(sender, args);
                return true;
            }
            case "givebait" -> {
                handleGiveBait(sender, args);
                return true;
            }
            case "setlevel" -> {
                handleSetLevel(sender, args);
                return true;
            }
            case "setxp" -> {
                handleSetXp(sender, args);
                return true;
            }
            case "addxp" -> {
                handleAddXp(sender, args);
                return true;
            }
            case "reset" -> {
                handleReset(sender, args);
                return true;
            }
            case "competition" -> {
                handleCompetition(sender, args);
                return true;
            }
            case "event" -> {
                handleEvent(sender, args);
                return true;
            }
            case "zone" -> {
                handleZone(sender, args);
                return true;
            }
            case "npc" -> {
                handleNpc(sender, args);
                return true;
            }
            case "starter" -> {
                handleStarterCommand(sender, args);
                return true;
            }
            case "security" -> {
                if (!sender.hasPermission("ahowtofish.admin.security") && !sender.hasPermission("ahowtofish.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                handleSecurityStatus(sender);
                return true;
            }
            case "securitycheck" -> {
                if (!sender.hasPermission("ahowtofish.admin.securitycheck") && !sender.hasPermission("ahowtofish.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                handleSecurityCheck(sender);
                return true;
            }
            case "diagnostics" -> {
                if (!sender.hasPermission("ahowtofish.admin.diagnostics") && !sender.hasPermission("ahowtofish.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return true;
                }
                handleDiagnostics(sender);
                return true;
            }
            default -> {
                sendUsageError(sender);
                return true;
            }
        }
    }

    private void sendUsageError(CommandSender sender) {
        String msg = plugin.getLanguageManager().getMessage(
                sender,
                "general.error-usage-help",
                "&cInvalid argument.\n\n&eUsage:\n&f/ahowtofish\n&f/ahowtofish stats\n&f/ahowtofish collection\n\n&7Use &b/ahowtofish help &7for more information."
        );
        for (String line : msg.split("\n")) {
            sender.sendMessage(TextUtil.colorize(line));
        }
    }

    private void handleSecurityStatus(CommandSender sender) {
        var sm = plugin.getSecurityManager();
        sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
        sender.sendMessage(TextUtil.colorize("&c&l✦ AHowToFish Security Diagnostics ✦"));
        sender.sendMessage(TextUtil.colorize("&7Security System: &a" + (sm.isEnabled() ? "ACTIVE" : "DISABLED")));
        sender.sendMessage(TextUtil.colorize("&7Transaction Protection: &a" + sm.isTransactionProtection()));
        sender.sendMessage(TextUtil.colorize("&7Inventory & GUI Protection: &a" + sm.isInventoryProtection()));
        sender.sendMessage(TextUtil.colorize("&7HMAC Item Verification: &a" + sm.isItemValidation()));
        sender.sendMessage(TextUtil.colorize("&7Fishing Rate Limiter: &a" + sm.isFishingRateLimit()));
        sender.sendMessage(TextUtil.colorize("&7Sell Lock Protection: &a" + sm.isSellRateLimit()));
        sender.sendMessage(TextUtil.colorize("&7Active GUI Sessions: &e" + plugin.getGuiSessionManager().getActiveSessionCount()));
        sender.sendMessage(TextUtil.colorize("&7Cached Player Profiles: &e" + plugin.getProfileManager().getCachedProfiles().size()));

        var alerts = sm.getRecentAlerts(5);
        sender.sendMessage(TextUtil.colorize("&7Recent Security Alerts (&e" + alerts.size() + "&7):"));
        if (alerts.isEmpty()) {
            sender.sendMessage(TextUtil.colorize("&a  No security incidents recorded."));
        } else {
            for (var alert : alerts) {
                sender.sendMessage(TextUtil.colorize(String.format("  &c[%s] &e%s: &7%s", alert.level(), alert.component(), alert.message())));
            }
        }
        sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
    }

    private void handleSecurityCheck(CommandSender sender) {
        sender.sendMessage(TextUtil.colorize("&e[AHowToFish] Running comprehensive security self-check..."));
        int issuesFound = 0;

        for (var fish : plugin.getFishManager().getAllFish()) {
            if (fish.minWeight() <= 0 || fish.maxWeight() < fish.minWeight()) {
                sender.sendMessage(TextUtil.colorize("&c[WARN] Fish '" + fish.id() + "' has invalid weight bounds!"));
                issuesFound++;
            }
            if (fish.sellPrice() < 0 || Double.isNaN(fish.sellPrice())) {
                sender.sendMessage(TextUtil.colorize("&c[WARN] Fish '" + fish.id() + "' has negative/NaN sell price!"));
                issuesFound++;
            }
        }

        try (var conn = plugin.getDatabaseManager().getConnection()) {
            if (conn.isClosed()) {
                sender.sendMessage(TextUtil.colorize("&c[FAIL] Database connection is closed!"));
                issuesFound++;
            }
        } catch (Exception e) {
            sender.sendMessage(TextUtil.colorize("&c[FAIL] Database check failed: " + e.getMessage()));
            issuesFound++;
        }

        if (issuesFound == 0) {
            sender.sendMessage(TextUtil.colorize("&a✔ Security Check Passed: All checks OK. No configuration or runtime vulnerabilities detected."));
        } else {
            sender.sendMessage(TextUtil.colorize("&c✘ Security Check Complete: Found " + issuesFound + " potential issue(s)."));
        }
    }

    private void handleDiagnostics(CommandSender sender) {
        var sm = plugin.getSecurityManager();
        sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
        sender.sendMessage(TextUtil.colorize("&b&l✦ AHowToFish System Diagnostics ✦"));
        sender.sendMessage(TextUtil.colorize("&7Plugin Version: &e" + plugin.getPluginMeta().getVersion()));
        sender.sendMessage(TextUtil.colorize("&7Environment: &a" + (sm != null && sm.isProductionMode() ? "PRODUCTION" : "DEVELOPMENT")));
        sender.sendMessage(TextUtil.colorize("&7Fish Registered: &e" + plugin.getFishManager().getAllFish().size()));
        sender.sendMessage(TextUtil.colorize("&7Rods Registered: &e" + plugin.getRodManager().getAllRods().size()));
        sender.sendMessage(TextUtil.colorize("&7Baits Registered: &e" + plugin.getBaitManager().getAllBaits().size()));
        sender.sendMessage(TextUtil.colorize("&7Active Zones: &e" + plugin.getZoneManager().getAllZones().size()));
        sender.sendMessage(TextUtil.colorize("&7Cached Profiles: &e" + plugin.getProfileManager().getCachedProfiles().size()));
        sender.sendMessage(TextUtil.colorize("&7Active Tournament: &e" + plugin.getCompetitionManager().isCompetitionActive()));
        if (sm != null) {
            sender.sendMessage(TextUtil.colorize("&7Security System: &a" + (sm.isEnabled() ? "ACTIVE" : "DISABLED")));
            sender.sendMessage(TextUtil.colorize("&7Rate Limiter Tracked Players: &e" + sm.getRateLimiter().getTrackedPlayerCount()));
            sender.sendMessage(TextUtil.colorize("&7Active GUI Sessions: &e" + plugin.getGuiSessionManager().getActiveSessionCount()));
            var alerts = sm.getRecentAlerts(3);
            sender.sendMessage(TextUtil.colorize("&7Recent Security Alerts: &e" + alerts.size()));
            for (var alert : alerts) {
                sender.sendMessage(TextUtil.colorize(String.format("  &c[%s] &e%s: &7%s", alert.level(), alert.component(), alert.message())));
            }
        }
        sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
    }

    private void handleLanguageInfo(CommandSender sender) {
        if (!sender.hasPermission("ahowtofish.admin.diagnostics") && !sender.hasPermission("ahowtofish.admin.language") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }

        String serverDefault = plugin.getLanguageManager().getDefaultLanguage();
        String playerLang = "N/A (Console)";
        String dbPref = "None";
        String resolvedLang = serverDefault;

        if (sender instanceof Player player) {
            FishingProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null) {
                dbPref = profile.hasExplicitLanguage() ? profile.getLanguage() : "None (Using Server Default)";
            }
            playerLang = plugin.getLanguageManager().getPlayerLanguage(player);
            resolvedLang = playerLang;
        }

        boolean isLoaded = plugin.getLanguageManager().isLanguageLoaded(resolvedLang);
        String langFile = "lang/" + resolvedLang + ".yml";

        sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
        sender.sendMessage(TextUtil.colorize("&b&l✦ AHowToFish Language Diagnostics ✦"));
        sender.sendMessage(TextUtil.colorize("&7Server Default Language: &e" + serverDefault));
        sender.sendMessage(TextUtil.colorize("&7Player Language: &e" + playerLang));
        sender.sendMessage(TextUtil.colorize("&7Resolved Language: &a" + resolvedLang));
        sender.sendMessage(TextUtil.colorize("&7Language File: &e" + langFile));
        sender.sendMessage(TextUtil.colorize("&7Language Loaded: &a" + isLoaded));
        sender.sendMessage(TextUtil.colorize("&7Language Cache: &aactive"));
        sender.sendMessage(TextUtil.colorize("&7Database Preference: &e" + dbPref));
        sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
    }

    private void sendHelp(CommandSender sender) {
        var lm = plugin.getLanguageManager();
        sender.sendMessage(lm.getMessage(sender, "help.header", "&8&m----------------------------------------"));
        sender.sendMessage(lm.getMessage(sender, "help.title", "&b&l✦ AHowToFish Commands ✦"));
        sender.sendMessage(lm.getMessage(sender, "help.menu", "&e/ahowtofish &7- Open main fishing menu"));
        sender.sendMessage(lm.getMessage(sender, "help.stats", "&e/ahowtofish stats &7- View fishing statistics"));
        sender.sendMessage(lm.getMessage(sender, "help.collection", "&e/ahowtofish collection &7- Open fish log journal"));
        sender.sendMessage(lm.getMessage(sender, "help.quests", "&e/ahowtofish quests &7- View fishing missions"));
        sender.sendMessage(lm.getMessage(sender, "help.quest", "&e/ahowtofish quest [id] &7- View active quest progress or details"));
        sender.sendMessage(lm.getMessage(sender, "help.shop", "&e/ahowtofish shop &7- Open fish market"));
        sender.sendMessage(lm.getMessage(sender, "help.sell", "&e/ahowtofish sell &7- Sell held fish"));
        sender.sendMessage(lm.getMessage(sender, "help.sellall", "&e/ahowtofish sellall &7- Sell all fish in inventory"));
        sender.sendMessage(lm.getMessage(sender, "help.leaderboard", "&e/ahowtofish leaderboard &7- View player rankings"));
        sender.sendMessage(lm.getMessage(sender, "help.language", "&e/ahowtofish language &7- Change personal language"));

        if (sender.hasPermission("ahowtofish.admin")) {
            sender.sendMessage(lm.getMessage(sender, "help.admin-header", "&cAdmin Commands:"));
            sender.sendMessage(lm.getMessage(sender, "help.admin-give", "&c/ahowtofish give <player> <fish> [amount]"));
            sender.sendMessage(lm.getMessage(sender, "help.admin-giverod", "&c/ahowtofish giverod <player> <rod>"));
            sender.sendMessage(lm.getMessage(sender, "help.admin-givebait", "&c/ahowtofish givebait <player> <bait> [amount]"));
            sender.sendMessage(lm.getMessage(sender, "help.admin-progression", "&c/ahowtofish setlevel/addxp/setxp/reset <player>"));
            sender.sendMessage(lm.getMessage(sender, "help.admin-quest", "&c/ahowtofish quest <list|info|start|reset|complete|skip>"));
            sender.sendMessage(lm.getMessage(sender, "help.admin-starter", "&c/ahowtofish starter reset <player>"));
            sender.sendMessage(lm.getMessage(sender, "help.admin-competition", "&c/ahowtofish competition start/stop/info"));
            sender.sendMessage(lm.getMessage(sender, "help.admin-event", "&c/ahowtofish event start/stop"));
            sender.sendMessage(lm.getMessage(sender, "help.admin-zone", "&c/ahowtofish zone create/delete/setpos1/setpos2"));
            sender.sendMessage(lm.getMessage(sender, "help.admin-diagnostics", "&c/ahowtofish diagnostics / security / performance / securitycheck"));
            sender.sendMessage(lm.getMessage(sender, "help.admin-reload", "&c/ahowtofish reload &7- Reload all configs & languages"));
        }
        sender.sendMessage(lm.getMessage(sender, "help.footer", "&8&m----------------------------------------"));
    }

    private void sellHand(Player player) {
        plugin.getFishSellService().sellHeldFish(player);
    }

    private void sellAll(Player player) {
        plugin.getFishSellService().sellAllFish(player);
    }

    private void handleGiveFish(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ahowtofish.admin.give") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }
        if (args.length < 3) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.INVALID_ARGS);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.PLAYER_NOT_FOUND);
            return;
        }

        String fishId = args[2];
        Fish fish = plugin.getFishManager().getFish(fishId);
        if (fish == null) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.FISH_NOT_FOUND, "fish", fishId);
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (NumberFormatException ignored) {}
        }

        FishRarity rarity = plugin.getFishManager().getRarity(fish.rarityId());
        if (rarity == null) rarity = new FishRarity("COMMON", "Common", "&f", 1, 1, 1, false, "", "", "");

        FishCatchResult result = new FishCatchResult(
                fish, rarity, (fish.minWeight() + fish.maxWeight()) / 2.0,
                fish.sellPrice(), fish.xpReward(), target.getUniqueId(), target.getName(), System.currentTimeMillis()
        );

        ItemStack item = result.buildItemStack();
        item.setAmount(amount);
        target.getInventory().addItem(item);

        plugin.getLanguageManager().sendMessage(sender, MessageKey.ADMIN_GIVE_FISH, "player", target.getName(), "fish", fish.displayName(), "amount", amount);
    }

    private void handleGiveRod(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ahowtofish.admin.give") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }
        if (args.length < 3) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.INVALID_ARGS);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.PLAYER_NOT_FOUND);
            return;
        }

        FishingRod rod = plugin.getRodManager().getRod(args[2]);
        if (rod == null) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.ROD_NOT_FOUND, "rod", args[2]);
            return;
        }

        target.getInventory().addItem(rod.createItemStack());
        plugin.getLanguageManager().sendMessage(sender, MessageKey.ADMIN_GIVE_ROD, "player", target.getName(), "rod", rod.displayName());
    }

    private void handleGiveBait(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ahowtofish.admin.give") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }
        if (args.length < 3) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.INVALID_ARGS);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.PLAYER_NOT_FOUND);
            return;
        }

        FishingBait bait = plugin.getBaitManager().getBait(args[2]);
        if (bait == null) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.BAIT_NOT_FOUND, "bait", args[2]);
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (NumberFormatException ignored) {}
        }

        target.getInventory().addItem(bait.createItemStack(amount));
        plugin.getLanguageManager().sendMessage(sender, MessageKey.ADMIN_GIVE_BAIT, "player", target.getName(), "bait", bait.displayName(), "amount", amount);
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ahowtofish.admin.setlevel") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }
        if (args.length < 3) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.INVALID_ARGS);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.PLAYER_NOT_FOUND);
            return;
        }

        try {
            int level = Math.max(1, Integer.parseInt(args[2]));
            FishingProfile profile = plugin.getProfileManager().getProfile(target);
            profile.setLevel(level);
            plugin.getProfileManager().saveProfileAsync(profile);
            plugin.getLanguageManager().sendMessage(sender, MessageKey.ADMIN_LEVEL_SET, "player", target.getName(), "level", level);
        } catch (NumberFormatException e) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.INVALID_ARGS);
        }
    }

    private void handleSetXp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ahowtofish.admin.setxp") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }
        if (args.length < 3) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.INVALID_ARGS);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.PLAYER_NOT_FOUND);
            return;
        }

        try {
            long xp = Math.max(0, Long.parseLong(args[2]));
            FishingProfile profile = plugin.getProfileManager().getProfile(target);
            profile.setXp(xp);
            plugin.getProgressionManager().checkLevelUp(target, profile);
            plugin.getProfileManager().saveProfileAsync(profile);
            plugin.getLanguageManager().sendMessage(sender, MessageKey.ADMIN_XP_SET, "player", target.getName(), "xp", xp);
        } catch (NumberFormatException e) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.INVALID_ARGS);
        }
    }

    private void handleAddXp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ahowtofish.admin.setxp") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }
        if (args.length < 3) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.INVALID_ARGS);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.PLAYER_NOT_FOUND);
            return;
        }

        try {
            long xp = Math.max(1, Long.parseLong(args[2]));
            plugin.getProgressionManager().addXp(target, xp);
            plugin.getLanguageManager().sendMessage(sender, MessageKey.ADMIN_XP_ADDED, "player", target.getName(), "xp", xp);
        } catch (NumberFormatException e) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.INVALID_ARGS);
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ahowtofish.admin.reset") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }
        if (args.length < 2) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.INVALID_ARGS);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.PLAYER_NOT_FOUND);
            return;
        }

        FishingProfile profile = new FishingProfile(target.getUniqueId(), target.getName());
        plugin.getProfileManager().saveProfileAsync(profile);
        plugin.getLanguageManager().sendMessage(sender, MessageKey.ADMIN_RESET, "player", target.getName());
    }

    private void handleCompetition(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ahowtofish.admin.competition") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(TextUtil.colorize("&cUsage: /ahowtofish competition <start|stop|info> [type] [seconds]"));
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "start" -> {
                CompetitionType type = CompetitionType.MOST_FISH;
                int duration = 300;
                if (args.length >= 3) {
                    try { type = CompetitionType.valueOf(args[2].toUpperCase()); } catch (IllegalArgumentException ignored) {}
                }
                if (args.length >= 4) {
                    try { duration = Math.max(30, Integer.parseInt(args[3])); } catch (NumberFormatException ignored) {}
                }
                boolean started = plugin.getCompetitionManager().startCompetition(type, duration);
                if (!started) {
                    sender.sendMessage(TextUtil.colorize("&cA tournament is already ongoing!"));
                }
            }
            case "stop" -> {
                if (plugin.getCompetitionManager().isCompetitionActive()) {
                    plugin.getCompetitionManager().stopCompetition();
                    sender.sendMessage(TextUtil.colorize("&aTournament stopped."));
                } else {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.COMPETITION_NO_ACTIVE);
                }
            }
            case "info" -> {
                if (plugin.getCompetitionManager().isCompetitionActive()) {
                    Competition c = plugin.getCompetitionManager().getActiveCompetition();
                    sender.sendMessage(TextUtil.colorize("&aActive Tournament: &e" + c.getType().getDisplayName()));
                    sender.sendMessage(TextUtil.colorize("&7Time Remaining: &f" + TextUtil.formatTime(c.getRemainingSeconds())));
                } else {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.COMPETITION_NO_ACTIVE);
                }
            }
        }
    }

    private void handleEvent(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ahowtofish.admin.event") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(TextUtil.colorize("&cUsage: /ahowtofish event <start|stop> [id]"));
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "start" -> {
                if (args.length < 3) {
                    sender.sendMessage(TextUtil.colorize("&cUsage: /ahowtofish event start <id>"));
                    return;
                }
                boolean started = plugin.getEventManager().startEvent(args[2]);
                if (!started) {
                    sender.sendMessage(TextUtil.colorize("&cFailed to start event. Check event ID or if another event is already running."));
                }
            }
            case "stop" -> {
                plugin.getEventManager().stopEvent();
                sender.sendMessage(TextUtil.colorize("&aFishing event stopped."));
            }
        }
    }

    private void handleZone(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ahowtofish.admin.zone") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(TextUtil.colorize("&cUsage: /ahowtofish zone <create|delete|setpos1|setpos2|info|reload>"));
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "setpos1" -> {
                if (!(sender instanceof Player p)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return;
                }
                plugin.getZoneManager().setPos1(p, p.getLocation());
                plugin.getLanguageManager().sendMessage(p, MessageKey.ADMIN_ZONE_POS1);
            }
            case "setpos2" -> {
                if (!(sender instanceof Player p)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return;
                }
                plugin.getZoneManager().setPos2(p, p.getLocation());
                plugin.getLanguageManager().sendMessage(p, MessageKey.ADMIN_ZONE_POS2);
            }
            case "create" -> {
                if (!(sender instanceof Player p)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return;
                }
                if (args.length < 3) {
                    p.sendMessage(TextUtil.colorize("&cUsage: /ahowtofish zone create <id> [reqLevel]"));
                    return;
                }
                Location p1 = plugin.getZoneManager().getPos1(p);
                Location p2 = plugin.getZoneManager().getPos2(p);
                if (p1 == null || p2 == null) {
                    plugin.getLanguageManager().sendMessage(p, MessageKey.ZONE_SET_POS_FIRST);
                    return;
                }
                int reqLevel = 1;
                if (args.length >= 4) {
                    try { reqLevel = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
                }
                boolean created = plugin.getZoneManager().createZone(args[2], args[2], p1, p2, reqLevel);
                if (created) {
                    plugin.getLanguageManager().sendMessage(p, MessageKey.ADMIN_ZONE_CREATED, "zone", args[2]);
                } else {
                    plugin.getLanguageManager().sendMessage(p, MessageKey.ZONE_SAME_WORLD);
                }
            }
            case "delete" -> {
                if (args.length < 3) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.INVALID_ARGS);
                    return;
                }
                boolean deleted = plugin.getZoneManager().deleteZone(args[2]);
                if (deleted) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ADMIN_ZONE_DELETED, "zone", args[2]);
                } else {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ZONE_NOT_FOUND, "zone", args[2]);
                }
            }
            case "reload" -> {
                plugin.getZoneManager().load();
                sender.sendMessage(TextUtil.colorize("&aZones reloaded successfully."));
            }
        }
    }

    private void sendStatus(Player player) {
        FishingProfile profile = plugin.getProfileManager().getProfile(player);
        int level = profile != null ? profile.getLevel() : 1;
        long xp = profile != null ? profile.getXp() : 0;
        double progress = profile != null ? plugin.getProgressionManager().getLevelProgressPercent(profile) : 0.0;
        long fishCaught = profile != null ? profile.getStats().getTotalFishCaught() : 0;
        double largestWeight = profile != null ? profile.getStats().getLargestFishWeight() : 0.0;

        double coins = 0.0;
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isAvailable()) {
            coins = plugin.getVaultHook().getBalance(player);
        } else if (profile != null) {
            coins = profile.getStats().getTotalMoneyEarned();
        }

        FishingZone zone = plugin.getZoneManager().getZoneAt(player.getLocation());
        String wildZone = plugin.getLanguageManager().getMessage(player, "command.status.wild_zone", "Open Waters");
        String zoneName = zone != null ? zone.displayName() : wildZone;

        var rod = plugin.getRodManager().getRodFromItem(player.getInventory().getItemInMainHand());
        String stdRod = plugin.getLanguageManager().getMessage(player, "command.status.standard_rod", "Standard Rod");
        String rodName = rod != null ? rod.displayName() : stdRod;

        var bait = plugin.getBaitManager().getActiveBait(player);
        String noBait = plugin.getLanguageManager().getMessage(player, "command.status.no_bait", "None");
        String baitName = bait != null ? bait.displayName() : noBait;

        player.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
        player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.status.header", "&6&l✦ AHowToFish &8| &eAngler Status &8(&f{player}&8) ✦").replace("{player}", player.getName())));
        player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.status.level_xp", "&7Skill Level: &e{level} &8| &7XP: &b{xp} &8(&a{progress}%&8)")
                .replace("{level}", String.valueOf(level))
                .replace("{xp}", TextUtil.formatInteger(xp))
                .replace("{progress}", TextUtil.formatDecimal(progress))));
        player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.status.coins_zone", "&7Balance: &a${coins} &8| &7Current Zone: &f{zone}")
                .replace("{coins}", TextUtil.formatDecimal(coins))
                .replace("{zone}", zoneName)));
        player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.status.rod_bait", "&7Active Rod: &f{rod} &8| &7Active Bait: &e{bait}")
                .replace("{rod}", rodName)
                .replace("{bait}", baitName)));
        player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.status.catches_weight", "&7Total Catches: &f{count} &8| &7Personal Best: &a{weight} kg")
                .replace("{count}", TextUtil.formatInteger(fishCaught))
                .replace("{weight}", TextUtil.formatDecimal(largestWeight))));

        Competition comp = plugin.getCompetitionManager() != null ? plugin.getCompetitionManager().getActiveCompetition() : null;
        if (comp != null && comp.isActive()) {
            int rank = comp.getRank(player.getUniqueId());
            double score = comp.getScore(player.getUniqueId());
            String unrankedStr = plugin.getLanguageManager().getMessage(player, "command.status.unranked", "Unranked");
            String rankStr = rank > 0 ? "&e#" + rank : "&7" + unrankedStr;

            player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.status.tourney_active", "&7Active Tourney: &e{tourney} &8(&a{time} left&8)")
                    .replace("{tourney}", comp.getType().getDisplayName())
                    .replace("{time}", TextUtil.formatTime(comp.getRemainingSeconds()))));
            player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.status.tourney_rank", "&7Your Rank: {rank} &8| &7Score: &a{score} {unit}")
                    .replace("{rank}", rankStr)
                    .replace("{score}", TextUtil.formatDecimal(score))
                    .replace("{unit}", comp.getType().getUnit())));
        }

        player.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
    }

    private void handleNpc(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ahowtofish.admin.npc") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }

        Player pSender = sender instanceof Player ? (Player) sender : null;

        if (args.length < 2) {
            sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
            sender.sendMessage(TextUtil.colorize("&6&l✦ Fisherman Lucas NPC Commands ✦"));
            sender.sendMessage(TextUtil.colorize("&e/ahowtofish npc set &7- Place Lucas at your current location"));
            sender.sendMessage(TextUtil.colorize("&e/ahowtofish npc remove &7- Disable and despawn Lucas"));
            sender.sendMessage(TextUtil.colorize("&e/ahowtofish npc teleport &7- Teleport to Lucas"));
            sender.sendMessage(TextUtil.colorize("&e/ahowtofish npc reload &7- Reload npc.yml & respawn"));
            sender.sendMessage(TextUtil.colorize("&e/ahowtofish npc info &7- Display NPC coordinates & status"));
            sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
            return;
        }

        String action = args[1].toLowerCase(java.util.Locale.ROOT);
        switch (action) {
            case "set" -> {
                if (!(sender instanceof Player p)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return;
                }
                if (plugin.getNpcManager() != null) {
                    plugin.getNpcManager().setNpcLocation(p.getLocation());
                    p.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(p, "command.npc.set_success", "&a[AHowToFish] Fisherman Lucas was placed at your location and saved to npc.yml!")));
                }
            }
            case "remove" -> {
                if (plugin.getNpcManager() != null) {
                    plugin.getNpcManager().removeNpc();
                    sender.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(pSender, "command.npc.remove_success", "&c[AHowToFish] Fisherman Lucas was disabled and removed.")));
                }
            }
            case "teleport", "tp" -> {
                if (!(sender instanceof Player p)) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.ONLY_PLAYERS);
                    return;
                }
                if (plugin.getNpcManager() != null) {
                    Location loc = plugin.getNpcManager().getNpcLocation();
                    if (loc == null || loc.getWorld() == null) {
                        p.sendMessage(TextUtil.colorize("&c[AHowToFish] Fisherman Lucas location is invalid or world does not exist."));
                        return;
                    }
                    p.teleport(loc);
                    p.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(p, "command.npc.teleport_success", "&a[AHowToFish] Teleported to Fisherman Lucas.")));
                }
            }
            case "reload" -> {
                if (plugin.getNpcManager() != null) {
                    plugin.getNpcManager().load();
                    sender.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(pSender, "command.npc.reload_success", "&a[AHowToFish] npc.yml was reloaded successfully! Fisherman Lucas respawned.")));
                }
            }
            case "info" -> {
                if (plugin.getNpcManager() != null) {
                    var npc = plugin.getNpcManager();
                    sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
                    sender.sendMessage(TextUtil.colorize("&6&lFisherman Lucas"));
                    sender.sendMessage(TextUtil.colorize("&7Enabled: &f" + npc.isEnabled()));
                    sender.sendMessage(TextUtil.colorize("&7World: &f" + npc.getWorldName()));
                    sender.sendMessage(TextUtil.colorize("&7Location: &f" + TextUtil.formatDecimal(npc.getLocX()) + ", " + TextUtil.formatDecimal(npc.getLocY()) + ", " + TextUtil.formatDecimal(npc.getLocZ())));
                    sender.sendMessage(TextUtil.colorize("&7Skin: &aConfigured"));
                    sender.sendMessage(TextUtil.colorize("&7Hologram: &f" + (npc.isHologramEnabled() ? "Enabled" : "Disabled")));
                    sender.sendMessage(TextUtil.colorize("&7Selling: &aEnabled"));
                    sender.sendMessage(TextUtil.colorize("&7Confirmation: &f" + (npc.isConfirmationEnabled() ? "Enabled ($" + TextUtil.formatDecimal(npc.getConfirmationMinValue()) + "+)" : "Disabled")));
                    sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
                }
            }
            default -> sender.sendMessage(TextUtil.colorize("&cUnknown NPC action. Use: /ahowtofish npc <set|remove|teleport|reload|info>"));
        }
    }

    private void handleQuestCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(TextUtil.colorize("&cUsage: /ahowtofish quest <list|info|start|reset|complete|skip>"));
                return;
            }
            if (plugin.getQuestManager() == null) return;
            FishingQuest active = plugin.getQuestManager().getActiveQuest(player);
            if (active == null) {
                player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.quest.no_active", "&7You do not have an active quest. Visit &6Fisherman Lucas &7or type &e/fish quests&7.")));
                return;
            }
            FishingProfile profile = plugin.getProfileManager().getProfile(player);
            PlayerQuestProgress qp = profile != null ? profile.getPlayerQuestProgress(active.id()) : null;
            double cur = qp != null ? qp.getProgress() : 0.0;
            double req = active.requiredAmount();
            String bar = TextUtil.buildProgressBar(cur, req, 15, "█", "&a", "&7");

            String qName = plugin.getQuestManager().getLocalizedQuestName(player, active);
            player.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
            player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.quest.card_title", "&6&l✦ Active Quest: &f{quest} ✦").replace("{quest}", qName)));
            player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.quest.category_type", "&7Category: &e{category} &8| &7Target Type: &f{type}")
                    .replace("{category}", active.category().name())
                    .replace("{type}", active.objectiveType().name())));

            List<String> descList = plugin.getLanguageManager().getMessageList(player, "quest_translations." + active.id() + ".desc");
            if (descList.isEmpty()) {
                for (String desc : active.description()) {
                    player.sendMessage(TextUtil.colorize("&7" + desc));
                }
            } else {
                for (String desc : descList) {
                    player.sendMessage(TextUtil.colorize("&7" + desc));
                }
            }

            player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.quest.progress", "&7Progress: &8[{bar}&8] &e{cur}/{req}")
                    .replace("{bar}", bar)
                    .replace("{cur}", String.valueOf((int)Math.min(cur, req)))
                    .replace("{req}", String.valueOf((int)req))));

            if (qp != null && qp.getExpiresAt() > 0) {
                player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.quest.time_left", "&7Time Left: &c{time}")
                        .replace("{time}", TextUtil.formatTime(qp.getRemainingSeconds()))));
            }

            player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "command.quest.rewards", "&7Rewards: &a+${money} &8| &b+{xp} XP")
                    .replace("{money}", TextUtil.formatDecimal(active.moneyReward()))
                    .replace("{xp}", String.valueOf(active.xpReward()))));
            player.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
            return;
        }

        String sub = args[1].toLowerCase(java.util.Locale.ROOT);
        switch (sub) {
            case "list" -> {
                if (plugin.getQuestManager() == null) return;
                sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
                sender.sendMessage(TextUtil.colorize("&6&l✦ Registered Quests ✦"));
                for (FishingQuest q : plugin.getQuestManager().getAllQuests()) {
                    sender.sendMessage(TextUtil.colorize("&e" + q.id() + " &8- &f" + q.name() + " &7(" + q.category().name() + ", " + q.objectiveType().name() + ")"));
                }
                sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
            }
            case "info" -> {
                if (args.length < 3) {
                    sender.sendMessage(TextUtil.colorize("&cUsage: /ahowtofish quest info <questId>"));
                    return;
                }
                if (plugin.getQuestManager() == null) return;
                FishingQuest q = plugin.getQuestManager().getQuest(args[2]);
                if (q == null) {
                    sender.sendMessage(TextUtil.colorize("&cQuest not found: " + args[2]));
                    return;
                }
                sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
                sender.sendMessage(TextUtil.colorize("&6&lQuest Info: &f" + q.name() + " &8(&e" + q.id() + "&8)"));
                sender.sendMessage(TextUtil.colorize("&7Category: &e" + q.category().name() + " &8| &7Type: &b" + q.objectiveType().name()));
                sender.sendMessage(TextUtil.colorize("&7Target: &f" + (q.targetRequirement().isBlank() ? "Any" : q.targetRequirement()) + " &8| &7Required: &a" + (int)q.requiredAmount()));
                if (q.timeLimitSeconds() > 0) {
                    sender.sendMessage(TextUtil.colorize("&7Time Limit: &c" + q.timeLimitSeconds() + "s &8| &7Retry Cooldown: &e" + q.retryCooldownSeconds() + "s"));
                }
                if (q.nextQuestId() != null) {
                    sender.sendMessage(TextUtil.colorize("&7Next Quest: &6" + q.nextQuestId()));
                }
                sender.sendMessage(TextUtil.colorize("&7Rewards: &a+$" + q.moneyReward() + " &8| &b+" + q.xpReward() + " XP"));
                sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
            }
            case "start" -> {
                if (!sender.hasPermission("ahowtofish.admin.quest") && !sender.hasPermission("ahowtofish.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return;
                }
                if (args.length < 4) {
                    sender.sendMessage(TextUtil.colorize("&cUsage: /ahowtofish quest start <player> <questId>"));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(TextUtil.colorize("&cPlayer not found: " + args[2]));
                    return;
                }
                if (plugin.getQuestManager() == null) return;
                boolean started = plugin.getQuestManager().startQuest(target, args[3]);
                if (started) {
                    sender.sendMessage(TextUtil.colorize("&aStarted quest &e" + args[3] + " &afor &f" + target.getName()));
                } else {
                    sender.sendMessage(TextUtil.colorize("&cFailed to start quest (invalid quest ID or prerequisites not met)."));
                }
            }
            case "complete" -> {
                if (!sender.hasPermission("ahowtofish.admin.quest") && !sender.hasPermission("ahowtofish.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return;
                }
                if (args.length < 4) {
                    sender.sendMessage(TextUtil.colorize("&cUsage: /ahowtofish quest complete <player> <questId>"));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(TextUtil.colorize("&cPlayer not found: " + args[2]));
                    return;
                }
                if (plugin.getQuestManager() == null) return;
                boolean done = plugin.getQuestManager().forceCompleteQuest(target, args[3]);
                if (done) {
                    sender.sendMessage(TextUtil.colorize("&aForce completed quest &e" + args[3] + " &afor &f" + target.getName()));
                } else {
                    sender.sendMessage(TextUtil.colorize("&cFailed to force complete quest (invalid quest ID)."));
                }
            }
            case "skip" -> {
                if (!sender.hasPermission("ahowtofish.admin.quest") && !sender.hasPermission("ahowtofish.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(TextUtil.colorize("&cUsage: /ahowtofish quest skip <player>"));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(TextUtil.colorize("&cPlayer not found: " + args[2]));
                    return;
                }
                if (plugin.getQuestManager() == null) return;
                boolean skipped = plugin.getQuestManager().skipQuest(target);
                if (skipped) {
                    sender.sendMessage(TextUtil.colorize("&aSkipped current quest for &f" + target.getName()));
                } else {
                    sender.sendMessage(TextUtil.colorize("&cPlayer has no active quest to skip."));
                }
            }
            case "reset" -> {
                if (!sender.hasPermission("ahowtofish.admin.quest") && !sender.hasPermission("ahowtofish.admin")) {
                    plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(TextUtil.colorize("&cUsage: /ahowtofish quest reset <player> [questId|all]"));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(TextUtil.colorize("&cPlayer not found: " + args[2]));
                    return;
                }
                String questTarget = args.length >= 4 ? args[3] : "all";
                if (plugin.getQuestManager() == null) return;
                plugin.getQuestManager().resetQuest(target, questTarget);
                sender.sendMessage(TextUtil.colorize("&aReset quest(s) &e" + questTarget + " &afor &f" + target.getName()));
            }
            default -> {
                
                if (plugin.getQuestManager() != null) {
                    FishingQuest q = plugin.getQuestManager().getQuest(args[1]);
                    if (q != null) {
                        sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
                        sender.sendMessage(TextUtil.colorize("&6&lQuest: &f" + q.name() + " &8(&e" + q.id() + "&8)"));
                        for (String d : q.description()) {
                            sender.sendMessage(TextUtil.colorize("&7" + d));
                        }
                        sender.sendMessage(TextUtil.colorize("&7Target: &a" + (int)q.requiredAmount() + " &8| &7Reward: &a+$" + q.moneyReward() + " &8| &b+" + q.xpReward() + " XP"));
                        sender.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
                        return;
                    }
                }
                sender.sendMessage(TextUtil.colorize("&cUnknown quest command. Use: /ahowtofish quest <list|info|start|reset|complete|skip>"));
            }
        }
    }

    private void handleStarterCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ahowtofish.admin.starter.reset") && !sender.hasPermission("ahowtofish.admin")) {
            plugin.getLanguageManager().sendMessage(sender, MessageKey.NO_PERMISSION);
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("reset")) {
            sender.sendMessage(TextUtil.colorize("&cUsage: /ahowtofish starter reset <player>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(TextUtil.colorize("&cPlayer not found: " + args[2]));
            return;
        }
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().resetStarterProgression(target);
            String successMsg = plugin.getLanguageManager().getMessage(sender, "command.starter.reset_success",
                    "&aStarter progression & starter rod flag reset for &f{player}&a. They can speak to Fisherman Lucas again.")
                    .replace("{player}", target.getName());
            sender.sendMessage(TextUtil.colorize(successMsg));

            String notifyMsg = plugin.getLanguageManager().getMessage(target, "command.starter.reset_notify",
                    "&8[&6Lucas&8] &eYour starter progression has been reset by an administrator.");
            target.sendMessage(TextUtil.colorize(notifyMsg));
        }
    }
}
