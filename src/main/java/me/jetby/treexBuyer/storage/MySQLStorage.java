package me.jetby.treexBuyer.storage;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.modules.UserData;
import me.jetby.treexBuyer.tools.Logger;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.UUID;

public class MySQLStorage
        implements Storage {
    private final TreexBuyer plugin;
    private HikariDataSource dataSource;
    private final Gson gson = new Gson();

    public MySQLStorage(TreexBuyer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + plugin.getCfg().getHost() + ":" + plugin.getCfg().getPort() + "/" + this.plugin.getCfg().getDatabase() + "?useSSL=false&autoReconnect=true&characterEncoding=utf8");
        config.setUsername(plugin.getCfg().getUsername());
        config.setPassword(plugin.getCfg().getPassword());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000L);
        config.setIdleTimeout(600000L);
        config.setMaxLifetime(1800000L);
        dataSource = new HikariDataSource(config);
        createTable();
    }

    @Override
    public void shutdown() {
        UserData.USERDATA_LIST.keySet().forEach(this::saveUserSync);
        UserData.USERDATA_LIST.clear();
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public void onPlayerJoin(UUID uuid) {
        UserData.USERDATA_LIST.put(uuid, loadUser(uuid));
    }

    @Override
    public void onPlayerQuit(UUID uuid) {
        saveUser(uuid);
        UserData.USERDATA_LIST.remove(uuid);
    }

    @Override
    @NotNull
    public UserData loadUser(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM userdata WHERE uuid = ?");) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return new UserData(uuid, plugin.getItems().createScore());
            return parseRow(rs);
        } catch (SQLException e) {
            Logger.warn("Failed to load user " + uuid + ": " + e.getMessage());
        }
        return new UserData(uuid, plugin.getItems().createScore());
    }

    @Override
    public void saveUser(UUID uuid) {
        UserData data = UserData.USERDATA_LIST.get(uuid);
        if (data == null) return;
        String scoresJson = gson.toJson(data.getScore().toJson());
        String autoBuyJson = gson.toJson(data.getAutoBuyItems().stream().map(Enum::name).toList());
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                writeUser(uuid, data.isAutoBuy(), autoBuyJson, scoresJson));
    }

    /** Synchronous save — used during shutdown when the Bukkit scheduler is unavailable. */
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
                     "INSERT INTO userdata (uuid, auto_buy, coefficient, auto_buy_items, scores) VALUES (?,?,?,?,?) " +
                             "ON DUPLICATE KEY UPDATE auto_buy=VALUES(auto_buy), coefficient=VALUES(coefficient), " +
                             "auto_buy_items=VALUES(auto_buy_items), scores=VALUES(scores)")) {
            ps.setString(1, uuid.toString());
            ps.setBoolean(2, autoBuy);
            ps.setDouble(3, 0.0);
            ps.setString(4, autoBuyJson);
            ps.setString(5, scoresJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save user " + uuid + ": " + e.getMessage());
        }
    }

    @Override
    public void deleteUser(UUID uuid) {
        UserData.USERDATA_LIST.remove(uuid);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM userdata WHERE uuid = ?");) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
               Logger.warn("Failed to delete user " + uuid + ": " + e.getMessage());
            }
        });
    }

    private UserData parseRow(ResultSet rs) throws SQLException {
        String scoresJson;
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        UserData data = new UserData(uuid, plugin.getItems().createScore());
        data.setAutoBuy(rs.getBoolean("auto_buy"));
        String autoBuyJson = rs.getString("auto_buy_items");
        if (autoBuyJson != null) {
            for (String s : gson.fromJson(autoBuyJson, String[].class)) {
                try {
                    data.addAutoBuyMaterial(Material.valueOf( s));
                } catch (IllegalArgumentException illegalArgumentException) {
                    // empty catch block
                }
            }
        }
        if ((scoresJson = rs.getString("scores")) != null) {
            data.getScore().fromJson(JsonParser.parseString( scoresJson));
        }
        return data;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS userdata (uuid VARCHAR(36) PRIMARY KEY,auto_buy BOOLEAN DEFAULT FALSE,coefficient DOUBLE DEFAULT 0.0,auto_buy_items TEXT DEFAULT '[]',scores TEXT DEFAULT '{}')";
        try (Connection conn = this.dataSource.getConnection();
             Statement stmt = conn.createStatement();) {
            stmt.execute(sql);
        } catch (SQLException e) {
            Logger.warn("Failed to create table: " + e.getMessage());
        }
    }
}