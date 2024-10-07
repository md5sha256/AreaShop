package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.parser.ParserException;

import javax.annotation.Nonnull;

public record WorldSelection(@Nonnull World world, @Nonnull WorldEditSelection selection) {

    public static WorldSelection fromPlayer(@Nonnull Player player,
                                            @Nonnull WorldEditInterface worldEditInterface) throws ParserException {
        WorldEditSelection selection = worldEditInterface.getPlayerSelection(player);
        if (selection == null) {
            throw new AreaShopCommandException("cmd-noSelection");
        }
        World world = selection.getWorld();
        return new WorldSelection(world, selection);
    }

}