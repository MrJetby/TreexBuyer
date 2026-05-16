package org.jetby.treexBuyer.storage;

import org.jetby.treexBuyer.BuyerManager;
import org.jetby.treexBuyer.modules.UserData;

import java.util.UUID;

public abstract class CachedStorage implements Storage {
    protected final BuyerManager manager;

    public CachedStorage(BuyerManager manager) {
        this.manager = manager;
    }

    @Override
    public void onPlayerJoin(UUID uuid) {
        if (!UserData.USERDATA_LIST.containsKey(uuid)) {
            UserData.USERDATA_LIST.put(uuid, this.loadUser(uuid));
        }
    }

    @Override
    public void onPlayerQuit(UUID uuid) {
        this.saveUser(uuid);
        UserData.USERDATA_LIST.remove(uuid);
    }

    @Override
    public void shutdown() {
        UserData.USERDATA_LIST.keySet().forEach(this::saveUser);
    }
}

