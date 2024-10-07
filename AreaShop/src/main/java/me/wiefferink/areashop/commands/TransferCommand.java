package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.GeneralRegionParser;
import me.wiefferink.areashop.commands.util.OfflinePlayerParser;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.commands.util.commandsource.PlayerCommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class TransferCommand extends AreashopCommandBean {

    private static final CloudKey<String> KEY_PLAYER = CloudKey.of("player", String.class);
    private final MessageBridge messageBridge;
    private final IFileManager fileManager;
    private final CommandFlag<GeneralRegion> regionFlag;
    private final OfflinePlayerHelper offlinePlayerHelper;

    @Inject
    public TransferCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull OfflinePlayerHelper offlinePlayerHelper
    ) {
        ParserDescriptor<PlayerCommandSource, GeneralRegion> regionParser =
                ParserDescriptor.of(new GeneralRegionParser<>(fileManager, this::suggestRegions), GeneralRegion.class);
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
        this.regionFlag = CommandFlag.<PlayerCommandSource>builder("region")
                .withComponent(regionParser)
                .build();
        this.offlinePlayerHelper = offlinePlayerHelper;
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (!target.hasPermission("areashop.transfer")) {
            return null;
        }
        return "help-transfer";
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("transfer")
                .senderType(PlayerCommandSource.class)
                .required(KEY_PLAYER, OfflinePlayerParser.parser())
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("transfer");
    }

    private void handleCommand(@Nonnull CommandContext<PlayerCommandSource> context) {
        Player sender = context.sender().sender();
        if (!sender.hasPermission("areashop.transfer")) {
            throw new AreaShopCommandException("transfer-noPermission");
        }
        GeneralRegion region = RegionParseUtil.getOrParseRegion(context, sender, this.regionFlag);
        if (!region.isTransferEnabled()) {
            throw new AreaShopCommandException("transfer-disabled");
        }
        this.offlinePlayerHelper.lookupOfflinePlayerAsync(context.get(KEY_PLAYER))
                .whenComplete((offlinePlayer, exception) -> {
                    if (exception != null) {
                        sender.sendMessage("failed to lookup offline player!");
                        exception.printStackTrace();
                        return;
                    }
                    if (!offlinePlayer.hasPlayedBefore() &&!offlinePlayer.isOnline()) {
                        this.messageBridge.message(sender, "cmd-invalidPlayer", offlinePlayer.getName());
                        return;
                    }
                    performTransfer(sender, offlinePlayer, region);
                });

    }

    private void performTransfer(
            @Nonnull Player sender,
            @Nonnull OfflinePlayer targetPlayer,
            @Nonnull GeneralRegion region
    ) {
        String targetPlayerName = targetPlayer.getName();
        if (Objects.equals(sender, targetPlayer)) {
            throw new AreaShopCommandException("transfer-transferSelf");
        }
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            // Unknown player
            throw new AreaShopCommandException("transfer-noPlayer", targetPlayerName);
        }
        if (region.isLandlord(sender.getUniqueId())) {
            // Transfer ownership if same as landlord
            region.getFriendsFeature().deleteFriend(region.getOwner(), null);
            region.setOwner(targetPlayer.getUniqueId());
            region.setLandlord(targetPlayer.getUniqueId(), targetPlayerName);
            this.messageBridge.message(sender, "transfer-transferred-owner", targetPlayerName, region);
            this.messageBridge.messagePersistent(targetPlayer, "transfer-transferred-owner", targetPlayerName, region);
            region.update();
            region.saveRequired();
            return;
        }
        if (!region.isOwner(sender.getUniqueId())) {
            // Cannot transfer tenant if we aren't the current tenant
            throw new AreaShopCommandException("transfer-notCurrentTenant");
        }
        region.getFriendsFeature().deleteFriend(region.getOwner(), null);
        // Swap the owner/occupant (renter or buyer)
        region.setOwner(targetPlayer.getUniqueId());

        this.messageBridge.message(sender, "transfer-transferred-tenant", targetPlayerName, region);
        this.messageBridge.messagePersistent(targetPlayer, "transfer-transferred-tenant", targetPlayerName, region);
        region.update();
        region.saveRequired();
    }

    private CompletableFuture<Iterable<Suggestion>> suggestRegions(
            @Nonnull CommandContext<PlayerCommandSource> context,
            @Nonnull CommandInput input
    ) {
        String text = input.peekString();
        UUID uuid = context.sender().sender().getUniqueId();
        List<Suggestion> suggestions = this.fileManager.getRegions()
                .stream()
                .filter(region -> region.isOwner(uuid) || region.isLandlord(uuid))
                .map(GeneralRegion::getName)
                .filter(name -> name.startsWith(text))
                .map(Suggestion::suggestion)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }

}
