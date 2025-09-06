package me.jetby.treexBuyer.menus.commands.impl.buyer;

import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.menus.Button;
import me.jetby.treexBuyer.menus.JGui;
import me.jetby.treexBuyer.menus.Manager;
import me.jetby.treexBuyer.menus.commands.Action;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.jetby.treexBuyer.functions.AutoBuy.isRegularItem;


public record ItemSell(Main plugin) implements Action {

    @Override
    public void execute(@Nullable Player player, @NotNull String context, Button button) {
        if (player == null) return;

        if (player.getGameMode().equals(GameMode.CREATIVE) && !player.hasPermission("treexbuyer.creative.bypass")) {
            return;
        }
        JGui jGui = plugin.getMenuLoader().getJGui().get(player.getUniqueId());

        Material item;
        int amount;
        try {
            item = button.itemStack().getType();
            try {
                amount = Integer.parseInt(context);
            } catch (NumberFormatException e) {
                amount = 0;
                if (!context.equalsIgnoreCase("all")) return;
                for (ItemStack itemStack : player.getInventory().getContents()) {
                    if (itemStack == null) continue;
                    if (itemStack.getType() != item) continue;
                    if (!isRegularItem(itemStack)) continue;
                    amount += itemStack.getAmount();
                }
            }
        } catch (Exception e) {
            return;
        }

        double price = plugin.getItems().getItemValues().get(item).price() * amount;
        price *= plugin.getCoefficient().get(player, button.itemStack().getType());

        int score = plugin.getItems().getItemValues().get(item).score();

        plugin.getEconomy().depositPlayer(player, price);
        String key = plugin.getCoefficient().determineKey(button.itemStack().getType());

        plugin.getStorage().setScore(player.getUniqueId(), key, plugin.getStorage().getScore(player.getUniqueId(), key) + score);

        player.getInventory().removeItem(new ItemStack(item, amount));

        jGui.setTotalPrice(price);
        jGui.setTotalScores(score);

        Manager.refreshMenu(player, jGui, false);

    }
}
