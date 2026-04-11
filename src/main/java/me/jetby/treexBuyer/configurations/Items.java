package me.jetby.treexBuyer.configurations;

import lombok.Getter;
import me.jetby.libb.plugin.LibbPlugin;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.storage.score.Score;
import me.jetby.treexBuyer.tools.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Items {

    private final TreexBuyer plugin;
    private final Map<Material, ItemData> itemValues = new LinkedHashMap<>();
    private final Map<Material, String> categories = new LinkedHashMap<>();
    private final FileConfiguration fileConfiguration;

    public Items(TreexBuyer plugin) {
        this.plugin = plugin;
        this.fileConfiguration = plugin.getFileConfiguration("prices.yml");
    }
    public void load() {

        categories.clear();
        itemValues.clear();

        ConfigurationSection categoriesSection = fileConfiguration.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String category : categoriesSection.getKeys(false)) {
                for (String name : categoriesSection.getStringList(category)) {
                    try {
                        Material material = Material.valueOf(name);
                        categories.put(material, category);
                        double price = fileConfiguration.getDouble(material.name() + ".price", 0.0);
                        double score = fileConfiguration.getDouble(material.name() + ".add-scores", 0);
                        itemValues.put(material, new ItemData(price, score, category));
                    } catch (IllegalArgumentException e) {
                        Logger.error("Invalid material in category " + category + ": " + name);
                    }
                }
            }
        }

        for (String key : fileConfiguration.getKeys(false)) {
            if (key.equals("categories")) continue;
            try {
                Material material = Material.valueOf(key);
                if (itemValues.containsKey(material)) continue;
                double price = fileConfiguration.getDouble(key + ".price", 0.0);
                double score = fileConfiguration.getDouble(key + ".add-scores", 0);
                itemValues.put(material, new ItemData(price, score, "none"));
            } catch (IllegalArgumentException e) {
                Logger.error("Invalid material in prices.yml: " + key);
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

    public double getScoreAmount(Material material) {
        Items.ItemData data = plugin.getItems().getItemValues().get(material);
        return data == null ? 0.0 : data.score();
    }
    public double getOriginalPrice(Material material) {
        Items.ItemData data = plugin.getItems().getItemValues().get(material);
        return data == null ? 0.0 : data.price();
    }
    public Score createScore() {
        return plugin.getCfg().getType().createScore(categories);
    }

    public record ItemData(double price, double score, String category) {
    }
}