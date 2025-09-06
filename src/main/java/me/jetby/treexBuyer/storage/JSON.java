package me.jetby.treexBuyer.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.tools.FileLoader;
import me.jetby.treexBuyer.tools.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JSON implements Storage {
    private final Main plugin;

    private final File jsonFile = FileLoader.getFile("storage.json");
    private JSONObject json;
    private final JSONParser parser = new JSONParser();
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public JSON(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean load() {
        boolean status = false;
        try {
            if (!jsonFile.exists()) {
                jsonFile.getParentFile().mkdirs();
                PrintWriter pw = new PrintWriter(jsonFile, "UTF-8");
                pw.print("{}");
                pw.flush();
                pw.close();
            }

            json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(jsonFile), "UTF-8"));
            cache.clear();

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                long start = System.currentTimeMillis();
                try {
                    for (Object key : json.keySet()) {
                        String uuidStr = (String) key;
                        UUID uuid = UUID.fromString(uuidStr);
                        JSONObject playerData = (JSONObject) json.get(uuidStr);

                        Map<String, Integer> scores = new HashMap<>();
                        JSONObject scoresObj = (JSONObject) playerData.getOrDefault("scores", new JSONObject());
                        for (Object scoreKey : scoresObj.keySet()) {
                            String k = (String) scoreKey;
                            Number value = (Number) scoresObj.get(k);
                            scores.put(k, value.intValue());
                        }

                        boolean autoBuy = (Boolean) playerData.getOrDefault("autoBuy", false);
                        JSONArray itemsArray = (JSONArray) playerData.getOrDefault("autoBuyItems", new JSONArray());
                        List<String> autoBuyItems = new ArrayList<>();
                        for (Object item : itemsArray) {
                            autoBuyItems.add((String) item);
                        }

                        cache.put(uuid, new PlayerData(autoBuy, autoBuyItems, scores));
                    }
                    Logger.success("Data from storage.json was loaded in " + (System.currentTimeMillis() - start) + " ms");
                } catch (Exception ex) {
                    Logger.error("Error loading JSON:" + ex.getMessage());
                }
            });
            status = true;
        } catch (Exception ex) {
            Logger.error("Error initializing JSON file: " + ex.getMessage());
        }
        return status;
    }

    @Override
    public boolean save() {
        boolean status = false;
        try {
            JSONObject toSave = new JSONObject();

            for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerData data = entry.getValue();
                JSONObject playerData = new JSONObject();

                JSONObject scoresObj = new JSONObject();
                for (Map.Entry<String, Integer> scoreEntry : data.scores.entrySet()) {
                    scoresObj.put(scoreEntry.getKey(), scoreEntry.getValue());
                }
                playerData.put("scores", scoresObj);

                playerData.put("autoBuy", data.autoBuy);
                JSONArray itemsArray = new JSONArray();
                itemsArray.addAll(data.autoBuyItems);
                playerData.put("autoBuyItems", itemsArray);

                toSave.put(uuid.toString(), playerData);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String prettyJsonString = gson.toJson(toSave);

            try (FileWriter fw = new FileWriter(jsonFile)) {
                fw.write(prettyJsonString);
                fw.flush();
            }

            Logger.success("Data is stored in storage.json");
            status = true;
        } catch (Exception ex) {
            Logger.error("Error when saving JSON: " + ex.getMessage());
        }
        return status;
    }

    @Override
    public String type() {
        return "JSON";
    }

    @Override
    public boolean playerExists(UUID uuid) {
        return cache.containsKey(uuid);
    }

    @Override
    public void setScore(UUID uuid, String key, int score) {
            PlayerData data = cache.computeIfAbsent(uuid, k -> new PlayerData());
            data.scores.put(key.toLowerCase(), score);
            if (plugin.getCfg().isYamlOrJsonForceSave()) {
                if (!save()) {
                    Logger.error("Failed to save autoBuyItems for UUID: " + uuid);
                }
            }
    }

    @Override
    public int getScore(UUID uuid, String key) {
        PlayerData data = cache.getOrDefault(uuid, new PlayerData());
        return data.scores.getOrDefault(key.toLowerCase(), 0);
    }

    @Override
    public void setAutoBuyItems(UUID uuid, List<String> items) {

            PlayerData data = cache.computeIfAbsent(uuid, k -> new PlayerData());
            data.autoBuyItems = new ArrayList<>(items);
            if (plugin.getCfg().isYamlOrJsonForceSave()) {
                if (!save()) {
                    Logger.error("Failed to save autoBuyItems for UUID: " + uuid);
                }
            }
    }

    @Override
    public List<String> getAutoBuyItems(UUID uuid) {
        PlayerData data = cache.getOrDefault(uuid, new PlayerData());
        return new ArrayList<>(data.autoBuyItems);
    }

    @Override
    public void setAutoBuyStatus(UUID uuid, boolean status) {

            PlayerData data = cache.computeIfAbsent(uuid, k -> new PlayerData());
            data.autoBuy = status;
            if (plugin.getCfg().isYamlOrJsonForceSave()) {
                if (!save()) {
                    Logger.error("Failed to save autoBuyItems for UUID: " + uuid);
                }
            }
    }

    @Override
    public boolean getAutoBuyStatus(UUID uuid) {
        PlayerData data = cache.getOrDefault(uuid, new PlayerData());
        return data.autoBuy;
    }
    @Override
    public String getTopName(int number) {
        if (cache.isEmpty()) return null;

        List<Map.Entry<UUID, PlayerData>> sorted = cache.entrySet().stream()
                .sorted((a, b) -> {
                    int sumA = a.getValue().scores.values().stream().mapToInt(Integer::intValue).sum();
                    int sumB = b.getValue().scores.values().stream().mapToInt(Integer::intValue).sum();
                    return Integer.compare(sumB, sumA);
                })
                .toList();

        if (number <= 0 || number > sorted.size()) return null;
        return getName(sorted.get(number - 1).getKey());
    }

    @Override
    public int getTopScore(int number) {
        if (cache.isEmpty()) return 0;

        List<Map.Entry<UUID, PlayerData>> sorted = cache.entrySet().stream()
                .sorted((a, b) -> {
                    int sumA = a.getValue().scores.values().stream().mapToInt(Integer::intValue).sum();
                    int sumB = b.getValue().scores.values().stream().mapToInt(Integer::intValue).sum();
                    return Integer.compare(sumB, sumA);
                })
                .toList( );

        if (number <= 0 || number > sorted.size()) return 0;

        return sorted.get(number - 1).getValue()
                .scores
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    private String getName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player != null ? player.getName() : null;
    }


}