package me.jetby.treexBuyer.menus;

import me.jetby.treexBuyer.menus.commands.Command;
import me.jetby.treexBuyer.menus.requirements.ViewRequirement;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public record Button(

        String id,
        String displayName,
        List<String> lore,
        int slot,
        int amount,
        int customModelData,
        boolean enchanted,
        boolean sellZone,
        String category,
        ItemStack itemStack,
        List<ViewRequirement> viewRequirements,
        List<Command> commands

) {
}
