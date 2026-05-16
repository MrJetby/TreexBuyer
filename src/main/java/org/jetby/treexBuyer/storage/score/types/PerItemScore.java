package org.jetby.treexBuyer.storage.score.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetby.treexBuyer.storage.score.Score;
import org.bukkit.Material;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerItemScore implements Score {

    private final Map<Material, Double> scores = new ConcurrentHashMap<>();

    @Override
    public void add(Material material, double score) {
        if (score <= 0) return;
        scores.merge(material, score, Double::sum);
    }

    @Override
    public double get(Material material) {
        return scores.getOrDefault(material, 0.0);
    }

    @Override
    public void set(Material material, double score) {
        scores.put(material, score);
    }

    @Override
    public double getTotal() {
        return scores.values().stream().mapToDouble(d -> d).sum();
    }

    @Override
    public void take(Material material, double score) {
        if (score <= 0) return;
        scores.merge(material, -score, (cur, d) -> Math.max(0.0, cur + d));
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        scores.forEach((m, v) -> obj.addProperty(m.name(), v));
        return obj;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) return;
        element.getAsJsonObject().entrySet().forEach(e -> {
            try {
                scores.put(Material.valueOf(e.getKey()), e.getValue().getAsDouble());
            } catch (IllegalArgumentException ignored) {
            }
        });
    }

    public Map<Material, Double> getRawScores() {
        return scores;
    }
}