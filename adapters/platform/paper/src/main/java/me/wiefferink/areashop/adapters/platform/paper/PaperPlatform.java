package me.wiefferink.areashop.adapters.platform.paper;

import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.adapters.platform.MinecraftPlatform;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PaperPlatform implements MinecraftPlatform {

    private final OfflinePlayerHelper offlinePlayerHelper;

    public PaperPlatform(@NotNull Plugin plugin) {
        this.offlinePlayerHelper = new PaperOfflinePlayerHelper(plugin);
    }

    @Override
    public OfflinePlayerHelper offlinePlayerHelper() {
        return this.offlinePlayerHelper;
    }
}
