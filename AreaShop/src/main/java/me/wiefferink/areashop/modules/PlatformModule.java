package me.wiefferink.areashop.modules;

import com.google.inject.AbstractModule;
import me.wiefferink.areashop.adapters.platform.MinecraftPlatform;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.tools.BukkitSchedulerExecutor;
import org.jetbrains.annotations.NotNull;

public class PlatformModule extends AbstractModule {

    private final MinecraftPlatform minecraftPlatform;

    public PlatformModule(@NotNull MinecraftPlatform platform) {
        this.minecraftPlatform = platform;
    }
    @Override
    protected void configure() {
        bind(MinecraftPlatform.class).toInstance(this.minecraftPlatform);
        bind(OfflinePlayerHelper.class).toInstance(this.minecraftPlatform.offlinePlayerHelper());
        bind(BukkitSchedulerExecutor.class).asEagerSingleton();
    }
}
