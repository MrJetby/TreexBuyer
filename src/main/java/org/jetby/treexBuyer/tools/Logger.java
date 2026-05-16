package org.jetby.treexBuyer.tools;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;

public class Logger {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static void info(Plugin plugin, String message) {
        plugin.getComponentLogger().info(MINI_MESSAGE.deserialize(message));
    }

    public static void info(Plugin plugin, Component message) {
        plugin.getComponentLogger().info(message);
    }

    public static void warn(Plugin plugin, String message) {
        plugin.getComponentLogger().warn(MINI_MESSAGE.deserialize(message));
    }

    public static void warn(Plugin plugin, String message, Object... objects) {
        plugin.getComponentLogger().warn(MINI_MESSAGE.deserialize(message), objects);
    }

    public static void warn(Plugin plugin, Component message) {
        plugin.getComponentLogger().warn(message);
    }

    public static void debug(Plugin plugin, String message) {
        plugin.getComponentLogger().debug(MINI_MESSAGE.deserialize(message));
    }

    public static void debug(Plugin plugin, Component message) {
        plugin.getComponentLogger().debug(message);
    }

    public static void error(Plugin plugin, String message) {
        plugin.getComponentLogger().error(MINI_MESSAGE.deserialize(message));
    }

    public static void error(Plugin plugin, Component message) {
        plugin.getComponentLogger().error(message);
    }

    public static void error(Plugin plugin, String message, Object... objects) {
        plugin.getComponentLogger().error(MINI_MESSAGE.deserialize(message), objects);
    }

    public static void trace(Plugin plugin, String message) {
        plugin.getComponentLogger().trace(MINI_MESSAGE.deserialize(message));
    }

    public static void trace(Plugin plugin, Component message) {
        plugin.getComponentLogger().trace(message);
    }
}
