package me.jetby.treexBuyer.tools;

import me.jetby.treexBuyer.TreexBuyer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Version implements Listener {

    private final TreexBuyer plugin;
    private static final String VERSION_URL = "https://raw.githubusercontent.com/MrJetby/TreexBuyer/refs/heads/master/VERSION";
    private static final String UPDATE_URL = "https://raw.githubusercontent.com/MrJetby/TreexBuyer/refs/heads/master/UPDATE_LINK";

    public Version(TreexBuyer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("treexcastle.version") && !isLastVersion()) {
            getAlert().forEach(player::sendMessage);
        }
    }

    public List<String> getAlert() {
        String current = getVersion();
        if (isLastVersion()) {
            return new ArrayList<>(List.of(
                    "",
                    "§7-------- §6TreexBuyer §7--------",
                    "§6● §7Plugin version: §a" + current,
                    "",
                    "§6● §aYou are using the latest version ✔",
                    "",
                    "§7------------------------",
                    ""
            ));
        } else {
            return new ArrayList<>(List.of(
                    "",
                    "§7-------- §6TreexBuyer §7--------",
                    "§6● §fAttention, update available, please update the plugin.",
                    "§6● §7Your version: §c" + current + " §7а latest §a" + getLastVersion(),
                    "",
                    "§6● §fDownload here: §b" + getUpdateLink(),
                    "§7------------------------",
                    ""
            ));
        }
    }

    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public String getLastVersion() {
        String result = getRaw(VERSION_URL);
        return result != null ? result : getVersion();
    }

    public String getUpdateLink() {
        String result = getRaw(UPDATE_URL);
        return result != null ? result : "";
    }

    public boolean isLastVersion() {
        String remote = getRaw(VERSION_URL);
        if (remote == null) return true;
        return getVersion().equalsIgnoreCase(remote);
    }

    private String getRaw(String link) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(link).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) sb.append(line);
                return sb.toString().trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}