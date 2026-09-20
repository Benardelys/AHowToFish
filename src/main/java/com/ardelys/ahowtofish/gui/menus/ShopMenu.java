package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.language.MessageKey;
import com.ardelys.ahowtofish.model.FishingBait;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.PdcKeys;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ShopMenu extends CustomGui {
    private final AHowToFishPlugin plugin;
    private final Player player;
    private final FishingProfile profile;

    public ShopMenu(AHowToFishPlugin plugin, Player player) {
        super(54, plugin.getLanguageManager().getMessage(player, "gui.shop.title", "&1&l✦ Fish Market & Supply Store"), "SHOP_MENU");
        this.plugin = plugin;
        this.player = player;
        this.profile = plugin.getProfileManager().getProfile(player);

        initialize();
    }

    private void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        setItem(11, ItemBuilder.from(Material.GOLD_BLOCK)
                .name(plugin.getLanguageManager().getMessage(player, "gui.shop.sell_all_name", "&e&lSell All Fish in Inventory"))
                .lore(
                        plugin.getLanguageManager().getMessage(player, "gui.shop.sell_all_desc1", "&7Instantly sell every caught fish"),
                        plugin.getLanguageManager().getMessage(player, "gui.shop.sell_all_desc2", "&7currently in your inventory."),
                        "",
                        plugin.getLanguageManager().getMessage(player, "gui.shop.sell_all_click", "&aClick to sell all!")
                ).build(), e -> sellAllFish());

        setItem(15, ItemBuilder.from(Material.RAW_GOLD)
                .name(plugin.getLanguageManager().getMessage(player, "gui.shop.sell_hand_name", "&6&lSell Held Fish"))
                .lore(
                        plugin.getLanguageManager().getMessage(player, "gui.shop.sell_hand_desc1", "&7Sell the fish currently held in"),
                        plugin.getLanguageManager().getMessage(player, "gui.shop.sell_hand_desc2", "&7your main hand."),
                        "",
                        plugin.getLanguageManager().getMessage(player, "gui.shop.sell_hand_click", "&aClick to sell hand!")
                ).build(), e -> sellHandFish());

        for (int c = 18; c < 27; c++) {
            setItem(c, ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
        }

        int[] baitSlots = {29, 30, 31, 32, 33};
        int idx = 0;
        for (FishingBait bait : plugin.getBaitManager().getAllBaits()) {
            if (idx >= baitSlots.length) break;
            int slot = baitSlots[idx++];

            String baitName = plugin.getLanguageManager().getMessage(player, "bait_translations." + bait.id() + ".name", bait.displayName());
            String maxUsesLabel = plugin.getLanguageManager().getMessage(player, "gui.shop.bait_max_uses", "&7Max Uses: &e{uses}")
                    .replace("{uses}", String.valueOf(bait.maxUses()));
            String speedLabel = plugin.getLanguageManager().getMessage(player, "gui.shop.bait_fish_speed", "&7Fish Speed: &a{speed}x")
                    .replace("{speed}", TextUtil.formatDecimal(bait.fishChanceMultiplier()));
            String costLabel = plugin.getLanguageManager().getMessage(player, "gui.shop.bait_cost", "&7Cost: &a${cost}")
                    .replace("{cost}", TextUtil.formatDecimal(bait.cost()));
            String buyClickLabel = plugin.getLanguageManager().getMessage(player, "gui.shop.bait_buy_click", "&eClick to purchase (1x)!");

            setItem(slot, ItemBuilder.from(bait.material() != null ? bait.material() : Material.WHEAT_SEEDS)
                    .name(baitName)
                    .lore(
                            maxUsesLabel,
                            speedLabel,
                            costLabel,
                            "",
                            buyClickLabel
                    ).build(), e -> buyBait(bait));
        }

        setItem(49, ItemBuilder.from(Material.DARK_OAK_DOOR)
                .name(plugin.getLanguageManager().getMessage(player, "gui.common.back", "&cBack to Main Menu"))
                .build(), e -> new MainFishingMenu(plugin, player).open(player));
    }

    private void sellAllFish() {
        player.closeInventory();
        plugin.getFishSellService().sellAllFish(player);
    }

    private void sellHandFish() {
        player.closeInventory();
        plugin.getFishSellService().sellHeldFish(player);
    }

    private void buyBait(FishingBait bait) {
        if (bait == null) return;

        if (plugin.getSecurityManager() != null) {
            var sm = plugin.getSecurityManager();
            if (!sm.getRateLimiter().tryAcquire(player.getUniqueId(), com.ardelys.ahowtofish.security.RateLimitCategory.PURCHASE)) {
                var resp = sm.handleViolation(
                        com.ardelys.ahowtofish.security.SecurityAlertLevel.MEDIUM,
                        com.ardelys.ahowtofish.security.RateLimitCategory.PURCHASE,
                        "ShopMenu",
                        "Exceeded purchase rate limit",
                        player.getUniqueId()
                );
                if (resp.shouldBlock()) {
                    return;
                }
            }
        }

        double cost = com.ardelys.ahowtofish.security.MultiplierService.sanitizeMoney(bait.cost());
        if (Double.isNaN(cost) || Double.isInfinite(cost) || cost < 0.0) {
            if (plugin.getSecurityManager() != null) {
                plugin.getSecurityManager().logAlert(
                        com.ardelys.ahowtofish.security.SecurityAlertLevel.CRITICAL,
                        "ShopMenu",
                        "Illegal bait price detected: " + bait.cost() + " for bait " + bait.id(),
                        player.getUniqueId()
                );
            }
            return;
        }

        if (plugin.getVaultHook() != null && plugin.getVaultHook().isAvailable()) {
            if (!plugin.getVaultHook().has(player, cost)) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.NOT_ENOUGH_MONEY);
                return;
            }

            ItemStack item = bait.createItemStack(1);
            if (player.getInventory().firstEmpty() == -1 && !player.getInventory().containsAtLeast(item, 1)) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.INVENTORY_FULL);
                return;
            }

            if (!plugin.getVaultHook().withdraw(player, cost)) {
                plugin.getLanguageManager().sendMessage(player, MessageKey.TRANSACTION_BUSY);
                return;
            }
        }

        ItemStack item = bait.createItemStack(1);
        player.getInventory().addItem(item);

        if (plugin.getTransactionManager() != null) {
            String txId = plugin.getTransactionManager().generateTransactionId(player.getUniqueId());
            plugin.getTransactionManager().recordTransaction(
                    txId,
                    player.getUniqueId(),
                    com.ardelys.ahowtofish.security.TransactionManager.TransactionStatus.COMPLETED,
                    "BUY_BAIT:" + bait.id() + ":$" + cost
            );
        }

        SoundParticleUtil.playSound(player, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.5f);
        plugin.getLanguageManager().sendMessage(player, MessageKey.BUY_SUCCESS, "item", bait.displayName(), "price", TextUtil.formatDecimal(cost));
    }
}
