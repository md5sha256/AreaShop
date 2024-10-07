package me.wiefferink.areashop.commands.util.commandsource;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public sealed class EntityCommandSource extends GenericCommandSource permits PlayerCommandSource {

    public EntityCommandSource(@Nonnull CommandSourceStack sourceStack) {
        super(sourceStack);
        if (!(sourceStack.getSender() instanceof Entity)) {
            throw new IllegalArgumentException("Source stack sender is not an entity!");
        }
    }

    @NotNull
    @Override
    public Entity sender() {
        return (Entity) super.sender();
    }
}
