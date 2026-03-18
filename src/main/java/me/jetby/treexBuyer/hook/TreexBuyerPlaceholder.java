package me.jetby.treexBuyer.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.modules.UserData;
import me.jetby.treexBuyer.tools.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TreexBuyerPlaceholder extends PlaceholderExpansion {

    private final TreexBuyer plugin;

    public TreexBuyerPlaceholder(TreexBuyer plugin) {
        this.plugin = plugin;
    }

    public void init() {
        if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Logger.warn("PlaceholderAPI not found. Placeholders are not being working");
            return;
        } else {
            plugin.setTreexBuyerPlaceholder(this);
            plugin.getTreexBuyerPlaceholder().register();
        }
        return;
    }

    @NotNull @Override public String getIdentifier() { return "treexbuyer"; }
    @NotNull @Override public String getAuthor() { return String.join(", ", plugin.getDescription().getAuthors()); }
    @NotNull @Override public String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return "";

        String args = params.toLowerCase();

        if (args.equalsIgnoreCase("autobuy")) return String.valueOf(user.isAutoBuy());

        if (args.equalsIgnoreCase("score"))
            return String.valueOf(user.getTotalScore());

        if (args.startsWith("score_item_")) {
            try {
                Material mat = Material.valueOf(params.substring("score_item_".length()).toUpperCase());
                return String.valueOf(user.getScore(mat));
            } catch (IllegalArgumentException e) {
                return "invalid_material";
            }
        }

        if (args.startsWith("score_category_"))
            return String.valueOf(plugin.getCoefficient().getTotalScoreByCategory(player, params.substring("score_category_".length())));

        if (args.equalsIgnoreCase("coefficient"))
            return String.valueOf(plugin.getCoefficient().getResult(player, null));

        if (args.startsWith("coefficient_category_"))
            return String.valueOf(plugin.getCoefficient().getTotalCoefficientByCategory(player, params.substring("coefficient_category_".length())));

        return null;
    }
}