package com.ardelys.ahowtofish.achievement;

import com.ardelys.ahowtofish.integration.VaultHook;
import com.ardelys.ahowtofish.language.LanguageManager;
import com.ardelys.ahowtofish.language.MessageKey;
import com.ardelys.ahowtofish.model.FishCatchResult;
import com.ardelys.ahowtofish.model.FishingAchievement;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.player.PlayerStats;
import com.ardelys.ahowtofish.player.ProfileManager;
import com.ardelys.ahowtofish.progression.ProgressionManager;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class AchievementManager {
    private final JavaPlugin plugin;
    private final ProfileManager profileManager;
    private final ProgressionManager progressionManager;
    private final VaultHook vaultHook;
    private final LanguageManager languageManager;

    private final Map<String, FishingAchievement> achievementRegistry = new ConcurrentHashMap<>();

    public AchievementManager(JavaPlugin plugin,
                              ProfileManager profileManager,
                              ProgressionManager progressionManager,
                              VaultHook vaultHook,
                              LanguageManager languageManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.progressionManager = progressionManager;
        this.vaultHook = vaultHook;
        this.languageManager = languageManager;
    }

    public void load() {
        achievementRegistry.clear();
        File file = new File(plugin.getDataFolder(), "achievements.yml");
        if (!file.exists()) {
            plugin.saveResource("achievements.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("achievements");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    String name = section.getString(key + ".name", key);
                    String desc = section.getString(key + ".description", "");
                    String typeStr = section.getString(key + ".type", "TOTAL_FISH_CAUGHT").toUpperCase();
                    FishingAchievement.AchievementType type = FishingAchievement.AchievementType.valueOf(typeStr);
                    double reqVal = section.getDouble(key + ".required-value", 1.0);
                    String target = section.getString(key + ".target", "");
                    long xpReward = section.getLong(key + ".rewards.xp", 100);
                    double moneyReward = section.getDouble(key + ".rewards.money", 250.0);
                    List<String> cmdRewards = section.getStringList(key + ".rewards.commands");
                    String title = section.getString(key + ".rewards.title", "");

                    FishingAchievement ach = new FishingAchievement(
                            key.toLowerCase(),
                            name,
                            desc,
                            type,
                            reqVal,
                            target,
                            xpReward,
                            moneyReward,
                            cmdRewards,
                            title
                    );
                    achievementRegistry.put(ach.id(), ach);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[AHowToFish] Failed to load achievement '" + key + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("[AHowToFish] Loaded " + achievementRegistry.size() + " fishing achievement(s).");
    }

    public void checkAchievements(Player player, FishingProfile profile, FishCatchResult lastCatch) {
        if (player == null || profile == null) return;
        PlayerStats stats = profile.getStats();

        for (FishingAchievement ach : achievementRegistry.values()) {
            if (profile.hasAchievement(ach.id())) continue;

            boolean unlocked = false;
            switch (ach.type()) {
                case TOTAL_FISH_CAUGHT -> unlocked = stats.getTotalFishCaught() >= ach.requiredValue();
                case TOTAL_WEIGHT_CAUGHT -> unlocked = stats.getTotalFishWeight() >= ach.requiredValue();
                case REACH_LEVEL -> unlocked = profile.getLevel() >= (int) ach.requiredValue();
                case DISCOVER_FISH_COUNT -> unlocked = profile.getCollection().getDiscoveredCount() >= (int) ach.requiredValue();
                case MAX_WEIGHT_CAUGHT -> unlocked = stats.getLargestFishWeight() >= ach.requiredValue();
                case TOTAL_MONEY_EARNED -> unlocked = stats.getTotalMoneyEarned() >= ach.requiredValue();
                case CATCH_RARITY -> {
                    if (lastCatch != null && ach.targetRequirement().equalsIgnoreCase(lastCatch.rarity().id())) {
                        unlocked = true;
                    }
                }
            }

            if (unlocked) {
                unlockAchievement(player, profile, ach);
            }
        }
    }

    private void unlockAchievement(Player player, FishingProfile profile, FishingAchievement ach) {
        synchronized (profile) {
            if (profile.hasAchievement(ach.id())) {
                return;
            }
            profile.unlockAchievement(ach.id());
        }

        SoundParticleUtil.playSound(player, "UI_TOAST_CHALLENGE_COMPLETE", 1.0f, 1.0f);
        SoundParticleUtil.spawnParticle(player.getLocation().add(0, 1.5, 0), "TOTEM_OF_UNDYING", 30, 0.5, 0.5, 0.5, 0.1);

        String localizedTitle = languageManager.getMessage(player, "achievement.unlocked_title", "&6&lACHIEVEMENT UNLOCKED!");
        String localizedName = getLocalizedAchievementName(player, ach);

        TextUtil.sendTitle(
                player,
                localizedTitle,
                "&e" + localizedName,
                10, 60, 20
        );
        languageManager.sendMessage(player, MessageKey.ACHIEVEMENT_UNLOCKED,
                "achievement", localizedName,
                "xp", String.valueOf(ach.xpReward()),
                "money", TextUtil.formatDecimal(ach.moneyReward()));

        if (ach.xpReward() > 0) {
            progressionManager.addXp(player, ach.xpReward());
        }
        if (ach.moneyReward() > 0 && vaultHook != null && vaultHook.isAvailable()) {
            vaultHook.deposit(player, ach.moneyReward());
        }
        if (!ach.commandRewards().isEmpty()) {
            for (String cmd : ach.commandRewards()) {
                if (plugin instanceof com.ardelys.ahowtofish.AHowToFishPlugin ahf && ahf.getSecurityManager() != null) {
                    ahf.getSecurityManager().executeConsoleReward(player, cmd);
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
                }
            }
        }

        profileManager.saveProfileAsync(profile);
    }

    public String getLocalizedAchievementName(Player player, FishingAchievement ach) {
        if (ach == null) return "";
        return languageManager.getMessage(player, "achievement_translations." + ach.id() + ".name", ach.name());
    }

    public String getLocalizedAchievementDesc(Player player, FishingAchievement ach) {
        if (ach == null) return "";
        return languageManager.getMessage(player, "achievement_translations." + ach.id() + ".desc", ach.description());
    }

    public Collection<FishingAchievement> getAllAchievements() {
        return Collections.unmodifiableCollection(achievementRegistry.values());
    }

    public FishingAchievement getAchievement(String id) {
        return achievementRegistry.get(id.toLowerCase());
    }
}
