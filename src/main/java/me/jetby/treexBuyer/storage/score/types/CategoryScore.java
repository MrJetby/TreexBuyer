package me.jetby.treexBuyer.storage.score.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.jetby.treexBuyer.storage.score.Score;
import org.bukkit.Material;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CategoryScore implements Score {

    private final Map<String, Double> scores = new ConcurrentHashMap<>();
    private final Map<Material, String> categoryMap;

    public CategoryScore(Map<Material, String> categoryMap) {
        this.categoryMap = categoryMap;
    }

    private String getCategory(Material material) {
        return categoryMap.getOrDefault(material, "unknown");
    }

    @Override
    public void add(Material material, double score) {
        if (score <= 0) return;
        scores.merge(getCategory(material), score, Double::sum);
    }

    public void add(String category, double score) {
        if (score <= 0) return;
        scores.merge(category, score, Double::sum);
    }

    @Override
    public double get(Material material) {
        return scores.getOrDefault(getCategory(material), 0.0);
    }

    public double get(String category) {
        return scores.getOrDefault(category, 0.0);
    }

    @Override
    public void set(Material material, double score) {
        scores.put(getCategory(material), score);
    }

    public void set(String category, double score) {
        scores.put(category, score);
    }

    @Override
    public double getTotal() {
        return scores.values().stream().mapToDouble(d -> d).sum();
    }

    @Override
    public void take(Material material, double score) {
        if (score <= 0) return;
        scores.merge(getCategory(material), -score, (cur, d) -> Math.max(0.0, cur + d));
    }

    public void take(String category, double score) {
        if (score <= 0) return;
        scores.merge(category, -score, (cur, d) -> Math.max(0.0, cur + d));
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        scores.forEach(obj::addProperty);
        return obj;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) return;
        element.getAsJsonObject().entrySet().forEach(e ->
                scores.put(e.getKey(), e.getValue().getAsDouble()));
    }
}