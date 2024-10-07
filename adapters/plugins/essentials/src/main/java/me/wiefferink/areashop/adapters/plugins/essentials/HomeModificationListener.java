package me.wiefferink.areashop.adapters.plugins.essentials;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.features.homeaccess.AccessControlValidator;
import me.wiefferink.areashop.features.homeaccess.HomeAccessFeature;
import me.wiefferink.areashop.features.homeaccess.HomeAccessType;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import net.ess3.api.IUser;
import net.essentialsx.api.v2.events.HomeModifyEvent;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.annotation.Nonnull;
import java.util.Optional;

public class HomeModificationListener implements Listener {

    private final AccessControlValidator accessControlValidator;
    private final IFileManager fileManager;
    private final WorldGuardInterface worldGuardInterface;
    private final MessageBridge messageBridge;
    private final Server server;

    @AssistedInject
    public HomeModificationListener(
            @Assisted AccessControlValidator validator,
            @Nonnull IFileManager fileManager,
            @Nonnull WorldGuardInterface worldGuardInterface,
            @Nonnull MessageBridge messageBridge,
            @Nonnull Server server
    ) {
        this.accessControlValidator = validator;
        this.fileManager = fileManager;
        this.worldGuardInterface = worldGuardInterface;
        this.server = server;
        this.messageBridge = messageBridge;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHomeModification(@Nonnull final HomeModifyEvent event) {
        final IUser homeOwner = event.getHomeOwner();
        final HomeModifyEvent.HomeModifyCause cause = event.getCause();
        if (cause != HomeModifyEvent.HomeModifyCause.UPDATE && cause != HomeModifyEvent.HomeModifyCause.CREATE) {
            return;
        }
        final Location newLocation = event.getNewLocation();
        final RegionManager regionManager = this.worldGuardInterface.getRegionManager(newLocation.getWorld());
        if (regionManager == null) {
            return;
        }
        final BlockVector3 position = BukkitAdapter.adapt(newLocation).toVector().toBlockPoint();
        final ApplicableRegionSet protectedRegions = regionManager.getApplicableRegions(position);
        for (ProtectedRegion protectedRegion : protectedRegions.getRegions()) {
            processHomeModification(event, homeOwner, protectedRegion);
        }
    }

    private void processHomeModification(
            @Nonnull Cancellable event,
            @Nonnull IUser homeOwner,
            @Nonnull ProtectedRegion protectedRegion) {
        final GeneralRegion region = this.fileManager.getRegion(protectedRegion.getId());
        if (region == null) {
            return;
        }
        final Optional<HomeAccessFeature> optionalFeature = region.getFeature(HomeAccessFeature.class);
        if (optionalFeature.isEmpty()) {
            return;
        }
        final HomeAccessType accessType = optionalFeature.get().homeAccessType();
        if (this.accessControlValidator.canAccess(homeOwner.getUUID(), region, accessType)) {
            return;
        }
        event.setCancelled(true);
        Player player = this.server.getPlayer(homeOwner.getUUID());
        if (player != null) {
            this.messageBridge.message(player, "togglehome-sethomeDenied");
        }
    }

}
