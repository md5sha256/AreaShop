package me.wiefferink.areashop.commands;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.SimpleMessageBridge;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.interactivemessenger.processing.Message;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class InfoRegionCommand extends AreashopCommandBean {

    private final CommandFlag<GeneralRegion> regionFlag;

    private final MessageBridge messageBridge;

    @Inject
    public InfoRegionCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager
    ) {
        this.messageBridge = messageBridge;
        this.regionFlag = RegionParseUtil.createDefault(fileManager);
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        return null;
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        return builder.literal("info").literal("region")
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("info region");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.info")) {
            messageBridge.message(sender, "info-noPermission");
            return;
        }
        // Region info
        GeneralRegion region = RegionParseUtil.getOrParseRegion(context, sender, this.regionFlag);
        if (region instanceof RentRegion rent) {
            handleRent(sender, rent);
        } else if (region instanceof BuyRegion buy) {
            handleBuy(sender, buy);
        }
    }

    private void handleBuy(@Nonnull CommandSender sender, @Nonnull BuyRegion buy) {
        messageBridge.message(sender, "info-regionHeaderBuy", buy);
        if (buy.isSold()) {
            if (buy.isInResellingMode()) {
                messageBridge.messageNoPrefix(sender, "info-regionReselling", buy);
                messageBridge.messageNoPrefix(sender, "info-regionReselPrice", buy);
            } else {
                messageBridge.messageNoPrefix(sender, "info-regionBought", buy);
            }
            // Money back
            if (!buy.getBooleanSetting("buy.sellDisabled")) {
                if (SellCommand.canUse(sender, buy)) {
                    messageBridge.messageNoPrefix(sender, "info-regionMoneyBackBuyClick", buy);
                } else {
                    messageBridge.messageNoPrefix(sender, "info-regionMoneyBackBuy", buy);
                }
            }
            // Friends
            if (!buy.getFriendsFeature().getFriendNames().isEmpty()) {
                String messagePart = "info-friend";
                if (DelFriendCommand.canUse(sender, buy)) {
                    messagePart = "info-friendRemove";
                }
                messageBridge.messageNoPrefix(sender,
                        "info-regionFriends",
                        buy,
                        Utils.combinedMessage(buy.getFriendsFeature().getFriendNames(), messagePart));
            }
        } else {
            messageBridge.messageNoPrefix(sender, "info-regionCanBeBought", buy);
        }
        if (buy.getLandlord() != null) {
            messageBridge.messageNoPrefix(sender, "info-regionLandlord", buy);
        }
        if (buy.getInactiveTimeUntilSell() != -1) {
            messageBridge.messageNoPrefix(sender, "info-regionInactiveSell", buy);
        }
        // Restoring
        if (buy.isRestoreEnabled()) {
            messageBridge.messageNoPrefix(sender, "info-regionRestoringBuy", buy);
        }
        // Restrictions
        if (!buy.isSold()) {
            if (buy.restrictedToRegion()) {
                messageBridge.messageNoPrefix(sender, "info-regionRestrictedRegionBuy", buy);
            } else if (buy.restrictedToWorld()) {
                messageBridge.messageNoPrefix(sender, "info-regionRestrictedWorldBuy", buy);
            }
        }
        messageBridge.messageNoPrefix(sender, "info-regionFooterBuy", buy);
    }

    private void handleRent(@Nonnull CommandSender sender, @Nonnull RentRegion rent) {
        messageBridge.message(sender, "info-regionHeaderRent", rent);
        if (rent.isRented()) {
            messageBridge.messageNoPrefix(sender, "info-regionRented", rent);
            messageBridge.messageNoPrefix(sender, "info-regionExtending", rent);
            // Money back
            if (UnrentCommand.canUse(sender, rent)) {
                messageBridge.messageNoPrefix(sender, "info-regionMoneyBackRentClick", rent);
            } else {
                messageBridge.messageNoPrefix(sender, "info-regionMoneyBackRent", rent);
            }
            // Friends
            if (!rent.getFriendsFeature().getFriendNames().isEmpty()) {
                String messagePart = "info-friend";
                if (DelFriendCommand.canUse(sender, rent)) {
                    messagePart = "info-friendRemove";
                }
                messageBridge.messageNoPrefix(sender,
                        "info-regionFriends",
                        rent,
                        Utils.combinedMessage(rent.getFriendsFeature().getFriendNames(), messagePart));
            }
        } else {
            messageBridge.messageNoPrefix(sender, "info-regionCanBeRented", rent);
        }
        if (rent.getLandlordName() != null) {
            messageBridge.messageNoPrefix(sender, "info-regionLandlord", rent);
        }
        // Maximum extends
        if (rent.getMaxExtends() != -1) {
            if (rent.getMaxExtends() == 0) {
                messageBridge.messageNoPrefix(sender, "info-regionNoExtending", rent);
            } else if (rent.isRented()) {
                messageBridge.messageNoPrefix(sender, "info-regionExtendsLeft", rent);
            } else {
                messageBridge.messageNoPrefix(sender, "info-regionMaxExtends", rent);
            }
        }
        // If maxExtends is zero it does not make sense to show this message
        if (rent.getMaxRentTime() != -1 && rent.getMaxExtends() != 0) {
            messageBridge.messageNoPrefix(sender, "info-regionMaxRentTime", rent);
        }
        if (rent.getInactiveTimeUntilUnrent() != -1) {
            messageBridge.messageNoPrefix(sender, "info-regionInactiveUnrent", rent);
        }
        displayMiscInfo(sender, rent);
        // Restoring
        if (rent.isRestoreEnabled()) {
            messageBridge.messageNoPrefix(sender, "info-regionRestoringRent", rent);
        }
        // Restrictions
        if (!rent.isRented()) {
            if (rent.restrictedToRegion()) {
                messageBridge.messageNoPrefix(sender, "info-regionRestrictedRegionRent", rent);
            } else if (rent.restrictedToWorld()) {
                messageBridge.messageNoPrefix(sender, "info-regionRestrictedWorldRent", rent);
            }
        }
        messageBridge.messageNoPrefix(sender, "info-regionFooterRent", rent);
    }

    private void displayMiscInfo(@Nonnull CommandSender sender, @Nonnull GeneralRegion region) {
        displayTeleportInfo(sender, region);
        displaySignInfo(sender, region);
        displayGroupInfo(sender, region);
    }

    private void displayGroupInfo(@Nonnull CommandSender sender, @Nonnull GeneralRegion region) {// Groups
        if (sender.hasPermission("areashop.groupinfo") && !region.getGroupNames().isEmpty()) {
            messageBridge.messageNoPrefix(sender,
                    "info-regionGroups",
                    Utils.createCommaSeparatedList(region.getGroupNames()));
        }
    }

    private void displaySignInfo(@Nonnull CommandSender sender, @Nonnull GeneralRegion region) {
        // Signs
        List<String> signLocations = new ArrayList<>();
        for (BlockPosition location : region.getSignsFeature().signManager().allSignLocations()) {
            signLocations.add(Message.fromKey("info-regionSignLocation")
                    .replacements(location.getWorld().getName(),
                            location.getX(),
                            location.getY(),
                            location.getZ())
                    .getPlain());
        }
        if (!signLocations.isEmpty()) {
            messageBridge.messageNoPrefix(sender,
                    "info-regionSigns",
                    Utils.createCommaSeparatedList(signLocations));
        }
    }

    private void displayTeleportInfo(@Nonnull CommandSender sender, @Nonnull GeneralRegion region) {
        // Teleport
        Message tp = Message.fromKey("info-prefix");
        boolean foundSomething = false;
        if (TeleportCommand.canUse(sender, region)) {
            foundSomething = true;
            tp.append(Message.fromKey("info-regionTeleport").replacements(region));
        }
        if (SetTeleportCommand.canUse(sender, region)) {
            if (foundSomething) {
                tp.append(", ");
            }
            foundSomething = true;
            tp.append(Message.fromKey("info-setRegionTeleport").replacements(region));
        }
        if (foundSomething) {
            tp.append(".");
            SimpleMessageBridge.send(tp, sender);
        }
    }

}


























