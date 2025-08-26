package me.jetby.treexBuyer.functions;

import lombok.RequiredArgsConstructor;
import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.configurations.Items;
import me.jetby.treexBuyer.menus.commands.ActionExecutor;
import me.jetby.treexBuyer.menus.commands.ActionRegistry;
import me.jetby.treexBuyer.tools.Logger;
import me.jetby.treexBuyer.tools.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static me.jetby.treexBuyer.Main.df;

@RequiredArgsConstructor
public class AutoBuy {
    private final Main plugin;
    private int task;

    public void start() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task -> {

            this.task = task.getTaskId();

            for (Player player : Bukkit.getOnlinePlayers()) {

                if (plugin.getItems().getItemValues().isEmpty()) break;

                if (player.getInventory().getContents().length == 0) continue;

                if (!plugin.getStorage().getAutoBuyStatus(player.getUniqueId())) continue;

                checkItems(player);

            }


        }, 0L, plugin.getCfg().getAutoBuyDelay());
    }

    public void stop() {
        Bukkit.getScheduler().cancelTask(task);
    }

    ItemStack air = new ItemStack(Material.AIR);

    public void checkItems(Player player) {

        if (player.getGameMode().equals(GameMode.CREATIVE) && !player.hasPermission("treexbuyer.creative.bypass")) {
            return;
        }

        if (plugin.getCfg().getDisabledWorlds().contains(player.getWorld().getName())) return;

        double totalPrice = 0.0;
        int totalScores = 0;
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null || itemStack.getType().isAir()) continue;
            if (!plugin.getStorage().getAutoBuyItems(player.getUniqueId()).contains(itemStack.getType().name()))
                continue;
            if (!isRegularItem(itemStack)) continue;

            if (!plugin.getItems().getItemValues().containsKey(itemStack.getType())) continue;
            Items.ItemData itemData = plugin.getItems().getItemValues().get(itemStack.getType());

            double price = itemData.price() * plugin.getCoefficient().get(player, itemStack.getType());
            int score = itemData.score();


            if (player.getEquipment().getItemInOffHand().equals(itemStack)) {
                player.getEquipment().setItemInOffHand(air);
            }
            if (player.getEquipment().getHelmet() != null && player.getEquipment().getHelmet().equals(itemStack)) {
                player.getEquipment().setHelmet(air);
            }
            if (player.getEquipment().getChestplate() != null && player.getEquipment().getChestplate().equals(itemStack)) {
                player.getEquipment().setChestplate(air);
            }
            if (player.getEquipment().getLeggings() != null && player.getEquipment().getLeggings().equals(itemStack)) {
                player.getEquipment().setLeggings(air);
            }
            if (player.getEquipment().getBoots() != null && player.getEquipment().getBoots().equals(itemStack)) {
                player.getEquipment().setBoots(air);
            }

            player.getInventory().removeItem(itemStack);


            totalPrice += price * itemStack.getAmount();
            totalScores += score * itemStack.getAmount();

            if (totalScores > 0) {
                String key = plugin.getCoefficient().determineKey(itemStack.getType());
                Logger.info(key );
                plugin.getStorage().setScore(player.getUniqueId(), key, plugin.getStorage().getScore(player.getUniqueId(), key) + totalScores);
            }
        }

        if (totalPrice <= 0L) return;
        plugin.getEconomy().depositPlayer(player, totalPrice);


        List<String> list = getStrings(totalPrice, totalScores);
        ActionExecutor.execute(player, ActionRegistry.transform(list), null);


    }

    private @NotNull List<String> getStrings(double totalPrice, int totalScores) {
        List<String> list = new ArrayList<>(plugin.getCfg().getAutoBuyActions());

        list.replaceAll(s -> s.replace("%sell_pay%", df.format(totalPrice)));
        list.replaceAll(s -> s.replace("%sell_pay_commas%", NumberUtils.formatWithCommas(totalPrice)));
        list.replaceAll(s -> s.replace("%sell_score%", df.format(totalScores)));
        list.replaceAll(s -> s.replace("%sell_score_commas%", NumberUtils.formatWithCommas(totalScores)));
        return list;
    }

    public static boolean isRegularItem(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null &&
                !(meta.hasDisplayName() && !meta.getDisplayName().isEmpty()) &&
                !meta.hasLore() &&
                !meta.hasAttributeModifiers() &&
                !meta.hasEnchants() &&
                !meta.hasCustomModelData();
    }
}
