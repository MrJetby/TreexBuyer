package me.jetby.treexBuyer.menus.actions;

import me.jetby.libb.action.Action;
import me.jetby.libb.action.ActionContext;
import me.jetby.libb.gui.item.ItemWrapper;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.functions.AutoBuy;
import me.jetby.treexBuyer.menus.BuyerGui;
import me.jetby.treexBuyer.modules.UserData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SellItem implements Action {
    @Override
    public void execute(@NotNull ActionContext ctx, @Nullable String s) {
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

        double score = TreexBuyer.getInstance().getItems().getScoreAmount(wrapper.itemStack().getType()) * amount;
        double price = TreexBuyer.getInstance().getCoefficient().getPriceWithCoefficient(player, wrapper.itemStack().getType()) * amount;

        UserData.findByUuid(player.getUniqueId()).addScore(wrapper.itemStack().getType(), score);
        TreexBuyer.getInstance().getEconomy().depositPlayer(player, price);
        gui.refresh();
    }
}