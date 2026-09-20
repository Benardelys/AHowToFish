package com.ardelys.ahowtofish.fishing;

import com.ardelys.ahowtofish.model.FishingBait;
import com.ardelys.ahowtofish.utility.PdcKeys;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BaitManager {
    private final JavaPlugin plugin;
    private final Map<String, FishingBait> baitRegistry = new ConcurrentHashMap<>();

    public BaitManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        baitRegistry.clear();
        File file = new File(plugin.getDataFolder(), "bait.yml");
        if (!file.exists()) {
            File altFile = new File(plugin.getDataFolder(), "baits.yml");
            if (altFile.exists()) {
                file = altFile;
            } else {
                try {
                    plugin.saveResource("bait.yml", false);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not save default bait.yml: " + e.getMessage());
                }
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("baits");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    String displayName = section.getString(key + ".display-name", key);
                    String matStr = section.getString(key + ".material", "WHEAT_SEEDS");
                    Material mat = Material.matchMaterial(matStr);
                    if (mat == null) mat = Material.WHEAT_SEEDS;

                    Integer cmd = section.contains(key + ".custom-model-data") ? section.getInt(key + ".custom-model-data") : null;
                    List<String> lore = section.getStringList(key + ".lore");
                    int maxUses = section.getInt(key + ".max-uses", 10);
                    double fishChanceMul = section.getDouble(key + ".fish-chance-multiplier", 1.0);
                    double rareChanceMul = section.getDouble(key + ".rare-chance-multiplier", 1.0);
                    double legChanceMul = section.getDouble(key + ".legendary-chance-multiplier", 1.0);
                    double xpMul = section.getDouble(key + ".xp-multiplier", 1.0);
                    double moneyMul = section.getDouble(key + ".money-multiplier", 1.0);
                    String specialEffect = section.getString(key + ".special-effect", "none");
                    double cost = section.getDouble(key + ".cost", 100.0);

                    FishingBait bait = new FishingBait(
                            key.toLowerCase(),
                            displayName,
                            mat,
                            cmd,
                            lore,
                            maxUses,
                            fishChanceMul,
                            rareChanceMul,
                            legChanceMul,
                            xpMul,
                            moneyMul,
                            specialEffect,
                            cost
                    );
                    baitRegistry.put(bait.id(), bait);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[AHowToFish] Failed to load bait '" + key + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("[AHowToFish] Loaded " + baitRegistry.size() + " custom bait(s).");
    }

    public FishingBait getBait(String id) {
        return baitRegistry.get(id.toLowerCase());
    }

    public FishingBait getBaitFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String baitId = item.getItemMeta().getPersistentDataContainer().get(PdcKeys.BAIT_ID, PersistentDataType.STRING);
        if (baitId != null) {
            return baitRegistry.get(baitId.toLowerCase());
        }
        return null;
    }

    public FishingBait getActiveBait(Player player) {
        if (player == null) return null;
        
        ItemStack offhand = player.getInventory().getItemInOffHand();
        FishingBait bait = getBaitFromItem(offhand);
        if (bait != null) return bait;

        for (ItemStack item : player.getInventory().getContents()) {
            bait = getBaitFromItem(item);
            if (bait != null) return bait;
        }
        return null;
    }

    public void consumeBaitUse(Player player) {
        if (player == null) return;
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (consumeFromItem(offhand)) {
            player.getInventory().setItemInOffHand(offhand);
            return;
        }

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (consumeFromItem(it)) {
                contents[i] = it;
                player.getInventory().setContents(contents);
                return;
            }
        }
    }

    private boolean consumeFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String baitId = meta.getPersistentDataContainer().get(PdcKeys.BAIT_ID, PersistentDataType.STRING);
        if (baitId == null) return false;

        Integer uses = meta.getPersistentDataContainer().get(PdcKeys.BAIT_USES, PersistentDataType.INTEGER);
        if (uses == null || uses <= 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            meta.getPersistentDataContainer().set(PdcKeys.BAIT_USES, PersistentDataType.INTEGER, uses - 1);
            item.setItemMeta(meta);
        }
        return true;
    }

    public Collection<FishingBait> getAllBaits() {
        return Collections.unmodifiableCollection(baitRegistry.values());
    }

    public void registerBait(FishingBait bait) {
        baitRegistry.put(bait.id().toLowerCase(), bait);
    }
}
