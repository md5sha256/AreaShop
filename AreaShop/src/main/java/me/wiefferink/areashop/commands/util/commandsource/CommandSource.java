package me.wiefferink.areashop.commands.util.commandsource;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;

public interface CommandSource<C extends CommandSender> {

    @Nonnull
    CommandSourceStack sourceStack();

    @Nonnull
    C sender();

}
