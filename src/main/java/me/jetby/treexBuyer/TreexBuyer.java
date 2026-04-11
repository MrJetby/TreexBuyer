package me.jetby.treexBuyer;

import lombok.Getter;
import lombok.Setter;
import me.jetby.libb.action.ActionRegistry;
import me.jetby.libb.plugin.LibbPlugin;
import me.jetby.libb.util.Logger;
import me.jetby.treexBuyer.configurations.Config;
import me.jetby.treexBuyer.configurations.GuiLoader;
import me.jetby.treexBuyer.configurations.Items;
import me.jetby.treexBuyer.functions.AutoBuy;
import me.jetby.treexBuyer.functions.Coefficient;
import me.jetby.treexBuyer.functions.InventoryPrice;
import me.jetby.treexBuyer.functions.PersistentActionBar;
import me.jetby.treexBuyer.hook.TreexBuyerPlaceholder;
import me.jetby.treexBuyer.hook.Vault;
import me.jetby.treexBuyer.menus.BuyerGui;
import me.jetby.treexBuyer.menus.actions.*;
import me.jetby.treexBuyer.modules.UserData;
import me.jetby.treexBuyer.storage.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

@Getter
public final class TreexBuyer extends LibbPlugin {

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
    private InventoryPrice inventoryPrice;
    @Getter
    @Setter
    private TreexBuyerPlaceholder treexBuyerPlaceholder;
    private GuiLoader guiLoader;
    public static final MiniMessage MM = MiniMessage.miniMessage();
    private PersistentActionBar actionBarUtil;

    @Override
    public void onEnable() {
        INSTANCE = this;

        Logger.info(this, "------------------------");
        setVersionUtil("https://raw.githubusercontent.com/MrJetby/TreexBuyer/refs/heads/master/VERSION");
        this.economy = Vault.setupEconomy(this);
        if (economy == null) return;

        Logger.info(this, "<green>Enabling TreexBuyer...");

        setBStats(this, 25141);

        try {
            cfg = new Config(this);
            cfg.load();
            Logger.info(this, "<green>✔ Config");
        } catch (Exception e) {
            Logger.error(this, "<red>✘ Config");
            e.printStackTrace();
        }


        try {
            items = new Items(this);
            items.load();
            Logger.info(this, "<green>✔ Items");
        } catch (Exception e) {
            Logger.error(this, "<red>✘ Items");
            e.printStackTrace();
        }

        try {
            guiLoader = new GuiLoader(this);
            guiLoader.loadGuis();
            Logger.info(this, "<green>✔ Guis (" + GuiLoader.ALL_GUIS.size() + " menus)");
        } catch (Exception e) {
            Logger.error(this, "<red>✘ Guis");
            e.printStackTrace();
        }

        registerActions();

        loadStorage();

        autoBuy = new AutoBuy(this);
        autoBuy.start();
        coefficient = new Coefficient(this);

        actionBarUtil = new PersistentActionBar(this);
        actionBarUtil.start();

        try {
            Bukkit.getServer().getPluginManager().getPlugin("ProtocolLib");

            inventoryPrice = new InventoryPrice(this);
            inventoryPrice.load();

            Logger.info(this, "<green>✔ ProtocolLib");
        } catch (Exception e) {
            Logger.error(this, "<red>✘ ProtocolLib not found. Inventory price not going to work.");
            e.printStackTrace();
        }

        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI")==null) {
                throw new RuntimeException("<red>✘ PlaceholderAPI not found. Disabling plugin");
            }
            treexBuyerPlaceholder = new TreexBuyerPlaceholder(this);
            treexBuyerPlaceholder.register();

            Logger.info(this, "<green>✔ PlaceholderAPI");
        } catch (Exception e) {
            Logger.error(this, e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        new BuyerCommand(this).register();

        Logger.info(this, "");
        Logger.info(this, "<green>Plugin was successfully enabled, enjoy it :)");
        Logger.info(this, "------------------------");
    }

    @Override
    public void onDisable() {
        unregisterCommands();
        ActionRegistry.unregisterAll("treexbuyer");

        if (autoBuy != null) {
            autoBuy.stop();
        }
        if (storage != null) storage.shutdown();
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