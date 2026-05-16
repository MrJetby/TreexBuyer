package org.jetby.treexBuyer.storage;

import org.jetby.treexBuyer.BuyerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final BuyerManager plugin;

    public PlayerListener(BuyerManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        plugin.getStorage().onPlayerJoin(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent e) {
        plugin.getStorage().onPlayerQuit(e.getPlayer().getUniqueId());
    }
}