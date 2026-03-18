package me.jetby.treexBuyer.menus.actions;

import me.jetby.libb.action.Action;
import me.jetby.libb.action.ActionContext;
import me.jetby.libb.gui.item.ItemWrapper;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.configurations.Items;
import me.jetby.treexBuyer.modules.UserData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoBuyItemToggle implements Action {
    @Override
    public void execute(@NotNull ActionContext ctx, @Nullable String s) {
        Player player = ctx.getPlayer();
        if (player == null) return;
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return;

        ItemWrapper wrapper = ctx.get(ItemWrapper.class);
        if (wrapper == null || wrapper.itemStack() == null) return;

        Material material = wrapper.itemStack().getType();
        Items.ItemData data = TreexBuyer.getInstance().getItems().getItemValues().get(material);
        if (data == null) return;

        if (user.getAutoBuyItems().contains(material)) {
            user.removeAutoBuyMaterial(material);
        } else {
            user.addAutoBuyMaterial(material);
        }
    }
}