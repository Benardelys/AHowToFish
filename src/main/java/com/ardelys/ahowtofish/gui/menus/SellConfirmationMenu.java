package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.economy.FishSellService;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SellConfirmationMenu extends CustomGui {

    private final AHowToFishPlugin plugin;
    private final Player player;
    private final FishSellService.SellSummary summary;

    public SellConfirmationMenu(AHowToFishPlugin plugin, Player player, FishSellService.SellSummary summary) {
        super(27, plugin.getLanguageManager().getMessage(player, "gui.lucas.confirm_title", "&8Fisherman Lucas &8| &cConfirm Sale"), "SELL_CONFIRM");
        this.plugin = plugin;
        this.player = player;
        this.summary = summary;

        initialize();
    }

    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);

        List<String> lore = new ArrayList<>();
        lore.add(plugin.getLanguageManager().getMessage(player, "gui.lucas.sell_all_total_fish", "&7Fish to Sell: &e{count}").replace("{count}", String.valueOf(summary.totalCount())));
        lore.add(plugin.getLanguageManager().getMessage(player, "gui.lucas.sell_all_total_weight", "&7Total Weight: &e{weight} kg").replace("{weight}", TextUtil.formatDecimal(summary.totalWeight())));
        lore.add(plugin.getLanguageManager().getMessage(player, "gui.lucas.sell_all_total_value", "&7Total Value: &a${value}").replace("{value}", TextUtil.formatDecimal(summary.totalValue())));
        lore.add("");
        lore.add(plugin.getLanguageManager().getMessage(player, "gui.lucas.confirm_buyer", "&7Buyer: &6Fisherman Lucas"));
        lore.add(plugin.getLanguageManager().getMessage(player, "gui.lucas.confirm_high_value_note", "&8High-value transaction confirmation required."));

        setItem(13, ItemBuilder.from(Material.GOLD_BLOCK)
                .name(plugin.getLanguageManager().getMessage(player, "gui.lucas.confirm_modal_title", "&6&lHigh-Value Fish Transaction"))
                .lore(lore)
                .build());

        List<String> confirmLore = new ArrayList<>();
        List<String> rawConfirmLore = plugin.getLanguageManager().getMessageList(player, "gui.lucas.confirm_btn_lore");
        if (rawConfirmLore.isEmpty()) {
            confirmLore.add("&7Click to sell all validated fish");
            confirmLore.add("&7and receive &a$" + TextUtil.formatDecimal(summary.totalValue()) + "&7.");
            confirmLore.add("");
            confirmLore.add("&a▶ Click to complete transaction");
        } else {
            for (String line : rawConfirmLore) {
                confirmLore.add(line.replace("{value}", TextUtil.formatDecimal(summary.totalValue())));
            }
        }

        setItem(11, ItemBuilder.from(Material.LIME_CONCRETE)
                .name(plugin.getLanguageManager().getMessage(player, "gui.lucas.confirm_btn", "&a&l[ CONFIRM SALE ]"))
                .lore(confirmLore).build(), e -> {
            player.closeInventory();
            if (plugin.getFishSellService() != null) {
                plugin.getFishSellService().sellAllFish(player);
                if (plugin.getNpcManager() != null) {
                    player.sendMessage(TextUtil.colorize(summary.hasRareOrLegendary() ?
                            plugin.getNpcManager().getDialogueSellRare() :
                            plugin.getNpcManager().getDialogueSell()));
                    SoundParticleUtil.playSound(player, summary.hasRareOrLegendary() ?
                            plugin.getNpcManager().getSoundSellRare() :
                            plugin.getNpcManager().getSoundSell(), 1.0f, 1.0f);
                }
            }
        });

        List<String> cancelLore = plugin.getLanguageManager().getMessageList(player, "gui.lucas.cancel_btn_lore");
        if (cancelLore.isEmpty()) {
            cancelLore = List.of(
                    "&7Cancel this transaction and keep all fish.",
                    "",
                    "&c▶ Click to return"
            );
        }

        setItem(15, ItemBuilder.from(Material.RED_CONCRETE)
                .name(plugin.getLanguageManager().getMessage(player, "gui.lucas.cancel_btn", "&c&l[ CANCEL ]"))
                .lore(cancelLore).build(), e -> new FishermanLucasMenu(plugin, player).open(player));
    }
}