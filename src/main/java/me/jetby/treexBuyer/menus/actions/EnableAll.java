package me.jetby.treexBuyer.menus.actions;

import me.jetby.libb.action.Action;
import me.jetby.libb.action.ActionContext;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.menus.BuyerGui;
import me.jetby.treexBuyer.modules.UserData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnableAll implements Action {
    @Override
    public void execute(@NotNull ActionContext ctx, @Nullable String s) {
        Player player = ctx.getPlayer();
        if (player == null) return;
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return;

        BuyerGui gui = ctx.get(BuyerGui.class);
        if (gui != null) {
            gui.getVisibleMaterials().forEach(user::addAutoBuyMaterial);
        } else {
            user.getAutoBuyItems().addAll(TreexBuyer.getInstance().getItems().getItemValues().keySet());
        }
    }
}