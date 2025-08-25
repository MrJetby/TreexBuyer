package me.jetby.treexBuyer.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.tools.FileLoader;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

public class JSON implements Storage {


    final File jsonFile = FileLoader.getFile("storage.json");

    private JSONObject json;
    private JSONParser parser = new JSONParser();
    private HashMap<String, Object> defaults = new HashMap<String, Object>();

    @Override
    @SuppressWarnings("unchecked")
    public boolean load() {


        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                PrintWriter pw = new PrintWriter(jsonFile, "UTF-8");
                pw.print("{");
                pw.print("}");
                pw.flush();
                pw.close();
                json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(jsonFile), "UTF-8"));

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        });

        defaults.put("MyString", "Some String");

        defaults.put("MyNumber", 1337);

        JSONObject myObject = new JSONObject();
        myObject.put("Test", "test");
        myObject.put("Test2", "test2");
        defaults.put("MyObject", myObject);

        JSONArray myArray = new JSONArray();
        myArray.add("Value1");
        myArray.add("Value2");
        defaults.put("MyArray", myArray);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean save() {
//        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance( ), storage::save);

        try {
            JSONObject toSave = new JSONObject();

            for (String s : defaults.keySet()) {
                Object o = defaults.get(s);
                if (o instanceof String) {
                    toSave.put(s, getString(s));
                } else if (o instanceof Double) {
                    toSave.put(s, getDouble(s));
                } else if (o instanceof Integer) {
                    toSave.put(s, getInteger(s));
                } else if (o instanceof JSONObject) {
                    toSave.put(s, getObject(s));
                } else if (o instanceof JSONArray) {
                    toSave.put(s, getArray(s));
                }
            }

            TreeMap<String, Object> treeMap = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
            treeMap.putAll(toSave);

            Gson g = new GsonBuilder().setPrettyPrinting().create();
            String prettyJsonString = g.toJson(treeMap);

            FileWriter fw = new FileWriter(jsonFile);
            fw.write(prettyJsonString);
            fw.flush();
            fw.close();

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public String getRawData(String key) {
        return json.containsKey(key) ? json.get(key).toString()
                : (defaults.containsKey(key) ? defaults.get(key).toString() : key);
    }

    public String getString(String key) {
        return ChatColor.translateAlternateColorCodes('&', getRawData(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.valueOf(getRawData(key));
    }

    public double getDouble(String key) {
        try {
            return Double.parseDouble(getRawData(key));
        } catch (Exception ex) {
        }
        return -1;
    }

    public double getInteger(String key) {
        try {
            return Integer.parseInt(getRawData(key));
        } catch (Exception ex) {
        }
        return -1;
    }

    public JSONObject getObject(String key) {
        return json.containsKey(key) ? (JSONObject) json.get(key)
                : (defaults.containsKey(key) ? (JSONObject) defaults.get(key) : new JSONObject());
    }

    public JSONArray getArray(String key) {
        return json.containsKey(key) ? (JSONArray) json.get(key)
                : (defaults.containsKey(key) ? (JSONArray) defaults.get(key) : new JSONArray());
    }

    @Override
    public String type() {
        return "JSON";
    }

    @Override
    public boolean playerExists(UUID uuid) {
        boolean[] status = {false};
//        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance( ), () ->{
//            status[0] = storage.has(uuid.toString() + ".score");
//        });
        return status[0];
    }

    @Override
    public void setScore(UUID uuid, String key, int score) {
//        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance( ), () ->{
//            storage.setInt(uuid.toString() + ".score", score);
//        });


    }

    @Override
    public int getScore(UUID uuid, String key) {
        final int[] score = {0};
//        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance( ), () ->{
//            score[0] = storage.getInt(uuid.toString() + ".score", 0);
//        });
        return score[0];
    }

    @Override
    public void setAutoBuyItems(UUID uuid, List<String> items) {
//        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance( ), () -> {
//            storage.setStringList(uuid.toString() + ".autoBuyItems", items);
//        });
    }

    @Override
    public List<String> getAutoBuyItems(UUID uuid) {
        final List<String>[] items = new List[]{List.of()};
//        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance( ), () -> {
//            if (storage.has(String.valueOf(uuid))) {
//                items[0] = storage.getStringList(uuid.toString() + ".autoBuyItems");
//            }
//        });
        return items[0];
    }

    @Override
    public void setAutoBuyStatus(UUID uuid, boolean status) {
//        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance( ), () -> {
//            storage.setBoolean(uuid.toString() + ".autoBuyStatus", status);
//        });
    }

    @Override
    public boolean getAutoBuyStatus(UUID uuid) {
        final boolean[] status = {false};
//        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance( ), () -> {
//            if (storage.has(String.valueOf(uuid))) {
//                status[0] = storage.getBoolean(uuid.toString() + ".autoBuyStatus", false);
//            }
//        });
        return status[0];
    }

}
