package me.jetby.treexBuyer.configurations;

import lombok.Getter;
import me.jetby.treexBuyer.functions.Boost;
import me.jetby.treexBuyer.storage.score.ScoreType;
import me.jetby.treexBuyer.tools.FileLoader;
import me.jetby.treexBuyer.tools.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Config {

    private String storageType;
    private ScoreType type;
    private double scores;
    private double coefficient;
    private double maxCoefficient;
    private double defaultCoefficient;
    private boolean boosters_except_legal_coefficient;
    private String enable;
    private String disable;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String itemsPrices;
    private int autoBuyDelay;
    private List<String> autoBuyActions;
    private List<String> disabledWorlds;
    private final Map<String, Boost> boosts = new HashMap<>();

    public void load() {
        FileConfiguration cfg = FileLoader.getFileConfiguration("config.yml");

        Logger.setDebug(cfg.getBoolean("debug", false));

        storageType = cfg.getString("storage.type", "yaml").toUpperCase();
        host = cfg.getString("storage.host");
        port = cfg.getInt("storage.port");
        database = cfg.getString("storage.database");
        username = cfg.getString("storage.username");
        password = cfg.getString("storage.password");
        autoBuyDelay = cfg.getInt("autobuy.delay", 60);
        autoBuyActions = cfg.getStringList("autobuy.actions");
        disabledWorlds = cfg.getStringList("autobuy.disabled-worlds");
        enable = cfg.getString("autobuy.status.enable", "<green>Включён");
        disable = cfg.getString("autobuy.status.disable", "<red>Выключен");

        ConfigurationSection ss = cfg.getConfigurationSection("score-system");
        if (ss == null) ss = cfg.createSection("score-system");

        type = ScoreType.valueOf(ss.getString("type", "GLOBAL").toUpperCase());
        scores = ss.getInt("multiplier-ratio.scores", 100);
        coefficient = ss.getDouble("multiplier-ratio.coefficient", 0.01);
        maxCoefficient = ss.getDouble("max-legal-coefficient", 3);
        defaultCoefficient = ss.getDouble("default-coefficient", 1);
        boosters_except_legal_coefficient = ss.getBoolean("boosters_except_legal_coefficient", false);

        itemsPrices = cfg.getString("items-prices-file", "prices.yml");
        loadBoosts(ss);
    }

    public void loadBoosts(ConfigurationSection cfg) {
        boosts.clear();
        ConfigurationSection boosterSection = cfg.getConfigurationSection("booster");
        if (boosterSection == null) return;
        for (String key : boosterSection.getKeys(false)) {
            String permission = boosterSection.getString(key + ".permission");
            double coeff = boosterSection.getDouble(key + ".external-coefficient", 0.0);
            boosts.put(key, new Boost(key, permission, coeff));
        }
    }
}