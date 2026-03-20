package me.jetby.treexBuyer;

import lombok.Getter;
import lombok.Setter;
import me.jetby.libb.action.ActionRegistry;
import me.jetby.libb.gui.CommandRegistrar;
import me.jetby.treexBuyer.command.AdminCommand;
import me.jetby.treexBuyer.configurations.Config;
import me.jetby.treexBuyer.configurations.GuiLoader;
import me.jetby.treexBuyer.configurations.Items;
import me.jetby.treexBuyer.functions.AutoBuy;
import me.jetby.treexBuyer.functions.Coefficient;
import me.jetby.treexBuyer.hook.TreexBuyerPlaceholder;
import me.jetby.treexBuyer.hook.Vault;
import me.jetby.treexBuyer.menus.BuyerGui;
import me.jetby.treexBuyer.menus.actions.*;
import me.jetby.treexBuyer.modules.UserData;
import me.jetby.treexBuyer.storage.*;
import me.jetby.treexBuyer.tools.Logger;
import me.jetby.treexBuyer.tools.Metrics;
import me.jetby.treexBuyer.tools.Version;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class TreexBuyer extends JavaPlugin {

    public static final NamespacedKey NAMESPACED_KEY = new NamespacedKey("treexbuyer", "item");
    private Economy economy;

    private static TreexBuyer INSTANCE;

    public static TreexBuyer getInstance() {
        return INSTANCE;
    }

    @Setter
    private Storage storage;

    private Config cfg;
    private Items items;
    private Coefficient coefficient;
    private AutoBuy autoBuy;
    @Getter
    @Setter
    private TreexBuyerPlaceholder treexBuyerPlaceholder;

    public static final MiniMessage MM = MiniMessage.miniMessage();


    @Override
    public void onEnable() {
        INSTANCE = this;

        Logger.success("Looking for updates..");
        Version version = new Version(this);
        for (String str : version.getAlert()) {
            Logger.success(str);
        }
        this.economy = Vault.setupEconomy(this);
        if (economy == null) return;
        Logger.success("Enabling TreexBuyer...");
        new Metrics(this, 25141);
        cfg = new Config();
        cfg.load();
        items = new Items(this);
        items.load();
        registerActions();
        new GuiLoader(this)
                .loadGuis();
        loadStorage();
        autoBuy = new AutoBuy(this);
        autoBuy.start();
        coefficient = new Coefficient(this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        new TreexBuyerPlaceholder(this).init();

        PluginCommand treexbuyer = getCommand("treexbuyer");
        if (treexbuyer != null)
            treexbuyer.setExecutor(new AdminCommand(this));
        Logger.success("");
        Logger.success("Plugin was successfully enabled, enjoy it :)");
        Logger.success("------------------------");
    }

    @Override
    public void onDisable() {
        CommandRegistrar.unregisterAll(this);
        if (autoBuy != null) {
            autoBuy.stop();
        }
        if (storage != null) storage.shutdown();
        ActionRegistry.unregisterAll("treexbuyer");
    }

    public void loadStorage() {
        switch (cfg.getStorageType()) {
            case "MYSQL":
                storage = new MySQLStorage(this);
                break;
            case "JSON":
                storage = new JsonStorage(this);
                break;
            default:
                storage = new SqliteStorage(this);
        }
        storage.init();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!UserData.USERDATA_LIST.containsKey(player.getUniqueId())) {
                UserData.USERDATA_LIST.put(player.getUniqueId(), storage.loadUser(player.getUniqueId()));
            }
        }
    }


    public void registerActions() {
        ActionRegistry.register("treexbuyer", "sell_item", new SellItem());
        ActionRegistry.register("treexbuyer", "sell_all", new SellAll());

        ActionRegistry.register("treexbuyer", "enable_all", new EnableAll());
        ActionRegistry.register("treexbuyer", "disable_all", new DisableAll());

        ActionRegistry.register("treexbuyer", "autobuy_toggle", new AutoBuyStatusToggle());
        ActionRegistry.register("treexbuyer", "autobuy_item_toggle", new AutoBuyItemToggle());
        ActionRegistry.override("treexbuyer", "refresh", (ctx, s) -> {
            BuyerGui gui = ctx.get(BuyerGui.class);

            Player player = ctx.getPlayer();
            if (player == null) return;

            if (gui == null) return;
            gui.refresh();
        });
        ActionRegistry.override("treexbuyer", "open", (ctx, s) -> {
            if (s == null) return;
            Player player = ctx.getPlayer();
            if (player == null) return;
            UserData data = UserData.getOrCreate(player.getUniqueId(), getItems().createScore());

            FileConfiguration gui = GuiLoader.ALL_GUIS.get(s);
            if (gui == null) return;
            new BuyerGui(player, data, gui, this).open(player);
        });
    }
}