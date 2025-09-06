package me.jetby.treexBuyer.menus.commands.impl.buyer;

import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.configurations.Items;
import me.jetby.treexBuyer.menus.Button;
import me.jetby.treexBuyer.menus.JGui;
import me.jetby.treexBuyer.menus.Manager;
import me.jetby.treexBuyer.menus.commands.Action;
import me.jetby.treexBuyer.tools.Logger;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.jetby.treexBuyer.Main.NAMESPACED_KEY;
import static me.jetby.treexBuyer.functions.AutoBuy.isRegularItem;

public record SellAll(Main plugin) implements Action {

    @Override
    public void execute(@Nullable Player player, @NotNull String context, Button button) {
        if (player == null) return;

        if (player.getGameMode().equals(GameMode.CREATIVE) && !player.hasPermission("treexbuyer.creative.bypass")) {
            return;
        }

        Inventory inventory = player.getOpenInventory().getTopInventory();

        if (inventory == plugin.getMenuLoader().getJGui().get(player.getUniqueId()).getInventory()) {
            JGui jGui = plugin.getMenuLoader().getJGui().get(player.getUniqueId());
            double totalPrice = 0.0;
            for (int i : jGui.getSellZoneSlots()) {

                ItemStack itemStack = inventory.getItem(i);
                if (itemStack == null) continue;
                if (itemStack.getItemMeta().getPersistentDataContainer().has(NAMESPACED_KEY, PersistentDataType.STRING)) continue;
                if (!isRegularItem(itemStack)) continue;

                if (!plugin.getItems().getItemValues().containsKey(itemStack.getType())) continue;
                Items.ItemData itemData = plugin.getItems().getItemValues().get(itemStack.getType());

                double price = itemData.price() * plugin.getCoefficient().get(player, itemStack.getType());
                int score = itemData.score() * itemStack.getAmount();

                totalPrice += price * itemStack.getAmount();

                if (score > 0) {
                    String key = plugin.getCoefficient().determineKey(itemStack.getType());
                    plugin.getStorage().setScore(player.getUniqueId(), key, plugin.getStorage().getScore(player.getUniqueId(), key) + score);
                }

                inventory.setItem(i, null);
            }



            if (totalPrice > 0L) {
                plugin.getEconomy().depositPlayer(player, totalPrice);
            }

            Manager.refreshMenu(player, jGui, true);


        }

    }
}