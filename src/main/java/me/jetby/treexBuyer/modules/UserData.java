package me.jetby.treexBuyer.modules;

import lombok.Getter;
import lombok.Setter;
import me.jetby.treexBuyer.storage.score.Score;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class UserData {

    public static final Map<UUID, UserData> USERDATA_LIST = new ConcurrentHashMap<>();

    private final UUID uuid;
    private final Score score;
    @Setter
    private boolean autoBuy = false;
    private final Set<Material> autoBuyItems;

    // ── Static finders ────────────────────────────────────────────────────────

    @Nullable
    public static UserData findByUuid(@NotNull UUID uuid) {
        return USERDATA_LIST.get(uuid);
    }

    @Nullable
    public static UserData findByUuid(@NotNull String uuid) {
        try {
            return USERDATA_LIST.get(UUID.fromString(uuid));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Nullable
    public static UserData findByName(@NotNull String name) {
        if (name.isBlank()) return null;

        Player player = Bukkit.getPlayerExact(name);
        if (player != null) return USERDATA_LIST.get(player.getUniqueId());

        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null && op.getName().equalsIgnoreCase(name))
                return USERDATA_LIST.get(op.getUniqueId());
        }
        return null;
    }

    @NotNull
    public static UserData getOrCreate(@NotNull UUID uuid, @NotNull Score score) {
        return USERDATA_LIST.computeIfAbsent(uuid, id -> new UserData(id, score));
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public UserData(@NotNull UUID uuid, @NotNull Score score) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        Objects.requireNonNull(score, "score cannot be null");
        this.uuid = uuid;
        this.score = score;
        this.autoBuyItems = Collections.synchronizedSet(new HashSet<>());
    }

    public UserData(@NotNull UUID uuid, @NotNull Score score, @NotNull Set<Material> autoBuyItems) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        Objects.requireNonNull(score, "score cannot be null");
        this.uuid = uuid;
        this.score = score;
        this.autoBuyItems = autoBuyItems;
    }

    // ── Score delegation ──────────────────────────────────────────────────────

    public void addScore(@NotNull Material material, double amount) {
        score.add(material, amount);
    }

    public double getScore(Material material) {
        return score.get(material);
    }

    public double getTotalScore() {
        return score.getTotal();
    }

    public void takeScore(@NotNull Material material, int amount) {
        score.take(material, amount);
    }

    // ── AutoBuy items ─────────────────────────────────────────────────────────

    public void addAutoBuyMaterial(@NotNull Material material) {
        autoBuyItems.add(material);
    }

    public void removeAutoBuyMaterial(@NotNull Material material) {
        autoBuyItems.remove(material);
    }
}