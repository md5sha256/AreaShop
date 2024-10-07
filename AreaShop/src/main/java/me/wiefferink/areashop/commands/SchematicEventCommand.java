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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.standard.EnumParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Singleton
public class SchematicEventCommand extends AreashopCommandBean {

    private static final CloudKey<GeneralRegion> KEY_REGION = CloudKey.of("region", GeneralRegion.class);
    private static final CloudKey<GeneralRegion.RegionEvent> KEY_EVENT_TYPE = CloudKey.of("eventType",
            GeneralRegion.RegionEvent.class);
    private final MessageBridge messageBridge;
    private final IFileManager fileManager;

    @Inject
    public SchematicEventCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("schemevent")
                .required(KEY_REGION, GeneralRegionParser.generalRegionParser(this.fileManager))
                .required(KEY_EVENT_TYPE, EnumParser.enumParser(GeneralRegion.RegionEvent.class))
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("schemevent");
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (target.hasPermission("areashop.schematicevents")) {
            return "help-schemevent";
        }
        return null;
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.schematicevents")) {
            throw new AreaShopCommandException("schemevent-noPermission");
        }

        GeneralRegion region = context.get(KEY_REGION);
        GeneralRegion.RegionEvent event = context.get(KEY_EVENT_TYPE);
        if (region.getRegion() == null) {
            throw new AreaShopCommandException("general-noRegion", region);
        }
        region.handleSchematicEvent(event);
        region.update();
        this.messageBridge.message(sender, "schemevent-success", event.getValue(), region);
    }

}
