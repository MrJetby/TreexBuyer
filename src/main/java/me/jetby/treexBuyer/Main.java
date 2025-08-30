package me.jetby.treexBuyer;

import com.jodexindustries.jguiwrapper.common.JGuiInitializer;
import lombok.Getter;
import lombok.Setter;
import me.jetby.treexBuyer.command.AdminCommand;
import me.jetby.treexBuyer.configurations.Config;
import me.jetby.treexBuyer.configurations.Items;
import me.jetby.treexBuyer.functions.AutoBuy;
import me.jetby.treexBuyer.functions.Coefficient;
import me.jetby.treexBuyer.menus.CommandRegistrar;
import me.jetby.treexBuyer.menus.JGui;
import me.jetby.treexBuyer.menus.Loader;
import me.jetby.treexBuyer.storage.JSON;
import me.jetby.treexBuyer.storage.SQL;
import me.jetby.treexBuyer.storage.Storage;
import me.jetby.treexBuyer.storage.Yaml;
import me.jetby.treexBuyer.tools.Logger;
import me.jetby.treexBuyer.tools.Metrics;
import me.jetby.treexBuyer.tools.TreexBuyerPlaceholders;
import me.jetby.treexBuyer.tools.Version;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.util.UUID;

@Getter
public final class Main extends JavaPlugin {

    private Loader menuLoader;
    public static final NamespacedKey NAMESPACED_KEY = new NamespacedKey("treexbuyer", "item");
    private Economy economy;

    private static Main INSTANCE;
    public static Main getInstance() {
        return INSTANCE;
    }

    @Setter
    private Storage storage;

    private Config cfg;
    private Items items;
    private Coefficient coefficient;
    private AutoBuy autoBuy;
    private TreexBuyerPlaceholders treexBuyerPlaceholders;

    public static final DecimalFormat df = new DecimalFormat("#.##");

    @Override
    public void onEnable() {
        Logger.success("Looking for updates..");
        Version version = new Version(this);
        for (String str : version.getAlert()) {
            Logger.success(str);
        }
        Logger.success("Enabling TreexBuyer...");
        INSTANCE = this;

        JGuiInitializer.init(this);

        new Metrics(this, 25141);

        cfg = new Config();
        cfg.load();

        loadStorage();

        items = new Items();
        items.load();
        menuLoader = new Loader(this, getDataFolder());
        menuLoader.load();

        autoBuy = new AutoBuy(this);
        autoBuy.start();

        coefficient = new Coefficient(this);


        if (!setupEconomy()) return;
        setupPlaceholders();

        CommandRegistrar.createCommands(this);

        PluginCommand treexbuyer = getCommand("treexbuyer");
        if (treexbuyer != null)
            treexbuyer.setExecutor(new AdminCommand(this));

        Logger.success("");
        Logger.success("Plugin was successfully enabled, enjoy it :)");

        Logger.success("------------------------");
    }

    public void loadStorage() {
        switch (cfg.getStorageType()) {
            case "MYSQL", "SQLITE":
                storage = new SQL(this);
                break;
            case "JSON":
                storage = new JSON(this);
                break;
            default:
                storage = new Yaml(this);
        }
        storage.load();
    }

    private boolean setupPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Logger.warn("PlaceholderAPI not found. Placeholders are not being working");
            return false;
        } else {
            treexBuyerPlaceholders = new TreexBuyerPlaceholders(this);
            treexBuyerPlaceholders.register();
        }
        return true;
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            Logger.error("Vault was not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            Logger.error("Vault economy plugin was not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        this.economy = rsp.getProvider();
        return true;
    }

    @Override
    public void onDisable() {

        for (UUID uuid : menuLoader.getJGui().keySet()) {
            JGui jGui = menuLoader.getJGui().get(uuid);
            jGui.close();
        }

        if (treexBuyerPlaceholders != null) {
            if (treexBuyerPlaceholders.isRegistered()) {
                treexBuyerPlaceholders.unregister();
            }
        }

        CommandRegistrar.unregisterAll(this);

        if (storage != null) storage.save();

    }
}
