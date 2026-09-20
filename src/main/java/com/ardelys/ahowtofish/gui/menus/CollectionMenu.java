package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.model.Fish;
import com.ardelys.ahowtofish.model.FishRarity;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.player.PlayerCollection;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class CollectionMenu extends CustomGui {
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int PAGE_SIZE = SLOTS.length; 

    private final AHowToFishPlugin plugin;
    private final Player player;
    private final int page;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public CollectionMenu(AHowToFishPlugin plugin, Player player, int page) {
        super(54, plugin.getLanguageManager().getMessage(player, "gui.collection.title", "&1&l✦ Fish Collection &8| &7Page {page}").replace("{page}", String.valueOf(page + 1)), "COLLECTION_MENU");
        this.plugin = plugin;
        this.player = player;
        this.page = page;

        initialize();
    }

    private void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);
        FishingProfile profile = plugin.getProfileManager().getProfile(player);

        List<Fish> allFish = new ArrayList<>(plugin.getFishManager().getAllFish());
        allFish.sort(Comparator.comparing(Fish::minLevel).thenComparing(Fish::id));

        int discCommon = 0, totCommon = 0;
        int discRare = 0, totRare = 0;
        int discEpic = 0, totEpic = 0;
        int discLegendary = 0, totLegendary = 0;
        int discMythic = 0, totMythic = 0;

        for (Fish f : allFish) {
            boolean disc = profile != null && profile.getCollection().hasDiscovered(f.id());
            String rId = f.rarityId() != null ? f.rarityId().toUpperCase(java.util.Locale.ROOT) : "COMMON";
            if (rId.contains("MYTHIC") || rId.contains("SECRET")) {
                totMythic++;
                if (disc) discMythic++;
            } else if (rId.contains("LEGENDARY")) {
                totLegendary++;
                if (disc) discLegendary++;
            } else if (rId.contains("EPIC")) {
                totEpic++;
                if (disc) discEpic++;
            } else if (rId.contains("RARE")) {
                totRare++;
                if (disc) discRare++;
            } else {
                totCommon++;
                if (disc) discCommon++;
            }
        }

        int totalDisc = profile != null ? profile.getCollection().getDiscoveredCount() : 0;
        double completionPercent = allFish.isEmpty() ? 0.0 : ((double) totalDisc / allFish.size()) * 100.0;

        setItem(4, ItemBuilder.from(Material.BOOK)
                .name(plugin.getLanguageManager().getMessage(player, "gui.collection.progress_title", "&e&l✦ Collection Progress &8(&a{percent}%&8) ✦").replace("{percent}", TextUtil.formatDecimal(completionPercent)))
                .lore(
                        plugin.getLanguageManager().getMessage(player, "gui.collection.total_discovered", "&7Total Discovered: &a{discovered}&7/&f{total}")
                                .replace("{discovered}", String.valueOf(totalDisc))
                                .replace("{total}", String.valueOf(allFish.size())),
                        "",
                        plugin.getLanguageManager().getMessage(player, "gui.collection.rarity_breakdown", "&7Rarity Breakdown:"),
                        " &fCommon: &a" + discCommon + "&7/&f" + totCommon,
                        " &9Rare: &a" + discRare + "&7/&f" + totRare,
                        " &5Epic: &a" + discEpic + "&7/&f" + totEpic,
                        " &6Legendary: &a" + discLegendary + "&7/&f" + totLegendary,
                        " &dMythic/Secret: &a" + discMythic + "&7/&f" + totMythic,
                        "",
                        plugin.getLanguageManager().getMessage(player, "gui.collection.tip", "&eCatch fish across different zones to complete your journal!")
                ).build());

        int totalPages = Math.max(1, (int) Math.ceil((double) allFish.size() / PAGE_SIZE));
        int startIndex = page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            int fishIndex = startIndex + i;
            int slot = SLOTS[i];

            if (fishIndex < allFish.size()) {
                Fish fish = allFish.get(fishIndex);
                boolean discovered = profile != null && profile.getCollection().hasDiscovered(fish.id());
                FishRarity rarity = plugin.getFishManager().getRarity(fish.rarityId());

                if (discovered) {
                    PlayerCollection.Record record = profile.getCollection().getRecord(fish.id());
                    String firstCaughtStr = dateFormat.format(new Date(record.firstCatchTimestamp()));

                    setItem(slot, ItemBuilder.from(fish.material() != null ? fish.material() : Material.COD)
                            .name(rarity.color() + fish.displayName())
                            .customModelData(fish.customModelData())
                            .lore(
                                    plugin.getLanguageManager().getMessage(player, "gui.collection.rarity_lbl", "&7Rarity: {color}{rarity}")
                                            .replace("{color}", rarity.color())
                                            .replace("{rarity}", rarity.displayName()),
                                    plugin.getLanguageManager().getMessage(player, "gui.collection.total_catches", "&7Total Catches: &e{count}")
                                            .replace("{count}", TextUtil.formatInteger(record.totalCatches())),
                                    plugin.getLanguageManager().getMessage(player, "gui.collection.record_weight", "&7Record Weight: &a{weight} kg")
                                            .replace("{weight}", TextUtil.formatDecimal(record.largestWeight())),
                                    plugin.getLanguageManager().getMessage(player, "gui.collection.total_weight", "&7Total Weight: &f{weight} kg")
                                            .replace("{weight}", TextUtil.formatDecimal(record.totalWeight())),
                                    plugin.getLanguageManager().getMessage(player, "gui.collection.first_caught", "&7First Caught: &7{date}")
                                            .replace("{date}", firstCaughtStr),
                                    "",
                                    plugin.getLanguageManager().getMessage(player, "gui.collection.discovered_in_journal", "&a✔ Discovered in journal")
                            ).build());
                } else {
                    setItem(slot, ItemBuilder.from(Material.GRAY_DYE)
                            .name(plugin.getLanguageManager().getMessage(player, "gui.collection.undiscovered_fish", "&8??? Undiscovered Fish"))
                            .lore(
                                    plugin.getLanguageManager().getMessage(player, "gui.collection.not_identified", "&7Species not yet identified."),
                                    plugin.getLanguageManager().getMessage(player, "gui.collection.req_level", "&7Required Level: &e{level}")
                                            .replace("{level}", String.valueOf(fish.minLevel())),
                                    "",
                                    plugin.getLanguageManager().getMessage(player, "gui.collection.locked_in_journal", "&c✘ Locked in journal")
                            ).build());
                }
            } else {
                setItem(slot, null);
            }
        }

        if (page > 0) {
            setItem(48, ItemBuilder.from(Material.ARROW)
                    .name(plugin.getLanguageManager().getMessage(player, "gui.common.previous_page", "&a« Previous Page"))
                    .build(), e -> new CollectionMenu(plugin, player, page - 1).open(player));
        }

        setItem(49, ItemBuilder.from(Material.DARK_OAK_DOOR)
                .name(plugin.getLanguageManager().getMessage(player, "gui.common.back", "&cBack to Main Menu"))
                .build(), e -> new MainFishingMenu(plugin, player).open(player));

        if (page < totalPages - 1) {
            setItem(50, ItemBuilder.from(Material.ARROW)
                    .name(plugin.getLanguageManager().getMessage(player, "gui.common.next_page", "&aNext Page »"))
                    .build(), e -> new CollectionMenu(plugin, player, page + 1).open(player));
        }
    }
}
