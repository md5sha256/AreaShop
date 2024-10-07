package me.wiefferink.areashop.commands.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

public class OfflinePlayerParser<C> implements ArgumentParser<C, String>, BlockingSuggestionProvider.Strings<C> {


    public static <C> ParserDescriptor<C, String> parser() {
        return ParserDescriptor.of(new OfflinePlayerParser<>(), String.class);
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull String> parse(@NonNull CommandContext<@NonNull C> commandContext,
                                                               @NonNull CommandInput commandInput) {
        final String input = commandInput.readString();
        if (input.length() > 16) {
            return ArgumentParseResult.failure(new AreaShopCommandException("cmd-invalidPlayer", input));
        }
        return ArgumentParseResult.success(input);

    }

    @Override
    public @NonNull Iterable<@NonNull String> stringSuggestions(@NonNull CommandContext<C> commandContext,
                                                                @NonNull CommandInput input) {
        return Bukkit.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
    }
}
