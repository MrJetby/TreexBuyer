package me.jetby.treexBuyer.command;

import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.configurations.Config;
import me.jetby.treexBuyer.menus.CommandRegistrar;
import me.jetby.treexBuyer.menus.JGui;
import me.jetby.treexBuyer.storage.Storage;
import me.jetby.treexBuyer.tools.Logger;
import me.jetby.treexBuyer.tools.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final Storage storage;
    private final Config config;

    public AdminCommand(Main plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorage();
        this.config = plugin.getCfg();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String[] args) {

        if (args.length == 0) {
            // help сообщение
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "open": {
                handleOpen(sender, args);
                break;
            }
            case "reload": {
                reload(sender);
                break;
            }
            case "score": {
                if (args.length < 2) {
                    sender.sendMessage(TextUtil.colorize("&#EF473AUsage: /treexbuyer score <give/take/set> <player> [key] <amount>"));
                    break;
                }
                handleScore(sender, args);
                break;
            }
            default: {
                sender.sendMessage(TextUtil.colorize("&#EF473AUnknown subcommand."));
            }
        }

        return false;
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(TextUtil.colorize("&#EF473AUsage: /treexbuyer open <menu> [player]"));
            return;
        }
        Player target;
        if (args.length == 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(TextUtil.colorize("&#EF473ASpecify player for console."));
                return;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(TextUtil.colorize("&#EF473APlayer not found."));
                return;
            }
        }
        if (!plugin.getMenuLoader().getMenus().containsKey(args[1])) {
            sender.sendMessage(TextUtil.colorize("&#EF473AMenu not found."));
            return;
        }
        new JGui(plugin.getMenuLoader().getMenus().get(args[1]), plugin, target).open(target);
    }

    private void handleScore(CommandSender sender, String[] args) {
        String action = args[1].toLowerCase();
        if (!action.equals("give") && !action.equals("take") && !action.equals("set")) {
            sender.sendMessage(TextUtil.colorize("&#EF473AInvalid action. Use give/take/set."));
            return;
        }

        Config.ScoreType scoreType = config.getType();
        int minArgs = scoreType == Config.ScoreType.GLOBAL ? 4 : 5;
        if (args.length < minArgs) {
            String usage = scoreType == Config.ScoreType.GLOBAL ?
                    "&#EF473AUsage: /treexbuyer score " + action + " <player> <amount>" :
                    "&#EF473AUsage: /treexbuyer score " + action + " <player> <key> <amount>";
            sender.sendMessage(TextUtil.colorize(usage));
            return;
        }

        String playerName = args[2];
        UUID uuid;
        Player player = Bukkit.getPlayer(playerName);
        if (player==null) {
            String string = "OfflinePlayer:" + playerName;
            uuid = UUID.nameUUIDFromBytes(string.getBytes(StandardCharsets.UTF_8));
        } else {
            uuid = player.getUniqueId();
        }

        String key;
        String amountStr;
        if (scoreType == Config.ScoreType.GLOBAL) {
            key = "global";
            amountStr = args[3];
        } else {
            key = args[3].toLowerCase();
            amountStr = args[4];
        }

        if (scoreType == Config.ScoreType.ITEM) {
            try {
                Material.valueOf(key.toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(TextUtil.colorize("&#EF473AInvalid itemStack key: " + key));
                return;
            }
        } else if (scoreType == Config.ScoreType.CATEGORY) {
            if (!plugin.getItems().getCategories().containsKey(key)) {
                sender.sendMessage(TextUtil.colorize("&#EF473AInvalid category key: " + key));
                return;
            }
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(TextUtil.colorize("&#EF473AAmount must be a non-negative integer."));
            return;
        }

        int currentScore = storage.getScore(uuid, key);

        switch (action) {
            case "give": {
                storage.setScore(uuid, key, storage.getScore(uuid, key)+amount);
                sender.sendMessage(TextUtil.colorize("&#82FB16Successfully given " + amount + " scores to " + playerName + " for key " + key));
                break;
            }
            case "take": {
                int newScore = Math.max(currentScore - amount, 0);
                storage.setScore(uuid, key, newScore);
                sender.sendMessage(TextUtil.colorize("&#82FB16Successfully taken " + amount + " scores from " + playerName + " for key " + key));
                break;
            }
            case "set": {
                storage.setScore(uuid, key, amount);
                sender.sendMessage(TextUtil.colorize("&#82FB16Successfully set " + amount + " scores for " + playerName + " for key " + key));
                break;
            }
        }
    }

    private void reload(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long start = System.currentTimeMillis();

            try {
                plugin.getCfg().load();
                plugin.getItems().load();

                for (UUID uuid : plugin.getMenuLoader().getJGui().keySet()) {
                    JGui jGui = plugin.getMenuLoader().getJGui().get(uuid);
                    Bukkit.getScheduler().runTask(plugin, () -> jGui.close());
                }
                plugin.getMenuLoader().load();

                plugin.getAutoBuy().stop();
                plugin.getAutoBuy().start();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    CommandRegistrar.unregisterAll(plugin);
                    CommandRegistrar.createCommands(plugin);
                });

            } catch (Exception ex) {
                Logger.error("Error with config reloading: " + ex);
                if (sender instanceof Player) sender.sendMessage(TextUtil.colorize("&#EF473AError with config reloading: "+ex));

                return;
            }

            sender.sendMessage(TextUtil.colorize("&#82FB16Successfully reloaded, took only " + (System.currentTimeMillis() - start) + " ms."));
        });
    }

    static final List<String> completions = new ArrayList<>();

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        completions.clear();

        if (args.length == 1) {
            completions.add("open");
            completions.add("score");
            completions.add("reload");
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("open")) {
                completions.addAll(plugin.getMenuLoader().getMenus().keySet());
            } else if (args[0].equalsIgnoreCase("score")) {
                completions.add("give");
                completions.add("take");
                completions.add("set");
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("open")) {
            completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("score") && args[1].matches("(?i)give|take|set")) {
            completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("score") && args[1].matches("(?i)give|take|set")) {
            Config.ScoreType type = config.getType();
            if (type != Config.ScoreType.GLOBAL) {
                if (type == Config.ScoreType.ITEM) {
                    completions.addAll(java.util.Arrays.stream(Material.values()).map(Material::name).map(String::toLowerCase).collect(Collectors.toList()));
                } else if (type == Config.ScoreType.CATEGORY) {
                    completions.addAll(plugin.getItems().getCategories().keySet());
                }
            }
        }

        return completions.stream()
                .filter(comp -> comp.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}