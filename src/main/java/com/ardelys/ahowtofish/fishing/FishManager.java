package com.ardelys.ahowtofish.fishing;

import com.ardelys.ahowtofish.model.Fish;
import com.ardelys.ahowtofish.model.FishRarity;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class FishManager {
    private final JavaPlugin plugin;
    private final Map<String, FishRarity> rarities = new ConcurrentHashMap<>();
    private final Map<String, Fish> fishRegistry = new ConcurrentHashMap<>();

    public FishManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        loadRarities();
        loadFish();
    }

    private void loadRarities() {
        rarities.clear();
        File file = new File(plugin.getDataFolder(), "rarities.yml");
        if (!file.exists()) {
            plugin.saveResource("rarities.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("rarities");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    String displayName = section.getString(key + ".display-name", key);
                    String color = section.getString(key + ".color", "&f");
                    double chanceMul = section.getDouble(key + ".chance-multiplier", 1.0);
                    double xpMul = section.getDouble(key + ".xp-multiplier", 1.0);
                    double valueMul = section.getDouble(key + ".value-multiplier", 1.0);
                    boolean broadcast = section.getBoolean(key + ".broadcast", false);
                    String sound = section.getString(key + ".sound", "ENTITY_ITEM_PICKUP");
                    String particle = section.getString(key + ".particle", "WATER_SPLASH");
                    String permission = section.getString(key + ".permission", "");

                    FishRarity rarity = new FishRarity(
                            key.toUpperCase(),
                            displayName,
                            color,
                            chanceMul,
                            xpMul,
                            valueMul,
                            broadcast,
                            sound,
                            particle,
                            permission
                    );
                    rarities.put(rarity.id(), rarity);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[AHowToFish] Failed to load rarity '" + key + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("[AHowToFish] Loaded " + rarities.size() + " fish rarities.");
    }

    private void loadFish() {
        fishRegistry.clear();
        File file = new File(plugin.getDataFolder(), "fish.yml");
        if (!file.exists()) {
            plugin.saveResource("fish.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("fish");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    String displayName = section.getString(key + ".display-name", key);
                    String matStr = section.getString(key + ".material", "COD");
                    Material material = Material.matchMaterial(matStr);
                    if (material == null) {
                        plugin.getLogger().warning("[AHowToFish] Invalid material '" + matStr + "' for fish " + key + ". Defaulting to COD.");
                        material = Material.COD;
                    }

                    Integer customModelData = section.contains(key + ".custom-model-data") ? section.getInt(key + ".custom-model-data") : null;
                    List<String> lore = section.getStringList(key + ".lore");
                    String rarityId = section.getString(key + ".rarity", "COMMON").toUpperCase();

                    double minWeight = section.getDouble(key + ".weight.min", 0.5);
                    double maxWeight = section.getDouble(key + ".weight.max", 5.0);
                    double sellPrice = section.getDouble(key + ".sell-price", 10.0);
                    double baseValue = section.getDouble(key + ".base-value", sellPrice);
                    long xpReward = section.getLong(key + ".xp", 15);
                    int minLevel = section.getInt(key + ".min-level", 1);
                    int maxLevel = section.getInt(key + ".max-level", 0);
                    String reqZone = section.getString(key + ".required-zone", "");
                    String reqBait = section.getString(key + ".required-bait", "");
                    double chance = section.getDouble(key + ".chance", 10.0);
                    String sound = section.getString(key + ".sound", "");
                    String particle = section.getString(key + ".particle", "");
                    List<String> commands = section.getStringList(key + ".commands");

                    Map<String, Object> specialProps = new HashMap<>();
                    ConfigurationSection propsSec = section.getConfigurationSection(key + ".special-properties");
                    if (propsSec != null) {
                        for (String propKey : propsSec.getKeys(false)) {
                            specialProps.put(propKey, propsSec.get(propKey));
                        }
                    }

                    Fish fish = new Fish(
                            key.toLowerCase(),
                            displayName,
                            material,
                            customModelData,
                            lore,
                            rarityId,
                            minWeight,
                            maxWeight,
                            sellPrice,
                            baseValue,
                            xpReward,
                            minLevel,
                            maxLevel,
                            reqZone,
                            reqBait,
                            chance,
                            sound,
                            particle,
                            commands,
                            specialProps
                    );
                    fishRegistry.put(fish.id(), fish);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[AHowToFish] Failed to load fish '" + key + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("[AHowToFish] Loaded " + fishRegistry.size() + " custom fish.");
    }

    public Fish getFish(String id) {
        return fishRegistry.get(id.toLowerCase());
    }

    public FishRarity getRarity(String id) {
        return rarities.get(id.toUpperCase());
    }

    public Collection<Fish> getAllFish() {
        return Collections.unmodifiableCollection(fishRegistry.values());
    }

    public Collection<FishRarity> getAllRarities() {
        return Collections.unmodifiableCollection(rarities.values());
    }

    public void registerFish(Fish fish) {
        fishRegistry.put(fish.id().toLowerCase(), fish);
    }

    public void registerRarity(FishRarity rarity) {
        rarities.put(rarity.id().toUpperCase(), rarity);
    }
}
