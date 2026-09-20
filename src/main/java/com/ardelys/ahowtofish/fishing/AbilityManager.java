package com.ardelys.ahowtofish.fishing;

import com.ardelys.ahowtofish.model.FishingUpgrade;
import com.ardelys.ahowtofish.player.FishingProfile;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class AbilityManager {
    private final JavaPlugin plugin;
    private final Map<String, FishingUpgrade> upgrades = new ConcurrentHashMap<>();

    public AbilityManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        upgrades.clear();
        File file = new File(plugin.getDataFolder(), "upgrades.yml");
        if (!file.exists()) {
            plugin.saveResource("upgrades.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("upgrades");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    String name = section.getString(key + ".name", key);
                    String desc = section.getString(key + ".description", "");
                    int maxLvl = section.getInt(key + ".max-level", 5);
                    double baseCost = section.getDouble(key + ".base-cost", 500.0);
                    double costMul = section.getDouble(key + ".cost-multiplier", 1.5);
                    double effectPerLvl = section.getDouble(key + ".effect-per-level", 0.05);
                    String iconMat = section.getString(key + ".icon", "BOOK");
                    Material mat = Material.matchMaterial(iconMat);
                    if (mat == null) mat = Material.BOOK;
                    String perm = section.getString(key + ".permission", "");

                    FishingUpgrade upgrade = new FishingUpgrade(
                            key.toLowerCase(),
                            name,
                            desc,
                            maxLvl,
                            baseCost,
                            costMul,
                            effectPerLvl,
                            mat,
                            perm
                    );
                    upgrades.put(upgrade.id(), upgrade);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[AHowToFish] Failed to load upgrade '" + key + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("[AHowToFish] Loaded " + upgrades.size() + " fishing upgrades.");
    }

    public FishingUpgrade getUpgrade(String id) {
        return upgrades.get(id.toLowerCase());
    }

    public Collection<FishingUpgrade> getAllUpgrades() {
        return Collections.unmodifiableCollection(upgrades.values());
    }

    public double getPlayerUpgradeEffect(FishingProfile profile, String upgradeId) {
        if (profile == null) return 0.0;
        int level = profile.getUpgradeLevel(upgradeId);
        if (level <= 0) return 0.0;
        FishingUpgrade upg = upgrades.get(upgradeId.toLowerCase());
        return upg != null ? upg.getTotalEffect(level) : 0.0;
    }
}
