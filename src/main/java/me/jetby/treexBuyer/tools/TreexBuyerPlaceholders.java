package me.jetby.treexBuyer.tools;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.jetby.treexBuyer.Main;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class TreexBuyerPlaceholders extends PlaceholderExpansion {

    private final Main plugin;
    @Override
    public @NotNull String getIdentifier() {
        return "treexbuyer";
    }

    @Override
    public @NotNull String getAuthor() {
        return "";
    }

    @Override
    public @NotNull String getVersion() {
        return "";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {

        if (identifier.startsWith("coefficient_category_".toLowerCase())) {
            String string = identifier.replace("coefficient_category_", "");
            return String.valueOf(plugin.getCoefficient().getByCategory(player, string));
        }
        if (identifier.startsWith("score_category_".toLowerCase())) {
            String string = identifier.replace("score_category_", "");
            return String.valueOf(plugin.getStorage().getScore(player.getUniqueId(), string));
        }
        if (identifier.startsWith("score_".toLowerCase())) {
            String string = identifier.replace("score_", "").toUpperCase();
            try {
                Material material = Material.valueOf(string);
                return String.valueOf(plugin.getStorage().getScore(player.getUniqueId(), material.name()));
            } catch (IllegalArgumentException e) {
                return "Material incorrect";
            }
        }

        if (identifier.startsWith("top_") && identifier.endsWith("_name")) {
            int number = Integer.parseInt(identifier
                    .replace("top_", "")
                    .replace("_name", ""));
            return plugin.getStorage().getTopName(number);
        }
        if (identifier.startsWith("top_") && identifier.endsWith("_score")) {
            int number = Integer.parseInt(identifier
                    .replace("top_", "")
                    .replace("_score", ""));
            return String.valueOf(plugin.getStorage().getTopScore(number));
        }


        return switch (identifier.toLowerCase()) {
            case "score" -> String.valueOf(plugin.getStorage().getScore(player.getUniqueId(), plugin.getCfg().getType().name()));
            case "coefficient" -> String.valueOf(plugin.getCoefficient().get(player, null));
            default -> null;
        };

    }

}
