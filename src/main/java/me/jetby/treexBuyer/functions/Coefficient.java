package me.jetby.treexBuyer.functions;

import lombok.RequiredArgsConstructor;
import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.configurations.Config;
import me.jetby.treexBuyer.configurations.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class Coefficient {

    private final Main plugin;

    public double get(Player player, Material material) {
        String key = determineKey(material);
        double playerScore = plugin.getStorage().getScore(player.getUniqueId(), key);
        int multiplierCount = (int) (playerScore / plugin.getCfg().getScores());
        double coefficient = plugin.getCfg().getDefaultCoefficient() + multiplierCount * plugin.getCfg().getCoefficient();
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

    private final Items.ItemData defaultData = new Items.ItemData(0,0,"uncategorized");

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

    private double round(double value) {
        double scale = Math.pow(10, 2);
        return Math.round(value * scale) / scale;
    }

}
