package me.jetby.treexBuyer.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.modules.UserData;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class JsonStorage extends CachedStorage {

    private final Path dataFolder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JsonStorage(TreexBuyer plugin) {
        super(plugin);
        this.dataFolder = plugin.getDataFolder().toPath().resolve("playerdata");
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create playerdata folder: " + e.getMessage());
        }
    }

    @Override
    public void onPlayerJoin(UUID uuid) {
        if (!UserData.USERDATA_LIST.containsKey(uuid)) {
            UserData loaded = loadUser(uuid);
            plugin.getLogger().info("[DEBUG] Loaded " + uuid + " score: " + loaded.getScore().getTotal());
            UserData.USERDATA_LIST.put(uuid, loaded);
        }
    }

    @Override
    @NotNull
    public UserData loadUser(UUID uuid) {
        Path file = dataFolder.resolve(uuid + ".json");

        if (!Files.exists(file)) {
            return new UserData(uuid, plugin.getItems().createScore());
        }

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            JsonObject obj = gson.fromJson(reader, JsonObject.class);

            UserData data = new UserData(uuid, plugin.getItems().createScore());
            data.setAutoBuy(obj.get("autoBuy").getAsBoolean());

            obj.getAsJsonArray("autoBuyItems").forEach(e -> {
                try {
                    data.addAutoBuyMaterial(Material.valueOf(e.getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            });

            plugin.getLogger().info("[DEBUG] scores json: " + obj.get("scores"));
            data.getScore().fromJson(obj.get("scores"));

            return data;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load user " + uuid + ": " + e.getMessage());
            return new UserData(uuid, plugin.getItems().createScore());
        }
    }

    @Override
    public void saveUser(UUID uuid) {
        UserData data = UserData.USERDATA_LIST.get(uuid);
        if (data == null) return;

        JsonObject obj = new JsonObject();
        obj.addProperty("autoBuy", data.isAutoBuy());

        JsonArray items = new JsonArray();
        data.getAutoBuyItems().forEach(m -> items.add(m.name()));
        obj.add("autoBuyItems", items);
        obj.add("scores", data.getScore().toJson());

        try {
            Files.createDirectories(dataFolder);
            Files.writeString(dataFolder.resolve(uuid + ".json"), gson.toJson(obj));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save user " + uuid + ": " + e.getMessage());
        }
    }

    @Override
    public void deleteUser(UUID uuid) {
        UserData.USERDATA_LIST.remove(uuid);
        try {
            Files.deleteIfExists(dataFolder.resolve(uuid + ".json"));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to delete user " + uuid + ": " + e.getMessage());
        }
    }
}