package me.wiefferink.areashop.adapters.platform.paper;

import com.destroystokyo.paper.profile.PlayerProfile;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PaperOfflinePlayerHelper implements OfflinePlayerHelper {

    private final Plugin plugin;
    private final Server server;

    public PaperOfflinePlayerHelper(Plugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
    }

    @Override
    public CompletableFuture<Optional<UUID>> lookupUuidAsync(String username) {
        final PlayerProfile profile = this.server.createProfile(username);
        return profile.update().thenApply(PlayerProfile::getId).thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<OfflinePlayer> lookupOfflinePlayerAsync(String username) {
        final CompletableFuture<OfflinePlayer> future = new CompletableFuture<>();
        OfflinePlayer onlinePlayer = this.server.getPlayerExact(username);
        if (onlinePlayer != null) {
            future.complete(onlinePlayer);
            return future;
        }
        OfflinePlayer cachedPlayer = this.server.getOfflinePlayerIfCached(username);
        if (cachedPlayer != null) {
            return CompletableFuture.completedFuture(cachedPlayer);
        }
        this.server.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(username);
            future.complete(offlinePlayer);
        });
        return future;
    }
}
