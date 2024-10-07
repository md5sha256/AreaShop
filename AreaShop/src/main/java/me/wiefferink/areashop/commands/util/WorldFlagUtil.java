package me.wiefferink.areashop.commands.util;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.incendo.cloud.bukkit.parser.WorldParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;

public final class WorldFlagUtil {

    public static final CommandFlag<World> DEFAULT_WORLD_FLAG = CommandFlag.builder("world")
            .withComponent(WorldParser.worldParser())
            .build();

    private WorldFlagUtil() {
        throw new IllegalStateException("Cannot instantiate static util class");
    }

    @Nonnull
    public static World parseOrDetectWorld(@Nonnull CommandContext<?> context, @Nonnull Entity sender) {
        return parseOrDetectWorld(context, sender, DEFAULT_WORLD_FLAG);
    }

    @Nonnull
    public static World parseOrDetectWorld(
            @Nonnull CommandContext<?> context,
            @Nonnull Entity sender,
            @Nonnull CommandFlag<World> flag
    ) {
        World world = context.flags().get(flag);
        if (world != null) {
            return world;
        }
        return sender.getWorld();
    }

}
