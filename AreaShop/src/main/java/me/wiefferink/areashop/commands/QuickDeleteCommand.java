package me.wiefferink.areashop.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldguard.protection.managers.RegionManager;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.GeneralRegionParser;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.commands.util.commandsource.PlayerCommandSource;
import me.wiefferink.areashop.events.ask.DeletingRegionEvent;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Singleton
public class QuickDeleteCommand extends AreashopCommandBean {

    private static final CloudKey<GeneralRegion> KEY_REGION = CloudKey.of("region", GeneralRegion.class);

    private static final CommandFlag<Void> FLAG_RETURN_MONEY = CommandFlag.builder("return-money")
            .withDescription(Description.of(
                    "If this flag is present, money is returned to the player if someone is currently holding this region"))
            .build();

    private final IFileManager fileManager;
    private final WorldGuardInterface worldGuardInterface;
    private final MessageBridge messageBridge;

    @Inject
    public QuickDeleteCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull WorldGuardInterface worldGuardInterface
    ) {
        this.fileManager = fileManager;
        this.worldGuardInterface = worldGuardInterface;
        this.messageBridge = messageBridge;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Nonnull
    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        return builder.literal("quickdelete", "quickdel")
                .permission("areashop.quickdelete")
                .senderType(PlayerCommandSource.class)
                .required(KEY_REGION, GeneralRegionParser.generalRegionParser(this.fileManager))
                .handler(this::handleCommand);
    }

    private void handleCommand(@Nonnull CommandContext<PlayerCommandSource> context) {
        Player player = context.sender().sender();
        GeneralRegion region = context.get(KEY_REGION);
        boolean giveMoneyBack = context.flags().isPresent(FLAG_RETURN_MONEY);
        DeletingRegionEvent event = this.fileManager.deleteRegion(region, giveMoneyBack);
        if (event.isCancelled()) {
            throw new AreaShopCommandException("general-cancelled", event.getReason());
        }
        this.messageBridge.message(player, "destroy-successRent", region);
        RegionManager regionManager = this.worldGuardInterface.getRegionManager(region.getWorld());
        regionManager.removeRegion(region.getRegion().getId());
        this.messageBridge.message(player, "quickdelete-WGRegionDeleted", region.getName());
    }

    @Nullable
    @Override
    public String getHelpKey(@Nonnull CommandSender target) {
        return "help-quickdelete";
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("quickdelete", "quickdel");
    }
}
