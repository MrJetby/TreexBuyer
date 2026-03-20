package me.jetby.treexBuyer.configurations;

import lombok.Getter;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.storage.score.Score;
import me.jetby.treexBuyer.tools.FileLoader;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Items {

    private final TreexBuyer plugin;
    private final Map<Material, ItemData> itemValues = new HashMap<>();
    private final Map<Material, String> categories = new HashMap<>();

    public Items(TreexBuyer plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = FileLoader.getFileConfiguration("prices.yml");

        categories.clear();
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String category : categoriesSection.getKeys(false)) {
                for (String name : categoriesSection.getStringList(category)) {
                    try {
                        categories.put(Material.valueOf(name), category);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid material in category " + category + ": " + name);
                    }
                }
            }
        }

        itemValues.clear();
        for (String key : config.getKeys(false)) {
            if (key.equals("categories")) continue;
            try {
                Material material = Material.valueOf(key);
                double price = config.getDouble(key + ".price", 0.0);
                double score = config.getDouble(key + ".add-scores", 0);
                String category = categories.getOrDefault(material, "none");
                itemValues.put(material, new ItemData(price, score, category));
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid material in prices.yml: " + key);
            }
        }
    }

    public String getCategory(Material material) {
        return categories.getOrDefault(material, "unknown");
    }

    public List<Material> getMaterials(String category) {
        return categories.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(category))
                .map(Map.Entry::getKey)
                .toList();
    }

    public Score createScore() {
        return plugin.getCfg().getType().createScore(categories);
    }

    public record ItemData(double price, double score, String category) {
    }
}