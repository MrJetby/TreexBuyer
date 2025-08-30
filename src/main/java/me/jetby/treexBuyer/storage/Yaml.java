package me.jetby.treexBuyer.storage;

import lombok.RequiredArgsConstructor;
import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.tools.FileLoader;
import me.jetby.treexBuyer.tools.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

@RequiredArgsConstructor
public class Yaml implements Storage {
    final Main plugin;
    final Map<UUID, PlayerData> cache = new HashMap<>( );
    final FileConfiguration configuration = FileLoader.getFileConfiguration("storage.yml");

    @Override
    public boolean load() {
        cache.clear();
        boolean status = false;

        long start = System.currentTimeMillis();
        try {
            for (String key : configuration.getKeys(false)) {
                UUID uuid = UUID.fromString(key);

                ConfigurationSection scoresSection = configuration.getConfigurationSection(key + ".scores");

                boolean autoBuy = configuration.getBoolean(key + ".autoBuy", false);
                List<String> items = configuration.getStringList(key + ".autoBuyItems");
                Map<String, Integer> scores = new HashMap<>();

                if (scoresSection != null) {
                    for (String scoreKey : scoresSection.getKeys(false)) {
                        scores.put(scoreKey, scoresSection.getInt(scoreKey));
                    }
                }
                cache.put(uuid, new PlayerData(autoBuy, items, scores));
            }
            Logger.success("PlayerData from storage.yml was loaded in " + (System.currentTimeMillis() - start) + " ms");
            status = true;

        } catch (Exception e) {
            Logger.error("Error with loading from the storage: " + e);
        }

        return status;
    }

    @Override
    public boolean save() {
        boolean status = false;

        long start = System.currentTimeMillis();
        try {
            for (String key : configuration.getKeys(false)) {
                configuration.set(key, null);
            }

            for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerData data = entry.getValue();

                ConfigurationSection scoresSection = configuration.createSection(uuid + ".scores");
                for (Map.Entry<String, Integer> scoreEntry : data.scores.entrySet()) {
                    scoresSection.set(scoreEntry.getKey(), scoreEntry.getValue());
                }

                configuration.set(uuid + ".autoBuy", data.autoBuy);
                configuration.set(uuid + ".autoBuyItems", data.autoBuyItems);
            }

            configuration.save(FileLoader.getFile("storage.yml"));
            Logger.success("PlayerData from storage.yml was loaded in " + (System.currentTimeMillis() - start) + " ms");
            status = true;

        } catch (Exception e) {
            Logger.error("Error with saving to the storage: " + e);
        }

        return status;
    }

    @Override
    public String type() {
        return "YAML";
    }

    @Override
    public boolean playerExists(UUID uuid) {
        PlayerData data = cache.get(uuid);
        return data != null;
    }

    @Override
    public void setScore(UUID uuid, String key, int score) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = cache.computeIfAbsent(uuid, k -> new PlayerData());
            data.scores.put(key.toLowerCase(), score);
            if (plugin.getCfg().isYamlOrJsonForceSave()) {
                if (!save()) {
                    Logger.error("Failed to save autoBuyItems for UUID: " + uuid);
                }
            }
        });

    }

    @Override
    public int getScore(UUID uuid, String key) {
        PlayerData data = cache.getOrDefault(uuid, new PlayerData());
        return data.scores.getOrDefault(key.toLowerCase(), 0);
    }

    @Override
    public void setAutoBuyItems(UUID uuid, List<String> items) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = cache.computeIfAbsent(uuid, k -> new PlayerData());
            data.autoBuyItems = new ArrayList<>(items);
            if (plugin.getCfg().isYamlOrJsonForceSave()) {
                if (!save()) {
                    Logger.error("Failed to save autoBuyItems for UUID: " + uuid);
                }
            }

        });

    }

    @Override
    public List<String> getAutoBuyItems(UUID uuid) {
        PlayerData data = cache.getOrDefault(uuid, new PlayerData());
        return new ArrayList<>(data.autoBuyItems);
    }

    @Override
    public void setAutoBuyStatus(UUID uuid, boolean status) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = cache.computeIfAbsent(uuid, k -> new PlayerData());
            data.autoBuy = status;
            if (plugin.getCfg().isYamlOrJsonForceSave()) {
                if (!save()) {
                    Logger.error("Failed to save autoBuyItems for UUID: " + uuid);
                }
            }
        });

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

