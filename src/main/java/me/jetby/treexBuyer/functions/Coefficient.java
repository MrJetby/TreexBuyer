package me.jetby.treexBuyer.functions;

import lombok.RequiredArgsConstructor;
import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.configurations.Config;
import me.jetby.treexBuyer.configurations.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class Coefficient {

    final Main plugin;

    public double get(Player player, Material material) {
        String key = determineKey(material);
        double playerScore = plugin.getStorage().getScore(player.getUniqueId(), key);
        double multiplierCount = Math.floor(playerScore / plugin.getCfg().getScores());
        double coefficient = plugin.getCfg().getDefaultCoefficient()
                + multiplierCount * plugin.getCfg().getCoefficient();

        double baseCoefficient = Math.min(coefficient, plugin.getCfg().getMaxCoefficient());
        baseCoefficient = Math.max(baseCoefficient, plugin.getCfg().getDefaultCoefficient());

        double boosterCoefficient = 0.0;
        for (Boost boost : plugin.getCfg().getBoosts().values()) {
            if (boost.permission() != null && player.hasPermission(boost.permission())) {
                boosterCoefficient += boost.coefficient();
            }
        }

        if (plugin.getCfg().isBoosters_except_legal_coefficient()) {
            return round(baseCoefficient + boosterCoefficient);
        } else {
            return round(Math.min(baseCoefficient + boosterCoefficient, plugin.getCfg().getMaxCoefficient()));
        }
    }

    public double getByCategory(Player player, String category) {
        if (category == null || category.isEmpty()) {
            return get(player, null);
        }
        Config.ScoreType type = plugin.getCfg().getType();
        String key = type == Config.ScoreType.CATEGORY ? category.toLowerCase() : "global";
        double playerScore = plugin.getStorage().getScore(player.getUniqueId(), key);
        double multiplierCount = Math.floor(playerScore / plugin.getCfg().getScores());
        double coefficient = plugin.getCfg().getDefaultCoefficient()
                + multiplierCount * plugin.getCfg().getCoefficient();

        double baseCoefficient = Math.min(coefficient, plugin.getCfg().getMaxCoefficient());
        baseCoefficient = Math.max(baseCoefficient, plugin.getCfg().getDefaultCoefficient());

        double boosterCoefficient = 0.0;
        for (Boost boost : plugin.getCfg().getBoosts().values()) {
            if (boost.permission() != null && player.hasPermission(boost.permission())) {
                boosterCoefficient += boost.coefficient();
            }
        }

        if (plugin.getCfg().isBoosters_except_legal_coefficient()) {
            return round(baseCoefficient + boosterCoefficient);
        } else {
            return round(Math.min(baseCoefficient + boosterCoefficient, plugin.getCfg().getMaxCoefficient()));
        }
    }
    final Items.ItemData defaultData = new Items.ItemData(0,0,"uncategorized");

    public String determineKey(Material material) {
        Config.ScoreType type = plugin.getCfg().getType();
        if (type == Config.ScoreType.GLOBAL) return "global";
        if (type == Config.ScoreType.ITEM) return material.name().toLowerCase();
        if (type == Config.ScoreType.CATEGORY) {
            String category = plugin.getItems().getItemValues().getOrDefault(material, defaultData).category();
            return category != null ? category.toLowerCase() : "uncategorized";
        }
        throw new IllegalStateException("Unknown score type");
    }

    double round(double value) {
        double scale = Math.pow(10, 2);
        return Math.round(value * scale) / scale;
    }

}
