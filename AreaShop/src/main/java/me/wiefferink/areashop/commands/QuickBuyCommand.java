package me.wiefferink.areashop.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.ArgumentParseExceptionHandler;
import me.wiefferink.areashop.commands.util.OfflinePlayerParser;
import me.wiefferink.areashop.commands.util.RegionCreationUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.commands.util.commandsource.PlayerCommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.RegionFactory;
import me.wiefferink.areashop.tools.BukkitSchedulerExecutor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.standard.DoubleParser;
import org.incendo.cloud.parser.standard.StringParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Singleton
public class QuickBuyCommand extends AreashopCommandBean {

    private static final CloudKey<String> KEY_REGION = CloudKey.of("region", String.class);
    private static final CloudKey<Double> KEY_PRICE = CloudKey.of("price", Double.class);
    private static final CloudKey<String> KEY_LANDLORD = CloudKey.of("landlord", String.class);
    private final RegionFactory regionFactory;
    private final IFileManager fileManager;
    private final MessageBridge messageBridge;
    private final RegionCreationUtil regionCreationUtil;
    private final BukkitSchedulerExecutor executor;
    private final OfflinePlayerHelper offlinePlayerHelper;

    @Inject
    public QuickBuyCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull RegionFactory regionFactory,
            @Nonnull IFileManager fileManager,
            @Nonnull RegionCreationUtil regionCreationUtil,
            @Nonnull BukkitSchedulerExecutor executor,
            @Nonnull OfflinePlayerHelper offlinePlayerHelper
    ) {
        this.messageBridge = messageBridge;
        this.regionFactory = regionFactory;
        this.fileManager = fileManager;
        this.regionCreationUtil = regionCreationUtil;
        this.executor = executor;
        this.offlinePlayerHelper = offlinePlayerHelper;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Nonnull
    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        return builder.literal("quickbuy")
                .permission("permission")
                .senderType(PlayerCommandSource.class)
                .required(KEY_REGION, StringParser.stringParser())
                .required(KEY_PRICE, DoubleParser.doubleParser(0))
                .required(KEY_LANDLORD, OfflinePlayerParser.parser())
                .handler(this::handleCommand);
    }

    // /as quickbuy <name> <price> <duration> <landlord>

    private void handleCommand(@Nonnull CommandContext<PlayerCommandSource> context) {
        Player player = context.sender().sender();
        if (!player.hasPermission("areashop.quickbuy")) {
            player.sendMessage("Insufficient permission");
            return;
        }
        this.regionCreationUtil.createRegion(context, player, KEY_REGION)
                .exceptionally(throwable -> {
                    if (throwable instanceof AreaShopCommandException exception) {
                        ArgumentParseExceptionHandler.handleException(this.messageBridge, context.sender(), exception);
                    } else {
                        throw new CommandExecutionException(throwable, context);
                    }
                    return null;
                }).thenAcceptAsync(region -> {
                    // Error handled previously
                    if (region == null) {
                        this.messageBridge.message(player, "quickadd-failedCreateWGRegion");
                        return;
                    }
                    double price = context.get(KEY_PRICE);
                    this.offlinePlayerHelper.lookupOfflinePlayerAsync(context.get(KEY_LANDLORD))
                            .whenCompleteAsync((landlord, exception) -> {
                                if (exception != null) {
                                    player.sendMessage("failed to lookup offline player!");
                                    exception.printStackTrace();
                                    return;
                                }
                                if (!landlord.isOnline() && !landlord.hasPlayedBefore()) {
                                    this.messageBridge.message(player, "me-noPlayer", landlord.getName());
                                    return;
                                }
                                String regionName = region.getId();
                                World world = player.getWorld();

                                BuyRegion buyRegion = this.regionFactory.createBuyRegion(regionName, world);
                                buyRegion.setPrice(price);
                                buyRegion.setLandlord(landlord.getUniqueId(), landlord.getName());
                                this.fileManager.addRegion(buyRegion);
                                this.messageBridge.message(player, "add-success", "buy", regionName);
                            }, this.executor);
                }, this.executor);
    }

    @Nullable
    @Override
    public String getHelpKey(@Nonnull CommandSender target) {
        if (target.hasPermission("areashop.quickbuy")) {
            return "help-quickbuy";
        }
        return null;
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("quickbuy");
    }
}
