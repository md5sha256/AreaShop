package me.wiefferink.areashop;

import me.wiefferink.areashop.features.signs.SignManager;
import me.wiefferink.areashop.interfaces.AreaShopInterface;
import me.wiefferink.areashop.managers.FeatureManager;
import me.wiefferink.areashop.managers.SignLinkerManager;
import me.wiefferink.areashop.services.ServiceManager;

import javax.annotation.Nonnull;

public interface AreaShopApi extends AreaShopInterface {

    /**
     * Get the SignLinkerManager.
     * Handles sign linking mode.
     *
     * @return The SignLinkerManager
     */
    @Nonnull SignLinkerManager getSignlinkerManager();

    /**
     * Get the FeatureManager.
     * Manages region specific features.
     *
     * @return The FeatureManager
     */
    @Nonnull FeatureManager getFeatureManager();


    @Nonnull SignManager getSignManager();

    @Nonnull
    ServiceManager getServiceManager();

}
