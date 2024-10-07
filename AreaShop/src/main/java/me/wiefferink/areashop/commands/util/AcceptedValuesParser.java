package me.wiefferink.areashop.commands.util;

import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AcceptedValuesParser<C> implements ArgumentParser<C, String>, SuggestionProvider<C> {

    private final Supplier< Collection<String>> valuesProvider;
    private final boolean caseSensitive;

    private final String failureMessageKey;

    private AcceptedValuesParser(
            @Nonnull Supplier< Collection<String>> valuesProvider,
            @Nonnull String failureMessageKey,
            boolean caseSensitive

    ) {
        this.valuesProvider = valuesProvider;
        this.failureMessageKey = failureMessageKey;
        this.caseSensitive = caseSensitive;
    }

    public static <C> AcceptedValuesParser<C> ofConstant(
            @Nonnull Collection<String> acceptedValues,
            @Nonnull String failureMessageKey,
            boolean caseSensitive
    ) {
        Collection<String> copy = List.copyOf(acceptedValues);
        return new AcceptedValuesParser<>(() -> copy, failureMessageKey, caseSensitive);
    }

    public static <C> AcceptedValuesParser<C> of(
            @Nonnull Supplier< Collection<String>> supplier,
            @Nonnull String failureMessageKey,
            boolean caseSensitive
    ) {
        return new AcceptedValuesParser<>(supplier, failureMessageKey, caseSensitive);
    }

    public static <C> AcceptedValuesParser<C> ofCached(
            @Nonnull Supplier< Collection<String>> supplier,
            @Nonnull String failureMessageKey,
            boolean caseSensitive
    ) {
        return ofConstant(supplier.get(), failureMessageKey, caseSensitive);
    }

    @Override
    public @Nonnull ArgumentParseResult<String> parse(@Nonnull CommandContext<C> commandContext,
                                                               @Nonnull CommandInput commandInput) {
        String toTest;
        if (this.caseSensitive) {
            toTest = commandInput.peekString().toLowerCase(Locale.ENGLISH);
        } else {
            toTest = commandInput.peekString();
        }
        Collection<String> acceptedValues = this.valuesProvider.get();
        if (acceptedValues.contains(toTest)) {
            return ArgumentParseResult.success(toTest);
        }
        return ArgumentParseResult.failure(new AreaShopCommandException(this.failureMessageKey));
    }

    @Override
    public @Nonnull CompletableFuture<Iterable<Suggestion>> suggestionsFuture(
            @Nonnull CommandContext<C> context,
            @Nonnull CommandInput input
    ) {
        String text = input.peekString();
        Iterable<Suggestion> suggestions = this.valuesProvider.get().stream()
                .filter(s -> s.startsWith(text))
                .map(Suggestion::suggestion)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }
}
