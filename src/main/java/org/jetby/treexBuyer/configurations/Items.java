package org.jetby.treexBuyer.configurations;

import lombok.Getter;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.storage.score.Score;
import org.jetby.treexBuyer.tools.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Items {

    private final BuyerManager manager;
    private final Map<Material, ItemData> itemValues = new LinkedHashMap<>();
    private final Map<Material, String> categories = new LinkedHashMap<>();
    private FileConfiguration fileConfiguration;

    public Items(BuyerManager manager) {
        this.manager = manager;
    }
    public void load() {
        this.fileConfiguration = manager.getPlugin().getFileConfiguration("prices.yml");

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
                        if (price<=0) continue;
                        itemValues.put(material, new ItemData(price, score, category));
                    } catch (IllegalArgumentException e) {
                        Logger.error(manager.getPlugin(), "Invalid material in category " + category + ": " + name);
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
                Logger.error(manager.getPlugin(), "Invalid material in prices.yml: " + key);
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
        Items.ItemData data = manager.getItems().getItemValues().get(material);
        return data == null ? 0.0 : data.score();
    }
    public double getOriginalPrice(Material material) {
        Items.ItemData data = manager.getItems().getItemValues().get(material);
        return data == null ? 0.0 : data.price();
    }
    public Score createScore() {
        return manager.getCfg().getType().createScore(categories);
    }

    public record ItemData(double price, double score, String category) {
    }
}