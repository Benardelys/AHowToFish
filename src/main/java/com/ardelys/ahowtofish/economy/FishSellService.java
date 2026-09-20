package com.ardelys.ahowtofish.economy;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.language.MessageKey;
import com.ardelys.ahowtofish.model.Fish;
import com.ardelys.ahowtofish.model.FishRarity;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.security.ItemSecurityManager;
import com.ardelys.ahowtofish.security.MultiplierService;
import com.ardelys.ahowtofish.security.SecurityAlertLevel;
import com.ardelys.ahowtofish.security.TransactionManager;
import com.ardelys.ahowtofish.utility.PdcKeys;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class FishSellService {
    private final AHowToFishPlugin plugin;

    public FishSellService(AHowToFishPlugin plugin) {
        this.plugin = plugin;
    }

    public void sellAllFish(Player player) {
        if (player == null || !player.isOnline()) return;

        if (plugin.getSecurityManager() != null) {
            var sm = plugin.getSecurityManager();
            if (!sm.getRateLimiter().tryAcquire(player.getUniqueId(), com.ardelys.ahowtofish.security.RateLimitCategory.SELL)) {
                var resp = sm.handleViolation(
                        com.ardelys.ahowtofish.security.SecurityAlertLevel.MEDIUM,
                        com.ardelys.ahowtofish.security.RateLimitCategory.SELL,
                        "FishSellService",
                        "Exceeded fish sell rate limit",
                        player.getUniqueId()
                );
                if (resp.shouldBlock()) {
                    plugin.getLanguageManager().sendMessage(player, MessageKey.TRANSACTION_BUSY);
                    return;
                }
            }
        }

        if (!plugin.getSellLockManager().acquireLock(player.getUniqueId())) {
            plugin.getLanguageManager().sendMessage(player, MessageKey.TRANSACTION_BUSY);
            return;
        }

        try {
            FishingProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile == null || !profile.isReady()) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.PROFILE_NOT_READY);
                return;
            }

            ItemStack[] contents = player.getInventory().getContents();
            List<ItemStack> removedItemsSnapshot = new ArrayList<>();
            List<Integer> modifiedSlots = new ArrayList<>();
            double totalEarned = 0.0;
            int totalFishCount = 0;

            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null || !item.hasItemMeta()) continue;

                String fishId = item.getItemMeta().getPersistentDataContainer().get(PdcKeys.FISH_ID, PersistentDataType.STRING);
                if (fishId == null) continue;

                Fish fishDef = plugin.getFishManager().getFish(fishId);
                FishRarity rarityDef = fishDef != null ? plugin.getFishManager().getRarity(fishDef.rarityId()) : null;

                ItemSecurityManager.ValidationResult vResult = plugin.getItemSecurityManager().validateFishItem(item, fishDef, rarityDef);
                if (vResult != ItemSecurityManager.ValidationResult.VALID) {
                    plugin.getSecurityManager().logAlert(
                            SecurityAlertLevel.HIGH,
                            "FishSellService",
                            "Rejected forged/corrupt fish item: " + vResult + " (ID: " + fishId + ")",
                            player.getUniqueId()
                    );
                    continue;
                }

                Double unitPrice = item.getItemMeta().getPersistentDataContainer().get(PdcKeys.FISH_PRICE, PersistentDataType.DOUBLE);
                if (unitPrice == null) continue;
                unitPrice = MultiplierService.sanitizeMoney(unitPrice);
                if (unitPrice <= 0.0) continue;

                int amount = item.getAmount();
                double lineTotal = MultiplierService.sanitizeMoney(unitPrice * amount);

                removedItemsSnapshot.add(item.clone());
                modifiedSlots.add(i);
                totalEarned += lineTotal;
                totalFishCount += amount;

                contents[i] = null;
            }

            if (totalFishCount <= 0 || totalEarned <= 0.0) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.NO_FISH_TO_SELL);
                return;
            }

            totalEarned = MultiplierService.sanitizeMoney(Math.round(totalEarned * 100.0) / 100.0);

            player.getInventory().setContents(contents);

            boolean depositSuccess = true;
            if (plugin.getVaultHook() != null && plugin.getVaultHook().isAvailable()) {
                depositSuccess = plugin.getVaultHook().deposit(player, totalEarned);
            }

            if (!depositSuccess) {
                plugin.getSecurityManager().logAlert(
                        SecurityAlertLevel.CRITICAL,
                        "FishSellService",
                        "Vault deposit failed! Rolling back removed items for player: " + player.getName(),
                        player.getUniqueId()
                );
                for (ItemStack restore : removedItemsSnapshot) {
                    player.getInventory().addItem(restore);
                }
                plugin.getLanguageManager().sendMessage(player, MessageKey.DEPOSIT_FAILED);
                return;
            }

            profile.getStats().addMoney(totalEarned);
            plugin.getQuestManager().onMoneyEarned(player, profile, totalEarned);
            plugin.getQuestManager().onFishSold(player, profile, totalFishCount, totalEarned);
            plugin.getAchievementManager().checkAchievements(player, profile, null);
            plugin.getProfileManager().saveProfileAsync(profile);

            String txId = plugin.getTransactionManager().generateTransactionId(player.getUniqueId());
            plugin.getTransactionManager().recordTransaction(
                    txId,
                    player.getUniqueId(),
                    TransactionManager.TransactionStatus.COMPLETED,
                    "SELLALL:" + totalFishCount + ":$" + totalEarned
            );

            SoundParticleUtil.playSound(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f);
            plugin.getLanguageManager().sendMessage(player, MessageKey.FISH_SOLD_ALL, "amount", totalFishCount, "money", TextUtil.formatDecimal(totalEarned));

        } finally {
            plugin.getSellLockManager().releaseLock(player.getUniqueId());
        }
    }

    public void sellHeldFish(Player player) {
        if (player == null || !player.isOnline()) return;

        if (plugin.getSecurityManager() != null) {
            var sm = plugin.getSecurityManager();
            if (!sm.getRateLimiter().tryAcquire(player.getUniqueId(), com.ardelys.ahowtofish.security.RateLimitCategory.SELL)) {
                var resp = sm.handleViolation(
                        com.ardelys.ahowtofish.security.SecurityAlertLevel.MEDIUM,
                        com.ardelys.ahowtofish.security.RateLimitCategory.SELL,
                        "FishSellService",
                        "Exceeded fish sell rate limit",
                        player.getUniqueId()
                );
                if (resp.shouldBlock()) {
                    plugin.getLanguageManager().sendMessage(player, MessageKey.TRANSACTION_BUSY);
                    return;
                }
            }
        }

        if (!plugin.getSellLockManager().acquireLock(player.getUniqueId())) {
            plugin.getLanguageManager().sendMessage(player, MessageKey.TRANSACTION_BUSY);
            return;
        }

        try {
            FishingProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile == null || !profile.isReady()) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.PROFILE_NOT_READY);
                return;
            }

            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType().isAir() || !held.hasItemMeta()) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.NO_FISH_TO_SELL);
                return;
            }

            String fishId = held.getItemMeta().getPersistentDataContainer().get(PdcKeys.FISH_ID, PersistentDataType.STRING);
            if (fishId == null) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.NO_FISH_TO_SELL);
                return;
            }

            Fish fishDef = plugin.getFishManager().getFish(fishId);
            FishRarity rarityDef = fishDef != null ? plugin.getFishManager().getRarity(fishDef.rarityId()) : null;

            ItemSecurityManager.ValidationResult vResult = plugin.getItemSecurityManager().validateFishItem(held, fishDef, rarityDef);
            if (vResult != ItemSecurityManager.ValidationResult.VALID) {
                plugin.getSecurityManager().logAlert(
                        SecurityAlertLevel.HIGH,
                        "FishSellService",
                        "Rejected forged held fish: " + vResult,
                        player.getUniqueId()
                );
                plugin.getLanguageManager().sendMessage(player, MessageKey.NO_FISH_TO_SELL);
                return;
            }

            Double unitPrice = held.getItemMeta().getPersistentDataContainer().get(PdcKeys.FISH_PRICE, PersistentDataType.DOUBLE);
            if (unitPrice == null || unitPrice <= 0.0) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.NO_FISH_TO_SELL);
                return;
            }

            int amount = held.getAmount();
            double totalEarned = MultiplierService.sanitizeMoney(unitPrice * amount);
            totalEarned = Math.round(totalEarned * 100.0) / 100.0;

            ItemStack snapshot = held.clone();
            player.getInventory().setItemInMainHand(null);

            boolean depositSuccess = true;
            if (plugin.getVaultHook() != null && plugin.getVaultHook().isAvailable()) {
                depositSuccess = plugin.getVaultHook().deposit(player, totalEarned);
            }

            if (!depositSuccess) {
                player.getInventory().setItemInMainHand(snapshot);
                plugin.getLanguageManager().sendMessage(player, MessageKey.DEPOSIT_FAILED);
                return;
            }

            profile.getStats().addMoney(totalEarned);
            plugin.getQuestManager().onMoneyEarned(player, profile, totalEarned);
            plugin.getQuestManager().onFishSold(player, profile, amount, totalEarned);
            plugin.getAchievementManager().checkAchievements(player, profile, null);
            plugin.getProfileManager().saveProfileAsync(profile);

            String txId = plugin.getTransactionManager().generateTransactionId(player.getUniqueId());
            plugin.getTransactionManager().recordTransaction(
                    txId,
                    player.getUniqueId(),
                    TransactionManager.TransactionStatus.COMPLETED,
                    "SELL_HAND:" + amount + ":$" + totalEarned
            );

            SoundParticleUtil.playSound(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f);
            plugin.getLanguageManager().sendMessage(player, MessageKey.FISH_SOLD, "amount", amount, "money", TextUtil.formatDecimal(totalEarned));

        } finally {
            plugin.getSellLockManager().releaseLock(player.getUniqueId());
        }
    }

    public record SellSummary(int totalCount, double totalWeight, double totalValue, boolean hasRareOrLegendary) {}

    public SellSummary getSellableFishSummary(Player player) {
        if (player == null || !player.isOnline()) return new SellSummary(0, 0.0, 0.0, false);

        int count = 0;
        double weight = 0.0;
        double value = 0.0;
        boolean rare = false;

        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack item : contents) {
            if (item == null || !item.hasItemMeta()) continue;
            String fishId = item.getItemMeta().getPersistentDataContainer().get(PdcKeys.FISH_ID, PersistentDataType.STRING);
            if (fishId == null) continue;

            Fish fishDef = plugin.getFishManager().getFish(fishId);
            FishRarity rarityDef = fishDef != null ? plugin.getFishManager().getRarity(fishDef.rarityId()) : null;

            if (plugin.getItemSecurityManager().validateFishItem(item, fishDef, rarityDef) != ItemSecurityManager.ValidationResult.VALID) {
                continue;
            }

            Double unitPrice = item.getItemMeta().getPersistentDataContainer().get(PdcKeys.FISH_PRICE, PersistentDataType.DOUBLE);
            Double fishWeight = item.getItemMeta().getPersistentDataContainer().get(PdcKeys.FISH_WEIGHT, PersistentDataType.DOUBLE);
            if (unitPrice == null || unitPrice <= 0.0) continue;

            int amt = item.getAmount();
            count += amt;
            value += MultiplierService.sanitizeMoney(unitPrice * amt);
            if (fishWeight != null) {
                weight += fishWeight * amt;
            }

            String rId = rarityDef != null ? rarityDef.id() : "";
            if (rId.contains("RARE") || rId.contains("EPIC") || rId.contains("LEGENDARY") || rId.contains("MYTHIC") || rId.contains("SECRET")) {
                rare = true;
            }
        }

        value = Math.round(value * 100.0) / 100.0;
        weight = Math.round(weight * 100.0) / 100.0;
        return new SellSummary(count, weight, value, rare);
    }

    public boolean sellSingleSlotFish(Player player, int slot) {
        if (player == null || !player.isOnline()) return false;

        if (!plugin.getSellLockManager().acquireLock(player.getUniqueId())) {
            plugin.getLanguageManager().sendMessage(player, MessageKey.TRANSACTION_BUSY);
            return false;
        }

        try {
            FishingProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile == null || !profile.isReady()) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.PROFILE_NOT_READY);
                return false;
            }

            if (slot < 0 || slot >= player.getInventory().getSize()) return false;
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || !item.hasItemMeta()) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.NO_FISH_TO_SELL);
                return false;
            }

            String fishId = item.getItemMeta().getPersistentDataContainer().get(PdcKeys.FISH_ID, PersistentDataType.STRING);
            if (fishId == null) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.NO_FISH_TO_SELL);
                return false;
            }

            Fish fishDef = plugin.getFishManager().getFish(fishId);
            FishRarity rarityDef = fishDef != null ? plugin.getFishManager().getRarity(fishDef.rarityId()) : null;

            ItemSecurityManager.ValidationResult vResult = plugin.getItemSecurityManager().validateFishItem(item, fishDef, rarityDef);
            if (vResult != ItemSecurityManager.ValidationResult.VALID) {
                plugin.getSecurityManager().logAlert(
                        SecurityAlertLevel.HIGH,
                        "FishSellService",
                        "Rejected forged single fish item: " + vResult,
                        player.getUniqueId()
                );
                plugin.getLanguageManager().sendMessage(player, MessageKey.NO_FISH_TO_SELL);
                return false;
            }

            Double unitPrice = item.getItemMeta().getPersistentDataContainer().get(PdcKeys.FISH_PRICE, PersistentDataType.DOUBLE);
            if (unitPrice == null || unitPrice <= 0.0) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.NO_FISH_TO_SELL);
                return false;
            }

            int amount = item.getAmount();
            double totalEarned = MultiplierService.sanitizeMoney(unitPrice * amount);
            totalEarned = Math.round(totalEarned * 100.0) / 100.0;

            ItemStack snapshot = item.clone();
            player.getInventory().setItem(slot, null);

            boolean depositSuccess = true;
            if (plugin.getVaultHook() != null && plugin.getVaultHook().isAvailable()) {
                depositSuccess = plugin.getVaultHook().deposit(player, totalEarned);
            }

            if (!depositSuccess) {
                player.getInventory().setItem(slot, snapshot);
                plugin.getLanguageManager().sendMessage(player, MessageKey.DEPOSIT_FAILED);
                return false;
            }

            profile.getStats().addMoney(totalEarned);
            plugin.getQuestManager().onMoneyEarned(player, profile, totalEarned);
            plugin.getQuestManager().onFishSold(player, profile, amount, totalEarned);
            plugin.getAchievementManager().checkAchievements(player, profile, null);
            plugin.getProfileManager().saveProfileAsync(profile);

            String txId = plugin.getTransactionManager().generateTransactionId(player.getUniqueId());
            plugin.getTransactionManager().recordTransaction(
                    txId,
                    player.getUniqueId(),
                    TransactionManager.TransactionStatus.COMPLETED,
                    "SELL_SLOT:" + slot + ":" + amount + ":$" + totalEarned
            );

            SoundParticleUtil.playSound(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f);
            plugin.getLanguageManager().sendMessage(player, MessageKey.FISH_SOLD, "color", rarityDef != null ? rarityDef.color() : "", "fish", fishDef != null ? fishDef.displayName() : "Fish", "price", TextUtil.formatDecimal(totalEarned));
            return true;
        } finally {
            plugin.getSellLockManager().releaseLock(player.getUniqueId());
        }
    }
}