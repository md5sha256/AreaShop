package me.wiefferink.areashop.commands.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.Nonnull;

public class AreaShopCommandException extends RuntimeException {

    private static final Object[] EMPTY = new Object[0];

    private final String messageKey;
    private final Object[] replacements;

    public AreaShopCommandException(@Nonnull String messageKey, @Nullable Object... replacements) {
        this.messageKey = messageKey;
        this.replacements = replacements == null ? EMPTY : replacements;
    }

    public String messageKey() {
        return this.messageKey;
    }

    public Object[] replacements() {
        return this.replacements;
    }

    @Override
    public String getMessage() {
        return this.messageKey;
    }
}
