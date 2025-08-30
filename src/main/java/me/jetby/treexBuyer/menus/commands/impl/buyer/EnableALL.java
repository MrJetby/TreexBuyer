package me.jetby.treexBuyer.menus.commands.impl.buyer;

import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.menus.Button;
import me.jetby.treexBuyer.menus.JGui;
import me.jetby.treexBuyer.menus.Manager;
import me.jetby.treexBuyer.menus.commands.Action;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static me.jetby.treexBuyer.Main.NAMESPACED_KEY;

public record EnableALL(Main plugin) implements Action {

    @Override
    public void execute(@Nullable Player player, @NotNull String context, Button button) {
        if (player == null) return;

        Inventory inventory = player.getOpenInventory().getTopInventory();

        if (inventory == plugin.getMenuLoader().getJGui().get(player.getUniqueId()).getInventory()) {
            JGui jGui = plugin.getMenuLoader().getJGui().get(player.getUniqueId());
            List<String> autoBuyItems = plugin.getStorage().getAutoBuyItems(player.getUniqueId());
            for (ItemStack itemStack : inventory.getContents()) {
                if (itemStack == null || itemStack.getType().isAir()) continue;
                if (autoBuyItems.contains(itemStack.getType().name())) continue;
                if (!itemStack.getItemMeta().getPersistentDataContainer().has(NAMESPACED_KEY, PersistentDataType.STRING))
                    continue;
                String itemId = itemStack.getItemMeta().getPersistentDataContainer().get(NAMESPACED_KEY, PersistentDataType.STRING);

                if (!"menu_priceItem".equalsIgnoreCase(itemId)) continue;
                autoBuyItems.add(itemStack.getType().name());
            }
            plugin.getStorage().setAutoBuyItems(player.getUniqueId(), autoBuyItems);
            Manager.refreshMenu(player, jGui, true);

        }

    }
}
