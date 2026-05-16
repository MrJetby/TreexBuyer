package org.jetby.treexBuyer.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.modules.UserData;
import org.jetby.treexBuyer.tools.NumberUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TreexBuyerPlaceholder extends PlaceholderExpansion {

    private final BuyerManager manager;

    public TreexBuyerPlaceholder(BuyerManager manager) {
        this.manager = manager;
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "treexbuyer";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return String.join(", ", manager.getPlugin().getDescription().getAuthors());
    }

    @NotNull
    @Override
    public String getVersion() {
        return manager.getPlugin().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return "";

        String args = params.toLowerCase();

        if (args.equalsIgnoreCase("autobuy")) return String.valueOf(user.isAutoBuy());

        if (args.equalsIgnoreCase("score"))
            return NumberUtils.format(user.getTotalScore());

        if (args.startsWith("score_item_")) {
            try {
                Material mat = Material.valueOf(params.substring("score_item_".length()).toUpperCase());
                return NumberUtils.format(user.getScore(mat));
            } catch (IllegalArgumentException e) {
                return "invalid_material";
            }
        }

        if (args.startsWith("score_category_"))
            return NumberUtils.format(manager.getCoefficient().getTotalScoreByCategory(player, params.substring("score_category_".length())));

        if (args.equalsIgnoreCase("coefficient"))
            return NumberUtils.format(manager.getCoefficient().getTotalCoefficient(player, user.getScore()));

        if (args.startsWith("coefficient_category_"))
            return NumberUtils.format(manager.getCoefficient().getTotalCoefficientByCategory(player, params.substring("coefficient_category_".length())));

        return null;
    }
}