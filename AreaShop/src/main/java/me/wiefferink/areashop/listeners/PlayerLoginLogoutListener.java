package me.wiefferink.areashop.listeners;

import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.areashop.wrapper.CacheWrapper;
import me.wiefferink.bukkitdo.Do;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Notify region expiry and track activity time.
 */
public final class PlayerLoginLogoutListener implements Listener {

	private final AreaShop plugin;
	private final MessageBridge messageBridge;

	/**
	 * Constructor.
	 * @param plugin The AreaShop plugin
	 */
	public PlayerLoginLogoutListener(@Nonnull AreaShop plugin, @Nonnull MessageBridge messageBridge) {
		this.plugin = plugin;
		this.messageBridge = messageBridge;
	}

	/**
	 * Called when a sign is changed.
	 * @param event The event
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerLogin(PlayerLoginEvent event) {
		if(event.getResult() != Result.ALLOWED) {
			return;
		}
		final Player player = event.getPlayer();

        plugin.getCacheManager().cache().computeIfAbsent(player.getUniqueId(), uuid -> new CacheWrapper()).setName(player.getName());

		// Schedule task to check for notifications, prevents a lag spike at login
		Do.syncTimerLater(25, 25, () -> {
			// Delay until all regions are loaded
			if(!plugin.isReady()) {
				return true;
			}
			if(!player.isOnline()) {
				return false;
			}
			// Notify for rents that almost run out
			for(RentRegion region : plugin.getFileManager().getRentsRef()) {
				if(region.isRenter(player)) {
					String warningSetting = region.getStringSetting("rent.warningOnLoginTime");
					if(warningSetting == null || warningSetting.isEmpty()) {
						continue;
					}
					long warningTime = Utils.durationStringToLong(warningSetting);
					if(region.getTimeLeft() < warningTime) {
						// Send the warning message later to let it appear after general MOTD messages
						 messageBridge.message(player, "rent-expireWarning", region);
					}
				}
			}

			// Notify admins for plugin updates
			this.plugin.notifyUpdate(player);
			return false;
		});

		// Check if the player has regions that use an old name of him and update them
		Do.syncTimerLater(22, 10, () -> {
			if(!plugin.isReady()) {
				return true;
			}

			List<GeneralRegion> regions = new ArrayList<>();
			for(GeneralRegion region : plugin.getFileManager().getRegionsRef()) {
				if(region.isOwner(player)) {
					regions.add(region);
				}
			}

			Do.forAll(
				plugin.getConfig().getInt("nameupdate.regionsPerTick"),
				regions,
				region -> {
					if(region instanceof BuyRegion) {
						if(!player.getName().equals(region.getStringSetting("buy.buyerName"))) {
							region.setSetting("buy.buyerName", player.getName());
							region.update();
						}
					} else if(region instanceof RentRegion) {
						if(!player.getName().equals(region.getStringSetting("rent.renterName"))) {
							region.setSetting("rent.renterName", player.getName());
							region.update();
						}
					}
				}
			);
			return false;
		});
	}

	// Active time updates
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerLogout(PlayerQuitEvent event) {
		updateLastActive(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerKick(PlayerKickEvent event) {
		updateLastActive(event.getPlayer());
	}

	/**
	 * Update the last active time for all regions the player is owner off.
	 * @param player The player to update the active times for
	 */
	private void updateLastActive(Player player) {
		for(GeneralRegion region : plugin.getFileManager().getRegionsRef()) {
			if(region.isOwner(player)) {
				region.updateLastActiveTime();
			}
		}
	}
}
































