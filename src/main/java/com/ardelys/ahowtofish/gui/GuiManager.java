package com.ardelys.ahowtofish.gui;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

public class GuiManager implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CustomGui gui)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ClickType clickType = event.getClick();
        if (clickType == ClickType.NUMBER_KEY ||
            clickType == ClickType.SWAP_OFFHAND ||
            clickType == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        InventoryAction action = event.getAction();
        if (action == InventoryAction.COLLECT_TO_CURSOR ||
            action == InventoryAction.HOTBAR_SWAP ||
            "HOTBAR_MOVE_AND_READD".equals(action.name())) {
            event.setCancelled(true);
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
            if (event.getRawSlot() >= gui.getInventory().getSize()) {
                
                return;
            }
        }

        gui.handleClick(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CustomGui gui) {
            
            event.setCancelled(true);
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < gui.getInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof CustomGui gui) {
            if (event.getPlayer() instanceof Player player) {
                if (AHowToFishPlugin.getInstance() != null && AHowToFishPlugin.getInstance().getGuiSessionManager() != null) {
                    AHowToFishPlugin.getInstance().getGuiSessionManager().endSession(player.getUniqueId(), gui.getSessionId());
                }
            }
        }
    }
}