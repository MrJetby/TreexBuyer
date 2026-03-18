package me.jetby.treexBuyer.tools;

import org.bukkit.Bukkit;

public final class Logger {

    private Logger() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void warn(String message) {
        Bukkit.getConsoleSender().sendMessage("§e[TreexBuyer] §e" + message);
    }

    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage("§a[TreexBuyer] §f" + message);
    }

    public static void success(String message) {
        Bukkit.getConsoleSender().sendMessage("§a[TreexBuyer] §a" + message);
    }

    public static void error(String message) {
        Bukkit.getConsoleSender().sendMessage("§c[TreexBuyer] §c" + message);
    }

    public static void msg(String message) {
        Bukkit.getConsoleSender().sendMessage("§6[TreexBuyer] §f" + message);
    }
}