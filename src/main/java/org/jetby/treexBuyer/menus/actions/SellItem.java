package org.jetby.treexBuyer.menus.actions;

import org.jetby.libb.action.Action;
import org.jetby.libb.action.ActionContext;
import org.jetby.libb.action.ActionInput;
import org.jetby.libb.gui.item.ItemWrapper;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.functions.AutoBuy;
import org.jetby.treexBuyer.menus.BuyerGui;
import org.jetby.treexBuyer.modules.UserData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SellItem implements Action {
    @Override
    public void execute(@NotNull ActionContext ctx, @NotNull ActionInput input) {

        String s = input.rawText();

        Player player = ctx.getPlayer();
        if (player == null) return;

        BuyerGui gui = ctx.get(BuyerGui.class);
        ItemWrapper wrapper = ctx.get(ItemWrapper.class);
        if (gui == null || wrapper == null || s == null) return;

        int amount;
        try {
            amount = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            if (!s.equalsIgnoreCase("all")) return;
            amount = 0;
            for (ItemStack itemStack : player.getInventory().getContents()) {
                if (itemStack == null) continue;
                if (itemStack.getType() != wrapper.itemStack().getType()) continue;
                if (!AutoBuy.isRegularItem(itemStack)) continue;
                amount += itemStack.getAmount();
            }
        }

        if (amount <= 0) return;

        player.getInventory().removeItem(new ItemStack(wrapper.itemStack().getType(), amount));

        double score = BuyerManager.MANAGER.getItems().getScoreAmount(wrapper.itemStack().getType()) * amount;
        double price = BuyerManager.MANAGER.getCoefficient().getPriceWithCoefficient(player, wrapper.itemStack().getType()) * amount;

        UserData.findByUuid(player.getUniqueId()).addScore(wrapper.itemStack().getType(), score);
        BuyerManager.MANAGER.getEconomy().depositPlayer(player, price);
        gui.refresh();
    }
}