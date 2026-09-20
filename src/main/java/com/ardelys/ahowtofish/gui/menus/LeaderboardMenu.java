package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.database.dao.ProfileDao;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class LeaderboardMenu extends CustomGui {
    private static final int[] RANK_SLOTS = {19, 20, 21, 22, 23, 24, 25, 29, 30, 31};

    private final AHowToFishPlugin plugin;
    private final Player player;
    private final String category;

    public LeaderboardMenu(AHowToFishPlugin plugin, Player player, String category) {
        super(54, plugin.getLanguageManager().getMessage(player, "gui.leaderboard.title", "&1&l✦ Fishing Leaderboards &8| &e{category}").replace("{category}", (category != null ? category.toUpperCase(java.util.Locale.ROOT) : "LEVEL")), "LEADERBOARD_MENU");
        this.plugin = plugin;
        this.player = player;
        this.category = (category != null) ? category.toUpperCase(java.util.Locale.ROOT) : "LEVEL";

        initialize();
    }

    private void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        setItem(1, createCategoryButton("LEVEL", "Highest Levels", Material.EXPERIENCE_BOTTLE));
        setItem(2, createCategoryButton("FISH_CAUGHT", "Most Catches", Material.COD));
        setItem(3, createCategoryButton("LARGEST_FISH", "Largest Weight", Material.ANVIL));
        setItem(4, createCategoryButton("TOTAL_WEIGHT", "Total Weight", Material.HEAVY_CORE != null ? Material.HEAVY_CORE : Material.IRON_BLOCK));
        setItem(5, createCategoryButton("MONEY_EARNED", "Most Wealth", Material.GOLD_INGOT));
        setItem(6, createCategoryButton("RARE_FISH", "Rare Fish", Material.DIAMOND));
        setItem(7, createCategoryButton("LEGENDARY_FISH", "Legendary Fish", Material.NETHER_STAR));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ProfileDao.LeaderboardEntry> list = plugin.getProfileDao().getLeaderboard(category, 10);
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (int i = 0; i < RANK_SLOTS.length; i++) {
                    int slot = RANK_SLOTS[i];
                    if (i < list.size()) {
                        ProfileDao.LeaderboardEntry entry = list.get(i);
                        int rank = i + 1;
                        Material skullMat = Material.PLAYER_HEAD;
                        String medal = switch (rank) {
                            case 1 -> "&e➊ 1st Place";
                            case 2 -> "&f➋ 2nd Place";
                            case 3 -> "&6➌ 3rd Place";
                            default -> "&7#" + rank;
                        };

                        setItem(slot, ItemBuilder.from(skullMat)
                                .name(medal + " &8- &f" + entry.name())
                                .lore(
                                        "&7Score: &a" + TextUtil.formatDecimal(entry.value()),
                                        "",
                                        "&7Player: &e" + entry.name()
                                ).build());
                    } else {
                        setItem(slot, ItemBuilder.from(Material.GRAY_DYE)
                                .name("&7#" + (i + 1) + " Empty")
                                .lore("&8No record yet.")
                                .build());
                    }
                }
            });
        });

        setItem(49, ItemBuilder.from(Material.DARK_OAK_DOOR)
                .name(plugin.getLanguageManager().getMessage(player, "gui.common.back", "&cBack to Main Menu"))
                .build(), e -> new MainFishingMenu(plugin, player).open(player));
    }

    private org.bukkit.inventory.ItemStack createCategoryButton(String cat, String title, Material mat) {
        boolean active = category.equalsIgnoreCase(cat);
        return ItemBuilder.from(mat)
                .name((active ? "&a&l✔ " : "&7") + title)
                .lore(active ? "&aCurrently viewing" : "&eClick to view standings")
                .glow(active)
                .build();
    }

    @Override
    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 1) { new LeaderboardMenu(plugin, player, "LEVEL").open(player); return; }
        if (slot == 2) { new LeaderboardMenu(plugin, player, "FISH_CAUGHT").open(player); return; }
        if (slot == 3) { new LeaderboardMenu(plugin, player, "LARGEST_FISH").open(player); return; }
        if (slot == 4) { new LeaderboardMenu(plugin, player, "TOTAL_WEIGHT").open(player); return; }
        if (slot == 5) { new LeaderboardMenu(plugin, player, "MONEY_EARNED").open(player); return; }
        if (slot == 6) { new LeaderboardMenu(plugin, player, "RARE_FISH").open(player); return; }
        if (slot == 7) { new LeaderboardMenu(plugin, player, "LEGENDARY_FISH").open(player); return; }

        super.handleClick(event);
    }
}
