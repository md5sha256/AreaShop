package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.OfflinePlayerParser;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.commands.util.commandsource.PlayerCommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.BukkitSchedulerExecutor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Singleton
public class AddFriendCommand extends AreashopCommandBean {

    private static final CloudKey<String> KEY_FRIEND = CloudKey.of("friend", String.class);
    private final CommandFlag<GeneralRegion> regionFlag;
    private final MessageBridge messageBridge;
    private final Plugin plugin;
    private final BukkitSchedulerExecutor executor;
    private final OfflinePlayerHelper offlinePlayerHelper;

    @Inject
    public AddFriendCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull Plugin plugin,
            @Nonnull BukkitSchedulerExecutor executor,
            @Nonnull OfflinePlayerHelper offlinePlayerHelper
    ) {
        this.messageBridge = messageBridge;
        this.plugin = plugin;
        this.regionFlag = RegionParseUtil.createDefault(fileManager);
        this.executor = executor;
        this.offlinePlayerHelper = offlinePlayerHelper;
    }

    @Override
    public String stringDescription() {
        return "Allows you to add friends to your regions";
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (target.hasPermission("areashop.addfriendall") || target.hasPermission("areashop.addfriend")) {
            return "help-addFriend";
        }
        return null;
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        return builder.literal("addfriend")
                .senderType(PlayerCommandSource.class)
                .required(KEY_FRIEND, OfflinePlayerParser.parser())
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    private void handleCommand(CommandContext<PlayerCommandSource> context) {
        Player sender = context.sender().sender();
        if (!sender.hasPermission("areashop.addfriend") && !sender.hasPermission("areashop.addfriendall")) {
           this.messageBridge.message(sender, "addfriend-noPermission");
            return;
        }
        GeneralRegion region = RegionParseUtil.getOrParseRegion(context, sender, this.regionFlag);
        this.offlinePlayerHelper.lookupOfflinePlayerAsync(context.get(KEY_FRIEND))
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
                    processWithFriend(sender, region, offlinePlayer);
                }, this.executor);
    }

    private void processWithFriend(@Nonnull Player sender,
                                   @Nonnull GeneralRegion region,
                                   @Nonnull OfflinePlayer friend
    ) {
        if (sender.hasPermission("areashop.addfriendall") && ((region instanceof RentRegion rentRegion && !rentRegion.isRented())
                || (region instanceof BuyRegion buyRegion && !buyRegion.isSold()))) {
            this.messageBridge.message(sender, "addfriend-noOwner", region);
            return;

        }
        if (!sender.hasPermission("areashop.addfriend")) {
            this.messageBridge.message(sender, "addfriend-noPermission", region);
            return;
        }
        if (!region.isOwner(sender)) {
            this.messageBridge.message(sender, "addfriend-noPermissionOther", region);
            return;
        }
        if (!friend.hasPlayedBefore() && !friend.isOnline() && !plugin.getConfig()
                .getBoolean("addFriendNotExistingPlayers")) {
            this.messageBridge.message(sender, "addfriend-notVisited", friend.getName(), region);
        } else if (region.getFriendsFeature().getFriends().contains(friend.getUniqueId())) {
            this.messageBridge.message(sender, "addfriend-alreadyAdded", friend.getName(), region);
        } else if (region.isOwner(friend.getUniqueId())) {
            this.messageBridge.message(sender, "addfriend-self", friend.getName(), region);
        } else if (region.getFriendsFeature().addFriend(friend.getUniqueId(), sender)) {
            region.update();
            this.messageBridge.message(sender, "addfriend-success", friend.getName(), region);
        }
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("addfriend");
    }
}








