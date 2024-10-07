package me.wiefferink.areashop.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.GeneralRegionParser;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.features.homeaccess.HomeAccessFeature;
import me.wiefferink.areashop.features.homeaccess.HomeAccessType;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Singleton
public final class ToggleHomeCommand extends AreashopCommandBean {

    private static final CloudKey<HomeAccessType> KEY_ACCESS_TYPE = CloudKey.of("accessType", HomeAccessType.class);
    private final MessageBridge messageBridge;
    private final IFileManager fileManager;
    private final CommandFlag<GeneralRegion> regionFlag;

    @Inject
    public ToggleHomeCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
        this.regionFlag = CommandFlag.<CommandSource<?>>builder("region")
                .withComponent(
                        CommandComponent.<CommandSource<?>, GeneralRegion>builder()
                                .name("region")
                                .description(Description.EMPTY)
                                .valueType(GeneralRegion.class)
                                .parser(GeneralRegionParser.generalRegionParser(fileManager))
                                .suggestionProvider(this::suggestRegions)
                                .build()
                ).build();
    }


    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (!target.hasPermission("sethomecontrol.control")) {
            return null;
        }
        return "help-togglehome";
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("togglehome")
                .required(KEY_ACCESS_TYPE, EnumParser.enumParser(HomeAccessType.class))
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("togglehome");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.togglehome")) {
            throw new AreaShopCommandException("togglehome-noPermission");
        }
        final HomeAccessType accessType = context.get(KEY_ACCESS_TYPE);
        final GeneralRegion region = RegionParseUtil.getOrParseRegion(context, sender, this.regionFlag);
        if (!(sender instanceof Player) && !sender.hasPermission("sethome.control.other")) {
            return;
        }
        if (sender instanceof Player player && !region.isOwner(player)) {
            throw new AreaShopCommandException("togglehome-noPermission");
        }
        region.getOrCreateFeature(HomeAccessFeature.class).homeAccessType(accessType);
        this.messageBridge.message(sender, "togglehome-success", accessType.name());
    }

    private CompletableFuture<Iterable<Suggestion>> suggestRegions(
            @Nonnull CommandContext<CommandSource<?>> context,
            @Nonnull CommandInput input
    ) {
        String text = input.peekString();
        CommandSender sender = context.sender().sender();
        Stream<GeneralRegion> regions;
        if (sender.hasPermission("sethome.control.other")) {
            regions = this.fileManager.getRegionsRef().stream();
        } else if (!sender.hasPermission("sethome.control.other") && sender instanceof Player player) {
            regions = this.fileManager.getRegionsRef().stream()
                    .filter(region -> region.isOwner(player.getUniqueId()));
        } else {
            regions = Stream.empty();
        }
        List<Suggestion> suggestions = regions.map(GeneralRegion::getName)
                .filter(name -> name.startsWith(text))
                .map(Suggestion::suggestion)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }
}
