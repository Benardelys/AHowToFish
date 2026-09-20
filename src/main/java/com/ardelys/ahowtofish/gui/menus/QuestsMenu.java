package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.model.FishingQuest;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class QuestsMenu extends CustomGui {
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final AHowToFishPlugin plugin;
    private final Player player;
    private final FishingProfile profile;

    public QuestsMenu(AHowToFishPlugin plugin, Player player) {
        super(45, plugin.getLanguageManager().getMessage(player, "gui.quests.title", "&1&l✦ Fishing Quests"), "QUESTS_MENU");
        this.plugin = plugin;
        this.player = player;
        this.profile = plugin.getProfileManager().getProfile(player);

        initialize();
    }

    private void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        List<FishingQuest> quests = new ArrayList<>(plugin.getQuestManager().getAllQuests());
        for (int i = 0; i < quests.size() && i < SLOTS.length; i++) {
            FishingQuest quest = quests.get(i);
            int slot = SLOTS[i];

            boolean completed = profile != null && profile.isQuestCompleted(quest.id());
            double progress = profile != null ? Math.min(quest.requiredAmount(), profile.getQuestProgress(quest.id())) : 0.0;
            double percent = Math.min(100.0, (progress / quest.requiredAmount()) * 100.0);

            Material mat = completed ? Material.WRITTEN_BOOK : Material.BOOK;
            String status = completed ?
                    plugin.getLanguageManager().getMessage(player, "gui.quests.completed", "&a✔ Completed") :
                    plugin.getLanguageManager().getMessage(player, "gui.quests.in_progress", "&eIn Progress ({percent}%)").replace("{percent}", TextUtil.formatDecimal(percent));

            String questName = plugin.getQuestManager().getLocalizedQuestName(player, quest);
            List<String> lore = new ArrayList<>();
            List<String> descList = plugin.getLanguageManager().getMessageList(player, "quest_translations." + quest.id() + ".desc");
            if (descList.isEmpty()) {
                for (String line : quest.description()) {
                    lore.add(TextUtil.colorize(line));
                }
            } else {
                for (String line : descList) {
                    lore.add(TextUtil.colorize(line));
                }
            }
            lore.add("");
            lore.add(plugin.getLanguageManager().getMessage(player, "gui.quests.type", "&7Type: &f{type}").replace("{type}", quest.type().name()));
            lore.add(plugin.getLanguageManager().getMessage(player, "gui.quests.progress", "&7Progress: &e{cur}&7/&f{req}")
                    .replace("{cur}", String.valueOf((int) progress))
                    .replace("{req}", String.valueOf((int) quest.requiredAmount())));
            lore.add(plugin.getLanguageManager().getMessage(player, "gui.quests.rewards", "&7Rewards: &b+{xp} XP &8| &a+${money}")
                    .replace("{xp}", String.valueOf(quest.xpReward()))
                    .replace("{money}", TextUtil.formatDecimal(quest.moneyReward())));
            lore.add("");
            lore.add(status);

            setItem(slot, ItemBuilder.from(mat)
                    .name("&6&l" + questName)
                    .lore(lore)
                    .build());
        }

        setItem(40, ItemBuilder.from(Material.DARK_OAK_DOOR)
                .name(plugin.getLanguageManager().getMessage(player, "gui.common.back", "&cBack to Main Menu"))
                .build(), e -> new MainFishingMenu(plugin, player).open(player));
    }
}
