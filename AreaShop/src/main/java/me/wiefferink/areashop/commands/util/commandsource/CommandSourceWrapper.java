package me.wiefferink.areashop.commands.util.commandsource;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;

public record CommandSourceWrapper(@Nonnull CommandSourceStack commandSourceStack) {

    @Nonnull
    public CommandSender sender() {
        return commandSourceStack().getSender();
    }

}
