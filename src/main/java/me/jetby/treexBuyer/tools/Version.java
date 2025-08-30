package me.jetby.treexBuyer.tools;

import lombok.RequiredArgsConstructor;
import me.jetby.treexBuyer.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class Version implements Listener {
    private final Main plugin;
    private final String VERSION = "https://raw.githubusercontent.com/MrJetby/TreexBuyer3/refs/heads/master/VERSION";
    private final String UPDATE = "https://raw.githubusercontent.com/MrJetby/TreexBuyer3/refs/heads/master/UPDATE_LINK";


    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("treexcastle.version")) {
            if (!isLastVersion()) {
                   for (String string : getAlert()) {
                       player.sendMessage(string);
                   }
            }
        }
    }



    public List<String> getAlert() {
        List<String> oldVersion = new ArrayList<>(List.of(
                "",
                "§7-------- §6TreexBuyer §7--------",
                "§6● §fAttention, update available, please update the plugin.",
                "§6● §7Your version: §c" + getVersion() + " §7а latest §a" + getLastVersion(),
                "",
                "§6● §fDownload here: §b" + getUPDATE(),
                "§7------------------------",
                ""
        ));
        List<String> lastVersion = new ArrayList<>(List.of(
                "",
                "§7-------- §6TreexBuyer §7--------",
                "§6● §7Plugin version: §a" + getVersion(),
                "",
                "§6● §aYou are using the latest version ✔",
                "",
                "§7------------------------",
                ""
        ));

        if (!isLastVersion()) {
            return oldVersion;
        }
        return lastVersion;
    }

    private String getRaw(String link) {
        try {
            URL url = new URL(link);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder builder = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                builder.append(inputLine);
            }
            in.close();
            return builder.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getLastVersion() {
        String result = getRaw(VERSION);
        assert result != null;
        return result;
    }
    public String getUPDATE() {
        String result = getRaw(UPDATE);
        assert result != null;
        return result;
    }

    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public boolean isLastVersion() {
        String result = getRaw(VERSION);
        if (result == null) {
            return true;
        }

        return plugin.getDescription().getVersion().equalsIgnoreCase(getLastVersion());
    }

}
