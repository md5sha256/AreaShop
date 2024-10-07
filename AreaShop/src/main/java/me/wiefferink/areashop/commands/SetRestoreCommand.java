package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.GeneralRegionParser;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class SetRestoreCommand extends AreashopCommandBean {

    private static final List<String> RESTORE_TYPES = List.of("true", "false", "general");
    private static final CloudKey<GeneralRegion> KEY_REGION = CloudKey.of("region", GeneralRegion.class);
    private static final CloudKey<String> KEY_RESTORE = CloudKey.of("restore", String.class);

    private static final CloudKey<String> KEY_PROFILE = CloudKey.of("profile", String.class);
    private final MessageBridge messageBridge;
    private final IFileManager fileManager;

    @Inject
    public SetRestoreCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (target.hasPermission("areashop.setrestore")) {
            return "help-setrestore";
        }
        return null;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("setrestore")
                .required(KEY_REGION, GeneralRegionParser.generalRegionParser(this.fileManager))
                .required(KEY_RESTORE, StringParser.stringParser(), this::suggestRestoreType)
                .optional(KEY_PROFILE, StringParser.stringParser(), this::suggestSchematicProfiles)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("setrestore");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.setrestore")) {
            throw new AreaShopCommandException("setrestore-noPermission");
        }
        GeneralRegion region = context.get(KEY_REGION);
        String restoreType = context.get(KEY_RESTORE);
        Optional<String> optionalProfile = context.optional(KEY_PROFILE);
        Boolean value = null;
        if (restoreType.equalsIgnoreCase("true")) {
            value = true;
        } else if (restoreType.equalsIgnoreCase("false")) {
            value = false;
        }
        region.setRestoreSetting(value);
        String valueString = "general";
        if (value != null) {
            valueString = value + "";
        }
        if (optionalProfile.isPresent()) {
            String profile = optionalProfile.get();
            region.setSchematicProfile(profile);
            this.messageBridge.message(sender, "setrestore-successProfile", valueString, profile, region);
        } else {
            this.messageBridge.message(sender, "setrestore-success", valueString, region);
        }
        region.update();
    }

    private CompletableFuture<Iterable<Suggestion>> suggestRestoreType(
            @Nonnull CommandContext<CommandSource<?>> context,
            @Nonnull CommandInput input
    ) {
        String text = input.peekString();
        List<Suggestion> suggestions = RESTORE_TYPES.stream()
                .filter(type -> type.startsWith(text))
                .map(Suggestion::suggestion)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }

    private CompletableFuture<Iterable<Suggestion>> suggestSchematicProfiles(
            @Nonnull CommandContext<CommandSource<?>> context,
            @Nonnull CommandInput input
    ) {
        ConfigurationSection schemProfiles = fileManager.getConfig().getConfigurationSection("schematicProfiles");
        if (schemProfiles == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        String text = input.peekString();
        List<Suggestion> suggestions = schemProfiles.getKeys(false).stream()
                .filter(profile -> profile.startsWith(text))
                .map(Suggestion::suggestion)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }
}
