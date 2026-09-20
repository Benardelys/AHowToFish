package com.ardelys.ahowtofish.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

@SuppressWarnings({"deprecation", "removal"})
public final class SoundParticleUtil {
    private SoundParticleUtil() {}

    public static void playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null || soundName == null || soundName.isBlank() || soundName.equalsIgnoreCase("none")) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            
        }
    }

    public static void playSound(Location location, String soundName, float volume, float pitch) {
        if (location == null || location.getWorld() == null || soundName == null || soundName.isBlank() || soundName.equalsIgnoreCase("none")) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            location.getWorld().playSound(location, sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static void spawnParticle(Location location, String particleName, int count, double offsetX, double offsetY, double offsetZ, double speed) {
        if (location == null || location.getWorld() == null || particleName == null || particleName.isBlank() || particleName.equalsIgnoreCase("none")) {
            return;
        }
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
