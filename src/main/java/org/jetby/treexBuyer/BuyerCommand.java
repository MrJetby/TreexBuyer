package org.jetby.treexBuyer;

import org.jetby.libb.command.AdvancedCommand;
import org.jetby.libb.command.annotations.Permission;
import org.jetby.libb.command.annotations.SubCommand;
import org.jetby.libb.command.annotations.TabComplete;
import org.jetby.libb.command.annotations.messages.InsufficientArgs;
import org.jetby.libb.util.Logger;
import org.jetby.treexBuyer.configurations.Config;
import org.jetby.treexBuyer.configurations.GuiLoader;
import org.jetby.treexBuyer.menus.BuyerGui;
import org.jetby.treexBuyer.modules.UserData;
import org.jetby.treexBuyer.storage.score.Score;
import org.jetby.treexBuyer.storage.score.ScoreType;
import org.jetby.treexBuyer.storage.score.types.CategoryScore;
import org.jetby.treexBuyer.storage.score.types.GlobalScore;
import org.jetby.treexBuyer.storage.score.types.PerItemScore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BuyerCommand extends AdvancedCommand {

    private final BuyerManager manager;
    private final Config config;

    public BuyerCommand(BuyerManager manager) {
        super(manager.getPlugin().getCommand("treexbuyer"), manager.getPlugin(), false);
        this.manager = manager;
        this.config = manager.getCfg();
    }

    @SubCommand({"open"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer open <menu> [player]")
    public void open(CommandSender sender, String menuName) {
        if (!GuiLoader.ALL_GUIS.containsKey(menuName)) {
            sender.sendMessage(Config.SERIALIZER.deserialize("<#EF473A>Menu not found."));
            return;
        }
        Player target = sender instanceof Player p ? p : null;
        if (target == null) {
            sender.sendMessage(Config.SERIALIZER.deserialize("<#EF473A>Specify a player for console."));
            return;
        }
        UserData user = UserData.getOrCreate(target.getUniqueId(), manager.getItems().createScore());
        new BuyerGui(target, user, GuiLoader.ALL_GUIS.get(menuName), manager).open(target);
    }

    @SubCommand({"open"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer open <menu> [player]")
    public void openFor(CommandSender sender, String menuName, Player target) {
        if (!GuiLoader.ALL_GUIS.containsKey(menuName)) {
            sender.sendMessage(Config.SERIALIZER.deserialize("<#EF473A>Menu not found."));
            return;
        }
        UserData user = UserData.getOrCreate(target.getUniqueId(), manager.getItems().createScore());
        new BuyerGui(target, user, GuiLoader.ALL_GUIS.get(menuName), manager).open(target);
    }
    @SubCommand({"test"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer open <menu> [player]")
    public void test(Player player) {
        for (String str : player.getInventory().getItemInMainHand().getLore()) {
            player.sendMessage(str);
        }
    }

    @TabComplete({"open"})
    public List<String> tabOpen(CommandSender sender, String[] args) {
        if (args.length == 1) return new ArrayList<>(GuiLoader.ALL_GUIS.keySet());
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }


    @SubCommand({"reload"})
    @Permission("treexbuyer.admin")
    public void reload(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(manager.getPlugin(), () -> {
            long start = System.currentTimeMillis();
            try {
                manager.getCfg().load();
                manager.getItems().load();
                manager.getStorage().shutdown();
                manager.getStorage().init();
                manager.getGuiLoader().loadGuis();
                if (manager.isProtocol())
                    manager.getInventoryPrice().load();
                manager.getActionBarUtil().stop();
                if (manager.getCfg().isPersistentActionbar()) {
                    manager.getActionBarUtil().start();
                }
            } catch (Exception ex) {
                Logger.error(manager.getPlugin(), "Error with config reloading: " + ex);
                sender.sendMessage(Config.SERIALIZER.deserialize("<#EF473A>Error: " + ex.getMessage()));
                return;
            }
            sender.sendMessage(Config.SERIALIZER.deserialize("<#82FB16>Reloaded in " + (System.currentTimeMillis() - start) + "ms."));
        });
    }

    @SubCommand({"score", "give"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer score give <player> <key> <amount>")
    public void scoreGive(CommandSender sender, String playerName, String[] args) {
        handleScore(sender, "give", playerName, args[0], args);
    }

    @SubCommand({"score", "take"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer score take <player> <key> <amount>")
    public void scoreTake(CommandSender sender, String playerName, String[] args) {
        handleScore(sender, "take", playerName, args[0], args);
    }

    @SubCommand({"score", "set"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer score set <player> <key> <amount>")
    public void scoreSet(CommandSender sender, String playerName, String[] args) {
        handleScore(sender, "set", playerName, args[0], args);
    }

    @TabComplete({"score", "give"})
    public List<String> tabScoreGive(CommandSender sender, String[] args) {
        return tabScoreArgs(args);
    }

    @TabComplete({"score", "take"})
    public List<String> tabScoreTake(CommandSender sender, String[] args) {
        return tabScoreArgs(args);
    }

    @TabComplete({"score", "set"})
    public List<String> tabScoreSet(CommandSender sender, String[] args) {
        return tabScoreArgs(args);
    }

    private List<String> tabScoreArgs(String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        if (args.length == 2) {
            ScoreType type = config.getType();
            if (type == ScoreType.GLOBAL) return List.of();
            if (type == ScoreType.ITEM)
                return Arrays.stream(Material.values()).map(m -> m.name().toLowerCase()).toList();
            if (type == ScoreType.CATEGORY) return new ArrayList<>(manager.getItems().getCategories().values());
        }
        return List.of();
    }

    private void handleScore(CommandSender sender, String action, String playerName, String key, String[] args) {
        ScoreType scoreType = config.getType();

        Player onlinePlayer = Bukkit.getPlayer(playerName);
        UUID uuid = onlinePlayer != null
                ? onlinePlayer.getUniqueId()
                : UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));

        double amount;

        if (scoreType == ScoreType.GLOBAL) {
            // Command: /tb score give <player> <amount>
            // key == args[0] == the amount string
            try {
                amount = Double.parseDouble(key);
                if (amount < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(Config.SERIALIZER.deserialize("<#EF473A>Amount must be a non-negative number."));
                return;
            }
        } else {
            // Command: /tb score give <player> <key> <amount>
            // key == args[0], args[1] == amount string
            key = key.toLowerCase();

            if (args.length < 2) {
                sender.sendMessage(Config.SERIALIZER.deserialize("<#EF473A>Usage: /treexbuyer score " + action + " <player> <key> <amount>"));
                return;
            }

            try {
                amount = Double.parseDouble(args[1]);
                if (amount < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(Config.SERIALIZER.deserialize("<#EF473A>Amount must be a non-negative number."));
                return;
            }

            if (scoreType == ScoreType.ITEM) {
                try {
                    Material.valueOf(key.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Config.SERIALIZER.deserialize("<#EF473A>Invalid material: " + key));
                    return;
                }
            } else if (scoreType == ScoreType.CATEGORY && !manager.getItems().getCategories().containsValue(key)) {
                sender.sendMessage(Config.SERIALIZER.deserialize("<#EF473A>Invalid category key: " + key));
                return;
            }
        }

        UserData user = UserData.getOrCreate(uuid, manager.getItems().createScore());
        Score score = user.getScore();
        String finalKey = key;
        double finalAmount = amount;

        switch (action) {
            case "give" -> {
                if (score instanceof CategoryScore s) s.add(finalKey, finalAmount);
                else if (score instanceof GlobalScore s) s.add(finalAmount);
                else if (score instanceof PerItemScore s) s.add(Material.valueOf(finalKey.toUpperCase()), finalAmount);
            }
            case "take" -> {
                if (score instanceof CategoryScore s) s.take(finalKey, finalAmount);
                else if (score instanceof GlobalScore s) s.take(finalAmount);
                else if (score instanceof PerItemScore s) s.take(Material.valueOf(finalKey.toUpperCase()), finalAmount);
            }
            case "set" -> {
                if (score instanceof CategoryScore s) s.set(finalKey, finalAmount);
                else if (score instanceof GlobalScore s) s.set(finalAmount);
                else if (score instanceof PerItemScore s) s.set(Material.valueOf(finalKey.toUpperCase()), finalAmount);
            }
        }

        sender.sendMessage(Config.SERIALIZER.deserialize("<#82FB16>Successfully " + action + "d " + finalAmount + " score for " + playerName));
    }
}