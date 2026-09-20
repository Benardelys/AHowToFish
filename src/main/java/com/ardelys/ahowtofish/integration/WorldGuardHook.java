package com.ardelys.ahowtofish.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldGuardHook {
    private final JavaPlugin plugin;
    private boolean available = false;

    public WorldGuardHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        available = (Bukkit.getPluginManager().getPlugin("WorldGuard") != null);
        return available;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean canFish(Player player, Location location) {
        if (!available || location == null) return true;
        try {
            
            com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location);
            return true;
        } catch (Throwable t) {
            return true;
        }
    }
}
