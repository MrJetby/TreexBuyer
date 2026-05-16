package org.jetby.treexBuyer;

import lombok.Getter;
import lombok.Setter;
import org.jetby.libb.action.ActionRegistry;
import org.jetby.libb.color.Serializer;
import org.jetby.libb.util.Metrics;
import org.jetby.libb.util.VersionUtil;
import org.jetby.treexBuyer.configurations.Config;
import org.jetby.treexBuyer.configurations.GuiLoader;
import org.jetby.treexBuyer.configurations.Items;
import org.jetby.treexBuyer.functions.AutoBuy;
import org.jetby.treexBuyer.functions.Coefficient;
import org.jetby.treexBuyer.functions.InventoryPrice;
import org.jetby.treexBuyer.functions.PersistentActionBar;
import org.jetby.treexBuyer.hook.TreexBuyerPlaceholder;
import org.jetby.treexBuyer.hook.Vault;
import org.jetby.treexBuyer.menus.BuyerGui;
import org.jetby.treexBuyer.menus.actions.*;
import org.jetby.treexBuyer.modules.UserData;
import org.jetby.treexBuyer.storage.*;
import org.jetby.treexBuyer.tools.Logger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Getter
public final class BuyerManager {

    public static BuyerManager MANAGER;

    @NotNull
    private final TreexBuyer plugin;

    public BuyerManager(@NotNull TreexBuyer plugin) {
        this.plugin = plugin;
    }

    private Economy economy;

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
    private PersistentActionBar actionBarUtil;

    private boolean isProtocol = false;

    public void onEnable() {
        MANAGER = this;


        Logger.info(plugin, "------------------------");
        new VersionUtil(plugin, plugin.getDescription().getVersion(), "https://raw.githubusercontent.com/MrJetby/TreexBuyer/refs/heads/master/VERSION", "treexbuyer.auto-update");
        this.economy = Vault.setupEconomy(plugin);
        if (economy == null) return;

        Logger.info(plugin, "<green>Enabling TreexBuyer...");

        new Metrics(plugin, 25141);

        try {
            cfg = new Config(this);
            cfg.load();
            Logger.info(plugin, "<green>✔ Config");
        } catch (Exception e) {
            Logger.error(plugin, "<red>✘ Config");
            e.printStackTrace();
        }


        try {
            items = new Items(this);
            items.load();
            Logger.info(plugin, "<green>✔ Items");
        } catch (Exception e) {
            Logger.error(plugin, "<red>✘ Items");
            e.printStackTrace();
        }

        try {
            guiLoader = new GuiLoader(this);
            guiLoader.loadGuis();
            Logger.info(plugin, "<green>✔ Guis (" + GuiLoader.ALL_GUIS.size() + " menus)");
        } catch (Exception e) {
            Logger.error(plugin, "<red>✘ Guis");
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

            this.isProtocol = true;
            Logger.info(plugin, "<green>✔ ProtocolLib");
        } catch (Exception e) {
            Logger.error(plugin, "<red>✘ ProtocolLib not found. Inventory price not going to work.");
            e.printStackTrace();
        }

        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
                throw new RuntimeException("<red>✘ PlaceholderAPI not found. Disabling plugin");
            }
            treexBuyerPlaceholder = new TreexBuyerPlaceholder(this);
            treexBuyerPlaceholder.register();

            Logger.info(plugin, "<green>✔ PlaceholderAPI");
        } catch (Exception e) {
            Logger.error(plugin, e.getMessage());
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), plugin);

        new BuyerCommand(this).register();

        Logger.info(plugin, "");
        Logger.info(plugin, "<green>Plugin was successfully enabled, enjoy it :)");
        Logger.info(plugin, "------------------------");
    }

    public void onDisable() {
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

        ActionRegistry.override("treexbuyer", "open", (ctx, input) -> {
            Player player = ctx.getPlayer();
            if (player == null) return;
            UserData data = UserData.getOrCreate(player.getUniqueId(), getItems().createScore());

            FileConfiguration gui = GuiLoader.ALL_GUIS.get(input.rawText());
            if (gui == null) return;
            new BuyerGui(player, data, gui, this).open(player);
        });
    }


}