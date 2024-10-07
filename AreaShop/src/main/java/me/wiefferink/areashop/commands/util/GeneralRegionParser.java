package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.SuggestionProvider;

import javax.annotation.Nonnull;

public class GeneralRegionParser<C> implements ArgumentParser<C, GeneralRegion> {

    protected final IFileManager fileManager;
    private final SuggestionProvider<C> suggestionProvider;

    public GeneralRegionParser(@Nonnull IFileManager fileManager, @Nonnull SuggestionProvider<C> suggestionProvider) {
        this.fileManager = fileManager;
        this.suggestionProvider = suggestionProvider;
    }

    public GeneralRegionParser(@Nonnull IFileManager fileManager) {
        this(fileManager, defaultProvider(fileManager));
    }

    public static <C> ParserDescriptor<C, GeneralRegion> generalRegionParser(@Nonnull IFileManager fileManager) {
        return ParserDescriptor.of(new GeneralRegionParser<>(fileManager), GeneralRegion.class);
    }

    @Nonnull
    private static <C> SuggestionProvider<C> defaultProvider(@Nonnull IFileManager fileManager) {
        return SuggestionProvider.blockingStrings((ctx, input) -> fileManager.getRegionNames());
    }

    @Override
    public @Nonnull ArgumentParseResult<GeneralRegion> parse(@Nonnull CommandContext<C> commandContext,
                                                             @Nonnull CommandInput commandInput) {
        String input = commandInput.peekString();
        GeneralRegion region = this.fileManager.getRegion(input);
        if (region != null) {
            commandInput.readString();
            return ArgumentParseResult.success(region);
        }
        AreaShopCommandException exception = new AreaShopCommandException("cmd-notRegistered", input);
        return ArgumentParseResult.failure(exception);
    }

    @Override
    public @Nonnull SuggestionProvider<C> suggestionProvider() {
        return this.suggestionProvider;
    }
}
