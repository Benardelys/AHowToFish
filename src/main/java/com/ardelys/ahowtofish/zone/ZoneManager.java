package com.ardelys.ahowtofish.zone;

import com.ardelys.ahowtofish.model.FishingZone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ZoneManager {
    private final JavaPlugin plugin;
    private final Map<String, FishingZone> zones = new ConcurrentHashMap<>();
    private final Map<String, List<FishingZone>> zonesByWorld = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos1Map = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos2Map = new ConcurrentHashMap<>();
    private File zoneFile;
    private FileConfiguration zoneConfig;

    public ZoneManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        zones.clear();
        zonesByWorld.clear();
        zoneFile = new File(plugin.getDataFolder(), "zones.yml");
        if (!zoneFile.exists()) {
            plugin.saveResource("zones.yml", false);
        }
        zoneConfig = YamlConfiguration.loadConfiguration(zoneFile);

        ConfigurationSection section = zoneConfig.getConfigurationSection("zones");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    String displayName = section.getString(key + ".display-name", key);
                    String worldName = section.getString(key + ".world", "world");
                    double minX = section.getDouble(key + ".bounds.min-x", 0);
                    double minY = section.getDouble(key + ".bounds.min-y", 0);
                    double minZ = section.getDouble(key + ".bounds.min-z", 0);
                    double maxX = section.getDouble(key + ".bounds.max-x", 0);
                    double maxY = section.getDouble(key + ".bounds.max-y", 256);
                    double maxZ = section.getDouble(key + ".bounds.max-z", 0);
                    int reqLevel = section.getInt(key + ".required-level", 1);
                    String reqPerm = section.getString(key + ".required-permission", "");
                    double xpMul = section.getDouble(key + ".xp-multiplier", 1.0);
                    double moneyMul = section.getDouble(key + ".money-multiplier", 1.0);
                    List<String> availableFish = section.getStringList(key + ".available-fish");
                    String wgRegion = section.getString(key + ".worldguard-region", "");

                    Map<String, Double> fishChanceMultipliers = new HashMap<>();
                    ConfigurationSection mulSec = section.getConfigurationSection(key + ".fish-multipliers");
                    if (mulSec != null) {
                        for (String fId : mulSec.getKeys(false)) {
                            fishChanceMultipliers.put(fId.toLowerCase(), mulSec.getDouble(fId, 1.0));
                        }
                    }

                    FishingZone zone = new FishingZone(
                            key.toLowerCase(),
                            displayName,
                            worldName,
                            minX, minY, minZ, maxX, maxY, maxZ,
                            reqLevel, reqPerm, xpMul, moneyMul,
                            availableFish, fishChanceMultipliers, wgRegion
                    );
                    zones.put(zone.id(), zone);
                    zonesByWorld.computeIfAbsent(zone.worldName().toLowerCase(), k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(zone);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[AHowToFish] Failed to load zone '" + key + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("[AHowToFish] Loaded " + zones.size() + " fishing zone(s).");
    }

    public FishingZone getZoneAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        List<FishingZone> worldZones = zonesByWorld.get(location.getWorld().getName().toLowerCase());
        if (worldZones == null || worldZones.isEmpty()) {
            return null;
        }
        for (FishingZone zone : worldZones) {
            if (zone.contains(location)) {
                return zone;
            }
        }
        return null;
    }

    public FishingZone getZone(String id) {
        return zones.get(id.toLowerCase());
    }

    public Collection<FishingZone> getAllZones() {
        return Collections.unmodifiableCollection(zones.values());
    }

    public void setPos1(Player player, Location loc) {
        pos1Map.put(player.getUniqueId(), loc);
    }

    public void setPos2(Player player, Location loc) {
        pos2Map.put(player.getUniqueId(), loc);
    }

    public Location getPos1(Player player) {
        return pos1Map.get(player.getUniqueId());
    }

    public Location getPos2(Player player) {
        return pos2Map.get(player.getUniqueId());
    }

    public boolean createZone(String id, String displayName, Location loc1, Location loc2, int reqLevel) {
        if (loc1.getWorld() == null || loc2.getWorld() == null || !loc1.getWorld().equals(loc2.getWorld())) {
            return false;
        }

        double minX = Math.min(loc1.getX(), loc2.getX());
        double minY = Math.min(loc1.getY(), loc2.getY());
        double minZ = Math.min(loc1.getZ(), loc2.getZ());
        double maxX = Math.max(loc1.getX(), loc2.getX());
        double maxY = Math.max(loc1.getY(), loc2.getY());
        double maxZ = Math.max(loc1.getZ(), loc2.getZ());

        FishingZone zone = new FishingZone(
                id.toLowerCase(),
                displayName,
                loc1.getWorld().getName(),
                minX, minY, minZ, maxX, maxY, maxZ,
                reqLevel, "", 1.0, 1.0,
                new ArrayList<>(), new HashMap<>(), ""
        );

        zones.put(zone.id(), zone);
        zonesByWorld.computeIfAbsent(zone.worldName().toLowerCase(), k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(zone);
        saveZoneToConfig(zone);
        return true;
    }

    public boolean deleteZone(String id) {
        FishingZone removed = zones.remove(id.toLowerCase());
        if (removed != null) {
            List<FishingZone> worldList = zonesByWorld.get(removed.worldName().toLowerCase());
            if (worldList != null) {
                worldList.remove(removed);
            }
            zoneConfig.set("zones." + removed.id(), null);
            saveConfigFile();
            return true;
        }
        return false;
    }

    private void saveZoneToConfig(FishingZone zone) {
        String path = "zones." + zone.id();
        zoneConfig.set(path + ".display-name", zone.displayName());
        zoneConfig.set(path + ".world", zone.worldName());
        zoneConfig.set(path + ".bounds.min-x", zone.minX());
        zoneConfig.set(path + ".bounds.min-y", zone.minY());
        zoneConfig.set(path + ".bounds.min-z", zone.minZ());
        zoneConfig.set(path + ".bounds.max-x", zone.maxX());
        zoneConfig.set(path + ".bounds.max-y", zone.maxY());
        zoneConfig.set(path + ".bounds.max-z", zone.maxZ());
        zoneConfig.set(path + ".required-level", zone.requiredLevel());
        zoneConfig.set(path + ".required-permission", zone.requiredPermission());
        zoneConfig.set(path + ".xp-multiplier", zone.xpMultiplier());
        zoneConfig.set(path + ".money-multiplier", zone.moneyMultiplier());
        saveConfigFile();
    }

    private void saveConfigFile() {
        try {
            zoneConfig.save(zoneFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[AHowToFish] Could not save zones.yml!", e);
        }
    }
}
