package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.economy.FishSellService;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.model.Fish;
import com.ardelys.ahowtofish.model.FishRarity;
import com.ardelys.ahowtofish.security.ItemSecurityManager;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.PdcKeys;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class FishermanLucasMenu extends CustomGui {

    private static final int[] FISH_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final AHowToFishPlugin plugin;
    private final Player player;

    public FishermanLucasMenu(AHowToFishPlugin plugin, Player player) {
        super(54, plugin.getLanguageManager().getMessage(player, "gui.lucas.title", "&8Fisherman Lucas &8| &9Fish Merchant"), "FISHERMAN_LUCAS");
        this.plugin = plugin;
        this.player = player;

        initialize();
    }

    private void initialize() {
        fillBorder(Material.CYAN_STAINED_GLASS_PANE);

        FishSellService sellService = plugin.getFishSellService();
        FishSellService.SellSummary summary = sellService != null ? sellService.getSellableFishSummary(player) : new FishSellService.SellSummary(0, 0, 0, false);

        List<String> headerLore = new ArrayList<>();
        List<String> rawHeaderLore = plugin.getLanguageManager().getMessageList(player, "gui.lucas.market_header_lore");
        if (rawHeaderLore.isEmpty()) {
            headerLore.add("&7\"Bring me your catches from any waters!\"");
            headerLore.add("");
            headerLore.add("&fSellable Fish in Bags: &e" + summary.totalCount());
            headerLore.add("&fTotal Biomass Weight: &e" + TextUtil.formatDecimal(summary.totalWeight()) + " kg");
            headerLore.add("&fPotential Payment: &a$" + TextUtil.formatDecimal(summary.totalValue()));
            headerLore.add("");
            headerLore.add("&eClick individual fish below to sell one stack,");
            headerLore.add("&eor click [SELL ALL] to cash out everything!");
        } else {
            for (String line : rawHeaderLore) {
                headerLore.add(line
                        .replace("{count}", String.valueOf(summary.totalCount()))
                        .replace("{weight}", TextUtil.formatDecimal(summary.totalWeight()))
                        .replace("{value}", TextUtil.formatDecimal(summary.totalValue())));
            }
        }

        setItem(4, ItemBuilder.from(Material.FISHING_ROD)
                .name(plugin.getLanguageManager().getMessage(player, "gui.lucas.market_header_name", "&6&l✦ Fisherman Lucas ✦"))
                .lore(headerLore)
                .build());

        ItemStack[] inv = player.getInventory().getContents();
        int slotIndex = 0;

        for (int invSlot = 0; invSlot < inv.length && slotIndex < FISH_SLOTS.length; invSlot++) {
            ItemStack item = inv[invSlot];
            if (item == null || !item.hasItemMeta()) continue;

            String fishId = item.getItemMeta().getPersistentDataContainer().get(PdcKeys.FISH_ID, PersistentDataType.STRING);
            if (fishId == null) continue;

            Fish fishDef = plugin.getFishManager().getFish(fishId);
            FishRarity rarityDef = fishDef != null ? plugin.getFishManager().getRarity(fishDef.rarityId()) : null;

            if (plugin.getItemSecurityManager().validateFishItem(item, fishDef, rarityDef) != ItemSecurityManager.ValidationResult.VALID) {
                continue;
            }

            Double unitPrice = item.getItemMeta().getPersistentDataContainer().get(PdcKeys.FISH_PRICE, PersistentDataType.DOUBLE);
            Double weight = item.getItemMeta().getPersistentDataContainer().get(PdcKeys.FISH_WEIGHT, PersistentDataType.DOUBLE);
            if (unitPrice == null || unitPrice <= 0.0) continue;

            int amount = item.getAmount();
            double lineTotal = Math.round(unitPrice * amount * 100.0) / 100.0;

            ItemStack displayItem = item.clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(TextUtil.toComponent("&8&m------------------------"));
                lore.add(TextUtil.toComponent(plugin.getLanguageManager().getMessage(player, "gui.lucas.stack_size", "&7Stack Size: &e{amount}").replace("{amount}", String.valueOf(amount))));
                lore.add(TextUtil.toComponent(plugin.getLanguageManager().getMessage(player, "gui.lucas.unit_price", "&7Unit Price: &a${price}").replace("{price}", TextUtil.formatDecimal(unitPrice))));
                lore.add(TextUtil.toComponent(plugin.getLanguageManager().getMessage(player, "gui.lucas.total_payout", "&7Total Payout: &a${total}").replace("{total}", TextUtil.formatDecimal(lineTotal))));
                lore.add(Component.empty());
                lore.add(TextUtil.toComponent(plugin.getLanguageManager().getMessage(player, "gui.lucas.click_to_sell_stack", "&a▶ Click to sell this stack")));
                meta.lore(lore);
                displayItem.setItemMeta(meta);
            }

            final int targetSlot = invSlot;
            int guiSlot = FISH_SLOTS[slotIndex++];
            setItem(guiSlot, displayItem, e -> {
                if (plugin.getFishSellService() != null) {
                    boolean success = plugin.getFishSellService().sellSingleSlotFish(player, targetSlot);
                    if (success && plugin.getNpcManager() != null) {
                        player.sendMessage(TextUtil.colorize(plugin.getNpcManager().getDialogueSell()));
                        SoundParticleUtil.playSound(player, plugin.getNpcManager().getSoundSell(), 1.0f, 1.0f);
                    }
                    new FishermanLucasMenu(plugin, player).open(player);
                }
            });
        }

        if (slotIndex == 0) {
            setItem(22, ItemBuilder.from(Material.GRAY_DYE)
                    .name(plugin.getLanguageManager().getMessage(player, "gui.lucas.no_fish_title", "&cNo Sellable Fish"))
                    .lore(
                            plugin.getLanguageManager().getMessage(player, "gui.lucas.no_fish_desc1", "&7Your inventory contains no authenticated fish."),
                            plugin.getLanguageManager().getMessage(player, "gui.lucas.no_fish_desc2", "&7Cast your line in fishing zones to catch fish,"),
                            plugin.getLanguageManager().getMessage(player, "gui.lucas.no_fish_desc3", "&7then return to Lucas to sell them."),
                            "",
                            plugin.getLanguageManager().getMessage(player, "gui.lucas.no_fish_tip", "&eTip: Hold custom bait for higher rarity catches!")
                    ).build());
        }

        String sellAllName = summary.totalCount() > 0 ?
                plugin.getLanguageManager().getMessage(player, "gui.lucas.sell_all_btn", "&a&l✦ SELL ALL FISH ✦") :
                plugin.getLanguageManager().getMessage(player, "gui.lucas.no_fish_btn", "&7&l✦ No Fish to Sell ✦");
        String sellAllClickPrompt = summary.totalCount() > 0 ?
                plugin.getLanguageManager().getMessage(player, "gui.lucas.sell_all_click", "&a▶ Click to cash out all fish") :
                plugin.getLanguageManager().getMessage(player, "gui.lucas.sell_all_empty", "&cYou don't have any fish to sell");

        setItem(48, ItemBuilder.from(summary.totalCount() > 0 ? Material.EMERALD_BLOCK : Material.COAL_BLOCK)
                .name(sellAllName)
                .lore(
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.sell_all_total_fish", "&7Total Fish: &e{count}").replace("{count}", String.valueOf(summary.totalCount())),
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.sell_all_total_weight", "&7Total Weight: &e{weight} kg").replace("{weight}", TextUtil.formatDecimal(summary.totalWeight())),
                        plugin.getLanguageManager().getMessage(player, "gui.lucas.sell_all_total_value", "&7Total Earnings: &a${value}").replace("{value}", TextUtil.formatDecimal(summary.totalValue())),
                        "",
                        sellAllClickPrompt
                ).build(), e -> {
            if (summary.totalCount() <= 0) {
                if (plugin.getNpcManager() != null) {
                    player.sendMessage(TextUtil.colorize(plugin.getNpcManager().getDialogueEmpty()));
                    SoundParticleUtil.playSound(player, plugin.getNpcManager().getSoundEmpty(), 1.0f, 1.0f);
                }
                return;
            }

            if (plugin.getNpcManager() != null && plugin.getNpcManager().isConfirmationEnabled()
                    && summary.totalValue() >= plugin.getNpcManager().getConfirmationMinValue()) {
                new SellConfirmationMenu(plugin, player, summary).open(player);
                return;
            }

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
                new FishermanLucasMenu(plugin, player).open(player);
            }
        });

        List<String> pricingLore = plugin.getLanguageManager().getMessageList(player, "gui.lucas.pricing_guide_lore");
        if (pricingLore.isEmpty()) {
            pricingLore = List.of(
                    "&7Prices paid by Lucas depend on:",
                    " &8• &fSpecies base rarity value",
                    " &8• &fIndividual specimen weight (kg)",
                    " &8• &fActive server event multipliers",
                    "",
                    "&8Heavier fish yield significantly higher payouts!"
            );
        }
        setItem(49, ItemBuilder.from(Material.COMPASS)
                .name(plugin.getLanguageManager().getMessage(player, "gui.lucas.pricing_guide_title", "&e&lMarket Pricing Guide"))
                .lore(pricingLore).build());

        setItem(50, ItemBuilder.from(Material.BARRIER)
                .name(plugin.getLanguageManager().getMessage(player, "gui.common.close", "&cClose"))
                .build(), e -> player.closeInventory());
    }
}