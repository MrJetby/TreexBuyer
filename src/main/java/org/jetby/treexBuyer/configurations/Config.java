package org.jetby.treexBuyer.configurations;

import lombok.Getter;
import org.jetby.libb.LibbApi;
import org.jetby.libb.action.record.ActionBlock;
import org.jetby.libb.color.HashedSerializer;
import org.jetby.libb.color.Serializer;
import org.jetby.libb.color.SerializerType;
import org.jetby.libb.gui.parser.ParseUtil;
import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.functions.Boost;
import org.jetby.treexBuyer.storage.score.ScoreType;
import net.kyori.adventure.text.Component;
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
    private ActionBlock autoBuyActions;
    private List<String> disabledWorlds;

    private boolean inventoryPrice;
    private Component inventoryPriceText;

    private boolean persistentActionbar;
    private Component persistentActionbarText;

    private final Map<String, Boost> boosts = new HashMap<>();
    private FileConfiguration config;
    public static Serializer SERIALIZER;


    private final BuyerManager manager;
    public Config(BuyerManager manager) {
        this.manager = manager;
    }

    public void load() {
        this.config = manager.getPlugin().getFileConfiguration("config.yml");

//        Logger.setDebug(fileConfiguration.getBoolean("debug", false));


        SERIALIZER = new HashedSerializer(
                SerializerType.valueOf(config.getString("serializer.type")),
                config.getBoolean("serializer.cache.enabled"),
                config.getInt("serializer.cache.max-size"));

        storageType = config.getString("storage.type", "yaml").toUpperCase();
        host = config.getString("storage.host");
        port = config.getInt("storage.port");
        database = config.getString("storage.database");
        username = config.getString("storage.username");
        password = config.getString("storage.password");
        autoBuyDelay = config.getInt("autobuy.delay", 60);
        autoBuyActions = ParseUtil.getActionBlock(config, "autobuy.actions");
        disabledWorlds = config.getStringList("autobuy.disabled-worlds");
        enable = config.getString("autobuy.status.enable", "<green>Включён");
        disable = config.getString("autobuy.status.disable", "<red>Выключен");


        inventoryPrice = config.getBoolean("inventory-price.enable", false);
        inventoryPriceText = LibbApi.Settings.CONFIG_COLORIZER.deserialize(config.getString("inventory-price.text", "<green>$%price%"));

        persistentActionbar = config.getBoolean("persistent-actionbar.enable", false);
        persistentActionbarText = LibbApi.Settings.CONFIG_COLORIZER.deserialize(config.getString("persistent-actionbar.text", ""));


        ConfigurationSection ss = config.getConfigurationSection("score-system");
        if (ss == null) ss = config.createSection("score-system");

        type = ScoreType.valueOf(ss.getString("type", "GLOBAL").toUpperCase());
        scores = ss.getInt("multiplier-ratio.scores", 100);
        coefficient = ss.getDouble("multiplier-ratio.coefficient", 0.01);
        maxCoefficient = ss.getDouble("max-legal-coefficient", 3);
        defaultCoefficient = ss.getDouble("default-coefficient", 1);
        boosters_except_legal_coefficient = ss.getBoolean("boosters_except_legal_coefficient", false);

        itemsPrices = config.getString("items-prices-file", "prices.yml");
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