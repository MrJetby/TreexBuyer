package me.jetby.treexBuyer.menus.commands.impl.standard;

import me.jetby.treexBuyer.menus.Button;
import me.jetby.treexBuyer.menus.commands.Action;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Player implements Action {
    @Override
    public void execute(@Nullable org.bukkit.entity.Player player, @NotNull String context, Button button) {
        if (player != null) {
            player.chat(context);
        }
    }
}
