package me.wiefferink.areashop.tools;

import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SpigotOfflinePlayerHelper implements OfflinePlayerHelper {

    private final Plugin plugin;

    public SpigotOfflinePlayerHelper(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Optional<UUID>> lookupUuidAsync(String username) {
        return lookupOfflinePlayerAsync(username).thenApply(player -> {
            if (player.hasPlayedBefore() || player.isOnline()) {
                return Optional.of(player.getUniqueId());
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<OfflinePlayer> lookupOfflinePlayerAsync(String username) {
        final CompletableFuture<OfflinePlayer> future = new CompletableFuture<>();
        OfflinePlayer onlinePlayer = this.plugin.getServer().getPlayerExact(username);
        if (onlinePlayer != null) {
            future.complete(onlinePlayer);
            return future;
        }
        final Server server = this.plugin.getServer();
        server.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = server.getOfflinePlayer(username);
            future.complete(player);
        });
        return future;
    }
}
