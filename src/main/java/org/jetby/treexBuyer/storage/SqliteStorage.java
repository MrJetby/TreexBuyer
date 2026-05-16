package org.jetby.treexBuyer.storage;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.modules.UserData;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.UUID;

public class SqliteStorage extends CachedStorage {

    private HikariDataSource dataSource;
    private final Gson gson = new Gson();

    public SqliteStorage(BuyerManager plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + manager.getPlugin().getDataFolder() + "/data.db");
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(5000L);
        dataSource = new HikariDataSource(config);
        createTable();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM userdata");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UserData data = parseRow(rs);
                UserData.USERDATA_LIST.put(data.getUuid(), data);
            }
        } catch (SQLException e) {
            manager.getPlugin().getLogger().severe("Failed to load SQLite data: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        UserData.USERDATA_LIST.keySet().forEach(this::saveUserSync);
        if (dataSource != null) dataSource.close();
    }

    @Override
    @NotNull
    public UserData loadUser(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM userdata WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return new UserData(uuid, manager.getItems().createScore());
            return parseRow(rs);
        } catch (SQLException e) {
            manager.getPlugin().getLogger().warning("Failed to load user " + uuid + ": " + e.getMessage());
        }
        return new UserData(uuid, manager.getItems().createScore());
    }

    @Override
    public void saveUser(UUID uuid) {
        UserData data = UserData.USERDATA_LIST.get(uuid);
        if (data == null) return;

        String scoresJson = gson.toJson(data.getScore().toJson());
        String autoBuyJson = gson.toJson(data.getAutoBuyItems().stream().map(Enum::name).toList());

        manager.getPlugin().getServer().getScheduler().runTaskAsynchronously(manager.getPlugin(), () ->
                writeUser(uuid, data.isAutoBuy(), autoBuyJson, scoresJson));
    }

    /**
     * Synchronous save — used during shutdown when the Bukkit scheduler is unavailable.
     */
    private void saveUserSync(UUID uuid) {
        UserData data = UserData.USERDATA_LIST.get(uuid);
        if (data == null) return;
        writeUser(uuid, data.isAutoBuy(),
                gson.toJson(data.getAutoBuyItems().stream().map(Enum::name).toList()),
                gson.toJson(data.getScore().toJson()));
    }

    private void writeUser(UUID uuid, boolean autoBuy, String autoBuyJson, String scoresJson) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO userdata (uuid, auto_buy, auto_buy_items, scores) VALUES (?,?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setBoolean(2, autoBuy);
            ps.setString(3, autoBuyJson);
            ps.setString(4, scoresJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            manager.getPlugin().getLogger().warning("Failed to save user " + uuid + ": " + e.getMessage());
        }
    }

    @Override
    public void deleteUser(UUID uuid) {
        UserData.USERDATA_LIST.remove(uuid);
        manager.getPlugin().getServer().getScheduler().runTaskAsynchronously(manager.getPlugin(), () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM userdata WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                manager.getPlugin().getLogger().warning("Failed to delete user " + uuid + ": " + e.getMessage());
            }
        });
    }

    private UserData parseRow(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        UserData data = new UserData(uuid, manager.getItems().createScore());
        data.setAutoBuy(rs.getBoolean("auto_buy"));

        String autoBuyJson = rs.getString("auto_buy_items");
        if (autoBuyJson != null) {
            for (String s : gson.fromJson(autoBuyJson, String[].class)) {
                try {
                    data.addAutoBuyMaterial(Material.valueOf(s));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        String scoresJson = rs.getString("scores");
        if (scoresJson != null) {
            data.getScore().fromJson(JsonParser.parseString(scoresJson));
        }

        return data;
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS userdata (
                    uuid VARCHAR(36) PRIMARY KEY,
                    auto_buy BOOLEAN DEFAULT FALSE,
                    auto_buy_items TEXT DEFAULT '[]',
                    scores TEXT DEFAULT '{}'
                )""";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            manager.getPlugin().getLogger().severe("Failed to create table: " + e.getMessage());
        }
    }
}