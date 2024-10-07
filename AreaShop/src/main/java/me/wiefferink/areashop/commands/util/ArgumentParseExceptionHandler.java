package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import org.incendo.cloud.exception.handling.ExceptionContext;
import org.incendo.cloud.exception.handling.ExceptionHandler;

import javax.annotation.Nonnull;

public class ArgumentParseExceptionHandler<C extends CommandSource<?>> implements ExceptionHandler<C, AreaShopCommandException> {

    private final MessageBridge messageBridge;

    public ArgumentParseExceptionHandler(@Nonnull MessageBridge messageBridge) {
        this.messageBridge = messageBridge;
    }

    public static void handleException(
            @Nonnull MessageBridge messageBridge,
            @Nonnull CommandSource<?> sender,
            @Nonnull AreaShopCommandException exception
    ) {
        String key = exception.messageKey();
        Object[] replacements = exception.replacements();
        if (replacements.length == 0) {
            messageBridge.message(sender.sender(), key);
            return;
        }
        // Pass the values as a var-args and not as a string[]
        messageBridge.message(sender.sender(), key, replacements);
    }

    @Override
    public void handle(@Nonnull ExceptionContext<C, AreaShopCommandException> context) {
        handleException(this.messageBridge, context.context().sender(), context.exception());
    }
}
