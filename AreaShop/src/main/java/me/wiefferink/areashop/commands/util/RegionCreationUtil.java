package me.wiefferink.areashop.commands.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.managers.IFileManager;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

@Singleton
public class RegionCreationUtil {


    private final Plugin plugin;
    private final IFileManager fileManager;
    private final Server server;
    private final WorldGuardInterface worldGuardInterface;

    @Inject
    public RegionCreationUtil(
            @Nonnull WorldGuardInterface worldGuardInterface,
            @Nonnull IFileManager fileManager,
            @Nonnull Server server,
            @Nonnull Plugin plugin
    ) {
        this.worldGuardInterface = worldGuardInterface;
        this.fileManager = fileManager;
        this.plugin = plugin;
        this.server = server;
    }

    public CompletableFuture<ProtectedRegion> createRegion(
            @Nonnull CommandContext<?> context,
            @Nonnull Player sender,
            @Nonnull CloudKey<String> regionKey
    ) {
        World world = sender.getWorld();
        String regionName = context.get(regionKey);
        if (this.fileManager.getRegion(regionName) != null) {
            return CompletableFuture.failedFuture(new AreaShopCommandException("add-failed", regionName));
        }
        this.server.dispatchCommand(sender, String.format("rg define %s", regionName));
        CompletableFuture<ProtectedRegion> future = new CompletableFuture<>();
        this.server.getScheduler().runTaskLater(this.plugin, () -> {
            ProtectedRegion region = this.worldGuardInterface.getRegionManager(world).getRegion(regionName);
            if (region == null) {
                future.completeExceptionally(new AreaShopCommandException("quickadd-failedCreateWGRegion"));
                return;
            }
            future.complete(region);
        }, 10);
        return future;
    }

}
