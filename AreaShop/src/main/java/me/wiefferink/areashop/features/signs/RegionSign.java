package me.wiefferink.areashop.features.signs;

import com.google.common.base.Objects;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.papermc.lib.PaperLib;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.managers.SignErrorLogger;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.tools.Materials;
import me.wiefferink.areashop.tools.SignUtils;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.interactivemessenger.processing.Message;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Sign that is connected to a region to display information and interact with the region.
 */
public class RegionSign {

	private final SignErrorLogger errorLogger;
	private final Server server;

	private final SignsFeature signsFeature;
	private final String key;


	@AssistedInject
	RegionSign(
			@Nonnull SignErrorLogger signErrorLogger,
			@Nonnull Server server,
			@Assisted @Nonnull SignsFeature signsFeature,
			@Assisted @Nonnull String key
	) {
		this.errorLogger = signErrorLogger;
		this.server = server;
		this.signsFeature = signsFeature;
		this.key = key;
	}

	public BlockPosition getPosition() {
		return new BlockPosition(getLocation());
	}

	/**
	 * Get the location of this sign.
	 * @return The location of this sign
	 */
	public Location getLocation() {
		return Utils.configToLocation(getRegion().getConfig().getConfigurationSection("general.signs." + key + ".location"));
	}

	/**
	 * Location string to be used as key in maps.
	 * @return Location string
	 */
	public String getStringLocation() {
		return SignsFeature.locationToString(getLocation());
	}

	/**
	 * Chunk string to be used as key in maps.
	 * @return Chunk string
	 */
	public String getStringChunk() {
		return SignsFeature.chunkToString(getLocation());
	}

	/**
	 * Get the region this sign is linked to.
	 * @return The region this sign is linked to
	 */
	public GeneralRegion getRegion() {
		return signsFeature.getRegion();
	}

	/**
	 * Remove this sign from the region.
	 */
	public void remove() {
		Location location = getLocation();
		if (location != null && location.getWorld() != null) {
			location.getBlock().setType(Material.AIR);
			this.signsFeature.signManager().removeSign(this);
		}
		getRegion().setSetting("general.signs." + key, null);
		// Remove the sign from the region's sign manager
	}

	/**
	 * Get the ConfigurationSection defining the sign layout.
	 * @return The sign layout config
	 */
	public ConfigurationSection getProfile() {
		return getRegion().getConfigurationSectionSetting("general.signProfile", "signProfiles", getRegion().getConfig().get("general.signs." + key + ".profile"));
	}

	/**
	 * Get the facing of the sign as saved in the config.
	 * @return BlockFace the sign faces, or null if unknown
	 */
	public BlockFace getFacing() {
		try {
			return BlockFace.valueOf(getRegion().getConfig().getString("general.signs." + key + ".facing"));
		} catch(NullPointerException | IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Get the material of the sign as saved in the config.
	 * @return Material of the sign,Material.AIR if none.
	 */
	public Material getMaterial() {
		String name = getRegion().getConfig().getString("general.signs." + key + ".signType");
		Material result = Materials.signNameToMaterial(name);
		return result == null ? Material.AIR : result;
	}

	/**
	 * Update this sign.
	 * @return true if the update was successful, otherwise false
	 */
	public boolean update() {
		// Ignore updates of signs in chunks that are not loaded
		Location signLocation = getLocation();
		if(signLocation == null
				|| signLocation.getWorld() == null
				|| !signLocation.getWorld().isChunkLoaded(signLocation.getBlockX() >> 4, signLocation.getBlockZ() >> 4)) {
			return false;
		}

		if(getRegion().isDeleted()) {
			return false;
		}

		YamlConfiguration regionConfig = getRegion().getConfig();
		ConfigurationSection signConfig = getProfile();
		Block block = signLocation.getBlock();
		if(signConfig == null || !signConfig.isSet(getRegion().getState().getValue())) {
			block.setType(Material.AIR);
			return true;
		}

		ConfigurationSection stateConfig = signConfig.getConfigurationSection(getRegion().getState().getValue());

		// Get the lines
		String[] signLines = new String[4];
		boolean signEmpty = true;
		for(int i = 0; i < 4; i++) {
			signLines[i] = stateConfig.getString("line" + (i + 1));
			signEmpty &= (signLines[i] == null || signLines[i].isEmpty());
		}
		if(signEmpty) {
			block.setType(Material.AIR);
			return true;
		}

		// Place the sign back (with proper rotation and type) after it has been hidden or (indirectly) destroyed
		if(!Materials.isSign(block.getType())) {
			Material signType = getMaterial();
			if (!block.canPlace(this.server.createBlockData(signType))) {
				errorLogger.submitWarning("Setting sign" +  key +  "of region" + getRegion().getName() +  "failed, could not set sign block back");
				this.remove();
				return false;
			}
			// Don't do physics here, we first need to update the direction
			block.setType(signType, false);

			BlockState blockState = PaperLib.getBlockState(block, false).getState();
			BlockData blockData = blockState.getBlockData();

			if(blockData instanceof WallSign) {
				((WallSign) blockData).setFacing(getFacing());
			} else if(blockData instanceof org.bukkit.block.data.type.Sign) {
				((org.bukkit.block.data.type.Sign) blockData).setRotation(getFacing());
			} else {
				errorLogger.submitWarning("Failed to update the facing direction of the sign at" + getStringLocation() + "to " + getFacing() + ", region:" + getRegion().getName());
				return false;
			}
			block.setBlockData(blockData);

			// Check if the sign has popped
			if(!Materials.isSign(block.getType())) {
				return false;
			}
		}

		// Save current rotation and type
		if(!regionConfig.isString("general.signs." + key + ".signType")) {
			getRegion().setSetting("general.signs." + key + ".signType", block.getType().name());
		}
		if(!regionConfig.isString("general.signs." + key + ".facing")) {
			BlockFace signFacing = SignUtils.getSignFacing(block);
			getRegion().setSetting("general.signs." + key + ".facing", signFacing == null ? null : signFacing.toString());
		}

		// Apply replacements and color and then set it on the sign
		Sign signState = (Sign) PaperLib.getBlockState(block, false).getState();
		for(int i = 0; i < signLines.length; i++) {
			if(signLines[i] == null) {
				signState.setLine(i, "");
				continue;
			}
			signLines[i] = Message.fromString(signLines[i]).replacements(getRegion()).getSingle();
			signLines[i] = Utils.applyColors(signLines[i]);
			signState.setLine(i, signLines[i]);
		}
		signState.update(false, false);
		return true;
	}

	/**
	 * Check if the sign needs to update periodically.
	 * @return true if it needs periodic updates, otherwise false
	 */
	public boolean needsPeriodicUpdate() {
		ConfigurationSection signConfig = getProfile();
		if(signConfig == null || !signConfig.isSet(getRegion().getState().getValue().toLowerCase())) {
			return false;
		}

		ConfigurationSection stateConfig = signConfig.getConfigurationSection(getRegion().getState().getValue().toLowerCase());
		if(stateConfig == null) {
			return false;
		}

		// Check the lines for the timeleft tag
		for(int i = 1; i <= 4; i++) {
			String line = stateConfig.getString("line" + i);
			if(line != null && line.contains(Message.VARIABLE_START + AreaShop.tagTimeLeft + Message.VARIABLE_END)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Run commands when a player clicks a sign.
	 * @param clicker   The player that clicked the sign
	 * @param clickType The type of clicking
	 * @return true if the commands ran successfully, false if any of them failed
	 */
	public boolean runSignCommands(Player clicker, GeneralRegion.ClickType clickType) {
		ConfigurationSection signConfig = getProfile();
		if(signConfig == null) {
			return false;
		}
		ConfigurationSection stateConfig = signConfig.getConfigurationSection(getRegion().getState().getValue().toLowerCase());

		// Run player commands if specified
		List<String> playerCommands = new ArrayList<>();
		for(String command : stateConfig.getStringList(clickType.getValue() + "Player")) {
			// TODO move variable checking code to InteractiveMessenger?
			playerCommands.add(command.replace(Message.VARIABLE_START + AreaShop.tagClicker + Message.VARIABLE_END, clicker.getName()));
		}
		getRegion().runCommands(clicker, playerCommands);

		// Run console commands if specified
		List<String> consoleCommands = new ArrayList<>();
		for(String command : stateConfig.getStringList(clickType.getValue() + "Console")) {
			consoleCommands.add(command.replace(Message.VARIABLE_START + AreaShop.tagClicker + Message.VARIABLE_END, clicker.getName()));
		}
		getRegion().runCommands(this.server.getConsoleSender(), consoleCommands);

		return !playerCommands.isEmpty() || !consoleCommands.isEmpty();
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof RegionSign && ((RegionSign)object).getRegion().equals(this.getRegion()) && ((RegionSign)object).key.equals(this.key);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(key, getRegion().getName());
	}
}
