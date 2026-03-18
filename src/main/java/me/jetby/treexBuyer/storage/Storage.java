package me.jetby.treexBuyer.storage;

import me.jetby.treexBuyer.modules.UserData;

import java.util.UUID;

public interface Storage {
    void init();
    void shutdown();
    UserData loadUser(UUID uuid);
    void saveUser(UUID uuid);
    void deleteUser(UUID uuid);
    void onPlayerJoin(UUID uuid);
    void onPlayerQuit(UUID uuid);
}