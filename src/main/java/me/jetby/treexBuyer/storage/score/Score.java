package me.jetby.treexBuyer.storage.score;

import com.google.gson.JsonElement;
import org.bukkit.Material;

public interface Score {
    void add(Material material, double amount);
    double get(Material material);
    void set(Material material, double score);
    void take(Material material, double score);
    double getTotal();
    JsonElement toJson();
    void fromJson(JsonElement element);
}