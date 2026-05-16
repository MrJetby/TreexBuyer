package org.jetby.treexBuyer;

import org.jetby.treexBuyer.hook.LibbStableDownloader;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class TreexBuyer extends JavaPlugin {

    public static TreexBuyer INSTANCE;

    private BuyerManager buyer;

    @Override
    public void onLoad() {

        try {
            new LibbStableDownloader("1.2").load();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load Libb", ex);
        }

        this.buyer = new BuyerManager(this);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        buyer.onEnable();
    }

    @Override
    public void onDisable() {
        buyer.onDisable();
    }


    public FileConfiguration getFileConfiguration(String fileName) {
        File file = new File(this.getDataFolder(), fileName);
        if (!file.exists()) {
            this.saveResource(fileName, false);
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    public File getFile(String fileName) {
        File file = new File(this.getDataFolder(), fileName);
        if (!file.exists()) {
            this.saveResource(fileName, false);
        }

        return file;
    }


}
