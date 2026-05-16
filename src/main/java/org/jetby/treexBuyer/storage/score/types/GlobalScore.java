package org.jetby.treexBuyer.storage.score.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jetby.treexBuyer.storage.score.Score;
import org.bukkit.Material;

public class GlobalScore implements Score {

    private double score = 0.0;

    @Override
    public void add(Material material, double score) {
        if (score <= 0) return;
        this.score += score;
    }

    public void add(double score) {
        if (score <= 0) return;
        this.score += score;
    }

    @Override
    public double get(Material material) {
        return score;
    }

    @Override
    public void set(Material material, double score) {
        this.score = score;
    }

    public void set(double score) {
        this.score = score;
    }

    @Override
    public double getTotal() {
        return score;
    }

    @Override
    public void take(Material material, double score) {
        if (score <= 0) return;
        this.score = Math.max(0.0, this.score - score);
    }

    public void take(double score) {
        if (score <= 0) return;
        this.score = Math.max(0.0, this.score - score);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(score);
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            score = element.getAsDouble();
        }
    }
}