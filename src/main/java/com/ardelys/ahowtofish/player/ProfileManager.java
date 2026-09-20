package com.ardelys.ahowtofish.player;

import com.ardelys.ahowtofish.database.dao.ProfileDao;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ProfileManager {
    private final Plugin plugin;
    private final ProfileDao profileDao;
    private final Map<UUID, FishingProfile> profileCache = new ConcurrentHashMap<>();

    public ProfileManager(Plugin plugin, ProfileDao profileDao) {
        this.plugin = plugin;
        this.profileDao = profileDao;
    }

    public FishingProfile getProfile(UUID uuid) {
        if (uuid == null) return null;
        return profileCache.get(uuid);
    }

    public FishingProfile getProfile(Player player) {
        if (player == null) return null;
        return profileCache.get(player.getUniqueId());
    }

    public CompletableFuture<FishingProfile> loadProfileAsync(UUID uuid, String name) {
        FishingProfile loadingProfile = new FishingProfile(uuid, name);
        loadingProfile.setState(ProfileState.LOADING);
        profileCache.put(uuid, loadingProfile);

        return CompletableFuture.supplyAsync(() -> {
            try {
                FishingProfile loaded = profileDao.loadProfile(uuid, name);
                loaded.setState(ProfileState.ACTIVE);
                profileCache.put(uuid, loaded);
                return loaded;
            } catch (Exception e) {
                loadingProfile.setState(ProfileState.FAILED);
                plugin.getLogger().log(Level.SEVERE, "[AHowToFish] Critical error loading profile for " + name + " (" + uuid + ")", e);
                return loadingProfile;
            }
        });
    }

    public void saveProfileAsync(FishingProfile profile) {
        if (profile == null || profile.getState() == ProfileState.LOADING || profile.getState() == ProfileState.FAILED) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                profileDao.saveProfile(profile);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[AHowToFish] Error saving profile async for " + profile.getPlayerName(), e);
            }
        });
    }

    public void saveProfileSync(FishingProfile profile) {
        if (profile == null || profile.getState() == ProfileState.LOADING || profile.getState() == ProfileState.FAILED) {
            return;
        }
        profileDao.saveProfile(profile);
    }

    public void unloadProfile(UUID uuid) {
        if (uuid == null) return;
        FishingProfile profile = profileCache.remove(uuid);
        if (profile != null) {
            profile.setState(ProfileState.SAVING);
            if (profile.isDirty()) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        profileDao.saveProfile(profile);
                        profile.setState(ProfileState.SAVED);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "[AHowToFish] Failed to persist profile during unload for " + profile.getPlayerName(), e);
                    }
                });
            }
        }
    }

    public int getDirtyProfileCount() {
        int count = 0;
        for (FishingProfile p : profileCache.values()) {
            if (p.isDirty()) count++;
        }
        return count;
    }

    public void saveAll(boolean async) {
        Runnable saveTask = () -> {
            for (FishingProfile profile : profileCache.values()) {
                if (profile.isDirty() && profile.getState() == ProfileState.ACTIVE) {
                    try {
                        profileDao.saveProfile(profile);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "[AHowToFish] Failed to save profile for " + profile.getPlayerName(), e);
                    }
                }
            }
        };

        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, saveTask);
        } else {
            saveTask.run();
        }
    }

    public Collection<FishingProfile> getCachedProfiles() {
        return Collections.unmodifiableCollection(profileCache.values());
    }
}