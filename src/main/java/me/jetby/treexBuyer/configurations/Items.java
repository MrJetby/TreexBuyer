package me.jetby.treexBuyer.configurations;

import lombok.Getter;
import me.jetby.treexBuyer.tools.FileLoader;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

@Getter
public class Items {

    private final Map<Material, ItemData> itemValues = new HashMap<>();
    private final Map<String, List<Material>> categories = new HashMap<>();

    public void load() {
        FileConfiguration config = FileLoader.getFileConfiguration("prices.yml");

        categories.clear();
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String category : categoriesSection.getKeys(false)) {
                List<String> materialNames = categoriesSection.getStringList(category);
                List<Material> materials = new ArrayList<>();
                for (String name : materialNames) {
                    try {
                        materials.add(Material.valueOf(name));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid material in category " + category + ": " + name);
                    }
                }
                categories.put(category.toLowerCase(), materials);
            }
        }

        itemValues.clear();
        for (String key : config.getKeys(false)) {
            if (key.equals("categories")) continue;
            try {
                Material material = Material.valueOf(key);
                double price = config.getDouble(key + ".price", 0);
                int addScores = config.getInt(key + ".add-scores", 0);
                String category = getCategory(material);
                itemValues.put(material, new ItemData(price, addScores, category != null ? category : "none"));
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid material in prices.yml: " + key);
            }
        }
    }

    public String getCategory(Material material) {
        for (Map.Entry<String, List<Material>> entry : categories.entrySet()) {
            if (entry.getValue().contains(material)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public record ItemData(double price, int score, String category) {
    }
}