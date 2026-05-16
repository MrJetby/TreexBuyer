package org.jetby.treexBuyer.configurations;

import lombok.Getter;
import org.jetby.libb.LibbApi;
import org.jetby.libb.action.record.ActionBlock;
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
    private FileConfiguration fileConfiguration;


    private final BuyerManager manager;
    public Config(BuyerManager manager) {
        this.manager = manager;
    }

    public void load() {
        this.fileConfiguration = manager.getPlugin().getFileConfiguration("config.yml");

//        Logger.setDebug(fileConfiguration.getBoolean("debug", false));

        storageType = fileConfiguration.getString("storage.type", "yaml").toUpperCase();
        host = fileConfiguration.getString("storage.host");
        port = fileConfiguration.getInt("storage.port");
        database = fileConfiguration.getString("storage.database");
        username = fileConfiguration.getString("storage.username");
        password = fileConfiguration.getString("storage.password");
        autoBuyDelay = fileConfiguration.getInt("autobuy.delay", 60);
        autoBuyActions = ParseUtil.getActionBlock(fileConfiguration, "autobuy.actions");
        disabledWorlds = fileConfiguration.getStringList("autobuy.disabled-worlds");
        enable = fileConfiguration.getString("autobuy.status.enable", "<green>Включён");
        disable = fileConfiguration.getString("autobuy.status.disable", "<red>Выключен");


        inventoryPrice = fileConfiguration.getBoolean("inventory-price.enable", false);
        inventoryPriceText = LibbApi.Settings.CONFIG_COLORIZER.deserialize(fileConfiguration.getString("inventory-price.text", "<green>$%price%"));

        persistentActionbar = fileConfiguration.getBoolean("persistent-actionbar.enable", false);
        persistentActionbarText = LibbApi.Settings.CONFIG_COLORIZER.deserialize(fileConfiguration.getString("persistent-actionbar.text", ""));


        ConfigurationSection ss = fileConfiguration.getConfigurationSection("score-system");
        if (ss == null) ss = fileConfiguration.createSection("score-system");

        type = ScoreType.valueOf(ss.getString("type", "GLOBAL").toUpperCase());
        scores = ss.getInt("multiplier-ratio.scores", 100);
        coefficient = ss.getDouble("multiplier-ratio.coefficient", 0.01);
        maxCoefficient = ss.getDouble("max-legal-coefficient", 3);
        defaultCoefficient = ss.getDouble("default-coefficient", 1);
        boosters_except_legal_coefficient = ss.getBoolean("boosters_except_legal_coefficient", false);

        itemsPrices = fileConfiguration.getString("items-prices-file", "prices.yml");
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