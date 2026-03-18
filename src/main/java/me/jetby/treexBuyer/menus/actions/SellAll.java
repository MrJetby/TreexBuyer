package me.jetby.treexBuyer.menus.actions;

import me.jetby.libb.action.Action;
import me.jetby.libb.action.ActionContext;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.functions.AutoBuy;
import me.jetby.treexBuyer.menus.BuyerGui;
import me.jetby.treexBuyer.modules.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SellAll implements Action {
    @Override
    public void execute(@NotNull ActionContext ctx, @Nullable String s) {
        BuyerGui gui = ctx.get(BuyerGui.class);
        Player player = ctx.getPlayer();
        if (gui == null || player == null) return;

        gui.getSellSlots().forEach(slot -> {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item == null || !AutoBuy.isRegularItem(item)) return;

            double score = TreexBuyer.getInstance().getCoefficient().getItemScore(item.getType()) * item.getAmount();
            double price = TreexBuyer.getInstance().getCoefficient().getPrice(player, item.getType()) * item.getAmount();

            if (score <= 0) return;

            gui.getInventory().setItem(slot, null);

            UserData.findByUuid(player.getUniqueId()).addScore(item.getType(), score);
            TreexBuyer.getInstance().getEconomy().depositPlayer(player, price);
        });

        Bukkit.getScheduler().runTask(TreexBuyer.getInstance(), gui::refresh);
    }
}