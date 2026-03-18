package me.jetby.treexBuyer.configurations;

import me.jetby.libb.gui.CommandRegistrar;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.menus.BuyerGui;
import me.jetby.treexBuyer.modules.UserData;
import me.jetby.treexBuyer.tools.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiLoader {

    public static final Map<String, FileConfiguration> ALL_GUIS = new HashMap<>();

    private final TreexBuyer plugin;

    public GuiLoader(TreexBuyer plugin) {
        this.plugin = plugin;
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
            Logger.info(file.getPath() + " (id: " + id + ") loaded");
        }
    }

    public void loadGuis() {
        File folder = new File(plugin.getDataFolder(), "Menu");

        Logger.success("------------------------");

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
                    plugin.saveResource("Menu/" + name, false);
                    Logger.info("The Menu/" + name + " created");
                }
            }
        }

        loadFilesRecursive(folder);

        Logger.success("------------------------");
        Logger.success(ALL_GUIS.size() + " menus has been founded");
        Logger.success("------------------------");
    }

    private void loadGui(String menuId, File file) {
        if (ALL_GUIS.containsKey(menuId)) {
            Logger.error("A duplicate of " + menuId + " was found");
            return;
        }
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            List<String> commands = config.getStringList("command");
            for (String cmd : commands) {
                CommandRegistrar.registerCommand(plugin, cmd, new GuiExecutor(config));
            }
            ALL_GUIS.put(menuId, config);
        } catch (Exception e) {
            Logger.error("Error trying to load menu: " + e.getMessage());
        }
    }


    public class GuiExecutor implements CommandExecutor {

        private final FileConfiguration guiConfig;

        public GuiExecutor(FileConfiguration guiConfig) {
            this.guiConfig = guiConfig;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (sender instanceof Player player) {
                UserData user = UserData.getOrCreate(player.getUniqueId(), plugin.getItems().createScore());
                new BuyerGui(player, user, guiConfig, plugin).open(player);
            }
            return true;
        }
    }

}
