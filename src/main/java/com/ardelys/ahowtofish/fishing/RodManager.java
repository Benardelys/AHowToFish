package com.ardelys.ahowtofish.fishing;

import com.ardelys.ahowtofish.model.FishingRod;
import com.ardelys.ahowtofish.utility.PdcKeys;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class RodManager {
    private final JavaPlugin plugin;
    private final Map<String, FishingRod> rodRegistry = new ConcurrentHashMap<>();

    public RodManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        rodRegistry.clear();
        File file = new File(plugin.getDataFolder(), "rods.yml");
        if (!file.exists()) {
            plugin.saveResource("rods.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("rods");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    String displayName = section.getString(key + ".display-name", key);
                    String matStr = section.getString(key + ".material", "FISHING_ROD");
                    Material mat = Material.matchMaterial(matStr);
                    if (mat == null) mat = Material.FISHING_ROD;

                    Integer cmd = section.contains(key + ".custom-model-data") ? section.getInt(key + ".custom-model-data") : null;
                    List<String> lore = section.getStringList(key + ".lore");
                    int durability = section.getInt(key + ".durability", 64);
                    double luck = section.getDouble(key + ".luck", 0.0);
                    double xpMul = section.getDouble(key + ".xp-multiplier", 1.0);
                    double moneyMul = section.getDouble(key + ".money-multiplier", 1.0);
                    double rareBonus = section.getDouble(key + ".rare-chance", 0.0);
                    double legBonus = section.getDouble(key + ".legendary-chance", 0.0);
                    double doubleCatch = section.getDouble(key + ".double-catch-chance", 0.0);
                    int reqLevel = section.getInt(key + ".required-level", 1);
                    String permission = section.getString(key + ".permission", "");

                    FishingRod rod = new FishingRod(
                            key.toLowerCase(),
                            displayName,
                            mat,
                            cmd,
                            lore,
                            durability,
                            luck,
                            xpMul,
                            moneyMul,
                            rareBonus,
                            legBonus,
                            doubleCatch,
                            reqLevel,
                            permission
                    );
                    rodRegistry.put(rod.id(), rod);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[AHowToFish] Failed to load rod '" + key + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("[AHowToFish] Loaded " + rodRegistry.size() + " custom fishing rod(s).");
    }

    public FishingRod getRod(String id) {
        return rodRegistry.get(id.toLowerCase());
    }

    public FishingRod getRodFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String rodId = item.getItemMeta().getPersistentDataContainer().get(PdcKeys.ROD_ID, PersistentDataType.STRING);
        if (rodId != null) {
            return rodRegistry.get(rodId.toLowerCase());
        }
        return null;
    }

    public Collection<FishingRod> getAllRods() {
        return Collections.unmodifiableCollection(rodRegistry.values());
    }

    public void registerRod(FishingRod rod) {
        rodRegistry.put(rod.id().toLowerCase(), rod);
    }
}
