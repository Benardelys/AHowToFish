package com.ardelys.ahowtofish;

import com.ardelys.ahowtofish.api.AHowToFishAPI;
import com.ardelys.ahowtofish.command.AHowToFishCommand;
import com.ardelys.ahowtofish.command.FishingTabCompleter;
import com.ardelys.ahowtofish.competition.CompetitionManager;
import com.ardelys.ahowtofish.database.DatabaseManager;
import com.ardelys.ahowtofish.database.dao.ProfileDao;
import com.ardelys.ahowtofish.economy.FishSellService;
import com.ardelys.ahowtofish.event.EventManager;
import com.ardelys.ahowtofish.fishing.*;
import com.ardelys.ahowtofish.gui.GuiManager;
import com.ardelys.ahowtofish.integration.PlaceholderHook;
import com.ardelys.ahowtofish.integration.VaultHook;
import com.ardelys.ahowtofish.integration.WorldGuardHook;
import com.ardelys.ahowtofish.language.LanguageManager;
import com.ardelys.ahowtofish.model.*;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.player.ProfileManager;
import com.ardelys.ahowtofish.progression.ProgressionManager;
import com.ardelys.ahowtofish.quest.QuestManager;
import com.ardelys.ahowtofish.security.SecurityManager;
import com.ardelys.ahowtofish.security.*;
import com.ardelys.ahowtofish.achievement.AchievementManager;
import com.ardelys.ahowtofish.utility.PdcKeys;
import com.ardelys.ahowtofish.hud.HudManager;
import com.ardelys.ahowtofish.npc.NpcManager;
import com.ardelys.ahowtofish.performance.PerformanceManager;
import com.ardelys.ahowtofish.scoreboard.ScoreboardManager;
import com.ardelys.ahowtofish.zone.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;
import java.util.logging.Level;

public class AHowToFishPlugin extends JavaPlugin implements AHowToFishAPI {
    private static AHowToFishPlugin instance;

    private DatabaseManager databaseManager;
    private ProfileDao profileDao;
    private ProfileManager profileManager;
    private LanguageManager languageManager;
    private ProgressionManager progressionManager;

    private FishManager fishManager;
    private RodManager rodManager;
    private BaitManager baitManager;
    private ZoneManager zoneManager;
    private AbilityManager abilityManager;
    private TreasureManager treasureManager;
    private EventManager eventManager;
    private CompetitionManager competitionManager;
    private QuestManager questManager;
    private AchievementManager achievementManager;
    private FishingEngine fishingEngine;
    private ScoreboardManager scoreboardManager;
    private HudManager hudManager;
    private NpcManager npcManager;
    private PerformanceManager performanceManager;

    private SecurityManager securityManager;
    private ItemSecurityManager itemSecurityManager;
    private TransactionManager transactionManager;
    private SellLockManager sellLockManager;
    private FishingRateLimiter fishingRateLimiter;
    private GuiSessionManager guiSessionManager;
    private FishSellService fishSellService;

    private VaultHook vaultHook;
    private WorldGuardHook worldGuardHook;
    private PlaceholderHook placeholderHook;

    public static AHowToFishPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        PdcKeys.init(this);

        saveDefaultConfig();
        checkConfigMigration();
        saveDefaultFile("database.yml");
        saveDefaultFile("messages.yml");
        saveDefaultFile("scoreboard.yml");
        saveDefaultFile("npc.yml");
        saveDefaultFile("quests.yml");
        saveDefaultFile("rods.yml");
        saveDefaultFile("bait.yml");
        saveDefaultFile("upgrades.yml");
        saveDefaultFile("fish.yml");
        saveDefaultFile("rarities.yml");
        saveDefaultFile("zones.yml");
        saveDefaultFile("achievements.yml");
        saveDefaultFile("competitions.yml");
        saveDefaultFile("events.yml");

        File dbFile = new File(getDataFolder(), "database.yml");
        FileConfiguration dbConfig = YamlConfiguration.loadConfiguration(dbFile);
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.init(dbConfig);

        this.profileDao = new ProfileDao(databaseManager, getLogger());
        this.profileManager = new ProfileManager(this, profileDao);

        this.securityManager = new SecurityManager(this);
        this.securityManager.load(getConfig());
        this.itemSecurityManager = new ItemSecurityManager(this, securityManager);
        this.transactionManager = new TransactionManager(this, databaseManager, securityManager);
        this.sellLockManager = new SellLockManager();
        this.fishingRateLimiter = new FishingRateLimiter(securityManager);
        this.guiSessionManager = new GuiSessionManager(securityManager);
        this.fishSellService = new FishSellService(this);

        this.languageManager = new LanguageManager(this, profileManager);
        this.languageManager.load(getConfiguredDefaultLanguage());

        this.vaultHook = new VaultHook(this);
        if (vaultHook.setupEconomy()) {
            getLogger().info("[AHowToFish] Vault integration enabled.");
        } else {
            getLogger().info("[AHowToFish] Vault not found or economy unavailable. Continuing with internal rewards.");
        }

        this.worldGuardHook = new WorldGuardHook(this);
        if (worldGuardHook.setup()) {
            getLogger().info("[AHowToFish] WorldGuard integration enabled.");
        }

        this.progressionManager = new ProgressionManager(this, profileManager, languageManager, vaultHook);
        this.progressionManager.load(getConfig());

        this.fishManager = new FishManager(this);
        this.fishManager.load();

        this.rodManager = new RodManager(this);
        this.rodManager.load();

        this.baitManager = new BaitManager(this);
        this.baitManager.load();

        this.zoneManager = new ZoneManager(this);
        this.zoneManager.load();

        this.abilityManager = new AbilityManager(this);
        this.abilityManager.load();

        this.treasureManager = new TreasureManager(this, vaultHook, progressionManager, languageManager);
        this.treasureManager.load(getConfig());

        this.eventManager = new EventManager(this, languageManager);
        this.eventManager.load();

        this.competitionManager = new CompetitionManager(this, languageManager, vaultHook, profileManager);
        this.competitionManager.load();

        this.questManager = new QuestManager(this, profileManager, progressionManager, vaultHook, languageManager);
        this.questManager.load();

        this.achievementManager = new AchievementManager(this, profileManager, progressionManager, vaultHook, languageManager);
        this.achievementManager.load();

        this.fishingEngine = new FishingEngine(fishManager, rodManager, baitManager, zoneManager, abilityManager, eventManager);

        this.scoreboardManager = new ScoreboardManager(this);
        this.scoreboardManager.load();

        this.hudManager = new HudManager(this);
        this.hudManager.load();

        this.npcManager = new NpcManager(this);
        this.npcManager.load();

        this.performanceManager = new PerformanceManager(this);
        this.performanceManager.load();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderHook = new PlaceholderHook(this, profileManager, progressionManager, zoneManager, competitionManager, profileDao);
            this.placeholderHook.register();
            getLogger().info("[AHowToFish] PlaceholderAPI integration enabled.");
        }

        Bukkit.getPluginManager().registerEvents(new FishingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiManager(), this);
        Bukkit.getPluginManager().registerEvents(this.npcManager, this);

        AHowToFishCommand cmdExecutor = new AHowToFishCommand(this);
        FishingTabCompleter tabCompleter = new FishingTabCompleter(this);

        registerCommand("ahowtofish", cmdExecutor, tabCompleter);
        registerCommand("fishing", cmdExecutor, tabCompleter);
        registerCommand("fish", cmdExecutor, tabCompleter);

        int autoSaveMinutes = getConfig().getInt("general.autosave-minutes", 5);
        long autoSaveTicks = autoSaveMinutes * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            profileManager.saveAll(false);
        }, autoSaveTicks, autoSaveTicks);

        getLogger().info("==========================================");
        getLogger().info("[AHowToFish] Advanced Fishing System Enabled!");
        getLogger().info("[AHowToFish] Environment: " + (securityManager != null && securityManager.isProductionMode() ? "PRODUCTION (Hardened Security)" : "DEVELOPMENT"));
        getLogger().info("[AHowToFish] Author: Ardelys | Version: " + getPluginMeta().getVersion());
        getLogger().info("==========================================");
    }

    private void checkConfigMigration() {
        int currentVersion = getConfig().getInt("config-version", 1);
        if (currentVersion < 2) {
            getLogger().info("[AHowToFish] Migrating config.yml from version " + currentVersion + " to 2...");
            getConfig().set("config-version", 2);
            saveConfig();
            getLogger().info("[AHowToFish] Config migration completed successfully.");
        }
    }

    private void registerCommand(String name, AHowToFishCommand executor, FishingTabCompleter completer) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(completer);
        }
    }

    private void saveDefaultFile(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            try {
                if (getResource(resourcePath) != null) {
                    saveResource(resourcePath, false);
                } else {
                    getLogger().warning("Could not find embedded default resource: " + resourcePath);
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to save default resource: " + resourcePath, e);
            }
        }
    }

    public void reloadAllConfigurations() {
        closeAllOpenCustomGuis();
        reloadConfig();
        if (securityManager != null) {
            securityManager.load(getConfig());
        }
        languageManager.load(getConfiguredDefaultLanguage());
        progressionManager.load(getConfig());
        fishManager.load();
        rodManager.load();
        baitManager.load();
        zoneManager.load();
        abilityManager.load();
        treasureManager.load(getConfig());
        eventManager.load();
        competitionManager.load();
        questManager.load();
        achievementManager.load();
        if (scoreboardManager != null) scoreboardManager.load();
        if (hudManager != null) hudManager.load();
        if (npcManager != null) npcManager.load();
        if (performanceManager != null) performanceManager.load();
    }

    public String getConfiguredDefaultLanguage() {
        FileConfiguration config = getConfig();
        if (config == null) return "en";

        if (config.contains("default-language")) {
            String val = config.getString("default-language");
            if (val != null && !val.isBlank()) {
                return val.trim().toLowerCase(java.util.Locale.ROOT);
            }
        }
        
        if (config.contains("general.default-language")) {
            String val = config.getString("general.default-language");
            if (val != null && !val.isBlank()) {
                return val.trim().toLowerCase(java.util.Locale.ROOT);
            }
        }
        
        if (config.contains("language.default")) {
            String val = config.getString("language.default");
            if (val != null && !val.isBlank()) {
                return val.trim().toLowerCase(java.util.Locale.ROOT);
            }
        }
        
        if (config.contains("language") && config.isString("language")) {
            String val = config.getString("language");
            if (val != null && !val.isBlank()) {
                return val.trim().toLowerCase(java.util.Locale.ROOT);
            }
        }

        return "en";
    }

    public void closeAllOpenCustomGuis() {
        try {
            if (Bukkit.getServer() != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getOpenInventory().getTopInventory().getHolder() instanceof com.ardelys.ahowtofish.gui.CustomGui) {
                        p.closeInventory();
                    }
                }
            }
        } catch (Throwable ignored) {}
        if (guiSessionManager != null) {
            guiSessionManager.clearAll();
        }
    }

    @Override
    public void onDisable() {
        closeAllOpenCustomGuis();
        if (npcManager != null) {
            npcManager.stop();
        }
        if (competitionManager != null) {
            competitionManager.stopCompetition();
        }
        if (eventManager != null) {
            eventManager.stopEvent();
        }
        if (scoreboardManager != null) {
            scoreboardManager.cleanupAllBoards();
        }
        if (hudManager != null) {
            hudManager.stop();
        }
        if (performanceManager != null) {
            performanceManager.stop();
        }
        if (placeholderHook != null) {
            try {
                placeholderHook.unregister();
            } catch (Exception ignored) {}
        }
        Bukkit.getScheduler().cancelTasks(this);
        if (profileManager != null) {
            profileManager.saveAll(false);
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (securityManager != null) {
            securityManager.shutdown();
        }

        getLogger().info("[AHowToFish] Plugin disabled successfully.");
    }

    @Override
    public int getFishingLevel(UUID uuid) {
        FishingProfile p = profileManager.getProfile(uuid);
        return p != null ? p.getLevel() : 1;
    }

    @Override
    public long getFishingXp(UUID uuid) {
        FishingProfile p = profileManager.getProfile(uuid);
        return p != null ? p.getXp() : 0;
    }

    @Override
    public void addFishingXp(Player player, long amount) {
        progressionManager.addXp(player, amount);
    }

    @Override
    public void setFishingLevel(Player player, int level) {
        FishingProfile p = profileManager.getProfile(player);
        if (p != null) {
            p.setLevel(level);
            profileManager.saveProfileAsync(p);
        }
    }

    @Override
    public FishingProfile getProfile(UUID uuid) {
        return profileManager.getProfile(uuid);
    }

    @Override
    public FishingZone getCurrentZone(Player player) {
        return zoneManager.getZoneAt(player.getLocation());
    }

    @Override
    public void registerCustomFish(Fish fish) {
        fishManager.registerFish(fish);
    }

    @Override
    public void registerCustomRarity(FishRarity rarity) {
        fishManager.registerRarity(rarity);
    }

    @Override
    public void registerCustomBait(FishingBait bait) {
        baitManager.registerBait(bait);
    }

    @Override
    public void registerCustomRod(FishingRod rod) {
        rodManager.registerRod(rod);
    }

    @Override
    public boolean giveFish(Player player, String fishId, int amount) {
        Fish fish = fishManager.getFish(fishId);
        if (fish == null || player == null) return false;
        FishRarity rarity = fishManager.getRarity(fish.rarityId());
        if (rarity == null) rarity = new FishRarity("COMMON", "Common", "&f", 1, 1, 1, false, "", "", "");

        FishCatchResult result = new FishCatchResult(
                fish, rarity, (fish.minWeight() + fish.maxWeight()) / 2.0,
                fish.sellPrice(), fish.xpReward(), player.getUniqueId(), player.getName(), System.currentTimeMillis()
        );
        org.bukkit.inventory.ItemStack item = result.buildItemStack();
        item.setAmount(Math.max(1, amount));
        player.getInventory().addItem(item);
        return true;
    }

    @Override
    public boolean giveRod(Player player, String rodId) {
        FishingRod rod = rodManager.getRod(rodId);
        if (rod == null || player == null) return false;
        player.getInventory().addItem(rod.createItemStack());
        return true;
    }

    @Override
    public boolean giveBait(Player player, String baitId, int amount) {
        FishingBait bait = baitManager.getBait(baitId);
        if (bait == null || player == null) return false;
        player.getInventory().addItem(bait.createItemStack(Math.max(1, amount)));
        return true;
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public ProfileDao getProfileDao() { return profileDao; }
    public ProfileManager getProfileManager() { return profileManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public ProgressionManager getProgressionManager() { return progressionManager; }
    public FishManager getFishManager() { return fishManager; }
    public RodManager getRodManager() { return rodManager; }
    public BaitManager getBaitManager() { return baitManager; }
    public ZoneManager getZoneManager() { return zoneManager; }
    public AbilityManager getAbilityManager() { return abilityManager; }
    public TreasureManager getTreasureManager() { return treasureManager; }
    public EventManager getEventManager() { return eventManager; }
    public CompetitionManager getCompetitionManager() { return competitionManager; }
    public QuestManager getQuestManager() { return questManager; }
    public AchievementManager getAchievementManager() { return achievementManager; }
    public FishingEngine getFishingEngine() { return fishingEngine; }
    public VaultHook getVaultHook() { return vaultHook; }
    public WorldGuardHook getWorldGuardHook() { return worldGuardHook; }
    public SecurityManager getSecurityManager() { return securityManager; }
    public ItemSecurityManager getItemSecurityManager() { return itemSecurityManager; }
    public TransactionManager getTransactionManager() { return transactionManager; }
    public SellLockManager getSellLockManager() { return sellLockManager; }
    public FishingRateLimiter getFishingRateLimiter() { return fishingRateLimiter; }
    public GuiSessionManager getGuiSessionManager() { return guiSessionManager; }
    public FishSellService getFishSellService() { return fishSellService; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public HudManager getHudManager() { return hudManager; }
    public NpcManager getNpcManager() { return npcManager; }
    public PerformanceManager getPerformanceManager() { return performanceManager; }
}
