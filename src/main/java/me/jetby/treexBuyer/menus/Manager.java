package me.jetby.treexBuyer.menus;

import com.jodexindustries.jguiwrapper.api.item.ItemWrapper;
import lombok.experimental.UtilityClass;
import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.configurations.Items;
import me.jetby.treexBuyer.functions.AutoBuy;
import me.jetby.treexBuyer.menus.commands.Command;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import static me.jetby.treexBuyer.Main.NAMESPACED_KEY;

@UtilityClass
public class Manager {

    public String check(boolean status) {
        if (status) {
            return Main.getInstance().getCfg().getEnable();
        }
        return Main.getInstance().getCfg().getDisable();
    }

    public void refreshMenu(Player player, JGui jGui, boolean resetPrice) {

        if (!player.getOpenInventory().getTopInventory().equals(jGui.getInventory())) return;
        jGui.runTask(() -> {
            if (resetPrice) {
                jGui.setTotalPrice(0.0);
                jGui.setTotalScores(0);
            }


            for (int i : jGui.sellZoneSlots) {

                ItemStack itemStack = jGui.getInventory().getItem(i);
                if (itemStack == null || itemStack.getType().equals(Material.AIR)) continue;
                if (!AutoBuy.isRegularItem(itemStack)) continue;

                if (itemStack.getItemMeta().getPersistentDataContainer().has(NAMESPACED_KEY, PersistentDataType.STRING))
                    continue;

                if (!jGui.plugin.getItems().getItemValues().containsKey(itemStack.getType())) continue;
                Items.ItemData itemData = jGui.plugin.getItems().getItemValues().get(itemStack.getType());

                double price = itemData.price() * jGui.plugin.getCoefficient().get(player, itemStack.getType());
                int score = itemData.score();


                jGui.setTotalPrice(jGui.getTotalPrice() + price * itemStack.getAmount());
                jGui.setTotalScores(jGui.getTotalScores( )+score * itemStack.getAmount());
            }

            for (Button button : jGui.getMenu().buttons()) {
                if (button.sellZone()) continue;

                jGui.getController(button.id() + button.slot()).get().updateItems(wrapper -> {
                    ItemStack itemStack = wrapper.itemStack();
                    if (itemStack.getType().equals(Material.AIR)) return;
                    if (!itemStack.getItemMeta().getPersistentDataContainer().has(NAMESPACED_KEY, PersistentDataType.STRING)) {
                        return;
                    }

                    ItemMeta itemMeta = itemStack.getItemMeta();
                    itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    itemMeta.setDisplayName(button.displayName());
                    itemMeta.setLore(button.lore());

                    if (itemMeta.getPersistentDataContainer().get(NAMESPACED_KEY, PersistentDataType.STRING).equalsIgnoreCase("menu_autobuy")) {
                        wrapper.enchanted(jGui.plugin.getStorage().getAutoBuyStatus(player.getUniqueId()));
                    }
                    if (!button.commands().isEmpty()) {
                        for (Command command : button.commands()) {
                            boolean hasCmd = false;
                            for (String cmd : command.actions()) {
                                if (cmd.startsWith("[AUTOBUY_ITEM_TOGGLE]") || cmd.startsWith("[SELL_ITEM]")) {
                                    hasCmd = true;
                                    break;
                                }
                            }
                            if (!hasCmd) continue;
                            if (!jGui.plugin.getItems().getItemValues().containsKey(wrapper.itemStack().getType()))
                                continue;
                            if (!itemMeta.getPersistentDataContainer().get(NAMESPACED_KEY, PersistentDataType.STRING).equalsIgnoreCase("menu_priceItem"))
                                continue;

                            if (jGui.plugin.getStorage().getAutoBuyItems(player.getUniqueId()).contains(wrapper.itemStack().getType().name())) itemMeta.addEnchant(Enchantment.KNOCKBACK, 0, false);
                            break;
                        }

                    }
                    itemStack.setItemMeta(itemMeta);
                    wrapper.update((HumanEntity) player);
                });
            }
        }, 1L);
    }

    public @NotNull String getAutoBuyItemToggle(ItemWrapper wrapper, String cmd) {
        String input;
        if (cmd.startsWith("[AUTOBUY_ITEM_TOGGLE] ")) {
            input = cmd.replace("[AUTOBUY_ITEM_TOGGLE] ", "");
        } else if (cmd.startsWith("[AUTOBUY_ITEM_TOGGLE]")) {
            input = cmd.replace("[AUTOBUY_ITEM_TOGGLE]", "");

        } else if (cmd.startsWith("[SELL_ITEM] ")) {
            input = cmd.replace("[SELL_ITEM] ", "");
        } else if (cmd.startsWith("[SELL_ITEM]")) {
            input = cmd.replace("[SELL_ITEM]", "");

        } else {
            input = wrapper.itemStack().getType().name();
        }
        if (input.equalsIgnoreCase("")) {
            input = wrapper.itemStack().getType().name();
        }
        return input;
    }

}
