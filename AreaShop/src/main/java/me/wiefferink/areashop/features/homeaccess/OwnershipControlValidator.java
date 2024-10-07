package me.wiefferink.areashop.features.homeaccess;

import me.wiefferink.areashop.features.FriendsFeature;
import me.wiefferink.areashop.regions.GeneralRegion;

import javax.annotation.Nonnull;
import java.util.UUID;

public class OwnershipControlValidator implements AccessControlValidator {

    public boolean canAccess(@Nonnull final UUID player,
                             @Nonnull final GeneralRegion region,
                             @Nonnull final HomeAccessType accessType) {
        return switch (accessType) {
            case ANY -> true;
            case NONE -> region.isOwner(player);
            case MEMBERS -> canAccessMembers(player, region);
        };
    }

    private boolean canAccessMembers(@Nonnull final UUID player,
                                     @Nonnull final GeneralRegion region) {
        if (region.isOwner(player)) {
            return true;
        }
        FriendsFeature friendsFeature = region.getFriendsFeature();
        if (friendsFeature == null) {
            // No friends exist therefore access denied
            return false;
        }
        return friendsFeature.getFriends().contains(player);
    }

}
