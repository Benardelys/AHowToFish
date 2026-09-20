package com.ardelys.ahowtofish.hud;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.model.FishCatchResult;
import com.ardelys.ahowtofish.model.FishingBait;
import com.ardelys.ahowtofish.model.FishingZone;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HudManager {

    private final AHowToFishPlugin plugin;

    private boolean actionBarsEnabled = true;
    private boolean titlesEnabled = true;
    private boolean zoneAlertsEnabled = true;

    private String waitingBarFormat = "&3🎣 &bLine cast... &7Waiting for bite &8[&fBait: &e{bait}&8]";
    private String biteBarFormat = "&c&l⚡ BITE! &eReel it in now! ⚡";
    private String caughtBarFormat = "{rarity_color}{fish} &7({weight} kg) &8| &a+${price} &8| &b+{xp} XP";

    private final Map<UUID, String> lastPlayerZones = new ConcurrentHashMap<>();

    public HudManager(AHowToFishPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();
        if (config == null) return;

        this.actionBarsEnabled = config.getBoolean("hud.actionbar.enabled", true);
        this.titlesEnabled = config.getBoolean("hud.titles.enabled", true);
        this.zoneAlertsEnabled = config.getBoolean("hud.zones.enabled", true);

        this.waitingBarFormat = config.getString("hud.actionbar.waiting", waitingBarFormat);
        this.biteBarFormat = config.getString("hud.actionbar.bite", biteBarFormat);
        this.caughtBarFormat = config.getString("hud.actionbar.caught", caughtBarFormat);
    }

    public void stop() {
        lastPlayerZones.clear();
    }

    public void removePlayer(UUID uuid) {
        lastPlayerZones.remove(uuid);
    }

    public boolean isZoneAlertsEnabled() {
        return zoneAlertsEnabled;
    }

    public void sendWaitingActionBar(Player player, FishingBait bait) {
        if (!actionBarsEnabled || player == null || !player.isOnline()) return;

        String none = plugin.getLanguageManager().getMessage(player, "command.status.no_bait", "None");
        String baitName = bait != null ? bait.displayName() : none;
        String format = plugin.getLanguageManager().getMessage(player, "hud.waiting", waitingBarFormat);
        String msg = format.replace("{bait}", baitName);
        player.sendActionBar(TextUtil.toComponent(msg));
    }

    public void sendBiteActionBar(Player player) {
        if (!actionBarsEnabled || player == null || !player.isOnline()) return;

        String format = plugin.getLanguageManager().getMessage(player, "hud.bite", biteBarFormat);
        player.sendActionBar(TextUtil.toComponent(format));
        SoundParticleUtil.playSound(player, "BLOCK_NOTE_BLOCK_BELL", 1.0f, 1.8f);
    }

    public void sendCaughtActionBar(Player player, FishCatchResult result) {
        if (!actionBarsEnabled || player == null || !player.isOnline() || result == null) return;

        String format = plugin.getLanguageManager().getMessage(player, "hud.caught", caughtBarFormat);
        String msg = format
                .replace("{rarity_color}", result.rarity().color())
                .replace("{rarity}", result.rarity().displayName())
                .replace("{fish}", result.fish().displayName())
                .replace("{weight}", TextUtil.formatDecimal(result.weight()))
                .replace("{price}", TextUtil.formatDecimal(result.finalPrice()))
                .replace("{xp}", TextUtil.formatInteger(result.finalXp()));

        player.sendActionBar(TextUtil.toComponent(msg));
    }

    public void sendRareCatchTitle(Player player, FishCatchResult result) {
        if (!titlesEnabled || player == null || !player.isOnline() || result == null) return;

        String rarityId = result.rarity().id();
        boolean isRare = result.rarity().broadcast() ||
                rarityId.contains("LEGENDARY") ||
                rarityId.contains("MYTHIC") ||
                rarityId.contains("SECRET");

        if (!isRare) return;

        String titleStr = plugin.getLanguageManager().getMessage(player, "hud.rare_catch_title", "{rarity_color}&l✦ {rarity} CAUGHT! ✦")
                .replace("{rarity_color}", result.rarity().color())
                .replace("{rarity}", result.rarity().displayName().toUpperCase(java.util.Locale.ROOT));
        String subtitleStr = plugin.getLanguageManager().getMessage(player, "hud.rare_catch_subtitle", "&f{fish} &7({weight} kg)")
                .replace("{fish}", result.fish().displayName())
                .replace("{weight}", TextUtil.formatDecimal(result.weight()));

        Component title = TextUtil.toComponent(titleStr);
        Component subtitle = TextUtil.toComponent(subtitleStr);

        Title.Times times = Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(600));
        player.showTitle(Title.title(title, subtitle, times));
    }

    public void sendLevelUpTitle(Player player, int newLevel) {
        if (!titlesEnabled || player == null || !player.isOnline()) return;

        String titleStr = plugin.getLanguageManager().getMessage(player, "hud.level_up_title", "&6&l✦ LEVEL UP! ✦");
        String subtitleStr = plugin.getLanguageManager().getMessage(player, "hud.level_up_subtitle", "&7Fishing Skill: &eLevel {level}")
                .replace("{level}", String.valueOf(newLevel));

        Component title = TextUtil.toComponent(titleStr);
        Component subtitle = TextUtil.toComponent(subtitleStr);

        Title.Times times = Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(600));
        player.showTitle(Title.title(title, subtitle, times));
    }

    public void checkAndNotifyZoneTransition(Player player, Location location) {
        if (!zoneAlertsEnabled || player == null || !player.isOnline() || location == null) return;

        FishingZone currentZone = plugin.getZoneManager().getZoneAt(location);
        String currentZoneId = currentZone != null ? currentZone.id() : "__wild__";
        String lastZoneId = lastPlayerZones.get(player.getUniqueId());

        if (currentZoneId.equals(lastZoneId)) {
            return;
        }

        lastPlayerZones.put(player.getUniqueId(), currentZoneId);

        if (currentZone != null) {
            String titleStr = plugin.getLanguageManager().getMessage(player, "hud.zone_title", "&3&l✦ {zone} ✦")
                    .replace("{zone}", currentZone.displayName());
            String subtitleStr = plugin.getLanguageManager().getMessage(player, "hud.zone_subtitle", "&7Required Level: &e{level} &8| &bSpecies: &f{count}")
                    .replace("{level}", String.valueOf(currentZone.requiredLevel()))
                    .replace("{count}", String.valueOf(currentZone.availableFish() != null ? currentZone.availableFish().size() : 0));

            Component title = TextUtil.toComponent(titleStr);
            Component subtitle = TextUtil.toComponent(subtitleStr);
            Title.Times times = Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2000), Duration.ofMillis(500));
            player.showTitle(Title.title(title, subtitle, times));

            String actionStr = plugin.getLanguageManager().getMessage(player, "hud.zone_entered", "&aEntered Fishing Zone: &f{zone} &7(Required Level: &e{level}&7)")
                    .replace("{zone}", currentZone.displayName())
                    .replace("{level}", String.valueOf(currentZone.requiredLevel()));
            player.sendActionBar(TextUtil.toComponent(actionStr));
            SoundParticleUtil.playSound(player, "BLOCK_BEACON_ACTIVATE", 0.8f, 1.6f);
        } else if (lastZoneId != null && !lastZoneId.equals("__wild__")) {
            String wildStr = plugin.getLanguageManager().getMessage(player, "hud.wilderness_entered", "&7Zone: &fOpen Waters &8(Wilderness)");
            player.sendActionBar(TextUtil.toComponent(wildStr));
        }
    }
}
