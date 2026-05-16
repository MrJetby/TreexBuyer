package org.jetby.treexBuyer.functions;

import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.modules.UserData;
import org.jetby.treexBuyer.storage.score.Score;
import org.jetby.treexBuyer.storage.score.types.CategoryScore;
import org.jetby.treexBuyer.storage.score.types.PerItemScore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Coefficient {

    final BuyerManager manager;

    public Coefficient(BuyerManager manager) {
        this.manager = manager;
    }

    public double getTotalCoefficient(Player player, Score score) {
        return getTotalCoefficient(player, score.getTotal());
    }
    public double getTotalCoefficient(Player player, double relevantScore) {
        double earned = relevantScore / manager.getCfg().getScores() * manager.getCfg().getCoefficient();
        double base = manager.getCfg().getDefaultCoefficient() + earned;
        double legal = Math.min(base, manager.getCfg().getMaxCoefficient());
        double boost = manager.getCfg().getBoosts().values().stream()
                .filter(b -> b.permission() != null && player.hasPermission(b.permission()))
                .mapToDouble(Boost::coefficient)
                .sum();
        return manager.getCfg().isBoosters_except_legal_coefficient()
                ? legal + boost
                : Math.min(base + boost, manager.getCfg().getMaxCoefficient());
    }
    public double getTotalCoefficientByCategory(Player player, String category) {
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null || !(user.getScore() instanceof CategoryScore cs)) return 0.0;

        CategoryScore isolated = new CategoryScore(manager.getItems().getCategories());
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
            relevantScore = s.get(manager.getItems().getCategory(material));
        } else {
            relevantScore = score.getTotal();
        }

        return manager.getItems().getOriginalPrice(material) * getTotalCoefficient(player, relevantScore);
    }

    public double getTotalScoreByCategory(Player player, String category) {
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null || !(user.getScore() instanceof CategoryScore cs)) return 0.0;
        return cs.get(category);
    }
}