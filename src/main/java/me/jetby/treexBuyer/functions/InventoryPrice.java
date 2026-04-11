package me.jetby.treexBuyer.functions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import me.jetby.libb.gui.AdvancedGui;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.tools.NumberUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InventoryPrice implements Listener {


    private final TreexBuyer plugin;
    private final ProtocolManager protocolManager;
    private PacketListener packetListener;

    public InventoryPrice(TreexBuyer plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!plugin.getCfg().isInventoryPrice()) return;
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 5L);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!plugin.getCfg().isInventoryPrice()) return;
        Player player = (Player) e.getWhoClicked();

        if (player.getGameMode()== GameMode.CREATIVE) return;

        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!plugin.getCfg().isInventoryPrice()) return;
        Player player = (Player) e.getWhoClicked();

        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        if (!plugin.getCfg().isInventoryPrice()) return;
        Player player = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
    }

    public void load() {
        if (plugin.getCfg().isInventoryPrice()) {
            packetListener = new PacketAdapter(plugin, ListenerPriority.NORMAL,
                    PacketType.Play.Server.WINDOW_ITEMS,
                    PacketType.Play.Server.SET_SLOT) {

                @Override
                public void onPacketSending(PacketEvent event) {
                    PacketContainer packet = event.getPacket();
                    Player player = event.getPlayer();

                    try {


                        if (packet.getType() == PacketType.Play.Server.WINDOW_ITEMS) {
                            List<ItemStack> items = packet.getItemListModifier().read(0);
                            List<ItemStack> modified = new ArrayList<>();

                            for (ItemStack item : items) {
                                modified.add(addPriceToDisplay(player, item));
                            }

                            packet.getItemListModifier().write(0, modified);

                        } else if (packet.getType() == PacketType.Play.Server.SET_SLOT) {
                            ItemStack item = packet.getItemModifier().read(0);
                            if (isCustomInventory(player)) {
                                if (!AutoBuy.isRegularItem(item)) return;
                            }
                            packet.getItemModifier().write(0, addPriceToDisplay(player, item));

                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            protocolManager.addPacketListener(packetListener);
        } else {
            if (packetListener!=null)
                protocolManager.removePacketListener(packetListener);
        }

    }
    private boolean isCustomInventory(Player player) {
        InventoryView view = player.getOpenInventory();
        Inventory top = view.getTopInventory();
        InventoryHolder holder = top.getHolder();

        if (holder instanceof AdvancedGui) return true;

        if (holder instanceof Container) return false;
        if (holder instanceof Player) return false;

        return !(holder instanceof Animals);
    }
    private ItemStack addPriceToDisplay(Player player, ItemStack original) {
        if (original == null || original.getType() == Material.AIR) {
            return original;
        }
        if (isCustomInventory(player)) {
            if (!AutoBuy.isRegularItem(original)) return original;
        }

        double originalPrice = plugin.getItems().getOriginalPrice(original.getType());

        if (originalPrice <= 0) return original;


        ItemStack display = original.clone();
        ItemMeta meta = display.getItemMeta();

        if (meta == null) {
            return original;
        }

        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        List<Component> newLore = new ArrayList<>();

        for (Component line : lore) {
            if ("price_line".equals(line.style().insertion())) {
                continue;
            }

            newLore.add(line);
        }
        double price = plugin.getCoefficient().getPriceWithCoefficient(player, original.getType()) * original.getAmount();
        Component priceLine = plugin.getCfg().getInventoryPriceText()
                .replaceText(builder ->
                        builder.matchLiteral("%price%")
                                .replacement(Component.text(NumberUtils.format(price)))
                )
                .style(style -> style.insertion("price_line")); // mark

        newLore.add(priceLine);

        meta.lore(newLore);
        display.setItemMeta(meta);

        return display;
    }
}