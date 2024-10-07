package me.wiefferink.areashop.features;

import me.wiefferink.areashop.features.homeaccess.HomeAccessFeature;
import me.wiefferink.areashop.features.signs.SignsFeature;
import me.wiefferink.areashop.regions.GeneralRegion;

import javax.annotation.Nonnull;

public interface FeatureFactory {

    @Nonnull
    SignsFeature createSignsFeature(@Nonnull GeneralRegion generalRegion);

    @Nonnull
    TeleportFeature createTeleportFeature(@Nonnull GeneralRegion generalRegion);

    @Nonnull
    FriendsFeature createFriendsFeature(@Nonnull GeneralRegion generalRegion);

    @Nonnull
    HomeAccessFeature createHomeAccessFeature(@Nonnull GeneralRegion generalRegion);

}
