package me.jetby.treexBuyer.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {
    boolean autoBuy = false;
    List<String> autoBuyItems = new ArrayList<>( );
    Map<String, Integer> scores = new ConcurrentHashMap<>();

    PlayerData(boolean autoBuy, List<String> autoBuyItems, Map<String, Integer> scores) {
        this.autoBuy = autoBuy;
        this.autoBuyItems = new ArrayList<>(autoBuyItems);
        this.scores = new ConcurrentHashMap<>(scores);
    }

    PlayerData(List<String> autoBuyItems) {
        this.autoBuyItems = new ArrayList<>(autoBuyItems);
    }

    PlayerData() {
    }
}