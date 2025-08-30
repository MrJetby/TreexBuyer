package me.jetby.treexBuyer.menus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.configurations.Items;
import me.jetby.treexBuyer.menus.commands.Command;
import me.jetby.treexBuyer.menus.requirements.ClickRequirement;
import me.jetby.treexBuyer.menus.requirements.ViewRequirement;
import me.jetby.treexBuyer.tools.Logger;
import me.jetby.treexBuyer.tools.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

@RequiredArgsConstructor
public class Loader {

    @Getter
    private final Map<String, Menu> menus = new HashMap<>();
    @Getter
    private final Map<UUID, JGui> jGui = new HashMap<>();

    private final Main plugin;
    private final File file;

    int amount = 0;

    public void load() {

        menus.clear();
        amount = 0;

        File folder = new File(file, "Menu");


        Logger.success("------------------------");
        if (!folder.exists() && folder.mkdirs()) {
            String[] defaults = {"mine.yml", "mobs.yml", "seller.yml"};

            for (String name : defaults) {
                File target = new File(folder, name);

                if (!target.exists()) {
                    plugin.saveResource("Menu/" + name, false);
                    Logger.info("The Menu/" + name + " created");
                }

                FileConfiguration config = YamlConfiguration.loadConfiguration(target);
                loadMenu(config.getString("id"), target);
            }
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.getName().endsWith(".yml")) continue;
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                loadMenu(config.getString("id", file.getName().replace(".yml", "")), file);
                Logger.info(file.getName()+" (id: "+config.getString("id")+")" + " loaded");
            }
        }
        Logger.success("------------------------");
        Logger.success(amount+" menus has been founded");
        Logger.success("------------------------");
    }

    private void loadMenu(String menuId, File file) {

        if (menus.containsKey(menuId)) {
            Logger.error("A duplicate of "+menuId+" was found");
            return;
        }
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            String title = TextUtil.colorize(config.getString("title"), true);
            InventoryType type = InventoryType.valueOf(config.getString("type", "CHEST"));
            int size = config.getInt("size", 27);
            String permission = config.getString("open_permission");
            InventoryType inventoryType = InventoryType.valueOf(config.getString("inventory", "CHEST"));
            List<String> openCommands = config.getStringList("open_commands");
            List<Button> buttons = loadButtons(config);

            amount++;
            menus.put(menuId, new Menu(menuId, title, type, size, inventoryType, permission, openCommands, buttons));
        } catch (Exception e) {
            Logger.error("Error trying to load menu: "+e.getMessage());
        }
    }

    private List<Button> loadButtons(FileConfiguration config) {
        List<Button> buttons = new ArrayList<>();
        ConfigurationSection itemsSection = config.getConfigurationSection("Items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    String displayName = itemSection.getString("display_name");
                    if (displayName!=null) displayName = TextUtil.colorize(displayName, true);
                    List<String> lore = TextUtil.colorize(itemSection.getStringList("lore"), true);
                    List<Integer> slots = parseSlots(itemSection.get("slot"));
                    int amount = itemSection.getInt("amount", 1);
                    int customModelData = itemSection.getInt("custom-model-data", 0);
                    boolean enchanted = itemSection.getBoolean("enchanted", false);
                    boolean sellZone = itemSection.getBoolean("sell-zone", false);
                    String category = itemSection.getString("category");
                    String defaultMaterial;
                    if (sellZone) {
                        defaultMaterial = "AIR";
                    } else {
                        defaultMaterial = "STONE";
                    }
                    String material = TextUtil.setPapi(null, itemSection.getString("material", defaultMaterial));
                    ItemStack itemStack;
                    if (material.startsWith("basehead-")) {
                        try {
                            itemStack = SkullCreator.itemFromBase64(material.replace("basehead-", ""));
                        } catch (Exception e) {
                            Bukkit.getLogger().warning("Error creating custom skull: " + e.getMessage());
                            itemStack = new ItemStack(SkullCreator.createSkull());
                        }
                    } else {
                        itemStack = new ItemStack(Material.valueOf(material));
                    }

                    itemStack.setAmount(amount);

                    int a = 0;
                    List<Material> items = null;

                    if (category != null) {
                        items = plugin.getItems().getCategories().get(category);
                    }
                    for (Integer slot : slots) {
                        if (category != null && items != null && !items.isEmpty()) {
                            if (a==items.size()) break;

                            Material mat = items.get(a);
                                buttons.add(new Button(key, displayName, lore, slot, amount, customModelData, enchanted, sellZone, category, new ItemStack(mat),
                                        requirements(itemSection),
                                        loadCommands(itemSection)));
                            a++;
                        } else {
                            buttons.add(new Button(key, displayName, lore, slot, amount, customModelData, enchanted, sellZone, null, itemStack,
                                    requirements(itemSection),
                                    loadCommands(itemSection)));
                        }

                    }
                }
            }
        }
        return buttons;
    }

    private List<Command> loadCommands(ConfigurationSection itemSection) {
        List<Command> commands = new ArrayList<>();

        if (itemSection.contains("left_click_commands")) {

            commands.add(new Command(false, ClickType.LEFT, TextUtil.colorize(itemSection.getStringList("left_click_commands")),
                    requirements(itemSection, "left_click_requirements", ClickType.LEFT, false)));

        }
        if (itemSection.contains("right_click_commands")) {
            commands.add(new Command(false, ClickType.RIGHT, TextUtil.colorize(itemSection.getStringList("right_click_commands")),
                    requirements(itemSection, "right_click_requirements", ClickType.RIGHT, false)));

        }
        if (itemSection.contains("shift_left_click_commands")) {

            commands.add(new Command(false, ClickType.SHIFT_LEFT, TextUtil.colorize(itemSection.getStringList("shift_left_click_commands")),
                    requirements(itemSection, "shift_left_click_requirements", ClickType.SHIFT_LEFT, false)));

        }
        if (itemSection.contains("shift_right_click_commands")) {
            commands.add(new Command(false, ClickType.SHIFT_RIGHT, TextUtil.colorize(itemSection.getStringList("shift_right_click_commands")),
                    requirements(itemSection, "shift_right_click_requirements", ClickType.SHIFT_RIGHT, false)));

        }
        if (itemSection.contains("click_commands")) {
            commands.add(new Command(true, ClickType.UNKNOWN, TextUtil.colorize(itemSection.getStringList("click_commands")),
                    requirements(itemSection, "click_requirements", ClickType.UNKNOWN, true)));

        }
        if (itemSection.contains("drop_commands")) {
            commands.add(new Command(false, ClickType.DROP, TextUtil.colorize(itemSection.getStringList("drop_commands")),
                    requirements(itemSection, "drop_requirements", ClickType.DROP, false)));
        }

        return commands;
    }

    private List<ViewRequirement> requirements(ConfigurationSection itemSection) {
        List<ViewRequirement> requirements = new ArrayList<>( );
        ConfigurationSection requirementsSection = itemSection.getConfigurationSection("view_requirements");
        if (requirementsSection == null) {
            return requirements;
        }
        for (String key : requirementsSection.getKeys(false)) {
            ConfigurationSection section = requirementsSection.getConfigurationSection(key);
            if (section == null) continue;
            requirements.add(new ViewRequirement(
                    section.getString("type"),
                    section.getString("input"),
                    section.getString("output"),
                    section.getString("permission")));
        }
        return requirements;
    }
    private List<ClickRequirement> requirements(ConfigurationSection itemSection, String name, ClickType clickType, boolean anyClick) {
        List<ClickRequirement> requirements = new ArrayList<>();
        ConfigurationSection requirementsSection = itemSection.getConfigurationSection(name);
        if (requirementsSection == null) {
            return requirements;
        }
        for (String key : requirementsSection.getKeys(false)) {
            ConfigurationSection section = requirementsSection.getConfigurationSection(key);
            if (section == null) continue;
            requirements.add(new ClickRequirement(anyClick, clickType,
                    section.getString("type"),
                    section.getString("input"),
                    section.getString("output"),
                    section.getString("permission"),
                    TextUtil.colorize(section.getStringList("deny_commands"))));
        }
        return requirements;
    }

    private List<Integer> parseSlots(Object slotObject) {
        List<Integer> slots = new ArrayList<>();

        if (slotObject instanceof Integer) {
            slots.add((Integer) slotObject);
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
            Bukkit.getLogger().warning("Unknown slot format: " + slotObject);
        }

        return slots;
    }

    private List<Integer> parseSlotString(String slotString) {
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
                Bukkit.getLogger().warning("Error parsing slot range: " + slotString);
            }
        } else {
            try {
                slots.add(Integer.parseInt(slotString));
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("Error parsing single slot: " + slotString);
            }
        }
        return slots;
    }
}
