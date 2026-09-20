package com.ardelys.ahowtofish.fishing;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.api.events.FishDiscoverEvent;
import com.ardelys.ahowtofish.api.events.FishingCatchEvent;
import com.ardelys.ahowtofish.language.MessageKey;
import com.ardelys.ahowtofish.model.*;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.player.PlayerStats;
import com.ardelys.ahowtofish.security.SecurityAlertLevel;
import com.ardelys.ahowtofish.security.TransactionManager;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class FishingListener implements Listener {
    private final AHowToFishPlugin plugin;

    public FishingListener(AHowToFishPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getProfileManager().loadProfileAsync(player.getUniqueId(), player.getName());
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updatePlayer(player);
        }
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().onPlayerJoin(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getFishingRateLimiter() != null) {
            plugin.getFishingRateLimiter().clear(player.getUniqueId());
        }
        if (plugin.getSecurityManager() != null && plugin.getSecurityManager().getRateLimiter() != null) {
            plugin.getSecurityManager().getRateLimiter().clear(player.getUniqueId());
        }
        if (plugin.getSellLockManager() != null) {
            plugin.getSellLockManager().releaseLock(player.getUniqueId());
        }
        if (plugin.getGuiSessionManager() != null) {
            plugin.getGuiSessionManager().endSession(player.getUniqueId());
        }
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().remove(player.getUniqueId());
        }
        if (plugin.getHudManager() != null) {
            plugin.getHudManager().removePlayer(player.getUniqueId());
        }
        if (plugin.getNpcManager() != null) {
            plugin.getNpcManager().removePlayer(player.getUniqueId());
        }
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().onPlayerQuit(player);
        }
        plugin.getProfileManager().unloadProfile(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        if (!player.isOnline() || player.isDead()) {
            event.setCancelled(true);
            return;
        }

        FishingProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null || !profile.isReady()) {
            
            plugin.getLanguageManager().sendMessage(player, MessageKey.PROFILE_LOADING);
            event.setCancelled(true);
            return;
        }

        PlayerFishEvent.State state = event.getState();

        if (state == PlayerFishEvent.State.FISHING) {
            profile.getStats().incrementCasts();
            if (plugin.getQuestManager() != null) {
                plugin.getQuestManager().onCast(player, profile);
            }
            if (plugin.getFishingRateLimiter() != null) {
                plugin.getFishingRateLimiter().recordCast(player.getUniqueId());
            }
            if (plugin.getHudManager() != null) {
                FishingBait activeBait = plugin.getBaitManager().getActiveBait(player);
                plugin.getHudManager().sendWaitingActionBar(player, activeBait);
            }
            return;
        }

        if (state == PlayerFishEvent.State.BITE) {
            if (plugin.getHudManager() != null) {
                plugin.getHudManager().sendBiteActionBar(player);
            }
            return;
        }

        if (state == PlayerFishEvent.State.FAILED_ATTEMPT) {
            profile.getStats().incrementFailedCatches();
            if (plugin.getFishingRateLimiter() != null) {
                plugin.getFishingRateLimiter().clear(player.getUniqueId());
            }
            return;
        }

        if (state == PlayerFishEvent.State.CAUGHT_FISH) {
            if (!(event.getCaught() instanceof Item caughtEntity)) {
                return;
            }

            Location hookLocation = event.getHook().getLocation();

            if (plugin.getFishingRateLimiter() != null && !plugin.getFishingRateLimiter().validateAndRecordCatch(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            if (plugin.getWorldGuardHook() != null && plugin.getWorldGuardHook().isAvailable()) {
                if (!plugin.getWorldGuardHook().canFish(player, hookLocation)) {
                    event.setCancelled(true);
                    return;
                }
            }

            FishingZone zone = plugin.getZoneManager().getZoneAt(hookLocation);
            if (zone != null && !zone.canFish(player, profile.getLevel())) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.ZONE_LEVEL_REQUIRED, "level", zone.requiredLevel(), "zone", zone.displayName());
                event.setCancelled(true);
                return;
            }

            FishingRod rod = plugin.getRodManager().getRodFromItem(player.getInventory().getItemInMainHand());
            if (rod != null && !rod.canUse(player, profile.getLevel())) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.ROD_LEVEL_REQUIRED, "level", rod.requiredLevel(), "rod", rod.displayName());
                event.setCancelled(true);
                return;
            }

            FishCatchResult result = plugin.getFishingEngine().rollFish(player, profile, hookLocation);
            if (result == null) {
                return;
            }

            FishingBait bait = plugin.getBaitManager().getActiveBait(player);

            FishingCatchEvent catchEvent = new FishingCatchEvent(player, result, rod, bait);
            Bukkit.getPluginManager().callEvent(catchEvent);
            if (catchEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }
            result = catchEvent.getCatchResult();

            String txId = plugin.getTransactionManager().generateTransactionId(player.getUniqueId());
            if (plugin.getTransactionManager().isTransactionProcessed(txId)) {
                plugin.getSecurityManager().logAlert(
                        SecurityAlertLevel.CRITICAL,
                        "FishingListener",
                        "Duplicate fishing transaction detected and blocked: " + txId,
                        player.getUniqueId()
                );
                event.setCancelled(true);
                return;
            }
            plugin.getTransactionManager().recordTransaction(txId, player.getUniqueId(), TransactionManager.TransactionStatus.PENDING, result.fish().id());

            ItemStack fishItem = result.buildItemStack();
            caughtEntity.setItemStack(fishItem);

            if (bait != null) {
                plugin.getBaitManager().consumeBaitUse(player);
                if (plugin.getQuestManager() != null) {
                    plugin.getQuestManager().onBaitUsed(player, profile);
                }
            }

            double treasureChanceBonus = plugin.getAbilityManager().getPlayerUpgradeEffect(profile, "treasure_chance") * 100.0;
            plugin.getTreasureManager().rollTreasure(player, treasureChanceBonus);

            if (plugin.getFishingEngine().shouldDoubleCatch(player, profile)) {
                ItemStack bonusFish = result.buildItemStack();
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(bonusFish);
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    plugin.getLanguageManager().sendMessage(player, MessageKey.INVENTORY_FULL);
                }
                SoundParticleUtil.playSound(player, "ENTITY_ITEM_PICKUP", 1.0f, 1.8f);
                plugin.getLanguageManager().sendMessage(player, MessageKey.DOUBLE_CATCH);
            }

            PlayerStats stats = profile.getStats();
            stats.incrementSuccessfulCatches();
            stats.incrementFishCaught();
            stats.recordWeight(result.weight());

            String rId = result.rarity().id();
            if (rId.contains("LEGENDARY") || rId.contains("MYTHIC") || rId.contains("SECRET")) {
                stats.incrementLegendary();
            } else if (rId.contains("RARE") || rId.contains("EPIC")) {
                stats.incrementRare();
            } else {
                stats.incrementCommon();
            }

            boolean isFirstDiscovery = profile.getCollection().recordCatch(
                    result.fish().id(),
                    result.weight(),
                    result.finalPrice(),
                    result.catchTimestamp()
            );

            if (isFirstDiscovery) {
                Bukkit.getPluginManager().callEvent(new FishDiscoverEvent(player, result.fish(), result.weight()));
                SoundParticleUtil.playSound(player, "UI_TOAST_CHALLENGE_COMPLETE", 1.0f, 1.2f);
            }

            plugin.getProgressionManager().addXp(player, result.finalXp());

            SoundParticleUtil.playSound(player, result.rarity().sound(), 1.0f, 1.0f);
            SoundParticleUtil.spawnParticle(player.getLocation().add(0, 1, 0), result.rarity().particle(), 15, 0.4, 0.4, 0.4, 0.05);

            if (plugin.getHudManager() != null) {
                plugin.getHudManager().sendCaughtActionBar(player, result);
                plugin.getHudManager().sendRareCatchTitle(player, result);
            }

            if (result.rarity().broadcast()) {
                String broadcast = plugin.getLanguageManager().getMessage(
                        Bukkit.getConsoleSender(),
                        MessageKey.FISH_CAUGHT_BROADCAST,
                        "player", player.getName(),
                        "fish", result.fish().displayName(),
                        "rarity", result.rarity().displayName(),
                        "color", result.rarity().color(),
                        "weight", TextUtil.formatDecimal(result.weight()),
                        "price", TextUtil.formatDecimal(result.finalPrice())
                );
                Bukkit.broadcast(TextUtil.toComponent(broadcast));
            }

            plugin.getCompetitionManager().onFishCatch(player, result);

            plugin.getQuestManager().onFishCatch(player, profile, result, zone != null ? zone.id() : null);

            plugin.getAchievementManager().checkAchievements(player, profile, result);

            plugin.getTransactionManager().recordTransaction(txId, player.getUniqueId(), TransactionManager.TransactionStatus.COMPLETED, result.fish().id());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (plugin.getHudManager() == null || !plugin.getHudManager().isZoneAlertsEnabled()) {
            return;
        }
        if (plugin.getZoneManager() == null || plugin.getZoneManager().getAllZones().isEmpty()) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ())) {
            return;
        }
        plugin.getHudManager().checkAndNotifyZoneTransition(event.getPlayer(), to);
    }
}