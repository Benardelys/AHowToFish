package com.ardelys.ahowtofish.gui;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.security.GuiSession;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class CustomGui implements InventoryHolder {
    protected final Inventory inventory;
    protected final String guiType;
    protected final UUID guiId = UUID.randomUUID();
    protected final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();

    public CustomGui(int size, String title, String guiType) {
        this.guiType = guiType;
        this.inventory = Bukkit.createInventory(this, size, TextUtil.toComponent(title));
    }

    public CustomGui(int size, String title) {
        this(size, title, "GENERIC");
    }

    public String getGuiType() {
        return guiType;
    }

    public UUID getGuiId() {
        return guiId;
    }

    public void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    public void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);
        if (handler != null) {
            clickHandlers.put(slot, handler);
        } else {
            clickHandlers.remove(slot);
        }
    }

    public void fillBorder(Material material) {
        ItemStack border = ItemBuilder.from(material).name(" ").build();
        int size = inventory.getSize();
        int rows = size / 9;

        for (int c = 0; c < 9; c++) {
            setItem(c, border);
            setItem(size - 9 + c, border);
        }
        for (int r = 1; r < rows - 1; r++) {
            setItem(r * 9, border);
            setItem(r * 9 + 8, border);
        }
    }

    public UUID getSessionId() {
        return guiId;
    }

    public void open(Player player) {
        if (player == null) return;
        player.openInventory(inventory);
        if (AHowToFishPlugin.getInstance() != null && AHowToFishPlugin.getInstance().getGuiSessionManager() != null) {
            AHowToFishPlugin.getInstance().getGuiSessionManager().createSession(player.getUniqueId(), guiType, guiId);
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (AHowToFishPlugin.getInstance() != null && AHowToFishPlugin.getInstance().getSecurityManager() != null) {
            var sm = AHowToFishPlugin.getInstance().getSecurityManager();
            if (!sm.getRateLimiter().tryAcquire(player.getUniqueId(), com.ardelys.ahowtofish.security.RateLimitCategory.GUI_ACTION)) {
                var resp = sm.handleViolation(
                        com.ardelys.ahowtofish.security.SecurityAlertLevel.MEDIUM,
                        com.ardelys.ahowtofish.security.RateLimitCategory.GUI_ACTION,
                        "CustomGui",
                        "Exceeded GUI action rate limit",
                        player.getUniqueId()
                );
                if (resp.shouldBlock()) {
                    return;
                }
            }
        }

        if (AHowToFishPlugin.getInstance() != null && AHowToFishPlugin.getInstance().getGuiSessionManager() != null) {
            if (!AHowToFishPlugin.getInstance().getGuiSessionManager().validateSession(player, guiType)) {
                player.closeInventory();
                return;
            }
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < inventory.getSize()) {
            Consumer<InventoryClickEvent> handler = clickHandlers.get(rawSlot);
            if (handler != null) {
                handler.accept(event);
            }
        }
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}