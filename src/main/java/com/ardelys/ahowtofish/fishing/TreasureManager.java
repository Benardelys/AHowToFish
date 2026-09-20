package com.ardelys.ahowtofish.fishing;

import com.ardelys.ahowtofish.integration.VaultHook;
import com.ardelys.ahowtofish.language.LanguageManager;
import com.ardelys.ahowtofish.language.MessageKey;
import com.ardelys.ahowtofish.progression.ProgressionManager;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TreasureManager {
    private final JavaPlugin plugin;
    private final VaultHook vaultHook;
    private final ProgressionManager progressionManager;
    private final LanguageManager languageManager;

    private double baseChance = 5.0; 
    private String sound = "BLOCK_CHEST_OPEN";
    private String particle = "TOTEM_OF_UNDYING";

    public record TreasureReward(String type, String value, double amount, double chance, String displayName) {}
    private final List<TreasureReward> rewards = new ArrayList<>();

    public TreasureManager(JavaPlugin plugin, VaultHook vaultHook, ProgressionManager progressionManager, LanguageManager languageManager) {
        this.plugin = plugin;
        this.vaultHook = vaultHook;
        this.progressionManager = progressionManager;
        this.languageManager = languageManager;
    }

    public void load(FileConfiguration config) {
        rewards.clear();
        this.baseChance = config.getDouble("treasure.chance", 5.0);
        this.sound = config.getString("treasure.sound", "BLOCK_CHEST_OPEN");
        this.particle = config.getString("treasure.particle", "TOTEM_OF_UNDYING");

        ConfigurationSection section = config.getConfigurationSection("treasure.rewards");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String type = section.getString(key + ".type", "MONEY");
                String value = section.getString(key + ".value", "");
                double amount = section.getDouble(key + ".amount", 100);
                double chance = section.getDouble(key + ".chance", 10.0);
                String display = section.getString(key + ".display", key);
                rewards.add(new TreasureReward(type.toUpperCase(), value, amount, chance, display));
            }
        }
    }

    public double getBaseChance() {
        return baseChance;
    }

    public boolean rollTreasure(Player player, double chanceBonus) {
        double roll = ThreadLocalRandom.current().nextDouble(0, 100);
        if (roll <= (baseChance + chanceBonus)) {
            dispatchReward(player);
            return true;
        }
        return false;
    }

    private void dispatchReward(Player player) {
        if (rewards.isEmpty()) return;

        double totalWeight = rewards.stream().mapToDouble(TreasureReward::chance).sum();
        double random = ThreadLocalRandom.current().nextDouble(0, totalWeight);
        double cumulative = 0.0;
        TreasureReward chosen = rewards.get(0);

        for (TreasureReward reward : rewards) {
            cumulative += reward.chance();
            if (random <= cumulative) {
                chosen = reward;
                break;
            }
        }

        switch (chosen.type()) {
            case "MONEY" -> {
                if (vaultHook != null && vaultHook.isAvailable()) {
                    vaultHook.deposit(player, chosen.amount());
                }
            }
            case "XP" -> {
                progressionManager.addXp(player, (long) chosen.amount());
            }
            case "COMMAND" -> {
                if (plugin instanceof com.ardelys.ahowtofish.AHowToFishPlugin ahf && ahf.getSecurityManager() != null) {
                    ahf.getSecurityManager().executeConsoleReward(player, chosen.value());
                } else {
                    String cmd = chosen.value().replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
            case "ITEM" -> {
                Material mat = Material.matchMaterial(chosen.value());
                if (mat != null) {
                    ItemStack it = ItemBuilder.from(mat, (int) chosen.amount()).name("&6" + chosen.displayName()).build();
                    player.getInventory().addItem(it);
                }
            }
        }

        SoundParticleUtil.playSound(player, sound, 1.0f, 1.0f);
        SoundParticleUtil.spawnParticle(player.getLocation().add(0, 1, 0), particle, 25, 0.5, 0.5, 0.5, 0.1);
        languageManager.sendMessage(player, MessageKey.TREASURE_CAUGHT, "reward", chosen.displayName());
    }
}
