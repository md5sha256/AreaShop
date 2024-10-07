package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RentRegion;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

import javax.annotation.Nonnull;

public class RentRegionParser<C> implements ArgumentParser<C, RentRegion> {

    private final IFileManager fileManager;
    private final SuggestionProvider<C> suggestionProvider;

    public RentRegionParser(@Nonnull IFileManager fileManager, @Nonnull SuggestionProvider<C> suggestionProvider) {
        this.fileManager = fileManager;
        this.suggestionProvider = suggestionProvider;
    }

    public RentRegionParser(@Nonnull IFileManager fileManager) {
        this(fileManager, defaultSuggestionProvider(fileManager));
    }

    private static <C> SuggestionProvider<C> defaultSuggestionProvider(@Nonnull IFileManager fileManager) {
        return SuggestionProvider.blockingStrings((ctx, input) -> {
                    String text = input.peekString();
                    return fileManager.getRentNames()
                            .stream()
                            .filter(name -> name.startsWith(text))
                            .toList();
                }
        );
    }

    @Override
    public @Nonnull ArgumentParseResult<RentRegion> parse(@Nonnull CommandContext<C> commandContext,
                                                         @Nonnull CommandInput commandInput) {
        String input = commandInput.peekString();
        RentRegion region = this.fileManager.getRent(input);
        if (region != null) {
            commandInput.readString();
            return ArgumentParseResult.success(region);
        }
        AreaShopCommandException exception = new AreaShopCommandException("rent-notRentable", input);
        return ArgumentParseResult.failure(exception);
    }

    @Override
    public @Nonnull SuggestionProvider<C> suggestionProvider() {
        return this.suggestionProvider;
    }
}
