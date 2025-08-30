package me.jetby.treexBuyer.menus.commands;

import lombok.experimental.UtilityClass;
import me.jetby.treexBuyer.menus.Button;
import me.jetby.treexBuyer.menus.Placeholder;
import me.jetby.treexBuyer.tools.TextUtil;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

@UtilityClass
public class ActionExecutor {

    public void execute(Player player, Map<ActionType, List<String>> actions, Button button, Placeholder... placeholders) {
        actions.keySet().forEach(type -> {
            var contexts = actions.get(type);
            for (String context : contexts) {
                for (Placeholder placeholder : placeholders) {
                    context = placeholder.replace(context);
                }
                var c = TextUtil.setPapi(player, context);
                type.getAction().execute(player, c, button);
            }
        });
    }
}
