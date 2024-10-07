package me.wiefferink.areashop.commands.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class ParserWrapper<C, T> implements ArgumentParser<C, T> {

    private final SuggestionProvider<C> suggestionProvider;
    private final ParserDescriptor<C, T> parser;

    public static <C, T> ParserDescriptor<C, T> wrap(@Nonnull ParserDescriptor<C, T> parser, @Nonnull SuggestionProvider<C> suggestionProvider) {
        ArgumentParser<C, T> wrapped = new ParserWrapper<>(parser, suggestionProvider);
        return ParserDescriptor.of(wrapped, parser.valueType());
    }

    public ParserWrapper(@Nonnull ParserDescriptor<C, T> parser, @Nonnull SuggestionProvider<C> suggestionProvider) {
        this.parser = parser;
        this.suggestionProvider = suggestionProvider;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull T> parse(@NonNull CommandContext<@NonNull C> commandContext,
                                                          @NonNull CommandInput commandInput) {
        return this.parser.parser().parse(commandContext, commandInput);
    }

    @NotNull
    @Override
    public SuggestionProvider<C> suggestionProvider() {
        return this.suggestionProvider;
    }
}
