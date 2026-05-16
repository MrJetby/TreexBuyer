package org.jetby.treexBuyer.hook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;


// Modrinth downloader example
public class LibbDownloader {


    public void load() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Libb");
        if (plugin!=null) return;

        downloadAndLoad(getLatestFileUrl("libb"));

    }

    private void downloadAndLoad(String link) {
        try {
            File file = getFile(link);

            Plugin pl = Bukkit.getPluginManager().loadPlugin(file);
            if (pl != null) {
                pl.onLoad();
                Bukkit.getPluginManager().enablePlugin(pl);
            } else {
                throw new RuntimeException("Something went wrong while downloading the plugin");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getLatestFileUrl(String projectSlug) {
        try {
            URL url = new URL("https://api.modrinth.com/v2/project/" + projectSlug + "/version");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Libb Plugin (Contact: mail@jetby.org)");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) return null;

            JsonArray versions = JsonParser.parseReader(
                    new InputStreamReader(conn.getInputStream())
            ).getAsJsonArray();

            if (versions.isEmpty()) return null;

            JsonObject latest = versions.get(0).getAsJsonObject();
            JsonArray files = latest.getAsJsonArray("files");

            if (files.isEmpty()) return null;

            for (var file : files) {
                JsonObject f = file.getAsJsonObject();
                if (f.get("primary").getAsBoolean()) {
                    return f.get("url").getAsString();
                }
            }

            return files.get(0).getAsJsonObject().get("url").getAsString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static @NotNull File getFile(String link) {
        try {
            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            File pluginDir = new File("plugins");
            if (!pluginDir.exists()) pluginDir.mkdirs();

            return getFile(url, pluginDir, connection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static @NotNull File getFile(URL url, File pluginDir, HttpURLConnection connection) {
        String fileName = new File(url.getPath()).getName();
        if (!fileName.endsWith(".jar")) fileName += ".jar";

        File file = new File(pluginDir, fileName);

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }
}
