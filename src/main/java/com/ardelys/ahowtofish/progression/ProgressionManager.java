package com.ardelys.ahowtofish.progression;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.api.events.FishingLevelUpEvent;
import com.ardelys.ahowtofish.integration.VaultHook;
import com.ardelys.ahowtofish.language.LanguageManager;
import com.ardelys.ahowtofish.language.MessageKey;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.player.ProfileManager;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class ProgressionManager {
    private final AHowToFishPlugin plugin;
    private final ProfileManager profileManager;
    private final LanguageManager languageManager;
    private final VaultHook vaultHook;

    private int maxLevel = 100;
    private long baseXp = 100;
    private double xpMultiplier = 1.25;
    private String levelUpSound = "ENTITY_PLAYER_LEVELUP";
    private String levelUpParticle = "TOTEM_OF_UNDYING";
    private final Map<Integer, List<String>> levelCommands = new HashMap<>();
    private final Map<Integer, Double> levelMoneyRewards = new HashMap<>();

    public ProgressionManager(AHowToFishPlugin plugin, ProfileManager profileManager, LanguageManager languageManager, VaultHook vaultHook) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.languageManager = languageManager;
        this.vaultHook = vaultHook;
    }

    public void load(FileConfiguration config) {
        this.maxLevel = config.getInt("progression.max-level", 100);
        this.baseXp = config.getLong("progression.base-xp", 100);
        this.xpMultiplier = config.getDouble("progression.xp-multiplier", 1.25);
        this.levelUpSound = config.getString("progression.sound", "ENTITY_PLAYER_LEVELUP");
        this.levelUpParticle = config.getString("progression.particle", "TOTEM_OF_UNDYING");

        levelCommands.clear();
        levelMoneyRewards.clear();

        ConfigurationSection rewardsSec = config.getConfigurationSection("progression.rewards");
        if (rewardsSec != null) {
            for (String key : rewardsSec.getKeys(false)) {
                try {
                    int lvl = Integer.parseInt(key);
                    List<String> cmds = rewardsSec.getStringList(key + ".commands");
                    double money = rewardsSec.getDouble(key + ".money", 0.0);
                    if (!cmds.isEmpty()) levelCommands.put(lvl, cmds);
                    if (money > 0) levelMoneyRewards.put(lvl, money);
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public long getRequiredXpForLevel(int level) {
        if (level <= 1) return 0;
        if (level > maxLevel) return Long.MAX_VALUE;
        return (long) (baseXp * Math.pow(level - 1, xpMultiplier));
    }

    public double getLevelProgressPercent(FishingProfile profile) {
        int currentLevel = profile.getLevel();
        if (currentLevel >= maxLevel) return 100.0;

        long currentLvlReq = getRequiredXpForLevel(currentLevel);
        long nextLvlReq = getRequiredXpForLevel(currentLevel + 1);
        long currentXp = profile.getXp();

        if (currentXp <= currentLvlReq) return 0.0;
        if (currentXp >= nextLvlReq) return 100.0;

        long diff = nextLvlReq - currentLvlReq;
        long playerProgress = currentXp - currentLvlReq;
        return Math.min(100.0, Math.max(0.0, ((double) playerProgress / diff) * 100.0));
    }

    public void addXp(Player player, long amount) {
        if (player == null || amount <= 0) return;
        FishingProfile profile = profileManager.getProfile(player);
        if (profile == null) return;

        profile.addXp(amount);
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().onXpEarned(player, profile, amount);
        }
        checkLevelUp(player, profile);
    }

    public void checkLevelUp(Player player, FishingProfile profile) {
        int currentLevel = profile.getLevel();
        if (currentLevel >= maxLevel) return;

        long currentXp = profile.getXp();
        int newLevel = currentLevel;

        while (newLevel < maxLevel && currentXp >= getRequiredXpForLevel(newLevel + 1)) {
            newLevel++;
        }

        if (newLevel > currentLevel) {
            int oldLevel = currentLevel;
            profile.setLevel(newLevel);

            if (plugin.getQuestManager() != null) {
                plugin.getQuestManager().onLevelUp(player, profile, newLevel);
            }

            FishingLevelUpEvent event = new FishingLevelUpEvent(player, profile, oldLevel, newLevel);
            Bukkit.getPluginManager().callEvent(event);

            SoundParticleUtil.playSound(player, levelUpSound, 1.0f, 1.0f);
            SoundParticleUtil.spawnParticle(player.getLocation().add(0, 1.5, 0), levelUpParticle, 30, 0.5, 0.5, 0.5, 0.1);

            String title = languageManager.getMessage(player, MessageKey.LEVEL_UP_TITLE, "level", newLevel);
            String subtitle = languageManager.getMessage(player, MessageKey.LEVEL_UP_SUBTITLE, "level", newLevel);
            TextUtil.sendTitle(player, title, subtitle, 10, 50, 20);

            languageManager.sendMessage(player, MessageKey.LEVEL_UP_MESSAGE, "level", newLevel, "old_level", oldLevel);

            for (int l = oldLevel + 1; l <= newLevel; l++) {
                if (levelMoneyRewards.containsKey(l) && vaultHook != null && vaultHook.isAvailable()) {
                    double reward = levelMoneyRewards.get(l);
                    vaultHook.deposit(player, reward);
                }
                if (levelCommands.containsKey(l)) {
                    for (String cmd : levelCommands.get(l)) {
                        String finalCmd = cmd.replace("{level}", String.valueOf(l));
                        if (plugin != null && plugin.getSecurityManager() != null) {
                            plugin.getSecurityManager().executeConsoleReward(player, finalCmd);
                        } else {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd.replace("{player}", player.getName()));
                        }
                    }
                }
            }

            profileManager.saveProfileAsync(profile);
        }
    }

    public int getMaxLevel() {
        return maxLevel;
    }
}
