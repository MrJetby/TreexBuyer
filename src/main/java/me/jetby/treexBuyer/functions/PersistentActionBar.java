package me.jetby.treexBuyer.functions;

import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.modules.UserData;
import me.jetby.treexBuyer.tools.NumberUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class PersistentActionBar implements Listener {
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    private final TreexBuyer plugin;

    public PersistentActionBar(TreexBuyer plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UserData user = UserData.findByUuid(player.getUniqueId());
        if (user == null) return;
        set(player, () -> replacePlaceholders(plugin.getCfg().getPersistentActionbarText(), Map.of(
                "%sell_pay%", NumberUtils.format(recalcSellPay(player)),
                "%sell_pay_commas%", NumberUtils.formatWithCommas(recalcSellPay(player)),
                "%sell_score%", NumberUtils.format(recalcSellScore(player)),
                "%sell_score_commas", NumberUtils.formatWithCommas(recalcSellScore(player)))
        ));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    public void start() {
        clearAll();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UserData user = UserData.findByUuid(player.getUniqueId());
            if (user == null) continue;

            set(player, () -> replacePlaceholders(plugin.getCfg().getPersistentActionbarText(), Map.of(
                    "%sell_pay%", NumberUtils.format(recalcSellPay(player)),
                    "%sell_pay_commas%", NumberUtils.formatWithCommas(recalcSellPay(player)),
                    "%sell_score%", NumberUtils.format(recalcSellScore(player)),
                    "%sell_score_commas", NumberUtils.formatWithCommas(recalcSellScore(player)))
            ));
        }

    }

    private Component replacePlaceholders(Component result, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replaceText(builder ->
                    builder.matchLiteral(entry.getKey())
                            .replacement(entry.getValue())
            );
        }
        return result;
    }

    private double recalcSellPay(Player player) {
        Inventory inv = player.getInventory();
        double total = 0.0;
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            total += plugin.getCoefficient().getPriceWithCoefficient(player, item.getType()) * item.getAmount();
        }
        return total;
    }

    private double recalcSellScore(Player player) {
        Inventory inv = player.getInventory();
        double total = 0.0;
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            total += plugin.getItems().getScoreAmount(item.getType()) * item.getAmount();
        }
        return total;
    }

    public void stop() {
        clearAll();
    }

    public void set(Player player, Supplier<Component> messageSupplier) {
        clear(player);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    clear(player);
                    return;
                }
                player.sendActionBar(messageSupplier.get());
            }
        }.runTaskTimer(plugin, 0L, 20L);

        tasks.put(player.getUniqueId(), task);
    }

    public void clear(Player player) {
        BukkitTask task = tasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    public void clearAll() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
    }
}
