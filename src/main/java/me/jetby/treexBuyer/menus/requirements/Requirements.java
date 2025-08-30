package me.jetby.treexBuyer.menus.requirements;

import lombok.experimental.UtilityClass;
import me.jetby.treexBuyer.menus.Button;
import me.jetby.treexBuyer.menus.commands.ActionExecutor;
import me.jetby.treexBuyer.menus.commands.ActionRegistry;
import me.jetby.treexBuyer.tools.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static me.jetby.treexBuyer.Main.df;
import static me.jetby.treexBuyer.functions.AutoBuy.isRegularItem;

@UtilityClass
public class Requirements {

    public boolean check(Player player, ViewRequirement req, double totalPrice, int totalScore, Button button) {
        return checkInternal(player, req.type(), req.permission(), req.input(), req.output(),
                totalPrice, totalScore, button);
    }

    public boolean check(Player player, ClickRequirement req, double totalPrice, int totalScore, Button button) {
        return checkInternal(player, req.type(), req.permission(), req.input(), req.output(),
                totalPrice, totalScore, button);
    }

    private boolean checkInternal(Player player,
                                  String type,
                                  String permission,
                                  String input,
                                  String output,
                                  double totalPrice,
                                  int totalScore,
                                  Button button) {

        switch (type.toLowerCase()) {
            case "has permission":
                return player.hasPermission(permission);
            case "!has permission":
                return !player.hasPermission(permission);
            case "string equals":
                return input.equalsIgnoreCase(output);
            case "!string equals":
                return !input.equalsIgnoreCase(output);
            case "javascript":
                return evalJavascriptLike(player, input, totalPrice, totalScore, button);
            default:
                return false;
        }
    }

    private boolean evalJavascriptLike(Player player, String input, double totalPrice, int totalScore, Button button) {
        String[] args = input.split(" ");
        if (args.length < 3) return false;

        args[0] = setPlaceholders(player, args[0], totalPrice, totalScore, button);
        args[2] = setPlaceholders(player, args[2], totalPrice, totalScore, button);

        try {
            double x = Double.parseDouble(args[0]);
            double x1 = Double.parseDouble(args[2]);
            return switch (args[1]) {
                case ">" -> x > x1;
                case ">=" -> x >= x1;
                case "==" -> x == x1;
                case "!=" -> x != x1;
                case "<=" -> x <= x1;
                case "<" -> x < x1;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return switch (args[1]) {
                case "==" -> args[0].equals(args[2]);
                case "!=" -> !args[0].equals(args[2]);
                default -> false;
            };
        }
    }

    private static String items(Player player, Button button) {
        int amount = 0;
        try {
            Material item = button.itemStack().getType();
            for (ItemStack stack : player.getInventory().getContents()) {
                if (stack == null) continue;
                if (stack.getType() != item) continue;
                if (!isRegularItem(stack)) continue;
                amount += stack.getAmount();
            }
        } catch (Exception e) {
            amount = 0;
        }
        return String.valueOf(amount);
    }

    public static void runDenyCommands(Player player, List<String> denyCommands,
                                       double totalPrice, int totalScore, Button button) {
        List<String> commands = new ArrayList<>();
        for (String str : denyCommands) {
            commands.add(setPlaceholders(player, str, totalPrice, totalScore, button));
        }
        ActionExecutor.execute(player, ActionRegistry.transform(commands), button);
    }

    private String setPlaceholders(Player player, String string,
                                   double totalPrice, int totalScore, Button button) {
        return TextUtil.setPapi(player, string
                .replace("%sell_pay%", df.format(totalPrice))
                .replace("%sell_score%", df.format(totalScore))
                .replace("%item_amount%", items(player, button))
        );
    }
}
