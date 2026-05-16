package org.jetby.treexBuyer.menus;

import lombok.Getter;
import org.jetby.libb.gui.parser.Item;
import org.jetby.libb.gui.parser.ParseUtil;
import org.jetby.libb.gui.parser.ParsedGui;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.modules.UserData;
import org.jetby.treexBuyer.tools.NumberUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class BuyerGui extends ParsedGui {
    @Getter
    private final List<Integer> sellSlots;
    private final BuyerManager manager;
    private final UserData user;

    public BuyerGui(@NotNull Player viewer, UserData user, @NotNull FileConfiguration config, BuyerManager manager) {
        super(viewer, config, manager.getPlugin());
        this.user = user;
        this.manager = manager;
        this.sellSlots = ParseUtil.parseSlots(config.getStringList("sell-slots"));

        lockEmptySlots(false);

        onClick(event -> {

            int slot = event.getRawSlot();
            int invSize = getInventory().getSize();

            boolean isGuiSlot = sellSlots.contains(slot);
            boolean isShiftFromPlayer = event.isShiftClick() && slot >= invSize;

            if (!isGuiSlot && !isShiftFromPlayer) return;

            event.setCancelled(false);
            manager.getPlugin().getServer().getScheduler().runTask(manager.getPlugin(), this::refresh);
        });

        onDrag(event -> {
            Set<Integer> slots = event.getRawSlots();
            boolean affectsSellSlots = slots.stream().anyMatch(sellSlots::contains);
            if (!affectsSellSlots) return;

            event.setCancelled(false);
            manager.getPlugin().getServer().getScheduler().runTask(manager.getPlugin(), this::refresh);
        });


        Consumer<InventoryCloseEvent> onClose = onClose();
        onClose(event -> {
            if (onClose != null)
                onClose.accept(event);
            sellSlots.forEach(slot -> {
                ItemStack item = event.getInventory().getItem(slot);
                if (item == null) return;
                player.getInventory().addItem(item);
            });
        });
    }

    private void recalcSellPay() {
        Inventory inv = getInventory();
        double total = 0.0;
        for (int slot : sellSlots) {
            ItemStack item = inv.getItem(slot);
            if (item == null) continue;

            total += manager.getCoefficient().getPriceWithCoefficient(player, item.getType()) * item.getAmount();
        }
        setReplace("%sell_pay%", NumberUtils.format(total));
        setReplace("%sell_pay_commas%", NumberUtils.formatWithCommas(total));
    }

    private void recalcSellScore() {
        Inventory inv = getInventory();
        double total = 0.0;
        for (int slot : sellSlots) {
            ItemStack item = inv.getItem(slot);
            if (item == null) continue;

            total += manager.getItems().getScoreAmount(item.getType()) * item.getAmount();
        }
        setReplace("%sell_score%", NumberUtils.format(total));
        setReplace("%sell_score_commas%", NumberUtils.formatWithCommas(total));
    }

    @Override
    public void refresh() {
        recalcSellPay();
        recalcSellScore();
        setReplace("%score%", NumberUtils.format(user.getScore().getTotal()));
        setReplace("%score_commas%", NumberUtils.formatWithCommas(user.getScore().getTotal()));
        setReplace("%coefficient%", NumberUtils.format(manager.getCoefficient().getTotalCoefficient(player, user.getScore())));
        super.refresh();
    }

    @Override
    public void buildItems(List<Item> items) {
        if (manager == null) {
            super.buildItems(items);
            return;
        }

        if (items == null) return;

        List<Item> expanded = new ArrayList<>();
        for (Item item : items) {
            String category = item.section() != null
                    ? item.section().getString("category")
                    : null;

            if (category == null) {
                expanded.add(item);
                continue;
            }
            expanded.addAll(expandCategoryItem(item, category));
        }

        super.buildItems(expanded);
    }

    @Override
    public void clearInventory() {
        getWrappers().clear();
        for (int i = 0; i < getInventory().getSize(); i++) {
            if (!sellSlots.contains(i)) {
                getInventory().setItem(i, null);
            }
        }
    }


    private List<Item> expandCategoryItem(Item template, String category) {
        List<Material> materials = manager.getItems().getMaterials(category);
        List<Integer> slots = template.slots();
        List<Item> result = new ArrayList<>();

        for (int i = 0; i < Math.min(materials.size(), slots.size()); i++) {
            Material mat = materials.get(i);
            int slot = slots.get(i);

            double price = manager.getItems().getOriginalPrice(mat);
            double priceWithCoeff = manager.getCoefficient().getPriceWithCoefficient(player, mat);

            Item copy = cloneWithMaterial(template, mat, slot, price, priceWithCoeff);
            result.add(copy);
        }

        return result;
    }


    private Item cloneWithMaterial(Item template, Material mat, int slot,
                                   double price, double priceWithCoeff) {
        Item copy = new Item(new ItemStack(mat));
        copy.slots(List.of(slot));
        copy.section(template.section());
        copy.onClick(template.onClick());
        copy.priority(template.priority());
        copy.viewRequirements(template.viewRequirements());
        copy.flags(template.flags());
        copy.enchanted(user.getAutoBuyItems().contains(mat));

        if (template.displayName() != null) {
            copy.displayName(substitutePrice(template.displayName(), price, priceWithCoeff, mat));
        }

        if (template.lore() != null) {
            copy.lore(template.lore().stream()
                    .map(line -> substitutePrice(line, price, priceWithCoeff, mat))
                    .collect(java.util.stream.Collectors.toList()));
        }

        return copy;
    }

    private String substitutePrice(String line, double price, double priceWithCoeff, Material mat) {
        return line
                .replace("%price%", NumberUtils.format(price))
                .replace("%price_commas%", NumberUtils.formatWithCommas(price))
                .replace("%price_with_coefficient%", NumberUtils.format(priceWithCoeff))
                .replace("%price_with_coefficient_commas%", NumberUtils.formatWithCommas(priceWithCoeff))
                .replace("%auto_sell_toggle_state%", user.getAutoBuyItems().contains(mat) ? manager.getCfg().getEnable() : manager.getCfg().getDisable());
    }

    public List<Material> getVisibleMaterials() {
        return getInventory().getContents() == null
                ? List.of()
                : Arrays.stream(getInventory().getContents())
                .filter(item -> item != null && !item.getType().isAir())
                .filter(item -> !getSellSlots().contains(/* slot */ 0))
                .map(ItemStack::getType)
                .filter(mat -> BuyerManager.MANAGER.getItems().getItemValues().containsKey(mat))
                .distinct()
                .toList();
    }

}