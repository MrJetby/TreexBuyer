package org.jetby.treexBuyer.configurations;

import org.jetby.libb.command.CommandRegistrar;
import org.jetby.libb.util.Logger;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.menus.BuyerGui;
import org.jetby.treexBuyer.modules.UserData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiLoader {

    public static final Map<String, FileConfiguration> ALL_GUIS = new HashMap<>();

    private final BuyerManager manager;

    public GuiLoader(BuyerManager manager) {
        this.manager = manager;
    }

    private void loadFilesRecursive(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadFilesRecursive(file);
                continue;
            }

            if (!file.getName().endsWith(".yml")) continue;

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id", file.getName().replace(".yml", ""));

            loadGui(id, file);
        }
    }

    public void loadGuis() {
        ALL_GUIS.clear();
        File folder = new File(manager.getPlugin().getDataFolder(), "Menu");


        if (!folder.exists() && folder.mkdirs()) {
            String[] defaults = {
                    "example/mine.yml",
                    "example/mobs.yml",
                    "example/seller.yml",
                    "donutsmp/donutsmp.yml"
            };

            for (String name : defaults) {
                File target = new File(folder, name);
                target.getParentFile().mkdirs();

                if (!target.exists()) {
                    manager.getPlugin().saveResource("Menu/" + name, false);
                }
            }
        }

        loadFilesRecursive(folder);
    }

    private void loadGui(String menuId, File file) {
        if (ALL_GUIS.containsKey(menuId)) {
            Logger.error(manager.getPlugin(), "A duplicate of " + menuId + " was found");
            return;
        }
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            List<String> commands = config.getStringList("command");
            for (String cmd : commands) {
                CommandRegistrar.registerCommand(manager.getPlugin(), cmd, (sender, command, label, args) -> {
                    if (sender instanceof Player player) {
                        UserData user = UserData.getOrCreate(player.getUniqueId(), manager.getItems().createScore());
                        new BuyerGui(player, user, config, manager).open(player);
                    }
                    return true;
                });
            }
            ALL_GUIS.put(menuId, config);
        } catch (Exception e) {
            Logger.error(manager.getPlugin(), "Error trying to load menu: " + e.getMessage());
        }
    }

}
