package me.jetby.treexBuyer.menus;

import com.jodexindustries.jguiwrapper.api.item.ItemWrapper;
import com.jodexindustries.jguiwrapper.api.placeholder.PlaceholderEngine;
import com.jodexindustries.jguiwrapper.gui.advanced.AdvancedGui;
import lombok.Getter;
import lombok.Setter;
import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.menus.commands.ActionExecutor;
import me.jetby.treexBuyer.menus.commands.ActionRegistry;
import me.jetby.treexBuyer.menus.commands.Command;
import me.jetby.treexBuyer.menus.requirements.Requirements;
import me.jetby.treexBuyer.menus.requirements.ClickRequirement;
import me.jetby.treexBuyer.tools.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

import static me.jetby.treexBuyer.Main.NAMESPACED_KEY;
import static me.jetby.treexBuyer.Main.df;


public class JGui extends AdvancedGui implements Listener {

    @Getter
    private final Menu menu;
    private final Player player;
    @Getter
    private Inventory inventory;

    @Getter @Setter
    private double totalPrice = 0.0;
    @Getter @Setter
    private int totalScores = 0;

    private final PlaceholderEngine mainPlaceholders = PlaceholderEngine.of();

    @Getter
    final List<Integer> sellZoneSlots = new ArrayList<>();
    final Main plugin;

    public JGui(Menu menu, Main plugin, Player player) {
        super(menu.size(), menu.title());

        this.menu = menu;
        this.player = player;
        this.plugin = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        // Placeholders start
        mainPlaceholders.register("%sell_pay%", (offlinePlayer) -> df.format(totalPrice));
        mainPlaceholders.register("%sell_score%", (offlinePlayer) -> df.format(totalScores));
        mainPlaceholders.register("%coefficient%", (offlinePlayer) ->  String.valueOf(plugin.getCoefficient().get(player, null)));

        mainPlaceholders.register("%global_auto_sell_toggle_state%", (offlinePlayer -> {
            if (offlinePlayer != null)
                return Manager.check(plugin.getStorage().getAutoBuyStatus(offlinePlayer.getUniqueId()));
            return "";
        }));

        mainPlaceholders.register("%score%", (offlinePlayer) -> {
            if (offlinePlayer != null)
                return String.valueOf(plugin.getStorage().getScore(offlinePlayer.getUniqueId(), "global"));
            return "";
        });
        // Placeholder end

        registerButtons();

        onOpen(event -> {
            plugin.getMenuLoader().getJGui().put(player.getUniqueId(), this);
            inventory = event.getInventory();
            Manager.refreshMenu(player, this, true);
        });

        setCancelEmptySlots(false);

        onDrag(event -> {
            Inventory top = event.getView().getTopInventory();
            if (top != inventory) return;
            int topSize = top.getSize();
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot >= topSize) continue;

                if (!sellZoneSlots.contains(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
            event.setCancelled(false);
        });

        onClose(event -> {
            for (ItemStack itemStack : event.getInventory().getContents()) {
                if (itemStack == null || itemStack.getType().equals(Material.AIR)) continue;
                if (itemStack.getItemMeta().getPersistentDataContainer().has(NAMESPACED_KEY, PersistentDataType.STRING))
                    continue;
                if (event.getPlayer().getInventory().firstEmpty() == -1) {
                    event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), itemStack);
                    continue;
                }
                event.getPlayer().getInventory().addItem(itemStack);
                event.getInventory().remove(itemStack);
            }
//            plugin.getMenuLoader( ).getJGui( ).remove(event.getPlayer( ).getUniqueId( ));
        });
    }


    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        Inventory topInventory = p.getOpenInventory().getTopInventory();
        Inventory clickedInv = e.getClickedInventory();
        int rawSlot = e.getRawSlot();
        ClickType click = e.getClick();

        if (!inventory.equals(topInventory)) return;

        if (clickedInv != null && clickedInv.equals(topInventory)) {
            if (!sellZoneSlots.contains(rawSlot)) {
                if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                    Manager.refreshMenu(p, this, true);
                } else {
                    e.setCancelled(true);
                }
                return;
            }
        }

        if ((click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT)
                && (clickedInv == null || clickedInv.equals(p.getInventory()))) {
            e.setCancelled(true);

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) {
                Manager.refreshMenu(p, this, true);
                return;
            }

            int remaining = clicked.getAmount();

            for (int slot : sellZoneSlots) {
                ItemStack slotItem = inventory.getItem(slot);

                if (slotItem == null || slotItem.getType().isAir()) {
                    ItemStack toPut = clicked.clone();
                    int putAmount = Math.min(remaining, toPut.getMaxStackSize());
                    toPut.setAmount(putAmount);
                    inventory.setItem(slot, toPut);
                    remaining -= putAmount;
                    if (remaining <= 0) {
                        e.setCurrentItem(null);
                        break;
                    } else {
                        continue;
                    }
                }

                if (slotItem.isSimilar(clicked) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                    int space = slotItem.getMaxStackSize() - slotItem.getAmount();
                    int toAdd = Math.min(space, remaining);
                    slotItem.setAmount(slotItem.getAmount() + toAdd);
                    inventory.setItem(slot, slotItem);
                    remaining -= toAdd;
                    if (remaining <= 0) {
                        e.setCurrentItem(null);
                        break;
                    }
                }
            }

            if (remaining > 0) {
                ItemStack left = clicked.clone();
                left.setAmount(remaining);
                e.setCurrentItem(left);
            } else {
                e.setCurrentItem(null);
            }

            Manager.refreshMenu(p, this, true);
            return;
        }

        if (rawSlot < inventory.getSize() && !sellZoneSlots.contains(rawSlot)) {
            e.setCancelled(true);
            return;
        }

        Manager.refreshMenu(p, this, true);
    }

    private void registerButtons() {
        for (Button button : menu.buttons()) {
            registerItem(button.id() + button.slot(), builder -> {
                builder.slots(button.slot());

                ItemWrapper wrapper = new ItemWrapper(button.material(), button.amount());
                wrapper.displayName(TextUtil.setPapi(player, button.displayName()));
                wrapper.lore(TextUtil.setPapi(player, button.lore()));
                wrapper.customModelData(button.customModelData());
                wrapper.enchanted(button.enchanted());
                wrapper.placeholderEngine(mainPlaceholders);

                updateCustomItem(wrapper, button);

                builder.defaultItem(wrapper);

                if (button.sellZone()) {
                    sellZoneSlots.add(button.slot());
                    return;
                }

                builder.defaultClickHandler((event, controller) -> {

                    event.setCancelled(true);

                    ClickType clickType = event.getClick();


                    for (Command cmd : button.commands()) {
                        if (cmd.clickType() == clickType || cmd.anyClick()) {

                            boolean allRequirementsPassed = true;
                            if (!cmd.requirements().isEmpty()) {
                                for (ClickRequirement clickRequirement : cmd.requirements()) {
                                    if ((clickRequirement.anyClick() || clickRequirement.clickType() == clickType)) {
                                        if (!Requirements.check(
                                                player, clickRequirement, totalPrice, totalScores, button)) {
                                            Requirements.runDenyCommands(player, clickRequirement.deny_commands(), totalPrice, totalScores, button);
                                            allRequirementsPassed = false;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (allRequirementsPassed) {
                                List<String> list = new ArrayList<>(cmd.actions());
                                if (plugin.getItems().getItemValues().containsKey(button.material())) {
                                    double price = plugin.getItems().getItemValues().get(button.material()).price();
                                    list.replaceAll(s -> s.replace("%price%", df.format(price)));
                                    list.replaceAll(s -> s.replace("%price_with_coefficient%", df.format(price * plugin.getCoefficient().get(player, button.material()))));
                                    list.replaceAll(s -> s.replace("%auto_sell_toggle_state%", Manager.check(plugin.getStorage().getAutoBuyItems(player.getUniqueId()).contains(button.material().name()))));

                                }
                                ActionExecutor.execute(player, ActionRegistry.transform(list), button,
                                        Placeholder.of("%coefficient%",() -> String.valueOf(plugin.getCoefficient().get(player, button.material()))),
                                        Placeholder.of("%sell_pay%", () -> df.format(totalPrice)),
                                        Placeholder.of("%sell_score%", () -> df.format(totalScores)));
                                break;
                            }
                        }
                    }


                });
            });

        }
    }

    private void updateCustomItem(ItemWrapper wrapper, Button button) {
        if(wrapper.itemStack().getType().equals(Material.AIR)) return;

        ItemMeta itemMeta = wrapper.itemStack().getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemMeta.getPersistentDataContainer().set(NAMESPACED_KEY, PersistentDataType.STRING, "menu_item");
        Material materialType = null;
        if (!button.commands().isEmpty()) {
            for (Command command : button.commands()) {
                for (String cmd : command.actions()) {
                    if (cmd.startsWith("[AUTOBUY_STATUS_TOGGLE]")) {
                        itemMeta.getPersistentDataContainer().set(NAMESPACED_KEY, PersistentDataType.STRING, "menu_autobuy");
                    }
                    if (cmd.startsWith("[AUTOBUY_ITEM_TOGGLE]".toUpperCase()) || cmd.startsWith("[SELL_ITEM]".toUpperCase())) {

                        try {
                            materialType = Material.valueOf(Manager.getAutoBuyItemToggle(wrapper, cmd));
                        } catch (IllegalArgumentException e) {
                            materialType = button.material();
                        }

                        if (plugin.getItems().getItemValues().containsKey(materialType)) {

                            PlaceholderEngine itemPlaceholders = PlaceholderEngine.of();
                            itemPlaceholders.addAll(mainPlaceholders);

                            double price = plugin.getItems().getItemValues().get(materialType).price();

                            itemPlaceholders.register("%price%", (offlinePlayer) -> df.format(price));
                            Material finalMaterialType = materialType;
                            itemPlaceholders.register("%price_with_coefficient%", (offlinePlayer) -> df.format(price * plugin.getCoefficient().get(player, finalMaterialType)));
                            itemPlaceholders.register("%auto_sell_toggle_state%", (offlinePlayer) -> Manager.check(plugin.getStorage().getAutoBuyItems(player.getUniqueId()).contains(finalMaterialType.name())));
                            itemPlaceholders.register("%sell_pay%", (offlinePlayer) -> df.format(totalPrice));
                            itemPlaceholders.register("%sell_score%", (offlinePlayer) -> df.format(totalScores));
                            wrapper.placeholderEngine(itemPlaceholders);
                            itemMeta.getPersistentDataContainer().set(NAMESPACED_KEY, PersistentDataType.STRING, "menu_priceItem");
                        }
                        break;
                    }
                }
            }
        }

        if (itemMeta.getPersistentDataContainer().get(NAMESPACED_KEY, PersistentDataType.STRING).equalsIgnoreCase("menu_priceItem")) {
            wrapper.enchanted(plugin.getStorage().getAutoBuyItems(player.getUniqueId()).contains(materialType.name()));
        } else if (itemMeta.getPersistentDataContainer().get(NAMESPACED_KEY, PersistentDataType.STRING).equalsIgnoreCase("menu_autobuy")) {
            wrapper.enchanted(plugin.getStorage().getAutoBuyStatus(player.getUniqueId()));
        }

        wrapper.itemStack().setItemMeta(itemMeta);
    }

}
