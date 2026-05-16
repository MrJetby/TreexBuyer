package org.jetby.treexBuyer.menus.actions;

import org.jetby.libb.action.Action;
import org.jetby.libb.action.ActionContext;
import org.jetby.libb.action.ActionInput;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.TreexBuyer;
import org.jetby.treexBuyer.menus.BuyerGui;
import org.jetby.treexBuyer.modules.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SellAll implements Action {
    @Override
    public void execute(@NotNull ActionContext ctx, @NotNull ActionInput input) {
        BuyerGui gui = ctx.get(BuyerGui.class);
        Player player = ctx.getPlayer();
        if (gui == null || player == null) return;

        gui.getSellSlots().forEach(slot -> {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item == null) return;

            double score = BuyerManager.MANAGER.getItems().getScoreAmount(item.getType()) * item.getAmount();
            double price = BuyerManager.MANAGER.getCoefficient().getPriceWithCoefficient(player, item.getType()) * item.getAmount();

            if (score <= 0) return;

            gui.getInventory().setItem(slot, null);

            UserData.findByUuid(player.getUniqueId()).addScore(item.getType(), score);
            BuyerManager.MANAGER.getEconomy().depositPlayer(player, price);
        });

        Bukkit.getScheduler().runTask(TreexBuyer.INSTANCE, gui::refresh);
    }
}