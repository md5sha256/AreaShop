package me.wiefferink.areashop.commands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.GeneralRegionParser;
import me.wiefferink.areashop.commands.util.WorldSelection;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.events.ask.DeletingRegionEvent;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Singleton
public class DelCommand extends AreashopCommandBean {

    private static final CloudKey<GeneralRegion> KEY_REGION = CloudKey.of("region", GeneralRegion.class);
    private final WorldEditInterface worldEditInterface;
    private final IFileManager fileManager;
    private final MessageBridge messageBridge;

    @Inject
    public DelCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull WorldEditInterface worldEditInterface,
            @Nonnull IFileManager fileManager
    ) {
        this.messageBridge = messageBridge;
        this.worldEditInterface = worldEditInterface;
        this.fileManager = fileManager;
    }

    @Override
    public String stringDescription() {
        return "Allows you to delete regions";
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (target.hasPermission("areashop.destroyrent") || target.hasPermission("areashop.destroybuy") || target.hasPermission(
                "areashop.destroyrent.landlord") || target.hasPermission("areashop.destroybuy.landlord")) {
            return "help-del";
        }
        return null;
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("delete", "del");
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        return builder.literal("del", "delete")
                .optional(KEY_REGION, GeneralRegionParser.generalRegionParser(this.fileManager))
                .handler(this::handleCommand);
    }

    public void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.destroybuy")
                && !sender.hasPermission("areashop.destroybuy.landlord")
                && !sender.hasPermission("areashop.destroyrent")
                && !sender.hasPermission("areashop.destroyrent.landlord")) {
            throw new AreaShopCommandException("del-noPermission");
        }
        Optional<GeneralRegion> inputRegion = context.optional(KEY_REGION);
        if (inputRegion.isPresent()) {
            handleSingleDeletion(sender, inputRegion.get());
            return;
        }
        List<GeneralRegion> regions;
        if (sender instanceof Player player) {
            WorldSelection selection = WorldSelection.fromPlayer(player, this.worldEditInterface);
            regions = Utils.getWorldEditRegionsInSelection(selection.selection()).stream()
                    .map(ProtectedRegion::getId)
                    .map(this.fileManager::getRegion)
                    .filter(Objects::nonNull)
                    .toList();
        } else {
            throw new AreaShopCommandException("cmd-weOnlyByPlayer");
        }
        if (regions.isEmpty()) {
            throw new AreaShopCommandException("cmd-noWERegionsFound");
        }
        handleMassDeletion(player, regions);
    }

    private void handleSingleDeletion(@Nonnull CommandSender sender, GeneralRegion region) {
        boolean isLandlord = sender instanceof Player player && region.isLandlord(player.getUniqueId());
        if (region instanceof RentRegion) {
            // Remove the rent if the player has permission
            if (sender.hasPermission("areashop.destroyrent") || (isLandlord && sender.hasPermission(
                    "areashop.destroyrent.landlord"))) {
                DeletingRegionEvent event = fileManager.deleteRegion(region, true);
                if (event.isCancelled()) {
                    this.messageBridge.message(sender, "general-cancelled", event.getReason());
                } else {
                    this.messageBridge.message(sender, "destroy-successRent", region);
                }
            } else {
                this.messageBridge.message(sender, "destroy-noPermissionRent", region);
            }
        } else if (region instanceof BuyRegion) {
            // Remove the buy if the player has permission
            if (sender.hasPermission("areashop.destroybuy") || (isLandlord && sender.hasPermission(
                    "areashop.destroybuy.landlord"))) {
                DeletingRegionEvent event = fileManager.deleteRegion(region, true);
                if (event.isCancelled()) {
                    messageBridge.message(sender, "general-cancelled", event.getReason());
                } else {
                    messageBridge.message(sender, "destroy-successBuy", region);
                }
            } else {
                messageBridge.message(sender, "destroy-noPermissionBuy", region);
            }
        }
    }

    private void handleMassDeletion(@Nonnull Player sender, @Nonnull List<GeneralRegion> regions) {
        List<String> namesSuccess = new ArrayList<>();
        Set<GeneralRegion> regionsFailed = new TreeSet<>();
        Set<GeneralRegion> regionsCancelled = new TreeSet<>();
        for (GeneralRegion region : regions) {
            if (cannotDelete(sender, region)) {
                regionsFailed.add(region);
                continue;
            }

            DeletingRegionEvent event = this.fileManager.deleteRegion(region, true);
            if (event.isCancelled()) {
                regionsCancelled.add(region);
            } else {
                namesSuccess.add(region.getName());
            }
        }

        // Send messages
        if (!namesSuccess.isEmpty()) {
            this.messageBridge.message(sender, "del-success", Utils.createCommaSeparatedList(namesSuccess));
        }
        if (!regionsFailed.isEmpty()) {
            this.messageBridge.message(sender, "del-failed", Utils.combinedMessage(regionsFailed, "region"));
        }
        if (!regionsCancelled.isEmpty()) {
            this.messageBridge.message(sender, "del-cancelled", Utils.combinedMessage(regionsCancelled, "region"));
        }
    }

    private boolean cannotDelete(@Nonnull Player sender, @Nonnull GeneralRegion region) {
        boolean isLandlord = region.isLandlord(sender.getUniqueId());
        if (region instanceof RentRegion
                && (!sender.hasPermission("areashop.destroyrent") && !(isLandlord && sender.hasPermission(
                "areashop.destroyrent.landlord")))) {
            return true;

        }
        return region instanceof BuyRegion
                && (!sender.hasPermission("areashop.destroybuy") && !(isLandlord && sender.hasPermission(
                "areashop.destroybuy.landlord")));
    }

}










