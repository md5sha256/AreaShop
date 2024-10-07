package me.wiefferink.areashop.features.homeaccess;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.features.RegionFeature;
import me.wiefferink.areashop.regions.GeneralRegion;

import javax.annotation.Nonnull;
import java.util.Locale;

public final class HomeAccessFeature extends RegionFeature {
    private HomeAccessType homeAccessType;

    @AssistedInject
    public HomeAccessFeature(
            @Assisted final GeneralRegion region,
            @Nonnull final AreaShop plugin
    ) {
        super(plugin);
        setRegion(region);
        this.homeAccessType = parseAccessType();
    }

    private @Nonnull HomeAccessType parseAccessType() {
        String rawAccessType = getRegion().getConfig()
                .getString("sethomecontrol.homeaccesstype", HomeAccessType.ANY.name());
        try {
            return HomeAccessType.valueOf(rawAccessType.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            AreaShop.error("Invalid HomeAccessType: " + rawAccessType + " in region: " + getRegion().getName());
            return HomeAccessType.ANY;
        }
    }

    private void saveAccessType(@Nonnull final HomeAccessType homeAccessType) {
        getRegion().getConfig().set("sethomecontrol.homeaccesstype", homeAccessType.name());
        getRegion().saveRequired();
    }

    public @Nonnull HomeAccessType homeAccessType() {
        return this.homeAccessType;
    }

    public void homeAccessType(@Nonnull final HomeAccessType homeAccessType) {
        if (this.homeAccessType != homeAccessType) {
            this.homeAccessType = homeAccessType;
            saveAccessType(homeAccessType);
        }
    }


}
