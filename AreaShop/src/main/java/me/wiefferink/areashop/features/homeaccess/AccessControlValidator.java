package me.wiefferink.areashop.features.homeaccess;

import me.wiefferink.areashop.regions.GeneralRegion;

import javax.annotation.Nonnull;
import java.util.UUID;

@FunctionalInterface
public interface AccessControlValidator {

    /**
     * Whether a player can access a given {@link GeneralRegion} via a home
     *
     * @param player     The uuid of the player attempting to access the region via a home
     * @param region     The region
     * @param accessType The type of access permission the player holds
     * @return true if a player is permitted to set a home, false otherwise.
     */
    boolean canAccess(
            @Nonnull final UUID player,
            @Nonnull final GeneralRegion region,
            @Nonnull final HomeAccessType accessType
    );

    default AccessControlValidator and(@Nonnull final AccessControlValidator other) {
        return (player, region, accessType) -> this.canAccess(player, region, accessType)
                && other.canAccess(player, region, accessType);
    }

    default AccessControlValidator or(@Nonnull final AccessControlValidator other) {
        return (player, region, accessType) -> this.canAccess(player, region, accessType)
                || other.canAccess(player, region, accessType);
    }

}
