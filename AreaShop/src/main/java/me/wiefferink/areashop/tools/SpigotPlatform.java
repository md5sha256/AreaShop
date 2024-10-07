package me.wiefferink.areashop.tools;

import me.wiefferink.areashop.adapters.platform.MinecraftPlatform;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class SpigotPlatform implements MinecraftPlatform {

    private final OfflinePlayerHelper offlinePlayerHelper;

    public SpigotPlatform(@NotNull Plugin plugin) {
        this.offlinePlayerHelper = new SpigotOfflinePlayerHelper(plugin);
    }

    @Override
    public OfflinePlayerHelper offlinePlayerHelper() {
        return this.offlinePlayerHelper;
    }
}
