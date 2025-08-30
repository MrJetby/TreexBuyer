package me.jetby.treexBuyer.menus;

import me.jetby.treexBuyer.Main;
import org.bukkit.command.*;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

public class CommandRegistrar extends BukkitCommand implements CommandExecutor {

    private final Main plugin;
    private final String menuId;

    private static final Map<String, BukkitCommand> registeredCommands = new HashMap<>();

    public CommandRegistrar(Main plugin, String menuId, String commandName) {
        super(commandName);
        this.plugin = plugin;
        this.menuId = menuId;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("The command is available only to players.");
            return true;
        }
        new JGui(plugin.getMenuLoader().getMenus().get(menuId), plugin, player).open(player);
        return true;
    }

    public static void createCommands(Main plugin) {
        Map<String, List<String>> commands = new HashMap<>();
        plugin.getMenuLoader().getMenus().forEach((key, item) -> commands.put(key, item.openCommands()));

        for (String menuId : commands.keySet()) {
            for (String command : commands.get(menuId)) {
                registerCommand(plugin, command, new CommandRegistrar(plugin, menuId, command));
            }
        }
    }

    private static void registerCommand(Main plugin, String commandName, @NotNull CommandExecutor executor) {
        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            BukkitCommand command = new BukkitCommand(commandName) {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
                    return executor.onCommand(sender, this, label, args);
                }
            };

            command.setAliases(Collections.emptyList());

            unregisterCommand(plugin, commandName, commandMap);

            commandMap.register(plugin.getName(), command);
            registeredCommands.put(commandName.toLowerCase(), command);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            plugin.getLogger().log(Level.WARNING, "Error with command registration", e);
        }
    }


    @SuppressWarnings("unchecked")
    private static void unregisterCommand(Main plugin, String commandName, CommandMap commandMap) {
        try {
            Field knownCommandsField;
            try {
                knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            } catch (NoSuchFieldException e) {
                knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            }

            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            Command existing = knownCommands.remove(commandName.toLowerCase());
            if (existing != null) {
                existing.unregister(commandMap);
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error with command unregistration", e);
        }
    }


    public static void unregisterAll(Main plugin) {
        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            for (BukkitCommand cmd : registeredCommands.values()) {
                cmd.unregister(commandMap);
            }
            registeredCommands.clear();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error with commands unregistration", e);
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        return false;
    }
}
