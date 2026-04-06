package me.jetby.treexBuyer.functions;

import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.configurations.Items;
import me.jetby.treexBuyer.modules.UserData;
import me.jetby.treexBuyer.storage.score.Score;
import me.jetby.treexBuyer.storage.score.types.CategoryScore;
import me.jetby.treexBuyer.storage.score.types.PerItemScore;
import me.jetby.treexBuyer.tools.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Coefficient {

    final TreexBuyer plugin;

    public Coefficient(TreexBuyer plugin) {
        this.plugin = plugin;
    }

    public double getTotalCoefficient(Player player, Score score) {
        return getTotalCoefficient(player, score.getTotal());
    }
    public double getTotalCoefficient(Player player, double relevantScore) {
        double earned = relevantScore / plugin.getCfg().getScores() * plugin.getCfg().getCoefficient();
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
        if (user == null || !(user.getScore() instanceof CategoryScore cs)) return 0.0;

        CategoryScore isolated = new CategoryScore(plugin.getItems().getCategories());
        isolated.set(category, cs.get(category));

        return getTotalCoefficient(player, cs);
    }

    public double getPriceWithCoefficient(Player player, Material material) {
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return 0.0;

        Score score = user.getScore();
        double relevantScore;

        if (score instanceof PerItemScore s) {
            relevantScore = s.get(material);
        } else if (score instanceof CategoryScore s) {
            relevantScore = s.get(plugin.getItems().getCategory(material));
        } else {
            relevantScore = score.getTotal();
        }

        return plugin.getItems().getOriginalPrice(material) * getTotalCoefficient(player, relevantScore);
    }

    public double getTotalScoreByCategory(Player player, String category) {
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null || !(user.getScore() instanceof CategoryScore cs)) return 0.0;
        return cs.get(category);
    }
}