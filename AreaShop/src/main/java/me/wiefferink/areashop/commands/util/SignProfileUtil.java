package me.wiefferink.areashop.commands.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.StringParser;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

public final class SignProfileUtil {

    public static final CommandFlag<String> DEFAULT_FLAG = CommandFlag.builder("profile")
            .withComponent(StringParser.stringParser(StringParser.StringMode.SINGLE))
            .build();

    private SignProfileUtil() {
        throw new IllegalArgumentException("Static utility class cannot be instantiated");
    }

    @Nonnull
    public static CommandFlag<String> createDefault(@Nonnull Plugin plugin) {
        Supplier<Collection<String>> valueSupplier = () -> {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("signProfiles");
            if (section == null) {
                return Collections.emptyList();
            }
            return section.getKeys(false);
        };
        return CommandFlag.builder("profile")
                .withComponent(
                        ParserDescriptor.of(
                        AcceptedValuesParser.of(valueSupplier, "addsign-wrongProfile", false),
                        String.class)
                )
                .build();
    }

    @Nullable
    public static String getOrParseProfile(@Nonnull CommandContext<?> context,
                                           @Nonnull Plugin plugin) {
        return getOrParseProfile(context, DEFAULT_FLAG, plugin);
    }

    @Nullable
    public static String getOrParseProfile(
            @Nonnull CommandContext<?> context,
            @Nonnull CommandFlag<String> flag,
            @Nonnull Plugin plugin
    ) throws AreaShopCommandException {
        String profile = context.flags().get(flag);
        if (profile == null) {
            return null;
        }
        Set<String> profiles = plugin.getConfig().getConfigurationSection("signProfiles").getKeys(false);
        if (profiles.contains(profile.toLowerCase(Locale.ENGLISH))) {
            return profile.toLowerCase(Locale.ENGLISH);
        }
        throw new AreaShopCommandException("addsign-wrongProfile");
    }

}
