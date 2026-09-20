package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.language.MessageKey;
import com.ardelys.ahowtofish.model.FishingUpgrade;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class UpgradesMenu extends CustomGui {
    private static final int[] SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};

    private final AHowToFishPlugin plugin;
    private final Player player;
    private final FishingProfile profile;

    public UpgradesMenu(AHowToFishPlugin plugin, Player player) {
        super(45, plugin.getLanguageManager().getMessage(player, "gui.upgrades.title", "&1&l✦ Permanent Fishing Upgrades"), "UPGRADES_MENU");
        this.plugin = plugin;
        this.player = player;
        this.profile = plugin.getProfileManager().getProfile(player);

        initialize();
    }

    private void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        List<FishingUpgrade> upgrades = new ArrayList<>(plugin.getAbilityManager().getAllUpgrades());
        for (int i = 0; i < upgrades.size() && i < SLOTS.length; i++) {
            FishingUpgrade upg = upgrades.get(i);
            int slot = SLOTS[i];
            int currentLevel = profile != null ? profile.getUpgradeLevel(upg.id()) : 0;
            boolean isMax = currentLevel >= upg.maxLevel();
            double cost = upg.getCost(currentLevel);

            List<String> lore = new ArrayList<>();
            lore.add("&7" + upg.description());
            lore.add("");
            lore.add(plugin.getLanguageManager().getMessage(player, "gui.upgrades.level", "&7Level: &e{cur}&7/&f{max}")
                    .replace("{cur}", String.valueOf(currentLevel))
                    .replace("{max}", String.valueOf(upg.maxLevel())));
            lore.add(plugin.getLanguageManager().getMessage(player, "gui.upgrades.current_effect", "&7Current Effect: &a+{effect}%")
                    .replace("{effect}", TextUtil.formatDecimal(upg.getTotalEffect(currentLevel) * 100)));

            if (isMax) {
                lore.add("");
                lore.add(plugin.getLanguageManager().getMessage(player, "gui.upgrades.max_level_reached", "&a✔ Maximum level reached!"));
            } else {
                lore.add(plugin.getLanguageManager().getMessage(player, "gui.upgrades.next_cost", "&7Next Level Cost: &6${cost}")
                        .replace("{cost}", TextUtil.formatDecimal(cost)));
                lore.add("");
                lore.add(plugin.getLanguageManager().getMessage(player, "gui.upgrades.click_to_upgrade", "&eClick to upgrade!"));
            }

            setItem(slot, ItemBuilder.from(upg.iconMaterial())
                    .name("&d&l" + upg.name())
                    .lore(lore)
                    .build(), e -> buyUpgrade(upg));
        }

        setItem(40, ItemBuilder.from(Material.DARK_OAK_DOOR)
                .name(plugin.getLanguageManager().getMessage(player, "gui.common.back", "&cBack to Main Menu"))
                .build(), e -> new MainFishingMenu(plugin, player).open(player));
    }

    private void buyUpgrade(FishingUpgrade upg) {
        int currentLevel = profile.getUpgradeLevel(upg.id());
        if (currentLevel >= upg.maxLevel()) {
            plugin.getLanguageManager().sendMessage(player, MessageKey.UPGRADE_MAX_LEVEL);
            return;
        }

        double cost = upg.getCost(currentLevel);
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isAvailable()) {
            if (!plugin.getVaultHook().has(player, cost)) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.NOT_ENOUGH_MONEY);
                return;
            }
            plugin.getVaultHook().withdraw(player, cost);
        }

        profile.incrementUpgradeLevel(upg.id());
        plugin.getProfileManager().saveProfileAsync(profile);

        SoundParticleUtil.playSound(player, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.2f);
        plugin.getLanguageManager().sendMessage(player, MessageKey.UPGRADE_PURCHASED, "upgrade", upg.name(), "level", currentLevel + 1);

        new UpgradesMenu(plugin, player).open(player);
    }
}
