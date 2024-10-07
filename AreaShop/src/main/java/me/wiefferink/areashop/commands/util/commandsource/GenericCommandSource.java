package me.wiefferink.areashop.commands.util.commandsource;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class GenericCommandSource implements CommandSource<CommandSender> {

    private final CommandSourceStack sourceStack;

    private final CommandSender sender;

    public GenericCommandSource(@Nonnull CommandSourceStack sourceStack) {
        this.sourceStack = sourceStack;
        this.sender = sourceStack.getSender();
    }

    @NotNull
    @Override
    public final CommandSourceStack sourceStack() {
        return this.sourceStack;
    }

    @NotNull
    @Override
    public CommandSender sender() {
        return this.sender;
    }
}
