package com.ardelys.ahowtofish.quest;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.integration.VaultHook;
import com.ardelys.ahowtofish.language.LanguageManager;
import com.ardelys.ahowtofish.language.MessageKey;
import com.ardelys.ahowtofish.model.FishCatchResult;
import com.ardelys.ahowtofish.model.FishingQuest;
import com.ardelys.ahowtofish.model.FishingQuest.QuestCategory;
import com.ardelys.ahowtofish.model.FishingQuest.QuestObjectiveType;
import com.ardelys.ahowtofish.model.FishingQuest.QuestState;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.player.ProfileManager;
import com.ardelys.ahowtofish.progression.ProgressionManager;
import com.ardelys.ahowtofish.security.MultiplierService;
import com.ardelys.ahowtofish.security.TransactionManager;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class QuestManager {
    private final AHowToFishPlugin plugin;
    private final ProfileManager profileManager;
    private final ProgressionManager progressionManager;
    private final VaultHook vaultHook;
    private final LanguageManager languageManager;

    private final Map<String, FishingQuest> questRegistry = new ConcurrentHashMap<>();
    private final List<String> tutorialQuestSequence = new ArrayList<>();

    private boolean starterEnabled = true;
    private boolean firstJoinGuide = true;
    private boolean autoStartFirstQuest = true;
    private String firstQuestId = "first-rod";
    private String starterRodId = "lucas_starter_rod";
    private boolean pauseOnDisconnect = false;

    private BukkitTask timerTask = null;
    private final Set<UUID> activeTimedQuestPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public QuestManager(AHowToFishPlugin plugin,
                        ProfileManager profileManager,
                        ProgressionManager progressionManager,
                        VaultHook vaultHook,
                        LanguageManager languageManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.progressionManager = progressionManager;
        this.vaultHook = vaultHook;
        this.languageManager = languageManager;
    }

    public void load() {
        activeTimedQuestPlayers.clear();
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        questRegistry.clear();
        tutorialQuestSequence.clear();

        File file = new File(plugin.getDataFolder(), "quests.yml");
        if (!file.exists()) {
            plugin.saveResource("quests.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection starterSection = config.getConfigurationSection("starter");
        if (starterSection != null) {
            this.starterEnabled = starterSection.getBoolean("enabled", true);
            this.firstJoinGuide = starterSection.getBoolean("first-join-guide", true);
            this.autoStartFirstQuest = starterSection.getBoolean("auto-start", true);
            this.firstQuestId = starterSection.getString("first-quest", "first-rod");
            this.starterRodId = starterSection.getString("rod.item-id", "lucas_starter_rod");
        }
        this.pauseOnDisconnect = config.getBoolean("quest-timer.pause-on-disconnect", false);

        ConfigurationSection questsSection = config.getConfigurationSection("quests");
        if (questsSection != null) {
            for (String key : questsSection.getKeys(false)) {
                try {
                    ConfigurationSection qs = questsSection.getConfigurationSection(key);
                    if (qs == null) continue;

                    String name = qs.getString("name", key);

                    List<String> desc;
                    if (qs.isList("description")) {
                        desc = qs.getStringList("description");
                    } else {
                        desc = List.of(qs.getString("description", ""));
                    }

                    String catStr = qs.getString("category", "TUTORIAL").toUpperCase(Locale.ROOT);
                    QuestCategory category;
                    try {
                        category = QuestCategory.valueOf(catStr);
                    } catch (Exception ignored) {
                        category = QuestCategory.TUTORIAL;
                    }

                    String typeStr = qs.getString("objective-type",
                            qs.getString("objective.type",
                            qs.getString("type", "CATCH_TOTAL"))).toUpperCase(Locale.ROOT);
                    QuestObjectiveType objectiveType;
                    try {
                        objectiveType = QuestObjectiveType.valueOf(typeStr);
                    } catch (Exception ignored) {
                        objectiveType = QuestObjectiveType.CATCH_TOTAL;
                    }

                    double reqAmount = qs.getDouble("required-amount",
                            qs.getDouble("objective.amount",
                            qs.getDouble("amount", 1.0)));
                    String target = qs.getString("target", "");

                    int timeLimit = qs.getInt("time-limit-seconds", 0);
                    if (timeLimit <= 0 && qs.getBoolean("time-limit.enabled", false)) {
                        timeLimit = qs.getInt("time-limit.duration", 300);
                    }

                    int retryCooldown = qs.getInt("retry-cooldown-seconds",
                            qs.getInt("on-expire.cooldown",
                            qs.getInt("retry-cooldown", 60)));
                    List<String> prereqs = qs.getStringList("requirements.completed-quests");
                    String nextQuest = qs.getString("next-quest", null);

                    long xpReward = qs.getLong("rewards.xp", 0);
                    double moneyReward = qs.getDouble("rewards.money", qs.getDouble("rewards.coins", 0.0));
                    String rewardRod = qs.getString("rewards.rod", null);
                    String rewardBait = qs.getString("rewards.bait", null);
                    List<String> cmdRewards = qs.getStringList("rewards.commands");
                    String sound = qs.getString("rewards.sound", qs.getString("sound", "UI_TOAST_CHALLENGE_COMPLETE"));

                    String dOffer = qs.getString("dialogues.offer", qs.getString("dialogue.offer", ""));
                    String dProgress = qs.getString("dialogues.progress", qs.getString("dialogue.progress", ""));
                    String dComplete = qs.getString("dialogues.complete", qs.getString("dialogue.complete", ""));
                    String dExpire = qs.getString("dialogues.expire", qs.getString("dialogue.expire", ""));

                    FishingQuest quest = new FishingQuest(
                            key.toLowerCase(Locale.ROOT),
                            name,
                            desc,
                            category,
                            objectiveType,
                            reqAmount,
                            target,
                            timeLimit,
                            retryCooldown,
                            prereqs,
                            nextQuest,
                            xpReward,
                            moneyReward,
                            rewardRod,
                            rewardBait,
                            cmdRewards,
                            sound,
                            dOffer,
                            dProgress,
                            dComplete,
                            dExpire
                    );

                    questRegistry.put(quest.id(), quest);

                    if (category == QuestCategory.STARTER || category == QuestCategory.TUTORIAL) {
                        tutorialQuestSequence.add(quest.id());
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[AHowToFish] Failed to load quest '" + key + "': " + e.getMessage());
                }
            }
        }

        startTimerWatchdog();
        plugin.getLogger().info("[AHowToFish] Loaded " + questRegistry.size() + " quest(s) with " + tutorialQuestSequence.size() + " tutorial steps.");
    }

    private void startTimerWatchdog() {
        
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeTimedQuestPlayers.isEmpty()) return;
            long now = System.currentTimeMillis();
            for (Iterator<UUID> it = activeTimedQuestPlayers.iterator(); it.hasNext(); ) {
                UUID uuid = it.next();
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    it.remove();
                    continue;
                }
                FishingProfile profile = profileManager.getProfile(player);
                if (profile == null || !profile.isReady()) continue;

                String activeId = profile.getActiveQuestId();
                if (activeId == null) {
                    it.remove();
                    continue;
                }

                PlayerQuestProgress qp = profile.getPlayerQuestProgress(activeId);
                if (qp == null || qp.getState() != QuestState.ACTIVE || qp.getExpiresAt() <= 0) {
                    it.remove();
                    continue;
                }

                if (now >= qp.getExpiresAt()) {
                    it.remove();
                    
                    qp.setState(QuestState.EXPIRED);
                    FishingQuest quest = questRegistry.get(activeId);
                    int cdSec = quest != null ? quest.retryCooldownSeconds() : 60;
                    qp.setCooldownUntil(now + (cdSec * 1000L));
                    profile.markDirty();

                    SoundParticleUtil.playSound(player, "BLOCK_ANVIL_LAND", 0.8f, 0.7f);
                    String qName = quest != null ? getLocalizedQuestName(player, quest) : activeId;
                    TextUtil.sendTitle(
                            player,
                            plugin.getLanguageManager().getMessage(player, "quests_system.expired_title", "&c&lQUEST EXPIRED"),
                            plugin.getLanguageManager().getMessage(player, "quests_system.expired_subtitle", "&7Time expired: &e{quest}").replace("{quest}", qName),
                            10, 50, 20
                    );
                    player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "quests_system.expired_dialogue", "&8[&6Lucas&8] &c\"Time ran out lad! Come over and we will try again.\"")));
                }
            }
        }, 20L, 20L);
    }

    public void onPlayerJoin(Player player) {
        if (player == null || !player.isOnline()) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            FishingProfile profile = profileManager.getProfile(player);
            if (profile == null || !profile.isReady()) return;

            String activeId = profile.getActiveQuestId();
            if (activeId != null) {
                PlayerQuestProgress activeProgress = profile.getPlayerQuestProgress(activeId);
                if (activeProgress != null && activeProgress.getState() == QuestState.ACTIVE && activeProgress.getExpiresAt() > 0) {
                    activeTimedQuestPlayers.add(player.getUniqueId());
                }
            }

            if (!starterEnabled) return;

            if (!profile.isStarterReceived()) {
                if (firstJoinGuide) {
                    player.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
                    player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "quests_system.welcome_banner_title", "&6&l✦ Welcome to AHowToFish Fishing World! ✦")));
                    player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "quests_system.welcome_banner_desc", "&7Visit &6Fisherman Lucas &7at the pier to get your starter rod and begin.")));
                    player.sendMessage(TextUtil.colorize("&8&m----------------------------------------"));
                }

                if (autoStartFirstQuest && firstQuestId != null && questRegistry.containsKey(firstQuestId)) {
                    PlayerQuestProgress qp = profile.getPlayerQuestProgress(firstQuestId);
                    if (qp == null || qp.getState() == QuestState.LOCKED) {
                        startQuest(player, profile, firstQuestId, false);
                    }
                }
            }
        }, 30L);
    }

    public void onPlayerQuit(Player player) {
        if (player != null) {
            activeTimedQuestPlayers.remove(player.getUniqueId());
        }
    }

    public boolean giveStarterRod(Player player) {
        if (player == null || !player.isOnline()) return false;
        FishingProfile profile = profileManager.getProfile(player);
        if (profile == null) return false;

        synchronized (profile) {
            if (profile.isStarterReceived()) {
                
                boolean hasRod = false;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == Material.FISHING_ROD) {
                        hasRod = true;
                        break;
                    }
                }
                if (hasRod) {
                    return false; 
                }
            }
            profile.setStarterReceived(true);
        }

        boolean given = plugin.giveRod(player, starterRodId);
        if (!given) {
            
            plugin.giveRod(player, "basic_rod");
        }

        SoundParticleUtil.playSound(player, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.2f);
        player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "quests_system.starter_rod_received", "&8[&6Lucas&8] &a\"Here is your starter rod! Take good care of it.\"")));

        onObjectiveProgress(player, profile, QuestObjectiveType.RECEIVE_ITEM, 1.0, starterRodId);

        profileManager.saveProfileAsync(profile);
        return true;
    }

    public void resetStarterProgression(Player player) {
        if (player == null) return;
        FishingProfile profile = profileManager.getProfile(player);
        if (profile == null) return;

        synchronized (profile) {
            profile.setStarterReceived(false);
            profile.setActiveQuestId(null);
            activeTimedQuestPlayers.remove(player.getUniqueId());
            for (String qId : tutorialQuestSequence) {
                PlayerQuestProgress qp = profile.getPlayerQuestProgress(qId);
                if (qp != null) {
                    qp.setState(QuestState.LOCKED);
                    qp.setProgress(0.0);
                    qp.setStartedAt(0L);
                    qp.setExpiresAt(0L);
                    qp.setCompletedAt(0L);
                    qp.setCooldownUntil(0L);
                }
                profile.getQuestProgress().remove(qId);
                profile.getCompletedQuests().remove(qId);
            }
        }
        profileManager.saveProfileAsync(profile);
    }

    public FishingQuest getActiveQuest(Player player) {
        if (player == null) return null;
        FishingProfile profile = profileManager.getProfile(player);
        if (profile == null || profile.getActiveQuestId() == null) return null;
        return questRegistry.get(profile.getActiveQuestId().toLowerCase(Locale.ROOT));
    }

    public boolean startQuest(Player player, String questId) {
        if (player == null || questId == null) return false;
        FishingProfile profile = profileManager.getProfile(player);
        if (profile == null) return false;
        return startQuest(player, profile, questId, true);
    }

    public boolean forceCompleteQuest(Player player, String questId) {
        if (player == null || questId == null) return false;
        FishingProfile profile = profileManager.getProfile(player);
        if (profile == null) return false;
        FishingQuest quest = questRegistry.get(questId.toLowerCase(Locale.ROOT));
        if (quest == null) return false;
        PlayerQuestProgress qp = profile.getPlayerQuestProgress(quest.id());
        if (qp == null) {
            qp = new PlayerQuestProgress(quest.id());
            profile.setPlayerQuestProgress(quest.id(), qp);
        }
        qp.setProgress(quest.requiredAmount());
        completeQuest(player, profile, quest, qp);
        return true;
    }

    public boolean skipQuest(Player player) {
        if (player == null) return false;
        FishingProfile profile = profileManager.getProfile(player);
        if (profile == null || profile.getActiveQuestId() == null) return false;
        FishingQuest current = questRegistry.get(profile.getActiveQuestId().toLowerCase(Locale.ROOT));
        if (current == null) return false;
        PlayerQuestProgress qp = profile.getPlayerQuestProgress(current.id());
        if (qp != null) {
            qp.setState(QuestState.CLAIMED);
            qp.setProgress(current.requiredAmount());
            qp.setCompletedAt(System.currentTimeMillis());
            profile.markQuestCompleted(current.id());
        }
        activeTimedQuestPlayers.remove(player.getUniqueId());
        if (current.nextQuestId() != null && questRegistry.containsKey(current.nextQuestId())) {
            startQuest(player, profile, current.nextQuestId(), true);
        } else {
            profile.setActiveQuestId(null);
        }
        profileManager.saveProfileAsync(profile);
        return true;
    }

    public boolean resetQuest(Player player, String questId) {
        if (player == null || questId == null) return false;
        FishingProfile profile = profileManager.getProfile(player);
        if (profile == null) return false;

        synchronized (profile) {
            if (questId.equalsIgnoreCase("all")) {
                profile.setActiveQuestId(null);
                activeTimedQuestPlayers.remove(player.getUniqueId());
                for (PlayerQuestProgress qp : profile.getPlayerQuests().values()) {
                    qp.setState(QuestState.LOCKED);
                    qp.setProgress(0.0);
                    qp.setStartedAt(0L);
                    qp.setExpiresAt(0L);
                    qp.setCompletedAt(0L);
                    qp.setCooldownUntil(0L);
                }
                profile.getQuestProgress().clear();
                profile.getCompletedQuests().clear();
            } else {
                String targetId = questId.toLowerCase(Locale.ROOT);
                if (targetId.equalsIgnoreCase(profile.getActiveQuestId())) {
                    profile.setActiveQuestId(null);
                    activeTimedQuestPlayers.remove(player.getUniqueId());
                }
                PlayerQuestProgress qp = profile.getPlayerQuestProgress(targetId);
                if (qp != null) {
                    qp.setState(QuestState.LOCKED);
                    qp.setProgress(0.0);
                    qp.setStartedAt(0L);
                    qp.setExpiresAt(0L);
                    qp.setCompletedAt(0L);
                    qp.setCooldownUntil(0L);
                }
                profile.getQuestProgress().remove(targetId);
                profile.getCompletedQuests().remove(targetId);
            }
        }
        profileManager.saveProfileAsync(profile);
        return true;
    }

    public boolean startQuest(Player player, FishingProfile profile, String questId, boolean notify) {
        if (player == null || profile == null || questId == null) return false;
        FishingQuest quest = questRegistry.get(questId.toLowerCase(Locale.ROOT));
        if (quest == null) return false;

        PlayerQuestProgress qp = profile.getPlayerQuestProgress(quest.id());
        if (qp == null) {
            qp = new PlayerQuestProgress(quest.id());
            profile.setPlayerQuestProgress(quest.id(), qp);
        }

        if (!quest.prerequisites().isEmpty()) {
            for (String pre : quest.prerequisites()) {
                if (!profile.isQuestCompleted(pre)) {
                    if (notify) {
                        player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "quests_system.prerequisite_required", "&cYou must first complete prerequisite quests: &e{prereq}").replace("{prereq}", pre)));
                    }
                    return false;
                }
            }
        }

        long now = System.currentTimeMillis();
        qp.setState(QuestState.ACTIVE);
        qp.setProgress(0.0);
        qp.setStartedAt(now);
        if (quest.timeLimitSeconds() > 0) {
            qp.setExpiresAt(now + (quest.timeLimitSeconds() * 1000L));
            activeTimedQuestPlayers.add(player.getUniqueId());
        } else {
            qp.setExpiresAt(0L);
            activeTimedQuestPlayers.remove(player.getUniqueId());
        }
        qp.setCooldownUntil(0L);

        profile.setActiveQuestId(quest.id());
        profile.markDirty();

        if (notify) {
            SoundParticleUtil.playSound(player, "BLOCK_NOTE_BLOCK_CHIME", 1.0f, 1.2f);
            TextUtil.sendTitle(
                    player,
                    plugin.getLanguageManager().getMessage(player, "quests_system.accepted_title", "&6&lQUEST ACCEPTED"),
                    getLocalizedQuestName(player, quest),
                    10, 40, 15
            );
            String dOffer = getLocalizedQuestDialogue(player, quest, QuestState.AVAILABLE);
            if (!dOffer.isBlank()) {
                player.sendMessage(TextUtil.colorize("&8[&6Lucas&8] &7\"" + dOffer + "\""));
            }
        }

        return true;
    }

    public boolean retryQuest(Player player, String questId) {
        if (player == null || questId == null) return false;
        FishingProfile profile = profileManager.getProfile(player);
        if (profile == null) return false;

        PlayerQuestProgress qp = profile.getPlayerQuestProgress(questId);
        if (qp == null || qp.getState() != QuestState.EXPIRED) return false;

        if (qp.isCooldownActive()) {
            player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "quests_system.retry_cooldown", "&cPlease wait &e{seconds}s &cbefore retrying this quest!").replace("{seconds}", String.valueOf(qp.getRemainingCooldownSeconds()))));
            return false;
        }

        return startQuest(player, profile, questId, true);
    }

    public boolean claimReward(Player player, String questId) {
        if (player == null || !player.isOnline() || questId == null) return false;
        FishingProfile profile = profileManager.getProfile(player);
        if (profile == null) return false;

        FishingQuest quest = questRegistry.get(questId.toLowerCase(Locale.ROOT));
        if (quest == null) return false;

        PlayerQuestProgress qp = profile.getPlayerQuestProgress(quest.id());
        if (qp == null || qp.getState() != QuestState.COMPLETED) {
            return false;
        }

        synchronized (profile) {
            if (qp.getState() == QuestState.CLAIMED) {
                return false;
            }
            qp.setState(QuestState.CLAIMED);
            qp.setCompletedAt(System.currentTimeMillis());
            profile.markQuestCompleted(quest.id());
        }

        String txId = plugin.getTransactionManager().generateTransactionId(player.getUniqueId());
        plugin.getTransactionManager().recordTransaction(txId, player.getUniqueId(), TransactionManager.TransactionStatus.COMPLETED, "quest_" + quest.id());

        if (quest.xpReward() > 0) {
            long xp = MultiplierService.sanitizeXp(quest.xpReward());
            progressionManager.addXp(player, xp);
        }
        if (quest.moneyReward() > 0 && vaultHook != null && vaultHook.isAvailable()) {
            double money = MultiplierService.sanitizeMoney(quest.moneyReward());
            vaultHook.deposit(player, money);
        }
        if (quest.rewardRod() != null && !quest.rewardRod().isBlank()) {
            plugin.giveRod(player, quest.rewardRod());
        }
        if (quest.rewardBait() != null && !quest.rewardBait().isBlank()) {
            plugin.giveBait(player, quest.rewardBait(), 5);
        }
        for (String cmd : quest.commandRewards()) {
            if (plugin.getSecurityManager() != null) {
                plugin.getSecurityManager().executeConsoleReward(player, cmd);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
            }
        }

        SoundParticleUtil.playSound(player, quest.soundReward(), 1.0f, 1.0f);
        TextUtil.sendTitle(
                player,
                plugin.getLanguageManager().getMessage(player, "quests_system.reward_claimed_title", "&6&lREWARD CLAIMED!"),
                plugin.getLanguageManager().getMessage(player, "quests_system.reward_claimed_subtitle", "&a+${money} &8| &b+{xp} XP").replace("{money}", TextUtil.formatDecimal(quest.moneyReward())).replace("{xp}", String.valueOf(quest.xpReward())),
                10, 45, 15
        );

        if (quest.nextQuestId() != null && questRegistry.containsKey(quest.nextQuestId())) {
            startQuest(player, profile, quest.nextQuestId(), true);
        } else {
            profile.setActiveQuestId(null);
            activeTimedQuestPlayers.remove(player.getUniqueId());
        }

        profileManager.saveProfileAsync(profile);
        return true;
    }

    public void onCast(Player player, FishingProfile profile) {
        onObjectiveProgress(player, profile, QuestObjectiveType.CAST, 1.0, null);
    }

    public void onFishCatch(Player player, FishingProfile profile, FishCatchResult result, String zoneId) {
        if (player == null || profile == null || result == null) return;

        onObjectiveProgress(player, profile, QuestObjectiveType.CATCH_TOTAL, 1.0, null);

        if (result.rarity() != null) {
            onObjectiveProgress(player, profile, QuestObjectiveType.CATCH_RARITY, 1.0, result.rarity().id());
        }

        onObjectiveProgress(player, profile, QuestObjectiveType.CATCH_WEIGHT, result.weight(), null);

        if (zoneId != null) {
            onObjectiveProgress(player, profile, QuestObjectiveType.VISIT_ZONE, 1.0, zoneId);
        }
    }

    public void onFishSold(Player player, FishingProfile profile, int count, double totalValue) {
        onObjectiveProgress(player, profile, QuestObjectiveType.SELL_FISH, count, null);
        onObjectiveProgress(player, profile, QuestObjectiveType.SELL_VALUE, totalValue, null);
    }

    public void onLevelUp(Player player, FishingProfile profile, int newLevel) {
        onObjectiveProgress(player, profile, QuestObjectiveType.REACH_LEVEL, newLevel, null);
    }

    public void onXpEarned(Player player, FishingProfile profile, long xp) {
        onObjectiveProgress(player, profile, QuestObjectiveType.EARN_XP, xp, null);
    }

    public void onMoneyEarned(Player player, FishingProfile profile, double money) {
        onObjectiveProgress(player, profile, QuestObjectiveType.EARN_COINS, money, null);
    }

    public void onNpcTalk(Player player, FishingProfile profile) {
        onObjectiveProgress(player, profile, QuestObjectiveType.TALK_NPC, 1.0, null);
    }

    public void onBaitUsed(Player player, FishingProfile profile) {
        onObjectiveProgress(player, profile, QuestObjectiveType.USE_BAIT, 1.0, null);
    }

    public void onUpgradePurchased(Player player, FishingProfile profile, String upgradeId) {
        onObjectiveProgress(player, profile, QuestObjectiveType.UPGRADE, 1.0, upgradeId);
    }

    public void onCompetitionFinished(Player player, FishingProfile profile) {
        onObjectiveProgress(player, profile, QuestObjectiveType.COMPLETE_COMPETITION, 1.0, null);
    }

    private void onObjectiveProgress(Player player, FishingProfile profile, QuestObjectiveType type, double amount, String target) {
        if (player == null || profile == null || amount <= 0) return;

        for (FishingQuest quest : questRegistry.values()) {
            if (quest.objectiveType() != type) continue;

            PlayerQuestProgress qp = profile.getPlayerQuestProgress(quest.id());
            if (qp == null || qp.getState() != QuestState.ACTIVE) continue;

            if (target != null && !quest.targetRequirement().isBlank()) {
                if (type == QuestObjectiveType.CATCH_RARITY) {
                    
                    if (!isRaritySufficient(target, quest.targetRequirement())) {
                        continue;
                    }
                } else if (!target.equalsIgnoreCase(quest.targetRequirement())) {
                    continue;
                }
            }

            double newProg;
            if (type == QuestObjectiveType.REACH_LEVEL) {
                newProg = amount;
                qp.setProgress(newProg);
            } else {
                qp.addProgress(amount);
                newProg = qp.getProgress();
            }

            profile.markDirty();

            if (newProg >= quest.requiredAmount()) {
                completeQuest(player, profile, quest, qp);
            } else {
                
                sendActionBarProgress(player, quest, qp);
            }
        }
    }

    private boolean isRaritySufficient(String caughtRarity, String requiredRarity) {
        List<String> tiers = List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "SECRET");
        int caughtIdx = tiers.indexOf(caughtRarity.toUpperCase(Locale.ROOT));
        int reqIdx = tiers.indexOf(requiredRarity.toUpperCase(Locale.ROOT));
        if (caughtIdx == -1 || reqIdx == -1) return caughtRarity.equalsIgnoreCase(requiredRarity);
        return caughtIdx >= reqIdx;
    }

    private void completeQuest(Player player, FishingProfile profile, FishingQuest quest, PlayerQuestProgress qp) {
        qp.setState(QuestState.COMPLETED);
        activeTimedQuestPlayers.remove(player.getUniqueId());
        profile.markDirty();

        String qName = getLocalizedQuestName(player, quest);
        SoundParticleUtil.playSound(player, "UI_TOAST_CHALLENGE_COMPLETE", 1.0f, 1.2f);
        TextUtil.sendTitle(
                player,
                plugin.getLanguageManager().getMessage(player, "quests_system.complete_title", "&6&lQUEST COMPLETED!"),
                "&f" + qName,
                10, 50, 20
        );

        String dComp = getLocalizedQuestDialogue(player, quest, QuestState.COMPLETED);
        if (!dComp.isBlank()) {
            player.sendMessage(TextUtil.colorize("&8[&6Lucas&8] &7\"" + dComp + "\""));
        } else {
            player.sendMessage(TextUtil.colorize(plugin.getLanguageManager().getMessage(player, "quests_system.complete_desc", "&a[AHowToFish] Quest &e{quest} &acompleted! Return to &6Fisherman Lucas &ato claim your reward.").replace("{quest}", qName)));
        }

        if (quest.objectiveType() == QuestObjectiveType.RECEIVE_ITEM) {
            claimReward(player, quest.id());
        }

        profileManager.saveProfileAsync(profile);
    }

    private void sendActionBarProgress(Player player, FishingQuest quest, PlayerQuestProgress qp) {
        int cur = (int) Math.min(qp.getProgress(), quest.requiredAmount());
        int req = (int) quest.requiredAmount();
        String timerStr = qp.getExpiresAt() > 0 ? " &8| &c" + TextUtil.formatTime(qp.getRemainingSeconds()) : "";
        String msg = plugin.getLanguageManager().getMessage(player, "quests_system.action_bar", "&6Quest: &f{quest} &8| &e{cur}/{req}{timer}")
                .replace("{quest}", getLocalizedQuestName(player, quest))
                .replace("{cur}", String.valueOf(cur))
                .replace("{req}", String.valueOf(req))
                .replace("{timer}", timerStr);
        TextUtil.sendActionBar(player, msg);
    }

    public String getLocalizedQuestName(Player player, FishingQuest quest) {
        if (quest == null) return "";
        return plugin.getLanguageManager().getMessage(player, "quest_translations." + quest.id() + ".name", quest.name());
    }

    public String getLocalizedQuestDialogue(Player player, FishingQuest quest, QuestState state) {
        double current = 0.0;
        double required = quest != null ? quest.requiredAmount() : 1.0;
        if (player != null && quest != null && profileManager != null) {
            FishingProfile profile = profileManager.getProfile(player);
            if (profile != null) {
                PlayerQuestProgress qp = profile.getPlayerQuestProgress(quest.id());
                if (qp != null) {
                    current = qp.getProgress();
                } else if (quest.objectiveType() == QuestObjectiveType.REACH_LEVEL) {
                    current = profile.getLevel();
                }
            }
        }
        return getLocalizedQuestDialogue(player, quest, state, current, required);
    }

    public String getLocalizedQuestDialogue(Player player, FishingQuest quest, QuestState state, double current, double required) {
        if (quest == null) return "";
        String key = switch (state) {
            case AVAILABLE -> "offer";
            case ACTIVE -> "progress";
            case COMPLETED -> "complete";
            default -> "";
        };
        if (key.isEmpty()) return "";
        String defaultVal = switch (state) {
            case AVAILABLE -> quest.dialogueOffer();
            case ACTIVE -> quest.dialogueProgress();
            case COMPLETED -> quest.dialogueComplete();
            default -> "";
        };
        String raw = plugin.getLanguageManager().getMessage(player, "quest_translations." + quest.id() + "." + key, defaultVal);
        int curInt = (int) current;
        int reqInt = (int) required;
        return raw.replace("{current}", String.valueOf(curInt))
                .replace("{required}", String.valueOf(reqInt))
                .replace("{cur}", String.valueOf(curInt))
                .replace("{req}", String.valueOf(reqInt));
    }

    public FishingQuest getQuest(String id) {
        if (id == null) return null;
        return questRegistry.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<FishingQuest> getAllQuests() {
        return Collections.unmodifiableCollection(questRegistry.values());
    }

    public List<String> getTutorialQuestSequence() {
        return Collections.unmodifiableList(tutorialQuestSequence);
    }

    public boolean isStarterEnabled() { return starterEnabled; }
    public boolean isFirstJoinGuide() { return firstJoinGuide; }
    public String getStarterRodId() { return starterRodId; }
    public String getFirstQuestId() { return firstQuestId; }
}