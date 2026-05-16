package org.jetby.treexBuyer.functions;

import org.jetby.libb.action.ActionContext;
import org.jetby.libb.action.ActionExecute;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.configurations.Items;
import org.jetby.treexBuyer.modules.UserData;
import org.jetby.treexBuyer.tools.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;

public class AutoBuy {

    final BuyerManager manager;
    private int task;
    private final ItemStack air = new ItemStack(Material.AIR);

    public AutoBuy(BuyerManager manager) {
        this.manager = manager;
    }

    public void start() {
        manager.getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(manager.getPlugin(), t -> {
            this.task = t.getTaskId();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (manager.getItems().getItemValues().isEmpty()) break;
                UserData user = UserData.findByUuid(player.getUniqueId());
                if (user == null || !user.isAutoBuy() || player.getInventory().getContents().length == 0) continue;
                checkItems(player);
            }
        }, 0L, manager.getCfg().getAutoBuyDelay());
    }

    public void stop() {
        Bukkit.getScheduler().cancelTask(task);
    }

    public void checkItems(Player player) {
        UserData user = UserData.getOrCreate(player.getUniqueId(), manager.getItems().createScore());

        if (player.getGameMode() == GameMode.CREATIVE && !player.hasPermission("treexbuyer.autobuy.creative.bypass"))
            return;
        if (manager.getCfg().getDisabledWorlds().contains(player.getWorld().getName()))
            return;

        double totalPrice = 0.0;
        double totalScores = 0.0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (!user.getAutoBuyItems().contains(item.getType())) continue;
            if (!isRegularItem(item)) continue;
            if (!manager.getItems().getItemValues().containsKey(item.getType())) continue;

            Items.ItemData data = manager.getItems().getItemValues().get(item.getType());
            double pricePerItem = manager.getCoefficient().getPriceWithCoefficient(player, item.getType());
            double price = pricePerItem * item.getAmount();
            double score = data.score() * item.getAmount();

            var eq = player.getEquipment();
            if (eq.getItemInOffHand().isSimilar(item)) eq.setItemInOffHand(air);
            if (item.isSimilar(eq.getHelmet())) eq.setHelmet(air);
            if (item.isSimilar(eq.getChestplate())) eq.setChestplate(air);
            if (item.isSimilar(eq.getLeggings())) eq.setLeggings(air);
            if (item.isSimilar(eq.getBoots())) eq.setBoots(air);

            user.addScore(item.getType(), price);
            player.getInventory().removeItem(item);
            totalPrice += price;
            if (score > 0) totalScores += score;
        }

        if (totalPrice <= 0) return;

        manager.getEconomy().depositPlayer(player, totalPrice);

        ActionExecute.run(ActionContext.of(player, manager.getPlugin())
                .replace("%sell_pay%", NumberUtils.format(totalPrice))
                .replace("%sell_pay_commas%", NumberUtils.formatWithCommas(totalPrice))
                .replace("%sell_score%", NumberUtils.format(totalScores))
                .replace("%sell_score_commas%", NumberUtils.formatWithCommas(totalScores)),
                manager.getCfg().getAutoBuyActions());
    }

    public static boolean isRegularItem(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!meta.getPersistentDataContainer().isEmpty()) return false;
        if (meta.hasDisplayName()) return false;
        if (meta.hasLore()) return false;
        if (meta.hasEnchants()) return false;
        if (meta.hasCustomModelData()) return false;
        if (!meta.getItemFlags().isEmpty()) return false;

        return !(meta instanceof LeatherArmorMeta lam)
                || lam.getColor().equals(Bukkit.getItemFactory().getDefaultLeatherColor());
    }
}