package org.jetby.treexBuyer.menus.actions;

import org.jetby.libb.action.Action;
import org.jetby.libb.action.ActionContext;
import org.jetby.libb.action.ActionInput;
import org.jetby.treexBuyer.modules.UserData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AutoBuyStatusToggle implements Action {
    @Override
    public void execute(@NotNull ActionContext ctx, @NotNull ActionInput input) {
        Player player = ctx.getPlayer();
        if (player == null) return;
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return;
        user.setAutoBuy(!user.isAutoBuy());
    }
}