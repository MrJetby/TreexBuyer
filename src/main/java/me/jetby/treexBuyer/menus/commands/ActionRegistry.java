package me.jetby.treexBuyer.menus.commands;

import lombok.experimental.UtilityClass;
import me.jetby.treexBuyer.tools.TextUtil;
import me.jetby.treexBuyer.tools.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@UtilityClass
public class ActionRegistry {
    private static final Pattern ACTION_PATTERN = Pattern.compile("\\[(\\S+)] ?(.*)");

    public Map<ActionType, List<String>> transform(List<String> settings) {
        Map<ActionType, List<String>> actions = new HashMap<>();
        settings.forEach(s -> {
            var matcher = ACTION_PATTERN.matcher(s);
            if (!matcher.matches()) {
                Logger.warn("Illegal action pattern " + s);
                return;
            }
            var type = ActionType.getType(matcher.group(1).toUpperCase());
            if (type == null) {
                Logger.warn("ActionType " + s + " is not available!");
                return;
            }
            var context = matcher.group(2).trim();
            actions.putIfAbsent(type, new ArrayList<>());
            actions.get(type).add(context);
        });
        return actions;
    }
}
