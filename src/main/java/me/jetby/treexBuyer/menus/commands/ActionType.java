package me.jetby.treexBuyer.menus.commands;

import lombok.Getter;
import me.jetby.treexBuyer.Main;
import me.jetby.treexBuyer.menus.commands.impl.buyer.*;
import me.jetby.treexBuyer.menus.commands.impl.standard.*;
import org.jetbrains.annotations.Nullable;

@Getter
public enum ActionType {

    MESSAGE(new Message()),
    SOUND(new Sound()),
    EFFECT(new Effect()),
    ACTIONBAR(new ActionBar()),
    TITLE(new Title()),
    CONSOLE(new Console()),
    PLAYER(new Player()),
    BROADCASTMESSAGE(new BroadcastMessage()),
    BROADCAST_MESSAGE(new BroadcastMessage()),
    BROADCASTSOUND(new BroadcastSound()),
    BROADCAST_SOUND(new BroadcastSound()),
    BROADCASTTITLE(new BroadcastTitle()),
    BROADCAST_TITLE(new BroadcastTitle()),
    BROADCASTACTIONBAR(new BroadcastActionBar()),
    CLOSE(new CloseMenu()),
    SELL_ITEM(new ItemSell(Main.getInstance())),
    SELL_ALL(new SellAll(Main.getInstance())),
    ENABLE_ALL(new EnableALL(Main.getInstance())),
    DISABLE_ALL(new DisableALL(Main.getInstance())),
    AUTOBUY_STATUS_TOGGLE(new AutoBuyStatusToggle(Main.getInstance())),
    AUTOBUY_ITEM_TOGGLE(new AutoBuyItemToggle(Main.getInstance())),
    OPEN(new OpenMenu(Main.getInstance(), Main.getInstance().getMenuLoader())),
    OPEN_MENU(new OpenMenu(Main.getInstance(), Main.getInstance().getMenuLoader())),
    BROADCAST_ACTIONBAR(new BroadcastActionBar());

    private final Action action;

    ActionType(Action action) {
        this.action = action;
    }

    @Nullable
    public static ActionType getType(String name) {
        if (name == null) return null;

        try {
            return ActionType.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
