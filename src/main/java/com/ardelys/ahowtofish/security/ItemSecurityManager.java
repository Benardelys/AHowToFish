package com.ardelys.ahowtofish.security;

import com.ardelys.ahowtofish.model.Fish;
import com.ardelys.ahowtofish.model.FishCatchResult;
import com.ardelys.ahowtofish.model.FishRarity;
import com.ardelys.ahowtofish.utility.PdcKeys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;

public class ItemSecurityManager {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final JavaPlugin plugin;
    private final SecurityManager securityManager;
    private byte[] secretKeyBytes;
    private volatile SecretKeySpec cachedSecretKeySpec;

    public enum ValidationResult {
        VALID,
        NOT_A_CUSTOM_FISH,
        MISSING_PDC_DATA,
        INVALID_SIGNATURE,
        PRICE_EXCEEDED_BOUNDS,
        WEIGHT_EXCEEDED_BOUNDS,
        NUMERIC_CORRUPTION
    }

    public ItemSecurityManager(JavaPlugin plugin, SecurityManager securityManager) {
        this.plugin = plugin;
        this.securityManager = securityManager;
        initSecretKey();
    }

    private void initSecretKey() {
        File keyFile = new File(plugin.getDataFolder(), "security.key");
        if (keyFile.exists()) {
            try {
                String encoded = Files.readString(keyFile.toPath()).trim();
                secretKeyBytes = Base64.getDecoder().decode(encoded);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[AHowToFish-Security] Could not read security.key, generating fresh key.", e);
            }
        }

        if (secretKeyBytes == null || secretKeyBytes.length < 32) {
            secretKeyBytes = new byte[32];
            new SecureRandom().nextBytes(secretKeyBytes);
            try {
                if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
                Files.writeString(keyFile.toPath(), Base64.getEncoder().encodeToString(secretKeyBytes));
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "[AHowToFish-Security] Failed to save security.key!", e);
            }
        }
        this.cachedSecretKeySpec = new SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM);
    }

    public String computeSignature(String fishId, String rarityId, double weight, double price, UUID catcherUuid) {
        String payload = fishId.toLowerCase() + ":" + rarityId.toUpperCase() + ":" +
                String.format(java.util.Locale.US, "%.2f", weight) + ":" +
                String.format(java.util.Locale.US, "%.2f", price) + ":" +
                (catcherUuid != null ? catcherUuid.toString() : "null");

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(cachedSecretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    public void stampSignature(ItemStack item, FishCatchResult result) {
        if (item == null || !item.hasItemMeta() || result == null) return;
        ItemMeta meta = item.getItemMeta();
        String signature = computeSignature(
                result.fish().id(),
                result.rarity().id(),
                result.weight(),
                result.finalPrice(),
                result.catcherUuid()
        );

        meta.getPersistentDataContainer().set(PdcKeys.ITEM_TYPE, PersistentDataType.STRING, "FISH");
        meta.getPersistentDataContainer().set(PdcKeys.ITEM_VERSION, PersistentDataType.INTEGER, 1);
        meta.getPersistentDataContainer().set(PdcKeys.ITEM_SIGNATURE, PersistentDataType.STRING, signature);
        item.setItemMeta(meta);
    }

    public ValidationResult validateFishItem(ItemStack item, Fish expectedFish, FishRarity expectedRarity) {
        if (item == null || !item.hasItemMeta()) {
            return ValidationResult.NOT_A_CUSTOM_FISH;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(PdcKeys.FISH_ID, PersistentDataType.STRING) ||
            !meta.getPersistentDataContainer().has(PdcKeys.FISH_PRICE, PersistentDataType.DOUBLE) ||
            !meta.getPersistentDataContainer().has(PdcKeys.FISH_WEIGHT, PersistentDataType.DOUBLE)) {
            return ValidationResult.MISSING_PDC_DATA;
        }

        String fishId = meta.getPersistentDataContainer().get(PdcKeys.FISH_ID, PersistentDataType.STRING);
        String rarityId = meta.getPersistentDataContainer().get(PdcKeys.FISH_RARITY, PersistentDataType.STRING);
        Double price = meta.getPersistentDataContainer().get(PdcKeys.FISH_PRICE, PersistentDataType.DOUBLE);
        Double weight = meta.getPersistentDataContainer().get(PdcKeys.FISH_WEIGHT, PersistentDataType.DOUBLE);
        String signature = meta.getPersistentDataContainer().get(PdcKeys.ITEM_SIGNATURE, PersistentDataType.STRING);
        String catcherStr = meta.getPersistentDataContainer().get(PdcKeys.FISH_CATCHER, PersistentDataType.STRING);

        if (price == null || weight == null || Double.isNaN(price) || Double.isInfinite(price) ||
            Double.isNaN(weight) || Double.isInfinite(weight) || price < 0 || weight <= 0) {
            return ValidationResult.NUMERIC_CORRUPTION;
        }

        UUID catcherUuid = null;
        if (catcherStr != null) {
            try { catcherUuid = UUID.fromString(catcherStr); } catch (IllegalArgumentException ignored) {}
        }

        if (securityManager.isItemValidation()) {
            if (signature == null || signature.isBlank()) {
                return ValidationResult.INVALID_SIGNATURE;
            }

            String expectedSig = computeSignature(fishId, rarityId != null ? rarityId : "COMMON", weight, price, catcherUuid);
            if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), expectedSig.getBytes(StandardCharsets.UTF_8))) {
                return ValidationResult.INVALID_SIGNATURE;
            }
        }

        if (expectedFish != null) {
            double maxAllowedWeight = expectedFish.maxWeight() * 3.0; 
            if (weight > maxAllowedWeight) {
                return ValidationResult.WEIGHT_EXCEEDED_BOUNDS;
            }

            double maxAllowedPrice = expectedFish.sellPrice() * 50.0; 
            if (price > maxAllowedPrice) {
                return ValidationResult.PRICE_EXCEEDED_BOUNDS;
            }
        }

        return ValidationResult.VALID;
    }
}