package me.jetby.treexBuyer.menus.commands.impl;

import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.menus.Button;
import me.jetby.treexBuyer.menus.JGui;
import me.jetby.treexBuyer.menus.Manager;
import me.jetby.treexBuyer.menus.commands.Action;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public record AutoBuyItemToggleAction(Main plugin) implements Action {

    @Override
    public void execute(@Nullable Player player, @NotNull String context, Button button) {
        if (player == null) return;

        Inventory inventory = player.getOpenInventory().getTopInventory();

        if (inventory == plugin.getMenuLoader().getJGui().get(player.getUniqueId()).getInventory()) {
            JGui jGui = plugin.getMenuLoader().getJGui().get(player.getUniqueId());

            String materialUpper;
            if (context.isEmpty()) {
                materialUpper = button.material().name().toUpperCase();
            } else {
                materialUpper = context.toUpperCase();
            }

            List<String> autoBuyItems = plugin.getStorage().getAutoBuyItems(player.getUniqueId());
            if (autoBuyItems.contains(materialUpper)) {
                autoBuyItems.remove(materialUpper);
            } else {
                autoBuyItems.add(materialUpper);
            }
            plugin.getStorage().setAutoBuyItems(player.getUniqueId(), autoBuyItems);
            Manager.refreshMenu(player, jGui, true);
        }
    }
}
