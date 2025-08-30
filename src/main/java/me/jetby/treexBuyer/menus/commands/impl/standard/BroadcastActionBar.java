package me.jetby.treexBuyer.menus.commands.impl.standard;

import com.jodexindustries.jguiwrapper.api.text.SerializerType;
import me.jetby.treexBuyer.menus.Button;
import me.jetby.treexBuyer.menus.commands.Action;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BroadcastActionBar implements Action {
    @Override
    public void execute(@Nullable Player player, @NotNull String context, Button button) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendActionBar(SerializerType.LEGACY_AMPERSAND.deserialize(context));
        }
    }
}
