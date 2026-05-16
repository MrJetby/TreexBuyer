package org.jetby.treexBuyer.hook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class LibbStableDownloader {

    private final String stableVersion;

    public LibbStableDownloader(String stableVersion) {
        this.stableVersion = stableVersion;
    }

    public void load() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Libb");

        if (plugin != null) {
            if (isLibbCompatible(plugin)) {
                return;
            }
            disableAndDelete(plugin);
        }

        String latestUrl = getFileUrl("libb", null);
        if (latestUrl != null) {
            File downloaded = downloadFile(latestUrl);
            if (downloaded != null && tryLoadPlugin(downloaded)) {
                Plugin loaded = Bukkit.getPluginManager().getPlugin("Libb");
                if (loaded != null && isLibbCompatible(loaded)) {
                    return;
                }
                disableAndDelete(loaded);
                if (downloaded.exists()) downloaded.delete();
            } else if (downloaded != null && downloaded.exists()) {
                downloaded.delete();
            }
        }

        String stableUrl = getFileUrl("libb", stableVersion);
        if (stableUrl == null) {
            throw new RuntimeException("Failed to find stable Libb version: " + stableVersion);
        }

        File stableFile = downloadFile(stableUrl);
        if (stableFile == null || !tryLoadPlugin(stableFile)) {
            throw new RuntimeException("Failed to load stable Libb version: " + stableVersion);
        }
    }

    private boolean isLibbCompatible(Plugin plugin) {
        try {
            Class.forName("org.jetby.libb.LibbApi$Settings", false, plugin.getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void disableAndDelete(@Nullable Plugin plugin) {
        if (plugin == null) return;
        try {
            Bukkit.getPluginManager().disablePlugin(plugin);
            File jarFile = getPluginFile(plugin);
            if (jarFile != null && jarFile.exists()) {
                jarFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private @Nullable File getPluginFile(Plugin plugin) {
        try {
            java.lang.reflect.Method method = plugin.getClass().getMethod("getFile");
            method.setAccessible(true);
            return (File) method.invoke(plugin);
        } catch (Exception e) {
            try {
                java.lang.reflect.Field field = plugin.getClass().getSuperclass().getDeclaredField("file");
                field.setAccessible(true);
                return (File) field.get(plugin);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private boolean tryLoadPlugin(File file) {
        try {
            Plugin pl = Bukkit.getPluginManager().loadPlugin(file);
            if (pl == null) return false;
            pl.onLoad();
            Bukkit.getPluginManager().enablePlugin(pl);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private @Nullable String getFileUrl(String projectSlug, @Nullable String version) {
        try {
            HttpURLConnection conn = getHttpURLConnection(projectSlug, version);

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

    private static @NotNull HttpURLConnection getHttpURLConnection(String projectSlug, @Nullable String version) throws IOException {
        String urlStr = "https://api.modrinth.com/v2/project/" + projectSlug + "/version";
        if (version != null) {
            urlStr += "?version_number=" + version;
        }

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Libb Plugin (Contact: mail@jetby.org)");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        return conn;
    }

    private @Nullable File downloadFile(String link) {
        try {
            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            File pluginDir = new File("plugins");

            return getFile(pluginDir, url, connection);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static @NotNull File getFile(File pluginDir, URL url, HttpURLConnection connection) throws IOException {
        if (!pluginDir.exists()) pluginDir.mkdirs();

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
        }
        return file;
    }
}