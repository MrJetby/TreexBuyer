
package me.jetby.treexBuyer.storage;

import java.util.UUID;
import me.jetby.treexBuyer.TreexBuyer;
import me.jetby.treexBuyer.modules.UserData;
import me.jetby.treexBuyer.storage.Storage;

public abstract class CachedStorage
implements Storage {
    protected final TreexBuyer plugin;

    public CachedStorage(TreexBuyer plugin) {
        this.plugin = plugin;
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

