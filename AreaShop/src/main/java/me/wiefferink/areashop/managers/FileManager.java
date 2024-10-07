package me.wiefferink.areashop.managers;

import com.google.common.io.Files;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.bakedlibs.dough.blocks.BlockPosition;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.events.ask.AddingRegionEvent;
import me.wiefferink.areashop.events.ask.DeletingRegionEvent;
import me.wiefferink.areashop.events.notify.AddedRegionEvent;
import me.wiefferink.areashop.events.notify.DeletedRegionEvent;
import me.wiefferink.areashop.interfaces.GeneralRegionInterface;
import me.wiefferink.areashop.features.signs.SignManager;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.GeneralRegion.RegionEvent;
import me.wiefferink.areashop.regions.GeneralRegion.RegionType;
import me.wiefferink.areashop.regions.IRegionContainer;
import me.wiefferink.areashop.regions.RegionFactory;
import me.wiefferink.areashop.regions.RegionGroup;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.regions.SimpleRegionContainer;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.bukkitdo.Do;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class FileManager extends Manager implements IFileManager {

	private final AreaShop plugin;

	private final IRegionContainer regionContainer = new SimpleRegionContainer();
	private final String regionsPath;
	private final Map<String, RegionGroup> groups;
	private final String configPath;
	private YamlConfiguration config = null;
	private final String groupsPath;
	private YamlConfiguration groupsConfig = null;
	private final String defaultPath;
	private YamlConfiguration defaultConfig = null;
	private YamlConfiguration defaultConfigFallback = null;
	private boolean saveGroupsRequired = false;
	private final Set<String> worldRegionsRequireSaving;

	private HashMap<String, Integer> versions = null;
	private final String versionPath;
	private final String schemFolder;

	private final SignManager signManager;
	private final WorldGuardInterface worldGuardInterface;
	private final MessageBridge messageBridge;
	private final RegionFactory regionFactory;

	/**
	 * Constructor, initialize variabeles.
	 */
	@Inject
	FileManager(
			@Nonnull AreaShop plugin,
			@Nonnull WorldGuardInterface worldGuardInterface,
			@Nonnull MessageBridge messageBridge,
			@Nonnull RegionFactory regionFactory,
			@Nonnull SignManager signManager
	) {
		this.plugin = plugin;
		this.worldGuardInterface = worldGuardInterface;
		this.messageBridge = messageBridge;
		this.regionFactory = regionFactory;
		this.signManager = signManager;
		regionsPath = plugin.getDataFolder() + File.separator + AreaShop.regionsFolder;
		configPath = plugin.getDataFolder() + File.separator + "config.yml";
		groups = new HashMap<>();
		groupsPath = plugin.getDataFolder() + File.separator + AreaShop.groupsFile;
		defaultPath = plugin.getDataFolder() + File.separator + AreaShop.defaultFile;
		versionPath = plugin.getDataFolder().getPath() + File.separator + AreaShop.versionFile;
		schemFolder = plugin.getDataFolder() + File.separator + AreaShop.schematicFolder;
		worldRegionsRequireSaving = new HashSet<>();
		File schemFile = new File(schemFolder);
		if(!schemFile.exists() & !schemFile.mkdirs()) {
			AreaShop.warn("Could not create schematic files directory: " + schemFile.getAbsolutePath());
		}
		loadVersions();
	}

	@Override
	public void shutdown() {
		// Update lastactive time for players that are online now
		for(GeneralRegion region : this.regionContainer.getRegionsRef()) {
			UUID ownerUuid = region.getOwner();
			if (ownerUuid == null) {
				continue;
			}
			Player player = Bukkit.getPlayer(ownerUuid);
			if(player != null) {
				region.updateLastActiveTime();
			}
		}
		// Save files that need to be saved
		saveRequiredFilesAtOnce();
	}


	//////////////////////////////////////////////////////////
	// GETTERS
	//////////////////////////////////////////////////////////

	/**
	 * Get the folder where schematics are stored.
	 * @return The folder where schematics are stored
	 */
	@Override
	public String getSchematicFolder() {
		return schemFolder;
	}

	/**
	 * Get a group.
	 * @param name The name of the group to get (will be normalized)
	 * @return The group if found, otherwise null
	 */
	@Override
	public RegionGroup getGroup(String name) {
		return groups.get(name.toLowerCase());
	}

	/**
	 * Get all groups.
	 * @return Collection with all groups (safe to modify)
	 */
	@Override
	public Collection<RegionGroup> getGroups() {
		return groups.values();
	}

	/**
	 * Get the default region settings as provided by the user (default.yml).
	 * @return YamlConfiguration with the settings (might miss settings, which should be filled in with {@link #getFallbackRegionSettings()})
	 */
	@Override
	public YamlConfiguration getRegionSettings() {
		return defaultConfig;
	}

	/**
	 * Get the default regions settings as provided by AreaShop (default.yml).
	 * @return YamlConfiguration with the default settings
	 */
	@Override
	public YamlConfiguration getFallbackRegionSettings() {
		return defaultConfigFallback;
	}

	/**
	 * Get the config file (config.yml).
	 * @return YamlConfiguration with the settings of users, with fallback to the settings provided by AreaShop
	 */
	@Override
	public YamlConfiguration getConfig() {
		return config;
	}

	@Nullable
	@Override
	public GeneralRegionInterface getRegionInterface(String name) {
		return this.regionContainer.getRegionInterface(name);
	}

	@Override
	public List<GeneralRegionInterface> getRegionInterfaces() {
		return this.regionContainer.getRegionInterfaces();
	}

	@Override
	public Collection<GeneralRegionInterface> getRegionInterfacesRef() {
		return this.regionContainer.getRegionInterfacesRef();
	}

	/**
	 * Get a region.
	 * @param name The name of the region to get (will be normalized)
	 * @return The region if found, otherwise null
	 */
	@Override
	public @Nullable GeneralRegion getRegion(String name) {
		return this.regionContainer.getRegion(name);
	}

	/**
	 * Get a rental region.
	 * @param name The name of the rental region (will be normalized)
	 * @return RentRegion if it could be found, otherwise null
	 */
	@Override
	public @Nullable RentRegion getRent(String name) {
		return this.regionContainer.getRent(name);
	}

	/**
	 * Get a buy region.
	 * @param name The name of the buy region (will be normalized)
	 * @return BuyRegion if it could be found, otherwise null
	 */
	@Override
	public @Nullable BuyRegion getBuy(String name) {
		return this.regionContainer.getBuy(name);
	}

	/**
	 * Get all rental regions.
	 * @return List of all rental regions
	 */
	@Override
	public List<RentRegion> getRents() {
		return this.regionContainer.getRents();
	}

	/**
	 * Get all rental regions.
	 * @return List of all rental regions
	 */
	@Override
	public Collection<RentRegion> getRentsRef() {
		return this.regionContainer.getRentsRef();
	}

	/**
	 * Get all buy regions.
	 * @return List of all buy regions
	 */
	@Override
	public List<BuyRegion> getBuys() {
		return this.regionContainer.getBuys();
	}

	@Override
	public Collection<BuyRegion> getBuysRef() {
		return this.regionContainer.getBuysRef();
	}


	/**
	 * Get all regions.
	 * @return List of all regions (it is safe to modify the list)
	 */
	@Override
	public List<GeneralRegion> getRegions() {
		return this.regionContainer.getRegions();
	}

	/**
	 * Get all regions.
	 * @return List of all regions (it is safe to modify the list)
	 */
	@Override
	public Collection<GeneralRegion> getRegionsRef() {
		return this.regionContainer.getRegionsRef();
	}

	/**
	 * Get a list of names of all buy regions.
	 * @return A String list with all the names
	 */
	@Override
	public List<String> getBuyNames() {
		return this.regionContainer.getBuyNames();
	}

	/**
	 * Get a list of names of all rent regions.
	 * @return A String list with all the names
	 */
	@Override
	public List<String> getRentNames() {
		return this.regionContainer.getRentNames();
	}

	/**
	 * Get a list of names of all regions.
	 * @return A String list with all the names
	 */
	@Override
	public List<String> getRegionNames() {
		return this.regionContainer.getRegionNames();
	}

	@Override
	public List<String> getRegionInterfaceNames() {
		return this.regionContainer.getRegionInterfaceNames();
	}

	/**
	 * Get a list of names of all groups.
	 * @return A String list with all the names
	 */
	@Override
	public List<String> getGroupNames() {
		ArrayList<String> result = new ArrayList<>();
		for(RegionGroup group : groups.values()) {
			result.add(group.getName());
		}
		return result;
	}

	/**
	 * Add a region to the list and mark it as to-be-saved.
	 * @param region Then region to add
	 * @return true when successful, otherwise false (denied by an event listener)
	 */
	@Override
	public AddingRegionEvent addRegion(GeneralRegion region) {
		AddingRegionEvent event = addRegionNoSave(region);
		if (event.isCancelled()) {
			return event;
		}
		region.saveRequired();
		markGroupsAutoDirty();
		return event;
	}

	/**
	 * Add a region to the list without saving it to disk (useful for loading at startup).
	 * @param region The region to add
	 * @return true when successful, otherwise false (denied by an event listener)
	 */
	@Override
	public AddingRegionEvent addRegionNoSave(GeneralRegion region) {
		AddingRegionEvent event = new AddingRegionEvent(region);
		if(region == null) {
			AreaShop.debug("Tried adding a null region!");
			event.cancel("null region");
			return event;
		}
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return event;
		}
		this.regionContainer.addRegionInterface(region);
		Bukkit.getPluginManager().callEvent(new AddedRegionEvent(region));
		return event;
	}

	/**
	 * Mark all RegionGroups that they should regenerate regions.
	 */
	@Override
	public void markGroupsAutoDirty() {
		for(RegionGroup group : getGroups()) {
			group.autoDirty();
		}
	}

	/**
	 * Add a RegionGroup.
	 * @param group The RegionGroup to add
	 */
	@Override
	public void addGroup(RegionGroup group) {
		groups.put(group.getName().toLowerCase(), group);
		String lowGroup = group.getName().toLowerCase();
		groupsConfig.set(lowGroup + ".name", group.getName());
		groupsConfig.set(lowGroup + ".priority", 0);
		saveGroupsIsRequired();
	}

	/**
	 * Check if a player can add a certain region as rent or buy region.
	 * @param sender The player/console that wants to add a region
	 * @param region The WorldGuard region to add
	 * @param world The world the ProtectedRegion is located in
	 * @param type   The type the region should have in AreaShop
	 * @return The result if a player would want to add this region
	 */
	@Override
	public AddResult checkRegionAdd(CommandSender sender,
									ProtectedRegion region,
									World world,
									RegionType type) {
		Player player = null;
		if(sender instanceof Player) {
			player = (Player)sender;
		}
		// Determine if the player is an owner or member of the region
		boolean isMember = player != null && worldGuardInterface.containsMember(region, player.getUniqueId());
		boolean isOwner = player != null && worldGuardInterface.containsOwner(region, player.getUniqueId());
		AreaShop.debug("checkRegionAdd: isOwner=" + isOwner + ", isMember=" + isMember);
		String typeString;
		if(type == RegionType.RENT) {
			typeString = "rent";
		} else {
			typeString = "buy";
		}
		AreaShop.debug("  permissions: .create=" + sender.hasPermission("areashop.create" + typeString) + ", .create.owner=" + sender.hasPermission("areashop.create" + typeString + ".owner") + ", .create.member=" + sender.hasPermission("areashop.create" + typeString + ".member"));
		if(!(sender.hasPermission("areashop.create" + typeString)
				|| (sender.hasPermission("areashop.create" + typeString + ".owner") && isOwner)
				|| (sender.hasPermission("areashop.create" + typeString + ".member") && isMember))) {
			return AddResult.NOPERMISSION;
		}
		GeneralRegion asRegion = plugin.getFileManager().getRegion(region.getId());
		if(asRegion != null) {
			if(asRegion.getWorld().equals(world)) {
				return AddResult.ALREADYADDED;
			} else {
				return AddResult.ALREADYADDEDOTHERWORLD;
			}
		} else if(plugin.getFileManager().isBlacklisted(region.getId())) {
			return AddResult.BLACKLISTED;
		} else {
			return AddResult.SUCCESS;
		}
	}


	/**
	 * Remove a region from the list.
	 * @param region The region to remove
	 * @param giveMoneyBack use true to give money back to the player if someone is currently holding this region, otherwise false
	 * @return true if the region has been removed, false otherwise
	 */
	@Override
	public DeletingRegionEvent deleteRegion(GeneralRegion region, boolean giveMoneyBack) {
		DeletingRegionEvent event = new DeletingRegionEvent(region);
		if(region == null) {
			event.cancel("null region");
			return event;
		}

		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return event;
		}

		region.setDeleted();
		if(region instanceof RentRegion && ((RentRegion)region).isRented()) {
			((RentRegion)region).unRent(giveMoneyBack, null);
		} else if (region instanceof BuyRegion && ((BuyRegion)region).isSold()) {
			((BuyRegion)region).sell(giveMoneyBack, null);
		}

		// Handle schematics
		region.handleSchematicEvent(RegionEvent.DELETED);

		// Delete the signs
		if(region.getWorld() != null) {
			SignManager regionSignManager = region.getSignsFeature().signManager();
			for(BlockPosition sign : regionSignManager.allSignLocations()) {
				sign.getBlock().setType(Material.AIR);
				regionSignManager.removeSign(sign);
				this.signManager.removeSign(sign);
			}
		}

		// Remove from RegionGroups
		RegionGroup[] regionGroups = getGroups().toArray(new RegionGroup[0]);
		for(RegionGroup group : regionGroups) {
			group.removeMember(region);
		}

		region.resetRegionFlags();
		String name = region.getLowerCaseName();
		this.regionContainer.removeRegion(name);

		// Remove file
		File file = new File(plugin.getDataFolder() + File.separator + AreaShop.regionsFolder + File.separator + region.getLowerCaseName() + ".yml");
		if(file.exists()) {
			boolean deleted;
			try {
				deleted = file.delete();
			} catch(Exception e) {
				deleted = false;
			}
			if(!deleted) {
				AreaShop.warn("File could not be deleted: " + file);
			}
		}

		// Broadcast event
		Bukkit.getPluginManager().callEvent(new DeletedRegionEvent(region));
		return event;
	}

	/**
	 * Remove a group.
	 * @param group Group to remove
	 */
	@Override
	public void removeGroup(RegionGroup group) {
		groups.remove(group.getLowerCaseName());
		groupsConfig.set(group.getLowerCaseName(), null);
		saveGroupsIsRequired();
	}

	/**
	 * Update all signs that need periodic updating.
	 */
	@Override
	public void performPeriodicSignUpdate() {
		Do.forAll(
			plugin.getConfig().getInt("signs.regionsPerTick"),
			getRentsRef(),
			region -> {
				if(region.needsPeriodicUpdate()) {
					region.update();
				}
			}
		);
	}

	/**
	 * Send out rent expire warnings.
	 */
	@Override
	public void sendRentExpireWarnings() {
		Do.forAll(
			plugin.getConfig().getInt("expireWarning.regionsPerTick"),
			getRentsRef(),
			RentRegion::sendExpirationWarnings
		);
	}

	/**
	 * Update regions in a task to minimize lag.
	 * @param regions              Regions to update
	 * @param confirmationReceiver The CommandSender that should be notified at completion
	 */
	@Override
	public void updateRegions(final Collection<GeneralRegion> regions,
							  final CommandSender confirmationReceiver) {
		final int regionsPerTick = plugin.getConfig().getInt("update.regionsPerTick");
		if(confirmationReceiver != null) {
			messageBridge.message(confirmationReceiver, "reload-updateStart", regions.size(), regionsPerTick * 20);
		}
		Do.forAll(
			regionsPerTick,
			regions,
			GeneralRegion::update,
			() -> {
				if(confirmationReceiver != null) {
					messageBridge.message(confirmationReceiver, "reload-updateComplete");
				}
			}
		);
	}

	/**
	 * Update a list of regions.
	 * @param regions The list of regions to update.
	 */
	@Override
	public void updateRegions(Collection<GeneralRegion> regions) {
		updateRegions(regions, null);
	}

	/**
	 * Update all regions, happens in a task to minimize lag.
	 */
	@Override
	public void updateAllRegions() {
		updateRegions(getRegionsRef(), null);
	}

	/**
	 * Update all regions.
	 * @param confirmationReceiver Optional CommandSender that should receive progress messages
	 */
	@Override
	public void updateAllRegions(CommandSender confirmationReceiver) {
		updateRegions(getRegionsRef(), confirmationReceiver);
	}


	/**
	 * Save the group file to disk.
	 */
	@Override
	public void saveGroupsIsRequired() {
		saveGroupsRequired = true;
	}

	/**
	 * Check if saving the groups file is required.
	 * @return true if changes are made and saving is required, otherwise false
	 */
	@Override
	public boolean isSaveGroupsRequired() {
		return saveGroupsRequired;
	}

	@Override
	public void addRegionInterface(@NotNull GeneralRegionInterface region) {
		this.regionContainer.addRegionInterface(region);
	}

	@Override
	public void removeRegion(@NotNull String region) {
		this.regionContainer.removeRegion(region);
	}

	@Override
	public void removeRegion(@NotNull GeneralRegionInterface region) {
		this.regionContainer.removeRegion(region);
	}

	/**
	 * Save the groups file to disk synchronously.
	 */
	@Override
	public void saveGroupsNow() {
		AreaShop.debug("saveGroupsNow() done");
		saveGroupsRequired = false;
		try {
			groupsConfig.save(groupsPath);
		} catch(IOException e) {
			AreaShop.warn("Groups file could not be saved: " + groupsPath);
		}
	}


	/**
	 * Save all region related files spread over time (low load).
	 */
	@Override
	public void saveRequiredFiles() {
		if(isSaveGroupsRequired()) {
			saveGroupsNow();
		}
		this.saveWorldGuardRegions();

		Do.forAll(
			plugin.getConfig().getInt("saving.regionsPerTick"),
			getRegionsRef(),
			region -> {
				if(region.isSaveRequired()) {
					region.saveNow();
				}
			}
		);
	}

	/**
	 * Save all region related files directly (only for cases like onDisable()).
	 */
	@Override
	public void saveRequiredFilesAtOnce() {
		if(isSaveGroupsRequired()) {
			saveGroupsNow();
		}
		for(GeneralRegion region : getRegionsRef()) {
			if(region.isSaveRequired()) {
				region.saveNow();
			}
		}
		this.saveWorldGuardRegions();
	}

	/**
	 * Indicates that a/multiple WorldGuard regions need to be saved.
	 * @param worldName The world where the regions that should be saved is in
	 */
	@Override
	public void saveIsRequiredForRegionWorld(String worldName) {
		worldRegionsRequireSaving.add(worldName);
	}

	/**
	 * Save all worldGuard regions that need saving.
	 */
	@Override
	public void saveWorldGuardRegions() {
		for(String world : worldRegionsRequireSaving) {
			World bukkitWorld = Bukkit.getWorld(world);
			if(bukkitWorld != null) {
				RegionManager manager = plugin.getRegionManager(bukkitWorld);
				if(manager != null) {
					try {
						manager.saveChanges();
					} catch(Exception e) {
						AreaShop.warn("WorldGuard regions in world " + world + " could not be saved");
					}
				}
			}
		}
	}

	/**
	 * Get the folder the region files are located in.
	 * @return The folder where the region.yml files are in
	 */
	@Override
	public String getRegionFolder() {
		return regionsPath;
	}

	/**
	 * Check if a region is on the adding blacklist.
	 * @param region The region name to check
	 * @return true if the region may not be added, otherwise false
	 */
	@Override
	public boolean isBlacklisted(String region) {
		for(String line : plugin.getConfig().getStringList("blacklist")) {
			Pattern pattern = Pattern.compile(line, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(region);
			if(matcher.matches()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Unrent regions that have no time left, regions to check per tick is in the config.
	 */
	@Override
	public void checkRents() {
		Do.forAll(
			plugin.getConfig().getInt("expiration.regionsPerTick"),
			getRentsRef(),
			RentRegion::checkExpiration
		);
	}

	/**
	 * Check all regions and unrent/sell them if the player is inactive for too long.
	 */
	@Override
	public void checkForInactiveRegions() {
		Do.forAll(
			plugin.getConfig().getInt("inactive.regionsPerTick"),
			getRegionsRef(),
			GeneralRegion::checkInactive
		);
	}


	/**
	 * Load the file with the versions, used to check if the other files need conversion.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void loadVersions() {
		File file = new File(versionPath);
		if(file.exists()) {
			// Load versions from the file
			try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(versionPath))) {
				versions = (HashMap<String, Integer>) input.readObject();
			} catch(IOException | ClassNotFoundException | ClassCastException e) {
				AreaShop.warn("Something went wrong reading file: " + versionPath);
				versions = null;
			}
		}
		if(versions == null || versions.isEmpty()) {
			versions = new HashMap<>();
			versions.put(AreaShop.versionFiles, 0);
			this.saveVersions();
		}
	}

	/**
	 * Save the versions file to disk.
	 */
	@Override
	public void saveVersions() {
		if(!(new File(versionPath).exists())) {
			AreaShop.debug("versions file created, this should happen only after installing or upgrading the plugin");
		}
		try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(versionPath))) {
			output.writeObject(versions);
		} catch(IOException e) {
			AreaShop.warn("File could not be saved: " + versionPath);
		}
	}

	/**
	 * Load all files from disk.
	 * @param thisTick Load files in the current tick or a tick later
	 * @return true if the files are loaded correctly, otherwise false
	 */
	@Override
	public boolean loadFiles(boolean thisTick) {
		// Load config.yml + add defaults from .jar
		boolean result = loadConfigFile();
		// Load default.yml + add defaults from .jar
		result &= loadDefaultFile();
		// Convert old formats to the latest (object saving to .yml saving)
		preUpdateFiles();
		if(thisTick) {
			// Load region files (regions folder)
			loadRegionFiles();
			// Convert old formats to the latest (changes in .yml saving format)
			postUpdateFiles();
			// Load groups.yml
			result &= loadGroupsFile();
		} else {
			Do.sync(() -> {
				// Load region files (regions folder)
				loadRegionFiles();
				// Convert old formats to the latest (changes in .yml saving format)
				postUpdateFiles();
				// Load groups.yml
				loadGroupsFile();
			});
		}
		return result;
	}

	/**
	 * Load the default.yml file
	 * @return true if it has been loaded successfully, otherwise false
	 */
	@Override
	public boolean loadDefaultFile() {
		boolean result = true;
		File defaultFile = new File(defaultPath);
		// Safe the file from the jar to disk if it does not exist
		if(!defaultFile.exists()) {
			try(
                    InputStream input = plugin.getResource(AreaShop.defaultFile);
                    OutputStream output = new FileOutputStream(defaultFile)
			) {
				int read;
				byte[] bytes = new byte[1024];
				while((read = input.read(bytes)) != -1) {
					output.write(bytes, 0, read);
				}
				AreaShop.info("File with default region settings has been saved, should only happen on first startup");
			} catch(IOException e) {
				AreaShop.warn("Something went wrong saving the default region settings: " + defaultFile.getAbsolutePath());
			}
		}
		// Load default.yml from the plugin folder, and as backup the default one
		try(
				InputStreamReader custom = new InputStreamReader(new FileInputStream(defaultFile), StandardCharsets.UTF_8);
				InputStreamReader normal = new InputStreamReader(plugin.getResource(AreaShop.defaultFile), StandardCharsets.UTF_8)
		) {
			defaultConfig = YamlConfiguration.loadConfiguration(custom);
			if(defaultConfig.getKeys(false).isEmpty()) {
				AreaShop.warn("File 'default.yml' is empty, check for errors in the log.");
				result = false;
			}
			defaultConfigFallback = YamlConfiguration.loadConfiguration(normal);
		} catch(IOException e) {
			result = false;
		}
		return result;
	}

	/**
	 * Load the default.yml file
	 * @return true if it has been loaded successfully, otherwise false
	 */
	@Override
	public boolean loadConfigFile() {
		boolean result = true;
		File configFile = new File(configPath);
		// Safe the file from the jar to disk if it does not exist
		if(!configFile.exists()) {
			try(
                    InputStream input = plugin.getResource(AreaShop.configFile);
                    OutputStream output = new FileOutputStream(configFile)
			) {
				int read;
				byte[] bytes = new byte[1024];
				while((read = input.read(bytes)) != -1) {
					output.write(bytes, 0, read);
				}
				AreaShop.info("Default config file has been saved, should only happen on first startup");
			} catch(IOException e) {
				AreaShop.warn("Something went wrong saving the config file: " + configFile.getAbsolutePath());
			}
		}
		// Load config.yml from the plugin folder
		try(
                InputStreamReader custom = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8);
                InputStreamReader normal = new InputStreamReader(plugin.getResource(AreaShop.configFile), StandardCharsets.UTF_8);
                InputStreamReader hidden = new InputStreamReader(plugin.getResource(AreaShop.configFileHidden), StandardCharsets.UTF_8)
		) {
			config = YamlConfiguration.loadConfiguration(custom);
			if(config.getKeys(false).isEmpty()) {
				AreaShop.warn("File 'config.yml' is empty, check for errors in the log.");
				result = false;
			} else {
				config.addDefaults(YamlConfiguration.loadConfiguration(normal));
				config.addDefaults(YamlConfiguration.loadConfiguration(hidden));
				// Set the debug and chatprefix variables
				plugin.setDebug(this.getConfig().getBoolean("debug"));
				if(getConfig().isList("chatPrefix")) {
					plugin.setChatprefix(getConfig().getStringList("chatPrefix"));
				} else {
					ArrayList<String> list = new ArrayList<>();
					list.add(getConfig().getString("chatPrefix"));
					plugin.setChatprefix(list);
				}
			}
		} catch(IOException e) {
			AreaShop.warn("Something went wrong while reading the config.yml file: " + configFile.getAbsolutePath());
			result = false;
		}
		Utils.initialize(config);
		return result;
	}

	/**
	 * Load the groups.yml file from disk
	 * @return true if succeeded, otherwise false
	 */
	@Override
	public boolean loadGroupsFile() {
		boolean result = true;
		File groupFile = new File(groupsPath);
		if(groupFile.exists() && groupFile.isFile()) {
			try(
					InputStreamReader reader = new InputStreamReader(new FileInputStream(groupFile), StandardCharsets.UTF_8)
			) {
				groupsConfig = YamlConfiguration.loadConfiguration(reader);
			} catch(IOException e) {
				AreaShop.warn("Could not load groups.yml file: " + groupFile.getAbsolutePath());
			}
		}
		if(groupsConfig == null) {
			groupsConfig = new YamlConfiguration();
		}
		for(String groupName : groupsConfig.getKeys(false)) {
			RegionGroup group = regionFactory.createRegionGroup(groupName);
			groups.put(groupName, group);
		}
		return result;
	}

	/**
	 * Load all region files.
	 */
	@Override
	public void loadRegionFiles() {
		this.regionContainer.clearRegions();
		final File file = new File(regionsPath);
		if(!file.exists()) {
			if(!file.mkdirs()) {
				AreaShop.warn("Could not create region files directory: " + file.getAbsolutePath());
				return;
			}
			plugin.setReady(true);
		} else if(file.isDirectory()) {
			loadRegionFilesNow();
		}
	}

	private void loadRegionFilesNow() {
		File file = new File(regionsPath);
		File[] regionFiles = file.listFiles();
		if(regionFiles == null) {
			plugin.setReady(true);
			return;
		}

		List<String> noRegionType = new ArrayList<>();
		List<String> noNamePaths = new ArrayList<>();
		List<GeneralRegion> noWorld = new ArrayList<>();
		Map<GeneralRegion, File> noRegion = new HashMap<>();
		List<GeneralRegion> incorrectDuration = new ArrayList<>();
		for(File regionFile : regionFiles) {
			if(regionFile.exists() && regionFile.isFile() && !regionFile.isHidden()) {

				// Load the region file from disk in UTF8 mode
				YamlConfiguration regionConfig;
				try(
						InputStreamReader reader = new InputStreamReader(new FileInputStream(regionFile), StandardCharsets.UTF_8)
				) {
					regionConfig = YamlConfiguration.loadConfiguration(reader);
					if(regionConfig.getKeys(false).isEmpty()) {
						AreaShop.warn("Region file '" + regionFile.getName() + "' is empty, check for errors in the log.");
						regionFile.delete();
						continue;
					}
				} catch(IOException e) {
					AreaShop.warn("Something went wrong reading region file: " + regionFile.getAbsolutePath());
					continue;
				}

				// Construct the correct type of region
				String type = regionConfig.getString("general.type");
				GeneralRegion region;
				if(RegionType.RENT.getValue().equals(type)) {
					region = regionFactory.createRentRegion(regionConfig);
				} else if(RegionType.BUY.getValue().equals(type)) {
					region = regionFactory.createBuyRegion(regionConfig);
				} else {
					noRegionType.add(regionFile.getPath());
					continue;
				}

				// Check consistency
				boolean added = false;
				if(region.getName() == null) {
					noNamePaths.add(regionFile.getPath());
				} else if(region.getWorld() == null) {
					noWorld.add(region);
				} else if(region.getRegion() == null) {
					noRegion.put(region, regionFile);
				} else if(region instanceof RentRegion rentRegion && !Utils.checkTimeFormat(rentRegion.getDurationString())) {
					incorrectDuration.add(region);
				} else {
					added = true;
					addRegionNoSave(region);
				}
				if(!added) {
					region.destroy();
				}
			}
		}

		// All files are loaded, print problems to the console
		if(!noRegionType.isEmpty()) {
			AreaShop.warn("The following region files do no have a region type: " + Utils.createCommaSeparatedList(noRegionType));
		}

		if(!noNamePaths.isEmpty()) {
			AreaShop.warn("The following region files do no have a name in their file: " + Utils.createCommaSeparatedList(noNamePaths));
		}

		if(!noRegion.isEmpty()) {
			List<String> noRegionNames = new ArrayList<>();
			for(Map.Entry<GeneralRegion, File> regionMap : noRegion.entrySet()) {
				noRegionNames.add(regionMap.getKey().getName());
				regionMap.getValue().delete();
			}
			AreaShop.warn("AreaShop regions that are missing their WorldGuard region are now being deleted: " + Utils.createCommaSeparatedList(noRegionNames));
		}

		boolean noWorldRegions = !noWorld.isEmpty();
		while(!noWorld.isEmpty()) {
			List<GeneralRegion> toDisplay = new ArrayList<>();
			String missingWorld = noWorld.getFirst().getWorldName();
			toDisplay.add(noWorld.getFirst());
			for(int i = 1; i < noWorld.size(); i++) {
				if(noWorld.get(i).getWorldName().equalsIgnoreCase(missingWorld)) {
					toDisplay.add(noWorld.get(i));
				}
			}
			List<String> noWorldNames = new ArrayList<>();
			for(GeneralRegion region : toDisplay) {
				noWorldNames.add(region.getName());
			}
			AreaShop.warn("World " + missingWorld + " is not loaded, the following AreaShop regions are not functional now: " + Utils.createCommaSeparatedList(noWorldNames));
			noWorld.removeAll(toDisplay);
		}
		if(noWorldRegions) {
			AreaShop.warn("Remove these regions from AreaShop with '/as del' or load the world(s) on the server again.");
		}

		if(!incorrectDuration.isEmpty()) {
			List<String> incorrectDurationNames = new ArrayList<>();
			for(GeneralRegion region : incorrectDuration) {
				incorrectDurationNames.add(region.getName());
			}
			AreaShop.warn("The following regions have an incorrect time format as duration: " + Utils.createCommaSeparatedList(incorrectDurationNames));
		}
		plugin.setReady(true);
	}


	/**
	 * Checks for old file formats and converts them to the latest format.
	 * After conversion the region files need to be loaded.
	 */
	@SuppressWarnings("unchecked")
	private void preUpdateFiles() {
		Integer fileStatus = versions.get(AreaShop.versionFiles);

		// If the the files are already the current version
		if(fileStatus != null && fileStatus == AreaShop.versionFilesCurrent) {
			return;
		}
		AreaShop.info("Updating AreaShop data to the latest format:");

		// Update to YAML based format
		if(fileStatus == null || fileStatus < 2) {
			String rentPath = plugin.getDataFolder() + File.separator + "rents";
			String buyPath = plugin.getDataFolder() + File.separator + "buys";
			File rentFile = new File(rentPath);
			File buyFile = new File(buyPath);
			String oldFolderPath = plugin.getDataFolder() + File.separator + "#old" + File.separator;
			File oldFolderFile = new File(oldFolderPath);
			// Convert old rent files
			boolean buyFileFound = false, rentFileFound = false;
			if(rentFile.exists()) {
				rentFileFound = true;
				if(!oldFolderFile.exists() & !oldFolderFile.mkdirs()) {
					AreaShop.warn("Could not create directory: " + oldFolderFile.getAbsolutePath());
				}

				versions.putIfAbsent("rents", -1);

				HashMap<String, HashMap<String, String>> rents = null;
				try {
					ObjectInputStream input = new ObjectInputStream(new FileInputStream(rentPath));
					rents = (HashMap<String, HashMap<String, String>>)input.readObject();
					input.close();
				} catch(IOException | ClassNotFoundException | ClassCastException e) {
					AreaShop.warn("  Error: Something went wrong reading file: " + rentPath);
				}
				// Delete the file if it is totally wrong
				if(rents == null) {
					try {
						if(!rentFile.delete()) {
							AreaShop.warn("Could not delete file: " + rentFile.getAbsolutePath());
						}
					} catch(Exception e) {
						AreaShop.warn("Could not delete file: " + rentFile.getAbsolutePath());
					}
				} else {
					// Move old file
					try {
						Files.move(new File(rentPath), new File(oldFolderPath + "rents"));
					} catch(Exception e) {
						AreaShop.warn("  Could not create a backup of '" + rentPath + "', check the file permissions (conversion to next version continues)");
					}
					// Check if conversion is needed
					if(versions.get("rents") < 1) {
						// Upgrade the rent to the latest version
						if(versions.get("rents") < 0) {
							for(String rentName : rents.keySet()) {
								HashMap<String, String> rent = rents.get(rentName);
								// Save the rentName in the hashmap and use a small caps rentName as key
								if(rent.get("name") == null) {
									rent.put("name", rentName);
									rents.remove(rentName);
									rents.put(rentName.toLowerCase(), rent);
								}
								// Save the default setting for region restoring
								rent.putIfAbsent("restore", "general");
								// Save the default setting for the region restore profile
								rent.putIfAbsent("profile", "default");
								// Change to version 0
								versions.put("rents", 0);
							}
							AreaShop.info("  Updated version of '" + buyPath + "' from -1 to 0 (switch to using lowercase region names, adding default schematic enabling and profile)");
						}
						if(versions.get("rents") < 1) {
							for(String rentName : rents.keySet()) {
								HashMap<String, String> rent = rents.get(rentName);
								if(rent.get("player") != null) {
									@SuppressWarnings("deprecation")  // Fake deprecation by Bukkit to inform developers, method will stay
									OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(rent.get("player"));
									rent.put("playeruuid", offlinePlayer.getUniqueId().toString());
									rent.remove("player");
								}
								// Change version to 1
								versions.put("rents", 1);
							}
							AreaShop.info("  Updated version of '" + rentPath + "' from 0 to 1 (switch to UUID's for player identification)");
						}
					}
					// Save rents to new format
					File regionsFile = new File(regionsPath);
					if(!regionsFile.exists() & !regionsFile.mkdirs()) {
						AreaShop.warn("Could not create directory: " + regionsFile.getAbsolutePath());
						return;
					}
					for(HashMap<String, String> rent : rents.values()) {
						YamlConfiguration regionConfig = new YamlConfiguration();
						regionConfig.set("general.name", rent.get("name").toLowerCase());
						regionConfig.set("general.type", "rent");
						regionConfig.set("general.world", rent.get("world"));
						regionConfig.set("general.signs.0.location.world", rent.get("world"));
						regionConfig.set("general.signs.0.location.x", Double.parseDouble(rent.get("x")));
						regionConfig.set("general.signs.0.location.y", Double.parseDouble(rent.get("y")));
						regionConfig.set("general.signs.0.location.z", Double.parseDouble(rent.get("z")));
						regionConfig.set("rent.price", Double.parseDouble(rent.get("price")));
						regionConfig.set("rent.duration", rent.get("duration"));
						if(rent.get("restore") != null && !rent.get("restore").equals("general")) {
							regionConfig.set("general.enableRestore", rent.get("restore"));
						}
						if(rent.get("profile") != null && !rent.get("profile").equals("default")) {
							regionConfig.set("general.schematicProfile", rent.get("profile"));
						}
						if(rent.get("tpx") != null) {
							regionConfig.set("general.teleportLocation.world", rent.get("world"));
							regionConfig.set("general.teleportLocation.x", Double.parseDouble(rent.get("tpx")));
							regionConfig.set("general.teleportLocation.y", Double.parseDouble(rent.get("tpy")));
							regionConfig.set("general.teleportLocation.z", Double.parseDouble(rent.get("tpz")));
							regionConfig.set("general.teleportLocation.yaw", rent.get("tpyaw"));
							regionConfig.set("general.teleportLocation.pitch", rent.get("tppitch"));
						}
						if(rent.get("playeruuid") != null) {
							regionConfig.set("rent.renter", rent.get("playeruuid"));
							regionConfig.set("rent.renterName", Utils.toName(rent.get("playeruuid")));
							regionConfig.set("rent.rentedUntil", Long.parseLong(rent.get("rented")));
						}
						try {
							regionConfig.save(new File(regionsPath + File.separator + rent.get("name").toLowerCase() + ".yml"));
						} catch(IOException e) {
							AreaShop.warn("  Error: Could not save region file while converting: " + regionsPath + File.separator + rent.get("name").toLowerCase() + ".yml");
						}
					}
					AreaShop.info("  Updated rent regions to new .yml format (check the /regions folder)");
				}

				// Change version number
				versions.remove("rents");
				versions.put(AreaShop.versionFiles, AreaShop.versionFilesCurrent);
				saveVersions();
			}
			if(buyFile.exists()) {
				buyFileFound = true;
				if(!oldFolderFile.exists() & !oldFolderFile.mkdirs()) {
					AreaShop.warn("Could not create directory: " + oldFolderFile.getAbsolutePath());
					return;
				}

				versions.putIfAbsent("buys", -1);

				HashMap<String, HashMap<String, String>> buys = null;
				try {
					ObjectInputStream input = new ObjectInputStream(new FileInputStream(buyPath));
					buys = (HashMap<String, HashMap<String, String>>)input.readObject();
					input.close();
				} catch(IOException | ClassNotFoundException | ClassCastException e) {
					AreaShop.warn("  Something went wrong reading file: " + buyPath);
				}
				// Delete the file if it is totally wrong
				if(buys == null) {
					try {
						if(!buyFile.delete()) {
							AreaShop.warn("Could not delete file: " + buyFile.getAbsolutePath());
						}
					} catch(Exception e) {
						AreaShop.warn("Could not delete file: " + buyFile.getAbsolutePath());
					}
				} else {
					// Backup current file
					try {
						Files.move(new File(buyPath), new File(oldFolderPath + "buys"));
					} catch(Exception e) {
						AreaShop.warn("  Could not create a backup of '" + buyPath + "', check the file permissions (conversion to next version continues)");
					}
					// Check if conversion is needed
					if(versions.get("buys") < 1) {
						// Upgrade the buy to the latest version
						if(versions.get("buys") < 0) {
							for(String buyName : buys.keySet()) {
								HashMap<String, String> buy = buys.get(buyName);
								// Save the buyName in the hashmap and use a small caps buyName as key
								if(buy.get("name") == null) {
									buy.put("name", buyName);
									buys.remove(buyName);
									buys.put(buyName.toLowerCase(), buy);
								}
								// Save the default setting for region restoring
								buy.putIfAbsent("restore", "general");
								// Save the default setting for the region restore profile
								buy.putIfAbsent("profile", "default");
								// Change to version 0
								versions.put("buys", 0);
							}
							AreaShop.info("  Updated version of '" + buyPath + "' from -1 to 0 (switch to using lowercase region names, adding default schematic enabling and profile)");
						}
						if(versions.get("buys") < 1) {
							for(String buyName : buys.keySet()) {
								HashMap<String, String> buy = buys.get(buyName);
								if(buy.get("player") != null) {
									@SuppressWarnings("deprecation")  // Fake deprecation by Bukkit to inform developers, method will stay
											OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(buy.get("player"));
									buy.put("playeruuid", offlinePlayer.getUniqueId().toString());
									buy.remove("player");
								}
								// Change version to 1
								versions.put("buys", 1);
							}
							AreaShop.info("  Updated version of '" + buyPath + "' from 0 to 1 (switch to UUID's for player identification)");
						}
					}

					// Save buys to new format
					File regionsFile = new File(regionsPath);
					if(!regionsFile.exists() & !regionsFile.mkdirs()) {
						AreaShop.warn("Could not create directory: " + regionsFile.getAbsolutePath());
					}
					for(HashMap<String, String> buy : buys.values()) {
						YamlConfiguration regionConfig = new YamlConfiguration();
						regionConfig.set("general.name", buy.get("name").toLowerCase());
						regionConfig.set("general.type", "buy");
						regionConfig.set("general.world", buy.get("world"));
						regionConfig.set("general.signs.0.location.world", buy.get("world"));
						regionConfig.set("general.signs.0.location.x", Double.parseDouble(buy.get("x")));
						regionConfig.set("general.signs.0.location.y", Double.parseDouble(buy.get("y")));
						regionConfig.set("general.signs.0.location.z", Double.parseDouble(buy.get("z")));
						regionConfig.set("buy.price", Double.parseDouble(buy.get("price")));
						if(buy.get("restore") != null && !buy.get("restore").equals("general")) {
							regionConfig.set("general.enableRestore", buy.get("restore"));
						}
						if(buy.get("profile") != null && !buy.get("profile").equals("default")) {
							regionConfig.set("general.schematicProfile", buy.get("profile"));
						}
						if(buy.get("tpx") != null) {
							regionConfig.set("general.teleportLocation.world", buy.get("world"));
							regionConfig.set("general.teleportLocation.x", Double.parseDouble(buy.get("tpx")));
							regionConfig.set("general.teleportLocation.y", Double.parseDouble(buy.get("tpy")));
							regionConfig.set("general.teleportLocation.z", Double.parseDouble(buy.get("tpz")));
							regionConfig.set("general.teleportLocation.yaw", buy.get("tpyaw"));
							regionConfig.set("general.teleportLocation.pitch", buy.get("tppitch"));
						}
						if(buy.get("playeruuid") != null) {
							regionConfig.set("buy.buyer", buy.get("playeruuid"));
							regionConfig.set("buy.buyerName", Utils.toName(buy.get("playeruuid")));
						}
						try {
							regionConfig.save(new File(regionsPath + File.separator + buy.get("name").toLowerCase() + ".yml"));
						} catch(IOException e) {
							AreaShop.warn("  Error: Could not save region file while converting: " + regionsPath + File.separator + buy.get("name").toLowerCase() + ".yml");
						}
					}
					AreaShop.info("  Updated buy regions to new .yml format (check the /regions folder)");
				}

				// Change version number
				versions.remove("buys");
			}

			// Separate try-catch blocks to try them all individually (don't stop after 1 has failed)
			try {
				Files.move(new File(rentPath + ".old"), new File(oldFolderPath + "rents.old"));
			} catch(Exception e) {
				// Ignore
			}
			try {
				Files.move(new File(buyPath + ".old"), new File(oldFolderPath + "buys.old"));
			} catch(Exception e) {
				// Ignore
			}
			if(buyFileFound || rentFileFound) {
				try {
					Files.move(new File(plugin.getDataFolder() + File.separator + "config.yml"), new File(oldFolderPath + "config.yml"));
				} catch(Exception e) {
					// Ignore
				}
			}
			// Update versions file to 2
			versions.put(AreaShop.versionFiles, 2);
			saveVersions();
			if(buyFileFound || rentFileFound) {
				AreaShop.info("  Updated to YAML based storage (v1 to v2)");
			}
		}
	}

	/**
	 * Checks for old file formats and converts them to the latest format.
	 * This is to be triggered after the load of the region files.
	 */
	private void postUpdateFiles() {
		Integer fileStatus = versions.get(AreaShop.versionFiles);

		// If the the files are already the current version
		if(fileStatus != null && fileStatus == AreaShop.versionFilesCurrent) {
			return;
		}

		// Add 'general.lastActive' to rented/bought regions (initialize at current time)
		if(fileStatus == null || fileStatus < 3) {
			for(GeneralRegion region : getRegionsRef()) {
				region.updateLastActiveTime();
			}
			// Update versions file to 3
			versions.put(AreaShop.versionFiles, 3);
			saveVersions();
			if(!getRegionsRef().isEmpty()) {
				AreaShop.info("  Added last active time to regions (v2 to v3)");
			}
		}
	}

	/**
	 * Get the settings of a group.
	 * @param groupName Name of the group to get the settings from
	 * @return The settings of the group
	 */
	@Override
	public ConfigurationSection getGroupSettings(String groupName) {
		return groupsConfig.getConfigurationSection(groupName.toLowerCase());
	}

	/**
	 * Set a setting for a group.
	 * @param group   The group to set it for
	 * @param path    The path to set
	 * @param setting The value to set
	 */
	@Override
	public void setGroupSetting(RegionGroup group, String path, Object setting) {
		groupsConfig.set(group.getName().toLowerCase() + "." + path, setting);
	}

	@Override
	public void clearRegions() {
		this.regionContainer.clearRegions();
	}
}















