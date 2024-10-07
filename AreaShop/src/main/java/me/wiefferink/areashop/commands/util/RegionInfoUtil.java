package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class RegionInfoUtil {

    public static void showRegionInfo(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull CommandSender sender,
            @Nonnull OfflinePlayer owner
    ) {
        // Get the regions owned by the player
        Set<RentRegion> rentRegions = new HashSet<>();
        for(RentRegion region : fileManager.getRentsRef()) {
            if(region.isOwner(owner)) {
                rentRegions.add(region);
            }
        }
        Set<BuyRegion> buyRegions = new HashSet<>();
        for(BuyRegion region : fileManager.getBuysRef()) {
            if(region.isOwner(owner)) {
                buyRegions.add(region);
            }
        }
        // Get the regions the player is added as friend
        Set<GeneralRegion> friendRegions = new HashSet<>();
        for(GeneralRegion region : fileManager.getRegionsRef()) {
            if(region.getFriendsFeature().getFriends().contains(owner.getUniqueId())) {
                friendRegions.add(region);
            }
        }

        // Send messages
        boolean foundSome = !rentRegions.isEmpty() || !buyRegions.isEmpty() || !friendRegions.isEmpty();
        if(!foundSome) {
            messageBridge.message(sender, "me-nothing", owner.getName());
            return;
        }
        messageBridge.message(sender, "me-header", owner.getName());
        if(!rentRegions.isEmpty()) {
            for(RentRegion region : rentRegions) {
                messageBridge.messageNoPrefix(sender, "me-rentLine", region);
            }
        }
        if(!buyRegions.isEmpty()) {
            for(BuyRegion region : buyRegions) {
                messageBridge.messageNoPrefix(sender, "me-buyLine", region);
            }
        }
        if(!friendRegions.isEmpty()) {
            for(GeneralRegion region : friendRegions) {
                messageBridge.messageNoPrefix(sender, "me-friendLine", region);
            }
        }
        messageBridge.messageNoPrefix(sender, "me-clickHint");
    }

}
