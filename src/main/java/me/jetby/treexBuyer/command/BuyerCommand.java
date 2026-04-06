package me.jetby.treexBuyer.command;

import me.jetby.libb.command.AdvancedCommand;
import me.jetby.libb.command.annotations.Permission;
import me.jetby.libb.command.annotations.SubCommand;
import me.jetby.libb.command.annotations.TabComplete;
import me.jetby.libb.command.annotations.messages.InsufficientArgs;
import me.jetby.libb.util.Logger;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.configurations.Config;
import me.jetby.treexBuyer.configurations.GuiLoader;
import me.jetby.treexBuyer.menus.BuyerGui;
import me.jetby.treexBuyer.modules.UserData;
import me.jetby.treexBuyer.storage.score.Score;
import me.jetby.treexBuyer.storage.score.ScoreType;
import me.jetby.treexBuyer.storage.score.types.CategoryScore;
import me.jetby.treexBuyer.storage.score.types.GlobalScore;
import me.jetby.treexBuyer.storage.score.types.PerItemScore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static me.jetby.treexBuyer.TreexBuyer.MM;

public class BuyerCommand extends AdvancedCommand {

    private final TreexBuyer plugin;
    private final Config config;

    public BuyerCommand(TreexBuyer plugin) {
        super(plugin.getCommand("treexbuyer"), plugin, false);
        this.plugin = plugin;
        this.config = plugin.getCfg();
    }

    @SubCommand({"open"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer open <menu> [player]")
    public void open(CommandSender sender, String menuName) {
        if (!GuiLoader.ALL_GUIS.containsKey(menuName)) {
            sender.sendMessage(MM.deserialize("<#EF473A>Menu not found."));
            return;
        }
        Player target = sender instanceof Player p ? p : null;
        if (target == null) {
            sender.sendMessage(MM.deserialize("<#EF473A>Specify a player for console."));
            return;
        }
        UserData user = UserData.getOrCreate(target.getUniqueId(), plugin.getItems().createScore());
        new BuyerGui(target, user, GuiLoader.ALL_GUIS.get(menuName), plugin).open(target);
    }

    @SubCommand({"open"})
    @Permission("treexbuyer.admin")
    @InsufficientArgs("<#EF473A>Usage: /treexbuyer open <menu> [player]")
    public void openFor(CommandSender sender, String menuName, Player target) {
        if (!GuiLoader.ALL_GUIS.containsKey(menuName)) {
            sender.sendMessage(MM.deserialize("<#EF473A>Menu not found."));
            return;
        }
        UserData user = UserData.getOrCreate(target.getUniqueId(), plugin.getItems().createScore());
        new BuyerGui(target, user, GuiLoader.ALL_GUIS.get(menuName), plugin).open(target);
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long start = System.currentTimeMillis();
            try {
                plugin.getCfg().load();
                plugin.getItems().load();
                plugin.getStorage().shutdown();
                plugin.getStorage().init();
                plugin.getGuiLoader().loadGuis();
            } catch (Exception ex) {
                Logger.error(plugin, "Error with config reloading: " + ex);
                sender.sendMessage(MM.deserialize("<#EF473A>Error: " + ex.getMessage()));
                return;
            }
            sender.sendMessage(MM.deserialize("<#82FB16>Reloaded in " + (System.currentTimeMillis() - start) + "ms."));
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
            if (type == ScoreType.CATEGORY) return new ArrayList<>(plugin.getItems().getCategories().values());
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
                sender.sendMessage(MM.deserialize("<#EF473A>Amount must be a non-negative number."));
                return;
            }
        } else {
            // Command: /tb score give <player> <key> <amount>
            // key == args[0], args[1] == amount string
            key = key.toLowerCase();

            if (args.length < 2) {
                sender.sendMessage(MM.deserialize("<#EF473A>Usage: /treexbuyer score " + action + " <player> <key> <amount>"));
                return;
            }

            try {
                amount = Double.parseDouble(args[1]);
                if (amount < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(MM.deserialize("<#EF473A>Amount must be a non-negative number."));
                return;
            }

            if (scoreType == ScoreType.ITEM) {
                try {
                    Material.valueOf(key.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(MM.deserialize("<#EF473A>Invalid material: " + key));
                    return;
                }
            } else if (scoreType == ScoreType.CATEGORY && !plugin.getItems().getCategories().containsValue(key)) {
                sender.sendMessage(MM.deserialize("<#EF473A>Invalid category key: " + key));
                return;
            }
        }

        UserData user = UserData.getOrCreate(uuid, plugin.getItems().createScore());
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

        sender.sendMessage(MM.deserialize("<#82FB16>Successfully " + action + "d " + finalAmount + " score for " + playerName));
    }
}