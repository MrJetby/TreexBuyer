package me.jetby.treexBuyer.functions;

import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.configurations.Items;
import me.jetby.treexBuyer.modules.UserData;
import me.jetby.treexBuyer.storage.score.Score;
import me.jetby.treexBuyer.storage.score.types.CategoryScore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class Coefficient {

    final TreexBuyer plugin;

    public Coefficient(TreexBuyer plugin) {
        this.plugin = plugin;
    }

    public double getResult(Player player, Score score) {
        double earned = score.getTotal() / plugin.getCfg().getScores() * plugin.getCfg().getCoefficient();
        double base = plugin.getCfg().getDefaultCoefficient() + earned;
        double legal = Math.min(base, plugin.getCfg().getMaxCoefficient());
        double boost = plugin.getCfg().getBoosts().values().stream()
                .filter(b -> b.permission() != null && player.hasPermission(b.permission()))
                .mapToDouble(Boost::coefficient)
                .sum();

        return plugin.getCfg().isBoosters_except_legal_coefficient()
                ? legal + boost
                : Math.min(base + boost, plugin.getCfg().getMaxCoefficient());
    }

    public double getTotalCoefficientByCategory(Player player, String category) {
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null || !(user.getScore() instanceof CategoryScore)) return 0.0;

        Set<Material> materials = new HashSet<>();
        plugin.getItems().getCategories().forEach((mat, cat) -> {
            if (cat.equalsIgnoreCase(category)) materials.add(mat);
        });

        return materials.stream().mapToDouble(mat -> getResult(player, user.getScore())).sum();
    }

    public double getPrice(Player player, Material material) {
        if (!plugin.getItems().getItemValues().containsKey(material)) return 0.0;
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return 0.0;
        return plugin.getItems().getItemValues().get(material).price() * plugin.getCoefficient().getResult(player, user.getScore());
    }

    public double getTotalScoreByCategory(Player player, String category) {
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null || !(user.getScore() instanceof CategoryScore cs)) return 0.0;

        Set<Material> mats = new HashSet<>();
        plugin.getItems().getCategories().forEach((mat, cat) -> {
            if (cat.equalsIgnoreCase(category)) mats.add(mat);
        });
        return mats.stream().mapToDouble(cs::get).sum();
    }

    public double getItemScore(Material material) {
        Items.ItemData data = plugin.getItems().getItemValues().get(material);
        return data == null ? 0.0 : data.score();
    }

    double round(double value) {
        double scale = Math.pow(10, 2);
        return Math.round(value * scale) / scale;
    }
}