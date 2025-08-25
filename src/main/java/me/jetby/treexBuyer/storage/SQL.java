package me.jetby.treexBuyer.storage;

import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.tools.Logger;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SQL implements Storage {
    private final Main plugin;

    private final boolean mySql;
    private final Connection connection;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>( );

    private static final String CREATE_DATABASE = "CREATE TABLE IF NOT EXISTS players (uuid TEXT NOT NULL, score INTEGER DEFAULT 0, autoBuy BOOLEAN DEFAULT false, autoBuyItems TEXT DEFAULT '', PRIMARY KEY (uuid));";
    private static final String GET_ALL_PLAYERS = "SELECT * FROM players;";
    private static final String GET_SCORES_FOR_UUID = "SELECT score_key, value FROM player_scores WHERE uuid = ?;";
    private static final String INSERT_PLAYER = "INSERT INTO players (uuid, score, autoBuy, autoBuyItems) VALUES (?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET score = ?, autoBuy = ?, autoBuyItems = ?;";
    private static final String INSERT_SCORE = "INSERT INTO player_scores (uuid, score_key, value) VALUES (?, ?, ?) ON CONFLICT(uuid, score_key) DO UPDATE SET value = ?;";
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";

    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUser;
    private final String mysqlPassword;

    public SQL(Main plugin) {
        this.plugin = plugin;

        this.mySql = plugin.getCfg( ).getStorageType( ).equalsIgnoreCase("MYSQL".toUpperCase( ));

        this.mysqlHost = plugin.getCfg( ).getHost( );
        this.mysqlPort = plugin.getCfg( ).getPort( );
        this.mysqlDatabase = plugin.getCfg( ).getDatabase( );
        this.mysqlUser = plugin.getCfg( ).getUsername( );
        this.mysqlPassword = plugin.getCfg( ).getPassword( );

        this.connection = connect();
    }


    private static class PlayerData {
        boolean autoBuy = false;
        List<String> autoBuyItems = new ArrayList<>( );
        Map<String, Integer> scores = new HashMap<>();

        PlayerData(boolean autoBuy, List<String> autoBuyItems, Map<String, Integer> scores) {
            this.autoBuy = autoBuy;
            this.autoBuyItems = new ArrayList<>(autoBuyItems);
            this.scores = new HashMap<>(scores);
        }

        PlayerData() {
        }
    }


    public void closeConnection() {
        try {
            connection.close( );
        } catch (SQLException e) {
            Logger.error("Connection close was failed: " + e);
        }
    }

    private Connection connect() {
        try {
            if (mySql) {
                try {
                    Class.forName(MYSQL_DRIVER);
                } catch (ClassNotFoundException e) {
                    throw new SQLException("MySQL driver not found", e);
                }
                return DriverManager.getConnection(
                        "jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase + "?useSSL=false",
                        mysqlUser, mysqlPassword);
            } else {
                return DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder( ) + "/storage.db");
            }
        } catch (SQLException e) {
            Logger.error(""+e);
        }

        return null;
    }

    @Override
    public boolean load() {
        boolean status = false;
        if (connection!=null) {
            createTables( );
            loadCacheAsync( );
            status = true;
        }
        return status;
    }


    private void createTables() {
        try (Statement statement = connection.createStatement( )) {
            statement.execute(CREATE_DATABASE);
            Logger.success("Таблица игроков успешно создана или уже существует.");
        } catch (SQLException e) {
            Logger.error("Ошибка при создании таблицы игроков: " + e.getMessage( ));
        }
    }

    private void loadCacheAsync() {
        Bukkit.getScheduler( ).runTaskAsynchronously(plugin, () -> {
            long start = System.currentTimeMillis( );
            try (PreparedStatement psPlayers = connection.prepareStatement(GET_ALL_PLAYERS);
                 ResultSet rsPlayers = psPlayers.executeQuery( )) {
                while (rsPlayers.next( )) {
                    UUID uuid = UUID.fromString(rsPlayers.getString("uuid"));
                    boolean autoBuy = rsPlayers.getBoolean("autoBuy");
                    String itemsStr = rsPlayers.getString("autoBuyItems");
                    List<String> items = itemsStr == null || itemsStr.isEmpty( ) ? new ArrayList<>( ) : Arrays.asList(itemsStr.split(","));

                    Map<String, Integer> scores = new HashMap<>();
                    try (PreparedStatement psScores = connection.prepareStatement(GET_SCORES_FOR_UUID)) {
                        psScores.setString(1, uuid.toString());
                        try (ResultSet rsScores = psScores.executeQuery()) {
                            while (rsScores.next()) {
                                String scoreKey = rsScores.getString("score_key");
                                int value = rsScores.getInt("value");
                                scores.put(scoreKey, value);
                            }
                        }
                    }

                    cache.put(uuid, new PlayerData(autoBuy, items, scores));
                }
                Logger.success("Данные из БД были загружены за " + (System.currentTimeMillis( ) - start) + " мс");
            } catch (SQLException e) {
                Logger.error("Ошибка загрузки кэша: " + e.getMessage( ));
            }
        });
    }

    @Override
    public boolean save() {
        boolean status = false;
        try {
            try (PreparedStatement psPlayer = connection.prepareStatement(INSERT_PLAYER);
                 PreparedStatement psScore = connection.prepareStatement(INSERT_SCORE)) {
                for (Map.Entry<UUID, PlayerData> entry : cache.entrySet( )) {
                    UUID uuid = entry.getKey( );
                    PlayerData data = entry.getValue( );
                    String itemsStr = String.join(",", data.autoBuyItems);

                    psPlayer.setString(1, uuid.toString( ));
                    psPlayer.setBoolean(2, data.autoBuy);
                    psPlayer.setString(3, itemsStr);
                    psPlayer.setBoolean(4, data.autoBuy);
                    psPlayer.setString(5, itemsStr);
                    psPlayer.addBatch( );

                    for (Map.Entry<String, Integer> scoreEntry : data.scores.entrySet()) {
                        psScore.setString(1, uuid.toString());
                        psScore.setString(2, scoreEntry.getKey());
                        psScore.setInt(3, scoreEntry.getValue());
                        psScore.setInt(4, scoreEntry.getValue());
                        psScore.addBatch();
                    }
                }
                psPlayer.executeBatch( );
                psScore.executeBatch( );
            }

            connection.commit( );
            status = true;
            Logger.success("Кэш сохранён в БД.");
        } catch (SQLException e) {
            Logger.error("Ошибка сохранения кэша: " + e.getMessage( ));
            try {
                connection.rollback( );
            } catch (SQLException ex) {
                Logger.error("Ошибка отката транзакции: " + ex.getMessage( ));
            }
        }

        closeConnection( );
        cache.clear( );
        return status;
    }

    @Override
    public String type() {
        return mySql ? "MYSQL" : "SQLITE";
    }

    @Override
    public boolean playerExists(UUID uuid) {
        return cache.containsKey(uuid);
    }

    @Override
    public void setScore(UUID uuid, String key, int score) {
        PlayerData data = cache.computeIfAbsent(uuid, k -> new PlayerData( ));
        data.scores.put(key.toLowerCase(), score);
        scheduleAsyncUpdate(uuid, data);
    }

    @Override
    public int getScore(UUID uuid, String key) {
        PlayerData data = cache.computeIfAbsent(uuid, k -> new PlayerData( ));
        return data.scores.getOrDefault(key.toLowerCase(), 0);
    }

    @Override
    public void setAutoBuyItems(UUID uuid, List<String> items) {
        PlayerData data = cache.computeIfAbsent(uuid, k -> new PlayerData( ));
        data.autoBuyItems = new ArrayList<>(items);
        scheduleAsyncUpdate(uuid, data);
    }

    @Override
    public List<String> getAutoBuyItems(UUID uuid) {
        PlayerData data = cache.computeIfAbsent(uuid, k -> new PlayerData( ));
        return new ArrayList<>(data.autoBuyItems);
    }

    @Override
    public void setAutoBuyStatus(UUID uuid, boolean status) {
        PlayerData data = cache.computeIfAbsent(uuid, k -> new PlayerData( ));
        data.autoBuy = status;
        scheduleAsyncUpdate(uuid, data);
    }

    @Override
    public boolean getAutoBuyStatus(UUID uuid) {
        PlayerData data = cache.computeIfAbsent(uuid, k -> new PlayerData( ));
        return data.autoBuy;
    }

    private void scheduleAsyncUpdate(UUID uuid, PlayerData data) {
        Bukkit.getScheduler( ).runTaskAsynchronously(plugin, () -> {
            String itemsStr = String.join(",", data.autoBuyItems);
            try {
                try (PreparedStatement psPlayer = connection.prepareStatement(INSERT_PLAYER)) {
                    psPlayer.setString(1, uuid.toString( ));
                    psPlayer.setBoolean(2, data.autoBuy);
                    psPlayer.setString(3, itemsStr);
                    psPlayer.setBoolean(4, data.autoBuy);
                    psPlayer.setString(5, itemsStr);
                    psPlayer.executeUpdate( );
                }

                try (PreparedStatement psScore = connection.prepareStatement(INSERT_SCORE)) {
                    for (Map.Entry<String, Integer> scoreEntry : data.scores.entrySet()) {
                        psScore.setString(1, uuid.toString());
                        psScore.setString(2, scoreEntry.getKey());
                        psScore.setInt(3, scoreEntry.getValue());
                        psScore.setInt(4, scoreEntry.getValue());
                        psScore.addBatch();
                    }
                    psScore.executeBatch();
                }
            } catch (SQLException e) {
                Logger.error("Ошибка обновления игрока " + uuid + ": " + e.getMessage( ));
            }
        });
    }
}
