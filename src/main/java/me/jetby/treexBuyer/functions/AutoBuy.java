package me.jetby.treexBuyer.functions;

import me.jetby.libb.action.ActionContext;
import me.jetby.libb.action.ActionExecute;
import me.jetby.libb.gui.parser.ParseUtil;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.configurations.Items;
import me.jetby.treexBuyer.modules.UserData;
import me.jetby.treexBuyer.tools.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;

public class AutoBuy {

    final TreexBuyer plugin;
    private int task;
    private final ItemStack air = new ItemStack(Material.AIR);

    public AutoBuy(TreexBuyer plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, t -> {
            this.task = t.getTaskId();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getItems().getItemValues().isEmpty()) break;
                UserData user = UserData.findByUuid(player.getUniqueId());
                if (user == null || !user.isAutoBuy() || player.getInventory().getContents().length == 0) continue;
                checkItems(player);
            }
        }, 0L, plugin.getCfg().getAutoBuyDelay());
    }

    public void stop() {
        Bukkit.getScheduler().cancelTask(task);
    }

    public void checkItems(Player player) {
        UserData user = UserData.getOrCreate(player.getUniqueId(), plugin.getItems().createScore());

        if (player.getGameMode() == GameMode.CREATIVE && !player.hasPermission("treexbuyer.autobuy.creative.bypass"))
            return;
        if (plugin.getCfg().getDisabledWorlds().contains(player.getWorld().getName()))
            return;

        double totalPrice = 0.0;
        double totalScores = 0.0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (!user.getAutoBuyItems().contains(item.getType())) continue;
            if (!isRegularItem(item)) continue;
            if (!plugin.getItems().getItemValues().containsKey(item.getType())) continue;

            Items.ItemData data = plugin.getItems().getItemValues().get(item.getType());
            double pricePerItem = plugin.getCoefficient().getPriceWithCoefficient(player, item.getType());
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

        plugin.getEconomy().depositPlayer(player, totalPrice);

        ActionExecute.run(ActionContext.of(player, plugin)
                .replace("%sell_pay%", NumberUtils.format(totalPrice))
                .replace("%sell_pay_commas%", NumberUtils.formatWithCommas(totalPrice))
                .replace("%sell_score%", NumberUtils.format(totalScores))
                .replace("%sell_score_commas%", NumberUtils.formatWithCommas(totalScores)),
                plugin.getCfg().getAutoBuyActions());
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