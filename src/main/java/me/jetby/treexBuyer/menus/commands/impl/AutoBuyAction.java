package me.jetby.treexBuyer.menus.commands.impl;

import lombok.RequiredArgsConstructor;
import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.menus.Button;
import me.jetby.treexBuyer.menus.JGui;
import me.jetby.treexBuyer.menus.Manager;
import me.jetby.treexBuyer.menus.commands.Action;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@RequiredArgsConstructor
public class AutoBuyAction implements Action {

    private final Main plugin;

    @Override
    public void execute(@Nullable Player player, @NotNull String context, Button button) {
        if (player == null) return;
        Inventory inventory = player.getOpenInventory().getTopInventory();

        if (inventory == plugin.getMenuLoader().getJGui().get(player.getUniqueId()).getInventory()) {
            JGui jGui = plugin.getMenuLoader().getJGui().get(player.getUniqueId());

            plugin.getStorage().setAutoBuyStatus(player.getUniqueId(), !plugin.getStorage().getAutoBuyStatus(player.getUniqueId()));
            Manager.refreshMenu(player, jGui, true);
        }

    }
}
