package me.jetby.treexBuyer.tools;

import me.jetby.treexBuyer.TreexBuyer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class FileLoader {

    public static FileConfiguration getFileConfiguration(String fileName) {
        File file = new File(TreexBuyer.getInstance().getDataFolder(), fileName);
        if (!file.exists()) TreexBuyer.getInstance().saveResource(fileName, false);
        return YamlConfiguration.loadConfiguration(file);
    }

    public static File getFile(String fileName) {
        File file = new File(TreexBuyer.getInstance().getDataFolder(), fileName);
        if (!file.exists()) TreexBuyer.getInstance().saveResource(fileName, false);
        return file;
    }
}