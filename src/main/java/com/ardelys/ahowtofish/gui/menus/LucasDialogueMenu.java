package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.model.FishingQuest;
import com.ardelys.ahowtofish.model.FishingQuest.QuestState;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.quest.PlayerQuestProgress;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LucasDialogueMenu extends CustomGui {

    private final AHowToFishPlugin plugin;
    private final Player player;

    public LucasDialogueMenu(AHowToFishPlugin plugin, Player player) {
        super(45, plugin.getLanguageManager().getMessage(player, "gui.lucas.dialogue_title", "&8Fisherman Lucas &8| &6Mentor & Merchant"), "LUCAS_DIALOGUE");
        this.plugin = plugin;
        this.player = player;

        initialize();
    }

    private void initialize() {
        fillBorder();

        FishingProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null) return;

        boolean hasRod = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.FISHING_ROD) {
                hasRod = true;
                break;
            }
        }
        if (!profile.isStarterReceived() || (!profile.isQuestCompleted("first-rod") && !hasRod)) {
            plugin.getQuestManager().giveStarterRod(player);
        }

        String activeQuestId = profile.getActiveQuestId();
        FishingQuest activeQuest = activeQuestId != null ? plugin.getQuestManager().getQuest(activeQuestId) : null;
        PlayerQuestProgress qp = activeQuest != null ? profile.getPlayerQuestProgress(activeQuest.id()) : null;

        if (activeQuest == null) {
            for (String qId : plugin.getQuestManager().getTutorialQuestSequence()) {
                if (!profile.isQuestCompleted(qId)) {
                    activeQuest = plugin.getQuestManager().getQuest(qId);
                    if (activeQuest != null) {
                        qp = profile.getPlayerQuestProgress(activeQuest.id());
                        if (qp == null) {
                            qp = new PlayerQuestProgress(activeQuest.id(), QuestState.AVAILABLE, 0.0, 0L, 0L, 0L, 0L);
                            profile.setPlayerQuestProgress(activeQuest.id(), qp);
                        }
                    }
                    break;
                }
            }
        }

        QuestState state = qp != null ? qp.getState() : QuestState.LOCKED;

        setupLucasAvatar(state, activeQuest, qp, profile);

        setupQuestCard(profile, activeQuest, qp);

        setItem(20, new ItemBuilder(Material.EMERALD)
                .setName(plugin.getLanguageManager().getMessage(player, "gui.lucas.btn_sell_title", "&a&l✦ Fish Market &8(Sell Fish)"))
                .setLore(
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.btn_sell_desc1", "&7Sell your fresh catches to Fisherman Lucas."),
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.btn_sell_desc2", "&7Choose individual fish or use Sell All!"),
                        "",
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.click_to_open", "&eClick to open market!")
                )
                .build(), e -> {
            new FishermanLucasMenu(plugin, player).open(player);
        });

        setItem(24, new ItemBuilder(Material.FISHING_ROD)
                .setName(plugin.getLanguageManager().getMessage(player, "gui.lucas.btn_shop_title", "&6&l✦ Tackle & Bait Shop"))
                .setLore(
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.btn_shop_desc1", "&7Purchase specialized baits and upgraded rods."),
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.btn_shop_desc2", "&7Upgrade your gear to land higher rarity fish."),
                        "",
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.click_to_open", "&eClick to open shop!")
                )
                .build(), e -> {
            new ShopMenu(plugin, player).open(player);
        });

        setItem(38, new ItemBuilder(Material.BOOK)
                .setName(plugin.getLanguageManager().getMessage(player, "gui.lucas.btn_quests_title", "&d&l✦ All Fishing Quests"))
                .setLore(
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.btn_quests_desc", "&7View daily, weekly, and permanent contracts."),
                        "",
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.click_to_view", "&eClick to inspect quests!")
                )
                .build(), e -> {
            new QuestsMenu(plugin, player).open(player);
        });

        setItem(40, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(plugin.getLanguageManager().getMessage(player, "gui.lucas.btn_progression_title", "&b&l✦ Angler Level & Mastery"))
                .setLore(
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.btn_progression_desc", "&7Track your fishing skill milestones and level unlocks."),
                        "",
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.click_to_open", "&eClick to open progression!")
                )
                .build(), e -> {
            new LevelProgressionMenu(plugin, player).open(player);
        });

        setItem(42, new ItemBuilder(Material.BARRIER)
                .setName(plugin.getLanguageManager().getMessage(player, "gui.common.close", "&cClose"))
                .build(), e -> player.closeInventory());
    }

    private void setupLucasAvatar(QuestState state, FishingQuest activeQuest, PlayerQuestProgress qp, FishingProfile profile) {
        String speech = "";
        if (activeQuest != null) {
            double cur = 0.0;
            if (qp != null) {
                cur = qp.getProgress();
            } else if (profile != null && activeQuest.objectiveType() == FishingQuest.QuestObjectiveType.REACH_LEVEL) {
                cur = profile.getLevel();
            }
            speech = plugin.getQuestManager().getLocalizedQuestDialogue(player, activeQuest, state, cur, activeQuest.requiredAmount());
        }
        if (speech.isBlank()) {
            if (state == QuestState.COMPLETED) {
                speech = plugin.getLanguageManager().getMessage(player, "gui.lucas.speech_completed", "Nice work! Let's see what you've accomplished.");
            } else if (state == QuestState.ACTIVE) {
                speech = plugin.getLanguageManager().getMessage(player, "gui.lucas.speech_active", "How's the job going? Keep your eyes on the bobber!");
            } else if (state == QuestState.AVAILABLE) {
                speech = plugin.getLanguageManager().getMessage(player, "gui.lucas.speech_available", "I've got a task for you when you're ready.");
            } else if (state == QuestState.EXPIRED) {
                speech = plugin.getLanguageManager().getMessage(player, "gui.lucas.speech_expired", "Time got away from you! Don't worry, we can try again.");
            } else {
                speech = plugin.getLanguageManager().getMessage(player, "gui.lucas.speech_default", "Keep fishing! There are plenty of waters and rare catches to explore.");
            }
        }

        setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.avatar_name", "&6&lFisherman Lucas &7(Mentor)")))
                .setLore(
                        "",
                        TextUtil.colorize("&7\"" + speech + "\""),
                        ""
                )
                .build(), null);
    }

    private void setupQuestCard(FishingProfile profile, FishingQuest quest, PlayerQuestProgress qp) {
        if (quest == null || qp == null) {
            setItem(22, new ItemBuilder(Material.MAP)
                    .setName(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_all_completed_title", "&a&lAll Tasks Completed!")))
                    .setLore(
                            TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_all_completed_desc1", "&7You have mastered all of Lucas's starter lessons.")),
                            TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_all_completed_desc2", "&7Visit &e/fish quests &7for daily and weekly contracts."))
                    )
                    .build(), null);
            return;
        }

        QuestState state = qp.getState();
        Material iconMat;
        String statusPrefix;
        List<String> lore = new ArrayList<>();

        lore.add("");
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

        int cur = (int) Math.min(qp.getProgress(), quest.requiredAmount());
        int req = (int) quest.requiredAmount();
        String progressBar = buildProgressBar(cur, req);
        lore.add(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_progress_lbl", "&7Progress: &f{cur} &8/ &f{req}  {bar}")
                .replace("{cur}", String.valueOf(cur))
                .replace("{req}", String.valueOf(req))
                .replace("{bar}", progressBar)));

        if (qp.getExpiresAt() > 0 && state == QuestState.ACTIVE) {
            lore.add(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_time_lbl", "&7Time Remaining: &c{time}")
                    .replace("{time}", TextUtil.formatTime(qp.getRemainingSeconds()))));
        }

        lore.add("");
        lore.add(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_rewards_lbl", "&6Rewards:")));
        if (quest.moneyReward() > 0) {
            lore.add(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_reward_money", " &8• &a+${money}")
                    .replace("{money}", TextUtil.formatDecimal(quest.moneyReward()))));
        }
        if (quest.xpReward() > 0) {
            lore.add(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_reward_xp", " &8• &b+{xp} Fishing XP")
                    .replace("{xp}", String.valueOf(quest.xpReward()))));
        }
        if (quest.rewardRod() != null && !quest.rewardRod().isBlank()) {
            lore.add(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_reward_rod", " &8• &6Rod: &f{rod}")
                    .replace("{rod}", quest.rewardRod())));
        }
        if (quest.rewardBait() != null && !quest.rewardBait().isBlank()) {
            lore.add(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_reward_bait", " &8• &eBait: &f{bait}")
                    .replace("{bait}", quest.rewardBait())));
        }
        lore.add("");

        switch (state) {
            case COMPLETED -> {
                iconMat = Material.GOLD_BLOCK;
                statusPrefix = plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_status_completed", "&a&l[CLAIM REWARD] ");
                lore.add(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_claim_click", "&a&l✔ COMPLETED! &eClick to claim your reward!")));
            }
            case ACTIVE -> {
                iconMat = Material.WRITABLE_BOOK;
                statusPrefix = plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_status_active", "&e&l[IN PROGRESS] ");
                lore.add(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_active_click", "&eClick to inspect progress.")));
            }
            case AVAILABLE -> {
                iconMat = Material.PAPER;
                statusPrefix = plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_status_available", "&6&l[ACCEPT QUEST] ");
                lore.add(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_accept_click", "&eClick to accept this quest!")));
            }
            case EXPIRED -> {
                iconMat = Material.CLOCK;
                statusPrefix = plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_status_expired", "&c&l[EXPIRED] ");
                if (qp.isCooldownActive()) {
                    lore.add(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_cooldown_msg", "&cCooldown: &e{time}s remaining before retry.")
                            .replace("{time}", String.valueOf(qp.getRemainingCooldownSeconds()))));
                } else {
                    lore.add(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_retry_click", "&eClick to retry this quest!")));
                }
            }
            default -> {
                iconMat = Material.GRAY_DYE;
                statusPrefix = plugin.getLanguageManager().getMessage(player, "gui.lucas.quest_status_locked", "&7&l[LOCKED] ");
            }
        }

        String questName = plugin.getQuestManager().getLocalizedQuestName(player, quest);
        setItem(22, new ItemBuilder(iconMat)
                .setName(TextUtil.colorize(statusPrefix + questName))
                .setLore(lore)
                .build(), e -> {
            if (state == QuestState.COMPLETED) {
                plugin.getQuestManager().claimReward(player, quest.id());
                player.closeInventory();
            } else if (state == QuestState.AVAILABLE) {
                plugin.getQuestManager().startQuest(player, profile, quest.id(), true);
                if ("first-rod".equalsIgnoreCase(quest.id())) {
                    plugin.getQuestManager().giveStarterRod(player);
                }
                player.closeInventory();
            } else if (state == QuestState.EXPIRED) {
                if (plugin.getQuestManager().retryQuest(player, quest.id())) {
                    player.closeInventory();
                }
            } else if (state == QuestState.ACTIVE) {
                if ("first-rod".equalsIgnoreCase(quest.id())) {
                    plugin.getQuestManager().giveStarterRod(player);
                }
                SoundParticleUtil.playSound(player, "BLOCK_NOTE_BLOCK_PLING", 1.0f, 1.2f);
                String barMsg = plugin.getLanguageManager().getMessage(player, "quests_system.action_bar", "&6Quest: &f{quest} &8| &e{cur}/{req}{timer}")
                        .replace("{quest}", questName)
                        .replace("{cur}", String.valueOf(cur))
                        .replace("{req}", String.valueOf(req))
                        .replace("{timer}", "");
                TextUtil.sendActionBar(player, barMsg);
            }
        });
    }

    private String buildProgressBar(int current, int max) {
        int totalBars = 10;
        float percent = max > 0 ? (float) current / max : 0f;
        int filled = Math.min(totalBars, (int) (percent * totalBars));
        StringBuilder sb = new StringBuilder("&a");
        for (int i = 0; i < filled; i++) sb.append("█");
        sb.append("&7");
        for (int i = filled; i < totalBars; i++) sb.append("░");
        return sb.toString();
    }

    private void fillBorder() {
        ItemStack glass = new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 9; i++) {
            if (i != 4) setItem(i, glass, null);
            setItem(36 + i, glass, null);
        }
        setItem(9, glass, null);
        setItem(17, glass, null);
        setItem(18, glass, null);
        setItem(26, glass, null);
        setItem(27, glass, null);
        setItem(35, glass, null);
    }
}