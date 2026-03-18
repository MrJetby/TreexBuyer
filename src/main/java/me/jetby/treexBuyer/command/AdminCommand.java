package me.jetby.treexBuyer.command;

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
import me.jetby.treexBuyer.tools.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.jetby.treexBuyer.TreexBuyer.MM;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final TreexBuyer plugin;
    private final Config config;

    public AdminCommand(TreexBuyer plugin) {
        this.plugin = plugin;
        this.config = plugin.getCfg();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) return true;

        switch (args[0].toLowerCase()) {
            case "open"   -> handleOpen(sender, args);
            case "reload" -> reload(sender);
            case "score"  -> {
                if (args.length < 2) {
                    sender.sendMessage(MM.deserialize("<#EF473A>Usage: /treexbuyer score <give/take/set> <player> [key] <amount>"));
                } else {
                    handleScore(sender, args);
                }
            }
            default -> sender.sendMessage(MM.deserialize("<#EF473A>Unknown subcommand."));
        }
        return true;
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MM.deserialize("<#EF473A>Usage: /treexbuyer open <menu> [player]"));
            return;
        }

        Player target;
        if (args.length == 2) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(MM.deserialize("<#EF473A>Specify player for console."));
                return;
            }
            target = p;
        } else {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(MM.deserialize("<#EF473A>Player not found."));
                return;
            }
        }

        if (!GuiLoader.ALL_GUIS.containsKey(args[1])) {
            sender.sendMessage(MM.deserialize("<#EF473A>Menu not found."));
            return;
        }

        UserData user = UserData.getOrCreate(target.getUniqueId(), plugin.getItems().createScore());
        new BuyerGui(target, user, GuiLoader.ALL_GUIS.get(args[1]), plugin).open(target);
    }

    private void handleScore(CommandSender sender, String[] args) {
        String action = args[1].toLowerCase();
        if (!action.equals("give") && !action.equals("take") && !action.equals("set")) {
            sender.sendMessage(MM.deserialize("<#EF473A>Invalid action. Use give/take/set."));
            return;
        }

        ScoreType scoreType = config.getType();
        int minArgs = scoreType == ScoreType.GLOBAL ? 4 : 5;
        if (args.length < minArgs) {
            String usage = scoreType == ScoreType.GLOBAL
                    ? "<#EF473A>Usage: /treexbuyer score " + action + " <player> <amount>"
                    : "<#EF473A>Usage: /treexbuyer score " + action + " <player> <key> <amount>";
            sender.sendMessage(MM.deserialize(usage));
            return;
        }

        String playerName = args[2];
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        UUID uuid = onlinePlayer != null
                ? onlinePlayer.getUniqueId()
                : UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));

        String key = scoreType == ScoreType.GLOBAL ? "global" : args[3].toLowerCase();
        String amountStr = scoreType == ScoreType.GLOBAL ? args[3] : args[4];

        if (scoreType == ScoreType.ITEM) {
            try { Material.valueOf(key.toUpperCase()); }
            catch (IllegalArgumentException e) {
                sender.sendMessage(MM.deserialize("<#EF473A>Invalid material: " + key));
                return;
            }
        } else if (scoreType == ScoreType.CATEGORY && !plugin.getItems().getCategories().containsValue(key)) {
            sender.sendMessage(MM.deserialize("<#EF473A>Invalid category key: " + key));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(MM.deserialize("<#EF473A>Amount must be a non-negative number."));
            return;
        }

        UserData user = UserData.getOrCreate(uuid, plugin.getItems().createScore());
        Score score = user.getScore();
        String finalKey = key;

        switch (action) {
            case "give" -> {
                if (score instanceof CategoryScore s) s.add(finalKey, amount);
                else if (score instanceof GlobalScore s) s.add(amount);
                else if (score instanceof PerItemScore s) s.add(Material.valueOf(finalKey.toUpperCase()), amount);
            }
            case "take" -> {
                if (score instanceof CategoryScore s) s.take(finalKey, amount);
                else if (score instanceof GlobalScore s) s.take(amount);
                else if (score instanceof PerItemScore s) s.take(Material.valueOf(finalKey.toUpperCase()), amount);
            }
            case "set" -> {
                if (score instanceof CategoryScore s) s.set(finalKey, amount);
                else if (score instanceof GlobalScore s) s.set(amount);
                else if (score instanceof PerItemScore s) s.set(Material.valueOf(finalKey.toUpperCase()), amount);
            }
        }

        sender.sendMessage(MM.deserialize("<#82FB16>Successfully " + action + " " + amount + " scores for " + playerName + " (key: " + key + ")"));
    }

    private void reload(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long start = System.currentTimeMillis();
            try {
                plugin.getCfg().load();
                plugin.getItems().load();
            } catch (Exception ex) {
                Logger.error("Error with config reloading: " + ex);
                sender.sendMessage(MM.deserialize("<#EF473A>Error: " + ex.getMessage()));
                return;
            }
            sender.sendMessage(MM.deserialize("<#82FB16>Reloaded in " + (System.currentTimeMillis() - start) + "ms."));
        });
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("open", "score", "reload"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("open")) completions.addAll(GuiLoader.ALL_GUIS.keySet());
            else if (args[0].equalsIgnoreCase("score")) completions.addAll(List.of("give", "take", "set"));
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("open") || (args[0].equalsIgnoreCase("score") && args[1].matches("(?i)give|take|set")))
                Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(completions::add);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("score") && args[1].matches("(?i)give|take|set")) {
            ScoreType type = config.getType();
            if (type == ScoreType.ITEM)
                Arrays.stream(Material.values()).map(m -> m.name().toLowerCase()).forEach(completions::add);
            else if (type == ScoreType.CATEGORY)
                completions.addAll(plugin.getItems().getCategories().values());
        }

        String filter = args[args.length - 1].toLowerCase();
        return completions.stream().filter(c -> c.toLowerCase().startsWith(filter)).collect(Collectors.toList());
    }
}