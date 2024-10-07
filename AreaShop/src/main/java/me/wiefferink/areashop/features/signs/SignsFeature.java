package me.wiefferink.areashop.features.signs;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.features.RegionFeature;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nonnull;

public class SignsFeature extends RegionFeature {

	private final SignManager internalSignManager = new SignManager();
	private final SignManager globalSignManager;
	private final SignFactory signFactory;

	public static boolean exists(@Nonnull GeneralRegion region) {
		ConfigurationSection section = region.getConfig().getConfigurationSection("general.signs");
		return section != null && !section.getKeys(false).isEmpty();
	}

	/**
	 * Constructor.
	 * @param region The region to bind to
	 */
	@AssistedInject
	SignsFeature(@Nonnull AreaShop plugin,
						@Nonnull SignManager signManager,
						@Nonnull SignFactory signFactory,
						@Assisted @Nonnull GeneralRegion region
	) {
		super(plugin);
		this.globalSignManager = signManager;
		this.signFactory = signFactory;
		setRegion(region);
		// Setup current signs
		ConfigurationSection signSection = region.getConfig().getConfigurationSection("general.signs");
		if (signSection == null) {
			return;
		}
		for(String signKey : signSection.getKeys(false)) {
			RegionSign sign = signFactory.createRegionSign(this, signKey);
			Location location = sign.getLocation();
			if(location == null) {
				AreaShop.warn("Sign with key " + signKey + " of region " + region.getName() + " does not have a proper location");
				continue;
			}
			this.globalSignManager.cacheForWorld(location.getWorld()).addSign(sign);
			this.internalSignManager.addSign(sign);
		}
	}

	/**
	 * Convert a location to a string to use as map key.
	 * @param location The location to get the key for
	 * @return A string to use in a map for a location
	 */
	public static String locationToString(Location location) {
		return location.getWorld().getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
	}

	/**
	 * Convert a chunk to a string to use as map key.
	 * @param location The location to get the key for
	 * @return A string to use in a map for a chunk
	 */
	public static String chunkToString(Location location) {
		return location.getWorld().getName() + ";" + (location.getBlockX() >> 4) + ";" + (location.getBlockZ() >> 4);
	}

	@Override
	public void shutdown() {

	}

	public SignManager signManager() {
		return this.internalSignManager;
	}

	/**
	 * Add a sign to this region.
	 * @param location The location of the sign
	 * @param signType The type of the sign (WALL_SIGN or SIGN_POST)
	 * @param facing   The orientation of the sign
	 * @param profile  The profile to use with this sign (null for default)
	 */
	public void addSign(Location location, Material signType, BlockFace facing, String profile) {
		int i = 0;
		while(getRegion().getConfig().isSet("general.signs." + i)) {
			i++;
		}
		String signPath = "general.signs." + i + ".";
		getRegion().setSetting(signPath + "location", Utils.locationToConfig(location));
		getRegion().setSetting(signPath + "facing", facing != null ? facing.name() : null);
		getRegion().setSetting(signPath + "signType", signType != null ? signType.name() : null);
		if(profile != null && !profile.isEmpty()) {
			getRegion().setSetting(signPath + "profile", profile);
		}
		// Add to the map
		RegionSign regionSign = this.signFactory.createRegionSign(this, String.valueOf(i));
		this.globalSignManager.addSign(regionSign);
		this.internalSignManager.addSign(regionSign);
	}

}
