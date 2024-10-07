package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.OfflinePlayerParser;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.BukkitSchedulerExecutor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Calendar;
import java.util.UUID;

@Singleton
public class SetOwnerCommand extends AreashopCommandBean {

    private static final CloudKey<String> KEY_PLAYER = CloudKey.of("player", String.class);
    private final CommandFlag<GeneralRegion> regionFlag;

    private final MessageBridge messageBridge;
    private final OfflinePlayerHelper offlinePlayerHelper;
    private final BukkitSchedulerExecutor executor;

    @Inject
    public SetOwnerCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull OfflinePlayerHelper offlinePlayerHelper,
            @Nonnull BukkitSchedulerExecutor executor
            ) {
        this.messageBridge = messageBridge;
        this.regionFlag = RegionParseUtil.createDefault(fileManager);
        this.offlinePlayerHelper = offlinePlayerHelper;
        this.executor = executor;
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (target.hasPermission("areashop.setownerrent") || target.hasPermission("areashop.setownerbuy")) {
            return "help-setowner";
        }
        return null;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("setowner")
                .required(KEY_PLAYER, OfflinePlayerParser.parser())
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("setowner");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.setownerrent") && !sender.hasPermission("areashop.setownerbuy")) {
            this.messageBridge.message(sender, "setowner-noPermission");
            return;
        }
        GeneralRegion region = RegionParseUtil.getOrParseRegion(context, sender, this.regionFlag);
        if (region instanceof RentRegion && !sender.hasPermission("areashop.setownerrent")) {
            this.messageBridge.message(sender, "setowner-noPermissionRent", region);
            return;
        }
        if (region instanceof BuyRegion && !sender.hasPermission("areashop.setownerbuy")) {
            this.messageBridge.message(sender, "setowner-noPermissionBuy", region);
            return;
        }
        this.offlinePlayerHelper.lookupOfflinePlayerAsync(context.get(KEY_PLAYER))
                .whenCompleteAsync((offlinePlayer, exception) -> {
                    if (exception != null) {
                        sender.sendMessage("failed to lookup offline player!");
                        exception.printStackTrace();
                        return;
                    }
                    if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                        this.messageBridge.message(sender, "cmd-invalidPlayer", offlinePlayer.getName());
                        return;
                    }
                    setOwner(sender, offlinePlayer, region);
                }, this.executor);
    }

    private void setOwner(@Nonnull CommandSender sender, @Nonnull OfflinePlayer player, GeneralRegion region) {
        final UUID uuid = player.getUniqueId();
        if (region instanceof RentRegion rent) {
            if (rent.isRenter(uuid)) {
                // extend
                rent.setRentedUntil(rent.getRentedUntil() + rent.getDuration());
                rent.setRenter(uuid);
                this.messageBridge.message(sender, "setowner-succesRentExtend", region);
            } else {
                // change
                if (!rent.isRented()) {
                    rent.setRentedUntil(Calendar.getInstance().getTimeInMillis() + rent.getDuration());
                }
                rent.setRenter(uuid);
                this.messageBridge.message(sender, "setowner-succesRent", region);
            }
        } else if (region instanceof BuyRegion buy) {
            buy.setBuyer(uuid);
            this.messageBridge.message(sender, "setowner-succesBuy", region);
        }
        region.getFriendsFeature().deleteFriend(region.getOwner(), null);
        region.update();
        region.saveRequired();
    }

}








