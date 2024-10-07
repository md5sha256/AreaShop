package me.wiefferink.areashop.commands.util.commandsource;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.SenderMapper;

public class CommandSourceMapper implements SenderMapper<CommandSourceStack, CommandSource<?>> {

    @Override
    public @NonNull CommandSource<?> map(@NonNull CommandSourceStack base) {
        CommandSender sender = base.getSender();
        if (sender instanceof Player) {
            return new PlayerCommandSource(base);
        } else if (sender instanceof Entity) {
            return new EntityCommandSource(base);
        }
        return new GenericCommandSource(base);
    }

    @Override
    public @NonNull CommandSourceStack reverse(@NonNull CommandSource<?> mapped) {
        return mapped.sourceStack();
    }
}
