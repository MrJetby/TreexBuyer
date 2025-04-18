package me.jetby.treexbuyer.configurations;

import me.jetby.treexbuyer.Main;
import me.jetby.treexbuyer.menu.Menu;
import me.jetby.treexbuyer.menu.MenuButton;
import me.jetby.treexbuyer.utils.SkullCreator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class MenuLoader {

    private static Map<String, Menu> listMenu = new HashMap<>();

    public static void loadMenus(FileConfiguration config, File dataFolder) {


        for (String menuId : config.getConfigurationSection("menu").getKeys(false)) {
            String filePath = config.getString("menu." + menuId + ".path");
            File menuFile = new File(dataFolder, filePath);

            if (menuFile.exists()) {
                loadMenuFromFile(menuId, menuFile);
            } else {
                Main.getInstance().getDataFolder().mkdirs();
                Main.getInstance().saveResource("Menu/"+menuId+".yml", true);
            }
        }
    }

    private static void loadMenuFromFile(String menuId, File menuFile) {

        FileConfiguration menuConfig = YamlConfiguration.loadConfiguration(menuFile);



        String titleMenu = menuConfig.getString("title");
        int size = menuConfig.getInt("size");
        List<String> commandOpenMenu = menuConfig.getStringList("open_commands");
        String permissionOpenMenu = menuConfig.getString("open_permission");


        List<MenuButton> buttons = new ArrayList<>();
        if (menuConfig.contains("Items")) {
            for (String buttonKey : menuConfig.getConfigurationSection("Items").getKeys(false)) {
                String materialName = menuConfig.getString("Items." + buttonKey + ".material");

                Object slotString = menuConfig.get("Items." + buttonKey + ".slot");
                List<Integer> slots = parseSlots(slotString); // Используем parseSlots для обработки слотов


                String titleButton = menuConfig.getString("Items." + buttonKey + ".display_name");
                List<String> loreButton = menuConfig.getStringList("Items." + buttonKey + ".lore");

                boolean hide_enchantments = menuConfig.getBoolean("Items." + buttonKey + ".hide_enchantments", false);
                boolean hide_attributes = menuConfig.getBoolean("Items." + buttonKey + ".hide_attributes", false);
                boolean enchanted = menuConfig.getBoolean("Items." + buttonKey + ".enchanted", false);

                Map<ClickType, List<String>> commandsMap = new HashMap<>();

                addCommandsToMap(commandsMap, ClickType.LEFT, menuConfig.getStringList("Items." + buttonKey + ".left_click_commands"));
                addCommandsToMap(commandsMap, ClickType.RIGHT, menuConfig.getStringList("Items." + buttonKey + ".right_click_commands"));
                addCommandsToMap(commandsMap, ClickType.MIDDLE, menuConfig.getStringList("Items." + buttonKey + ".middle_click_commands"));
                addCommandsToMap(commandsMap, ClickType.SHIFT_LEFT, menuConfig.getStringList("Items." + buttonKey + ".shift_left_click_commands"));
                addCommandsToMap(commandsMap, ClickType.SHIFT_RIGHT, menuConfig.getStringList("Items." + buttonKey + ".shift_right_click_commands"));
                addCommandsToMap(commandsMap, ClickType.DROP, menuConfig.getStringList("Items." + buttonKey + ".drop_commands"));

                List<String> oldCommands = menuConfig.getStringList("Items." + buttonKey + ".click_commands");
                if (!oldCommands.isEmpty()) {

                    if (!commandsMap.containsKey(ClickType.LEFT)) {
                        addCommandsToMap(commandsMap, ClickType.LEFT, oldCommands);
                    }
                    if (!commandsMap.containsKey(ClickType.RIGHT)) {
                        addCommandsToMap(commandsMap, ClickType.RIGHT, oldCommands);
                    }
                    if (!commandsMap.containsKey(ClickType.SHIFT_LEFT)) {
                        addCommandsToMap(commandsMap, ClickType.SHIFT_LEFT, oldCommands);
                    }
                    if (!commandsMap.containsKey(ClickType.SHIFT_RIGHT)) {
                        addCommandsToMap(commandsMap, ClickType.SHIFT_RIGHT, oldCommands);
                    }
                    if (!commandsMap.containsKey(ClickType.MIDDLE)) {
                        addCommandsToMap(commandsMap, ClickType.MIDDLE, oldCommands);
                    }
                    if (!commandsMap.containsKey(ClickType.DROP)) {
                        addCommandsToMap(commandsMap, ClickType.DROP, oldCommands);
                    }

                }

                for (int slot : slots) {
                    MenuButton menuButton = new MenuButton(slot,
                            titleButton,
                            loreButton,
                            materialName,
                            commandsMap,
                            oldCommands,
                            hide_enchantments,
                            hide_attributes,
                            enchanted);
                    buttons.add(menuButton);
                }
            }
        }

        Menu menu = new Menu(menuId, titleMenu, size, commandOpenMenu, permissionOpenMenu, buttons);
        listMenu.put(menuId, menu);
    }

    private static void addCommandsToMap(Map<ClickType, List<String>> map, ClickType clickType, List<String> commands) {
        if (commands != null && !commands.isEmpty()) {
            map.put(clickType, commands);
        }
    }

    public static List<Integer> parseSlots(Object slotObject) {
        List<Integer> slots = new ArrayList<>();

        if (slotObject instanceof Integer) {
            slots.add((Integer) slotObject); // Одиночное число
        } else if (slotObject instanceof String) {
            String slotString = ((String) slotObject).trim();
            slots.addAll(parseSlotString(slotString));
        } else if (slotObject instanceof List<?>) {
            for (Object obj : (List<?>) slotObject) {
                if (obj instanceof Integer) {
                    slots.add((Integer) obj);
                } else if (obj instanceof String) {
                    slots.addAll(parseSlotString((String) obj));
                }
            }
        } else {
            Bukkit.getLogger().warning("Неизвестный формат слотов: " + slotObject);
        }

        return slots;
    }

    private static List<Integer> parseSlotString(String slotString) {
        List<Integer> slots = new ArrayList<>();
        if (slotString.contains("-")) {
            try {
                String[] range = slotString.split("-");
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                for (int i = start; i <= end; i++) {
                    slots.add(i);
                }
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("Ошибка парсинга диапазона слотов: " + slotString);
            }
        } else {
            try {
                slots.add(Integer.parseInt(slotString));
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("Ошибка парсинга одиночного слота: " + slotString);
            }
        }
        return slots;
    }


    public static Map<String, Menu> getListMenu() {
        return listMenu;
    }
}
