package org.jetby.treexBuyer.menus.actions;

import org.jetby.libb.action.Action;
import org.jetby.libb.action.ActionContext;
import org.jetby.libb.action.ActionInput;
import org.jetby.libb.gui.item.ItemWrapper;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.configurations.Items;
import org.jetby.treexBuyer.modules.UserData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AutoBuyItemToggle implements Action {
    @Override
    public void execute(@NotNull ActionContext ctx, @NotNull ActionInput input) {
        Player player = ctx.getPlayer();
        if (player == null) return;
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return;

        ItemWrapper wrapper = ctx.get(ItemWrapper.class);
        if (wrapper == null || wrapper.itemStack() == null) return;

        Material material = wrapper.itemStack().getType();
        Items.ItemData data = BuyerManager.MANAGER.getItems().getItemValues().get(material);
        if (data == null) return;

        if (user.getAutoBuyItems().contains(material)) {
            user.removeAutoBuyMaterial(material);
        } else {
            user.addAutoBuyMaterial(material);
        }
    }
}