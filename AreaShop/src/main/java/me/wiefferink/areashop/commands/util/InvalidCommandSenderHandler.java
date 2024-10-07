package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.commands.util.commandsource.PlayerCommandSource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.exception.InvalidCommandSenderException;
import org.incendo.cloud.exception.handling.ExceptionContext;
import org.incendo.cloud.exception.handling.ExceptionHandler;

import javax.annotation.Nonnull;

public class InvalidCommandSenderHandler implements ExceptionHandler<CommandSource<?>, InvalidCommandSenderException> {

    private final MessageBridge messageBridge;

    public InvalidCommandSenderHandler(@Nonnull MessageBridge messageBridge) {
        this.messageBridge = messageBridge;
    }

    @Override
    public void handle(@NonNull ExceptionContext<CommandSource<?>, InvalidCommandSenderException> context) throws Throwable {
        InvalidCommandSenderException exception = context.exception();
        if (exception.requiredSenderTypes().contains(PlayerCommandSource.class)) {
            this.messageBridge.message(exception.commandSender(), "cmd-onlyByPlayer");
            return;
        }
        throw exception;
    }
}
