package com.ardelys.ahowtofish.integration;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultHook {
    private final JavaPlugin plugin;
    private Economy economy = null;
    private boolean available = false;

    public VaultHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        available = (economy != null);
        return available;
    }

    public boolean isAvailable() {
        return available && economy != null;
    }

    public double getBalance(Player player) {
        if (!isAvailable() || player == null) return 0.0;
        return economy.getBalance(player);
    }

    public boolean has(Player player, double amount) {
        if (!isAvailable() || player == null) return true;
        return economy.has(player, amount);
    }

    public boolean deposit(Player player, double amount) {
        if (!isAvailable() || player == null || amount <= 0) return true;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean withdraw(Player player, double amount) {
        if (!isAvailable() || player == null || amount <= 0) return true;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
}
