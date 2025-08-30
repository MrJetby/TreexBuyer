package me.jetby.treexBuyer.menus.commands.impl.standard;

import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.menus.Button;
import me.jetby.treexBuyer.menus.JGui;
import me.jetby.treexBuyer.menus.Loader;
import me.jetby.treexBuyer.menus.commands.Action;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record OpenMenu(Main plugin, Loader menuLoader) implements Action {

    @Override
    public void execute(@Nullable Player player, @NotNull String context, Button button) {
        if (player == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> new JGui(plugin.getMenuLoader().getMenus().get(context), plugin, player).open(player), 1L);
    }
}