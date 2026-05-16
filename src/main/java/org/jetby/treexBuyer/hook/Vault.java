package org.jetby.treexBuyer.hook;

import org.jetby.treexBuyer.TreexBuyer;
import org.jetby.treexBuyer.tools.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class Vault {
    public static Economy setupEconomy(TreexBuyer plugin) {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            Logger.error(plugin, "Vault was not found! Disabling plugin.");
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            Logger.error(plugin, "Economy plugin was not found! Disabling plugin.");
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }
        return rsp.getProvider();
    }
}
