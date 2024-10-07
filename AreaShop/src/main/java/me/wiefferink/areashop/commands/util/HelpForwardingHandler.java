package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.interactivemessenger.source.LanguageManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.exception.CommandParseException;
import org.incendo.cloud.exception.handling.ExceptionContext;
import org.incendo.cloud.exception.handling.ExceptionHandler;

public class HelpForwardingHandler<C> implements ExceptionHandler<C, CommandParseException> {

    private final LanguageManager languageManager;
    private final MessageBridge messageBridge;

    public HelpForwardingHandler(@NonNull MessageBridge messageBridge, @NonNull LanguageManager languageManager) {
        this.languageManager = languageManager;
        this.messageBridge = messageBridge;
    }

    @Override
    public void handle(@NonNull ExceptionContext<C, CommandParseException> context) throws Throwable {
        CommandParseException exception = context.exception();
        // walk back to the first literal
        CommandComponent<?> literal = null;
        for (CommandComponent<?> component : exception.currentChain()) {
            if (component.type() == CommandComponent.ComponentType.LITERAL) {
                literal = component;
                break;
            }
        }
        // If we can't find the label, rethrow the exception
        if (literal == null) {
            throw exception;
        }
        String name = literal.name();
        String helpKey = name + "-help";
        if (languageManager.getMessage(helpKey).isEmpty()) {
            // We don't have a help message, rethrow the exception
            throw exception;
        }
        this.messageBridge.message(context.context().sender(), helpKey);
    }
}
