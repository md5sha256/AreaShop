package me.wiefferink.areashop.commands.util.commandsource;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public final class PlayerCommandSource extends EntityCommandSource {

    public PlayerCommandSource(@Nonnull CommandSourceStack sourceStack) {
        super(sourceStack);
        if (!(sourceStack.getSender() instanceof Player)) {
            throw new IllegalArgumentException("Source stack sender is not a player!");
        }
    }

    @Override
    public @NotNull Player sender() {
        return (Player) super.sender();
    }
}
