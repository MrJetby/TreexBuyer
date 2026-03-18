package me.jetby.treexBuyer.hook;

import me.jetby.treexBuyer.tools.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Vault {
    public static Economy setupEconomy(JavaPlugin plugin) {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            Logger.error("Vault was not found! Disabling plugin.");
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            Logger.error("Economy plugin was not found! Disabling plugin.");
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }
        return rsp.getProvider();
    }
}
