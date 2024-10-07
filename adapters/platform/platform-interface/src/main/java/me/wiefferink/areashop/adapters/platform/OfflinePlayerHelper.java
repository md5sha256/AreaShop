package me.wiefferink.areashop.adapters.platform;

import org.bukkit.OfflinePlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface OfflinePlayerHelper {

    CompletableFuture<Optional<UUID>> lookupUuidAsync(String username);

    CompletableFuture<OfflinePlayer> lookupOfflinePlayerAsync(String username);

}
