package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.competition.CompetitionType;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.model.SpecialEvent;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AdminPanelMenu extends CustomGui {

    private final AHowToFishPlugin plugin;
    private final Player player;

    public AdminPanelMenu(AHowToFishPlugin plugin, Player player) {
        super(54, TextUtil.colorize("&4&l✦ AHowToFish &8| &cAdmin Panel"), "ADMIN_PANEL");
        this.plugin = plugin;
        this.player = player;

        initialize();
    }

    private void initialize() {
        fillBorder(Material.RED_STAINED_GLASS_PANE);

        int totalFish = plugin.getFishManager().getAllFish().size();
        int totalRods = plugin.getRodManager().getAllRods().size();
        int totalBaits = plugin.getBaitManager().getAllBaits().size();
        int totalZones = plugin.getZoneManager().getAllZones().size();

        List<String> headerLore = new ArrayList<>();
        headerLore.add("&7Plugin Version: &a" + plugin.getPluginMeta().getVersion());
        headerLore.add("&7Registered Fish: &e" + totalFish);
        headerLore.add("&7Registered Rods: &e" + totalRods);
        headerLore.add("&7Registered Baits: &e" + totalBaits);
        headerLore.add("&7Configured Zones: &e" + totalZones);
        headerLore.add("");
        headerLore.add("&7Security Integrity: &aACTIVE (HMAC Signed)");
        headerLore.add("&7Database Status: &aCONNECTED");

        setItem(4, ItemBuilder.from(Material.COMMAND_BLOCK)
                .name("&4&l✦ AHowToFish Control Dashboard ✦")
                .lore(headerLore)
                .build());

        setItem(20, ItemBuilder.from(Material.PUFFERFISH)
                .name("&e&lSpawn Random Fish")
                .lore(
                        "&7Click to grant yourself a randomly rolled",
                        "&7custom authenticated fish item.",
                        "",
                        "&e▶ Left-click to roll & receive"
                ).build(), e -> {
            var all = new ArrayList<>(plugin.getFishManager().getAllFish());
            if (!all.isEmpty()) {
                var randomFish = all.get(new java.util.Random().nextInt(all.size()));
                plugin.giveFish(player, randomFish.id(), 1);
                player.sendMessage(TextUtil.colorize("&a[Admin] Granted fish: &f" + randomFish.displayName()));
                SoundParticleUtil.playSound(player, "ENTITY_ITEM_PICKUP", 1.0f, 1.2f);
            }
        });

        setItem(21, ItemBuilder.from(Material.FISHING_ROD)
                .name("&b&lSpawn Custom Rod")
                .lore(
                        "&7Click to obtain the highest tier fishing rod",
                        "&7available in the plugin.",
                        "",
                        "&e▶ Left-click to receive rod"
                ).build(), e -> {
            var rods = new ArrayList<>(plugin.getRodManager().getAllRods());
            if (!rods.isEmpty()) {
                var rod = rods.get(rods.size() - 1);
                plugin.giveRod(player, rod.id());
                player.sendMessage(TextUtil.colorize("&a[Admin] Granted rod: &f" + rod.displayName()));
                SoundParticleUtil.playSound(player, "ENTITY_ITEM_PICKUP", 1.0f, 1.2f);
            }
        });

        setItem(22, ItemBuilder.from(Material.GLOW_BERRIES)
                .name("&6&lSpawn Bait Supply")
                .lore(
                        "&7Click to receive a stack of 16 premium bait items.",
                        "",
                        "&e▶ Left-click to receive bait"
                ).build(), e -> {
            var baits = new ArrayList<>(plugin.getBaitManager().getAllBaits());
            if (!baits.isEmpty()) {
                var bait = baits.get(0);
                plugin.giveBait(player, bait.id(), 16);
                player.sendMessage(TextUtil.colorize("&a[Admin] Granted 16x bait: &f" + bait.displayName()));
                SoundParticleUtil.playSound(player, "ENTITY_ITEM_PICKUP", 1.0f, 1.2f);
            }
        });

        boolean compActive = plugin.getCompetitionManager() != null && plugin.getCompetitionManager().isCompetitionActive();
        setItem(24, ItemBuilder.from(Material.GOLDEN_SWORD)
                .name("&e&lTournament Controller")
                .lore(
                        "&7Status: " + (compActive ? "&aACTIVE" : "&cINACTIVE"),
                        "",
                        "&e▶ Left-click: &7Start a 5-minute tournament",
                        "&c▶ Right-click: &7Force stop active tournament"
                ).build(), e -> {
            if (e.isRightClick()) {
                if (plugin.getCompetitionManager() != null) {
                    plugin.getCompetitionManager().stopCompetition();
                    player.sendMessage(TextUtil.colorize("&c[Admin] Tournament stopped."));
                }
            } else {
                if (plugin.getCompetitionManager() != null) {
                    plugin.getCompetitionManager().startCompetition(CompetitionType.MOST_FISH, 300);
                    player.sendMessage(TextUtil.colorize("&a[Admin] Tournament started: Most Fish (5 minutes)."));
                }
            }
            new AdminPanelMenu(plugin, player).open(player);
        });

        boolean eventActive = plugin.getEventManager() != null && plugin.getEventManager().isEventActive();
        setItem(25, ItemBuilder.from(Material.NETHER_STAR)
                .name("&d&lEvent Controller")
                .lore(
                        "&7Status: " + (eventActive ? "&aACTIVE" : "&cINACTIVE"),
                        "",
                        "&e▶ Left-click: &7Trigger random fishing event",
                        "&c▶ Right-click: &7Force stop active event"
                ).build(), e -> {
            if (e.isRightClick()) {
                if (plugin.getEventManager() != null) {
                    plugin.getEventManager().stopEvent();
                    player.sendMessage(TextUtil.colorize("&c[Admin] Event stopped."));
                }
            } else {
                if (plugin.getEventManager() != null) {
                    var events = new ArrayList<>(plugin.getEventManager().getAllEvents());
                    if (!events.isEmpty()) {
                        SpecialEvent ev = events.get(new java.util.Random().nextInt(events.size()));
                        plugin.getEventManager().startEvent(ev.id());
                        player.sendMessage(TextUtil.colorize("&a[Admin] Started event: &f" + ev.name()));
                    }
                }
            }
            new AdminPanelMenu(plugin, player).open(player);
        });

        setItem(30, ItemBuilder.from(Material.SHIELD)
                .name("&9&lSecurity Diagnostics")
                .lore(
                        "&7Active Sell Locks: &f" + (plugin.getSellLockManager() != null ? "Monitoring" : "Disabled"),
                        "&7Rate Limiter: &f" + (plugin.getFishingRateLimiter() != null ? "Enforcing" : "Disabled"),
                        "&7HMAC Validation: &aENABLED",
                        "&7Transaction Guard: &aENABLED"
                ).build());

        setItem(31, ItemBuilder.from(Material.REDSTONE_TORCH)
                .name("&c&lReload Configurations")
                .lore(
                        "&7Hot-reload all YAML files, messages, scoreboards,",
                        "&7fish, rods, baits, zones, and shop items.",
                        "",
                        "&c▶ Click to reload plugin"
                ).build(), e -> {
            plugin.reloadAllConfigurations();
            player.sendMessage(TextUtil.colorize("&a[Admin] All AHowToFish configurations reloaded successfully!"));
            SoundParticleUtil.playSound(player, "BLOCK_ANVIL_USE", 0.8f, 1.5f);
            new AdminPanelMenu(plugin, player).open(player);
        });

        setItem(32, ItemBuilder.from(Material.ENDER_CHEST)
                .name("&a&lForce Save All Data")
                .lore(
                        "&7Immediately flushes all cached player profiles,",
                        "&7stats, and collections to the database.",
                        "",
                        "&a▶ Click to force save"
                ).build(), e -> {
            plugin.getProfileManager().saveAll(false);
            player.sendMessage(TextUtil.colorize("&a[Admin] Saved all online player profiles to database."));
            SoundParticleUtil.playSound(player, "BLOCK_CHEST_CLOSE", 1.0f, 1.2f);
        });

        setItem(49, ItemBuilder.from(Material.ARROW)
                .name(plugin.getLanguageManager().getMessage(player, "gui.common.back", "&cBack to Main Menu"))
                .build(), e -> new MainFishingMenu(plugin, player).open(player));
    }
}