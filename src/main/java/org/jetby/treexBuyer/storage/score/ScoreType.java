package org.jetby.treexBuyer.storage.score;

import org.jetby.treexBuyer.storage.score.types.CategoryScore;
import org.jetby.treexBuyer.storage.score.types.GlobalScore;
import org.jetby.treexBuyer.storage.score.types.PerItemScore;
import org.bukkit.Material;

import java.util.Map;
import java.util.function.Supplier;

public enum ScoreType {
    GLOBAL(GlobalScore::new),
    ITEM(PerItemScore::new),
    CATEGORY(() -> {
        throw new UnsupportedOperationException("Use createScore(Map) for CATEGORY type");
    });

    private final Supplier<Score> factory;

    ScoreType(Supplier<Score> factory) {
        this.factory = factory;
    }

    public Score createScore() {
        return factory.get();
    }

    public Score createScore(Map<Material, String> categoryMap) {
        if (this == CATEGORY) {
            return new CategoryScore(categoryMap);
        }
        return createScore();
    }
}