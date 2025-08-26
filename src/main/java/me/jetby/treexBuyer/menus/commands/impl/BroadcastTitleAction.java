package me.jetby.treexBuyer.menus.commands.impl;

import com.jodexindustries.jguiwrapper.api.text.SerializerType;
import me.jetby.treexBuyer.menus.Button;
import me.jetby.treexBuyer.menus.commands.Action;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public class BroadcastTitleAction implements Action {
    @Override
    public void execute(@Nullable Player player, @NotNull String context, Button button) {
        var args = context.split(";");
        var title = args.length > 0 ? args[0] : "";
        var subTitle = args.length > 1 ? args[1] : "";
        int fadeIn = (args.length > 2 ? Integer.parseInt(args[2]) : 10) * 50;
        int stayIn = (args.length > 3 ? Integer.parseInt(args[3]) : 70) * 50;
        int fadeOut = (args.length > 4 ? Integer.parseInt(args[4]) : 20) * 50;

        for (var p : Bukkit.getOnlinePlayers()) {
            p.showTitle(Title.title(
                    SerializerType.LEGACY_AMPERSAND.deserialize(title),
                    SerializerType.LEGACY_AMPERSAND.deserialize(subTitle),
                    Title.Times.of(Duration.ofMillis(fadeIn), Duration.ofMillis(stayIn), Duration.ofMillis(fadeOut))
            ));
        }
    }
}
