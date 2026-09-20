package com.ardelys.ahowtofish.utility;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder implements Cloneable {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material != null ? material : Material.STONE, Math.max(1, amount));
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder(ItemStack itemStack) {
        this.item = itemStack != null ? itemStack.clone() : new ItemStack(Material.STONE);
        this.meta = this.item.getItemMeta();
    }

    public static ItemBuilder from(Material material, int amount) {
        return new ItemBuilder(material, amount);
    }

    public static ItemBuilder from(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder from(ItemStack item) {
        return new ItemBuilder(item);
    }

    public ItemBuilder name(String displayName) {
        if (meta != null && displayName != null) {
            meta.displayName(TextUtil.toComponent(displayName));
        }
        return this;
    }

    public ItemBuilder lore(List<String> loreLines) {
        if (meta != null && loreLines != null) {
            meta.lore(TextUtil.toComponentList(loreLines));
        }
        return this;
    }

    public ItemBuilder lore(String... loreLines) {
        return lore(Arrays.asList(loreLines));
    }

    public ItemBuilder setName(String displayName) {
        return name(displayName);
    }

    public ItemBuilder setLore(List<String> loreLines) {
        return lore(loreLines);
    }

    public ItemBuilder setLore(String... loreLines) {
        return lore(loreLines);
    }

    public ItemBuilder appendLore(String line) {
        if (meta != null && line != null) {
            List<net.kyori.adventure.text.Component> current = meta.lore();
            if (current == null) current = new ArrayList<>();
            current.add(TextUtil.toComponent(line));
            meta.lore(current);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    @SuppressWarnings("deprecation")
    public ItemBuilder customModelData(Integer modelData) {
        if (meta != null && modelData != null && modelData > 0) {
            meta.setCustomModelData(modelData);
        }
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        if (meta != null && flags != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder hideAllFlags() {
        return flags(ItemFlag.values());
    }

    public ItemBuilder glow(boolean glow) {
        if (meta != null) {
            meta.setEnchantmentGlintOverride(glow);
        }
        return this;
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
            if (unbreakable) {
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            }
        }
        return this;
    }

    public ItemBuilder setStringPdc(NamespacedKey key, String value) {
        if (meta != null && key != null && value != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        }
        return this;
    }

    public ItemBuilder setDoublePdc(NamespacedKey key, double value) {
        if (meta != null && key != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
        }
        return this;
    }

    public ItemBuilder setLongPdc(NamespacedKey key, long value) {
        if (meta != null && key != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.LONG, value);
        }
        return this;
    }

    public ItemBuilder setIntPdc(NamespacedKey key, int value) {
        if (meta != null && key != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public ItemBuilder clone() {
        return new ItemBuilder(this.build());
    }
}
