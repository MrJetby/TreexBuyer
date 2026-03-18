package me.jetby.treexBuyer.menus.actions;

import me.jetby.libb.action.Action;
import me.jetby.libb.action.ActionContext;
import me.jetby.treexBuyer.modules.UserData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoBuyStatusToggle implements Action {
    @Override
    public void execute(@NotNull ActionContext ctx, @Nullable String s) {
        Player player = ctx.getPlayer();
        if (player == null) return;
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return;
        user.setAutoBuy(!user.isAutoBuy());
    }
}