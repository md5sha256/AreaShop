package me.wiefferink.areashop;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import io.papermc.lib.PaperLib;
import me.wiefferink.areashop.adapters.platform.MinecraftPlatform;
import me.wiefferink.areashop.adapters.platform.paper.PaperPlatform;
import me.wiefferink.areashop.commands.util.AreashopCommands;
import me.wiefferink.areashop.extensions.AreashopExtension;
import me.wiefferink.areashop.features.signs.SignManager;
import me.wiefferink.areashop.interfaces.AreaShopInterface;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.listeners.PlayerLoginLogoutListener;
import me.wiefferink.areashop.managers.CacheManager;
import me.wiefferink.areashop.managers.FeatureManager;
import me.wiefferink.areashop.managers.FileManager;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.managers.Manager;
import me.wiefferink.areashop.managers.SignErrorLogger;
import me.wiefferink.areashop.managers.SignLinkerManager;
import me.wiefferink.areashop.modules.AreaShopModule;
import me.wiefferink.areashop.modules.BukkitModule;
import me.wiefferink.areashop.modules.DependencyModule;
import me.wiefferink.areashop.modules.PlatformModule;
import me.wiefferink.areashop.services.ServiceManager;
import me.wiefferink.areashop.tools.GithubUpdateCheck;
import me.wiefferink.areashop.tools.LanguageConverter;
import me.wiefferink.areashop.tools.SimpleMessageBridge;
import me.wiefferink.areashop.tools.SpigotPlatform;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.areashop.tools.version.Version;
import me.wiefferink.areashop.tools.version.VersionUtil;
import me.wiefferink.bukkitdo.Do;
import me.wiefferink.interactivemessenger.source.LanguageManager;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Main class for the AreaShop plugin.
 * Contains methods to get parts of the plugins functionality and definitions for constants.
 */
public final class AreaShop extends JavaPlugin implements AreaShopApi {
	// Statically available instance
	private static AreaShop instance = null;

	// General variables
	private Injector injector;
	private WorldGuardPlugin worldGuard = null;
	private WorldGuardInterface worldGuardInterface = null;
	private WorldEditPlugin worldEdit = null;
	private WorldEditInterface worldEditInterface = null;
	private MessageBridge messageBridge;
	private IFileManager fileManager = null;
	private LanguageManager languageManager = null;
	private CacheManager cacheManager = null;
	private SignLinkerManager signLinkerManager = null;
	private FeatureManager featureManager = null;
	private SignManager signManager;
	private SignErrorLogger signErrorLogger;
	private Set<Manager> managers = null;
	private boolean debug = false;
	private List<String> chatprefix = null;
	private boolean ready = false;
	private GithubUpdateCheck githubUpdateCheck = null;

	private final ServiceManager serviceManager = new ServiceManager();

	// Folders and file names
	public static final String languageFolder = "lang";
	public static final String schematicFolder = "schem";
	public static final String regionsFolder = "regions";
	public static final String groupsFile = "groups.yml";
	public static final String defaultFile = "default.yml";
	public static final String configFile = "config.yml";
	public static final String configFileHidden = "hiddenConfig.yml";
	public static final String signLogFile = "sign-errors.log";
	public static final String versionFile = "versions";

	// Euro tag for in the config
	public static final String currencyEuro = "%euro%";

	// Constants for handling file versions
	public static final String versionFiles = "files";
	public static final int versionFilesCurrent = 3;

	// Keys for replacing parts of flags, commands, strings
	public static final String tagPlayerName = "player";
	public static final String tagPlayerUUID = "uuid";
	public static final String tagWorldName = "world";
	public static final String tagRegionName = "region";
	public static final String tagRegionType = "type";
	public static final String tagPrice = "price";
	public static final String tagRawPrice = "rawprice";
	public static final String tagDuration = "duration";
	public static final String tagRentedUntil = "until";
	public static final String tagRentedUntilShort = "untilshort";
	public static final String tagWidth = "width"; // x-axis
	public static final String tagHeight = "height"; // y-axis
	public static final String tagDepth = "depth"; // z-axis
	public static final String tagVolume = "volume"; // Number of blocks in the region (accounting for polygon regions)
	public static final String tagTimeLeft = "timeleft";
	public static final String tagClicker = "clicker";
	public static final String tagResellPrice = "resellprice";
	public static final String tagRawResellPrice = "rawresellprice";
	public static final String tagFriends = "friends";
	public static final String tagFriendsUUID = "friendsuuid";
	public static final String tagMoneyBackPercentage = "moneybackpercent";
	public static final String tagMoneyBackAmount = "moneyback";
	public static final String tagRawMoneyBackAmount = "rawmoneyback";
	public static final String tagTimesExtended = "timesExtended";
	public static final String tagMaxExtends = "maxextends";
	public static final String tagExtendsLeft = "extendsleft";
	public static final String tagMaxRentTime = "maxrenttime";
	public static final String tagMaxInactiveTime = "inactivetime";
	public static final String tagLandlord = "landlord";
	public static final String tagLandlordUUID = "landlorduuid";
	public static final String tagDateTime = "datetime";
	public static final String tagDateTimeShort = "datetimeshort";
	public static final String tagYear = "year";
	public static final String tagMonth = "month";
	public static final String tagDay = "day";
	public static final String tagHour = "hour";
	public static final String tagMinute = "minute";
	public static final String tagSecond = "second";
	public static final String tagMillisecond = "millisecond";
	public static final String tagEpoch = "epoch";
	public static final String tagTeleportX = "tpx";
	public static final String tagTeleportY = "tpy";
	public static final String tagTeleportZ = "tpz";
	public static final String tagTeleportBlockX = "tpblockx";
	public static final String tagTeleportBlockY = "tpblocky";
	public static final String tagTeleportBlockZ = "tpblockz";
	public static final String tagTeleportPitch = "tppitch";
	public static final String tagTeleportYaw = "tpyaw";
	public static final String tagTeleportPitchRound = "tppitchround";
	public static final String tagTeleportYawRound = "tpyawround";
	public static final String tagTeleportWorld = "tpworld";

	public static AreaShop getInstance() {
		return AreaShop.instance;
	}

	/**
	 * Called on start or reload of the server.
	 */
	@Override
	public void onEnable() {
		AreaShop.instance = this;
		Do.init(this);
		managers = new HashSet<>();
		messageBridge = new SimpleMessageBridge(this.serviceManager);
		signErrorLogger = new SignErrorLogger(new File(getDataFolder(), signLogFile));

		// Setup NMS Impl
		Version currentServerVersion = VersionUtil.parseMinecraftVersion(Bukkit.getBukkitVersion());
		if (currentServerVersion.versionData().isOlderThan(VersionUtil.MC_1_21)) {
			error("Unsupported minecraft version: " + currentServerVersion + "! Minimum is 1.21");
			shutdownOnError();
			return;
		}

		final MinecraftPlatform platform;
		if (PaperLib.isPaper()) {
			platform = new PaperPlatform(this);
			info("Detected Paper; using the PaperPlatform impl");
		} else {
			platform = new SpigotPlatform(this);
			info("Detected Spigot; using the SpigotPlatform impl");
		}
		final PlatformModule platformModule = new PlatformModule(platform);

		// Check if Vault is present
		if(getServer().getPluginManager().getPlugin("Vault") == null) {
			error("Vault plugin is not present or has not loaded correctly");
			shutdownOnError();
			return;
		}

		// Find WorldEdit integration version to load
		String weHandlerVersion;
		Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
		if(!(plugin instanceof WorldEditPlugin) || !plugin.isEnabled()) {
			error("WorldEdit plugin is not present or has not loaded correctly");
			return;
		} else {
			this.worldEdit = (WorldEditPlugin) plugin;
			Plugin fawePlugin = getServer().getPluginManager().getPlugin("FastAsyncWorldEdit");
			if (fawePlugin != null && fawePlugin.isEnabled()) {
				weHandlerVersion = "FastAsyncWorldEditHandler";
				info("Detected FastAsyncWorldEdit, using the fawe handler");
			} else {
				weHandlerVersion = "WorldEditHandler";
			}
		}

		plugin = getServer().getPluginManager().getPlugin("WorldGuard");
		if(!(plugin instanceof WorldGuardPlugin) || !plugin.isEnabled()) {
			error("WorldGuard plugin is not present or has not loaded correctly");
			shutdownOnError();
			return;
		} else {
			this.worldGuard = (WorldGuardPlugin) plugin;
		}

		DependencyModule dependencyModule = new DependencyModule(this.worldEdit, this.worldGuard, getEconomy(), getPermissionProvider());

		// Load WorldEdit
		try {
			Class<?> clazz = Class.forName("me.wiefferink.areashop.adapters.plugins." + weHandlerVersion);
			if(WorldEditInterface.class.isAssignableFrom(clazz)) { // Make sure it actually implements WorldEditInterface
				worldEditInterface = (WorldEditInterface)clazz.getConstructor(AreaShopInterface.class).newInstance(this); // Set our handler
			}
		} catch(Exception e) {
			error("Could not load the handler for WorldEdit (tried to load " + weHandlerVersion + "), report this problem to the author: " + ExceptionUtils.getStackTrace(e));
			shutdownOnError();
			return;
		}

		// Load WorldGuard
		try {
			Class<?> clazz = Class.forName("me.wiefferink.areashop.adapters.plugins.WorldGuardHandler");
			// Check if we have a NMSHandler class at that location.
			if(WorldGuardInterface.class.isAssignableFrom(clazz)) { // Make sure it actually implements WorldGuardInterface
				worldGuardInterface = (WorldGuardInterface)clazz.getConstructor(AreaShopInterface.class).newInstance(this); // Set our handler
			}
		} catch(Exception e) {
			error("Could not load the handler for WorldGuard, report this problem to the author:" + ExceptionUtils.getStackTrace(e));
			shutdownOnError();
			return;
		}

		AreaShopModule asModule = new AreaShopModule(this,
				this.messageBridge,
				this.worldEditInterface,
				this.worldGuardInterface,
				this.signErrorLogger,
				this.serviceManager,
				platformModule,
				dependencyModule
		);
		injector = Guice.createInjector(Stage.PRODUCTION, new BukkitModule(getServer()), asModule);

		// Load all data from files and check versions
		fileManager = injector.getInstance(IFileManager.class);
		managers.add((FileManager) fileManager);
		boolean loadFilesResult = fileManager.loadFiles(false);
		if (!loadFilesResult) {
			shutdownOnError();
			return;
		}

		performLanguageMigrations();
		setupLanguageManager();

		featureManager = injector.getInstance(FeatureManager.class);
		featureManager.initializeFeatures(injector);
		managers.add(featureManager);
		signManager = injector.getInstance(SignManager.class);
		managers.add(signManager);
		cacheManager = injector.getInstance(CacheManager.class);
		String rawExpiryDuration = fileManager.getConfig().getString("cacheExpiryDuration", "7d");
		long millis = Utils.durationStringToLong(rawExpiryDuration);
		cacheManager.initialize(new File(getDataFolder(), "uuid-cache.bin"), Duration.ofMillis(millis));
		cacheManager.loadCache();
		managers.add(cacheManager);

		loadExtensions();

		// Register the event listeners
		getServer().getPluginManager().registerEvents(new PlayerLoginLogoutListener(this, messageBridge, this.cacheManager), this);

		setupTasks();

		// Register commands
		AreashopCommands commands = injector.getInstance(AreashopCommands.class);
		commands.registerCommands();

		// Create a signLinkerManager
		signLinkerManager = injector.getInstance(SignLinkerManager.class);
		managers.add(signLinkerManager);

		// Register dynamic permission (things declared in config)
		registerDynamicPermissions();

		// Don't initialize the updatechecker if disabled in the config
		if(getConfig().getBoolean("checkForUpdates")) {
			githubUpdateCheck = new GithubUpdateCheck(
					this,
					"md5sha256",
					"AreaShop"
			).withVersionComparator((latestVersion, currentVersion) -> {
				Version latest = Version.parse(cleanVersion(latestVersion));
				Version current = Version.parse(cleanVersion(currentVersion));
				return latest.versionData().isNewerThan(current.versionData());
			}
			).checkUpdate(result -> {
				AreaShop.debug("Update check result:", result);
				if(!result.hasUpdate()) {
					return;
				}

				AreaShop.info("Update from AreaShop V" + cleanVersion(result.getCurrentVersion()) + " to AreaShop V" + cleanVersion(result.getLatestVersion()) + " available, get the latest version at https://github.com/md5sha256/AreaShop/releases");
				for(Player player : Utils.getOnlinePlayers()) {
					notifyUpdate(player);
				}
			});
		}
	}

	/**
	 * Notify a player about an update if he wants notifications about it and an update is available.
	 * @param sender CommandSender to notify
	 */
	public void notifyUpdate(CommandSender sender) {
		if(githubUpdateCheck != null && githubUpdateCheck.hasUpdate() && sender.hasPermission("areashop.notifyupdate")) {
			messageBridge.message(sender, "update-playerNotify", cleanVersion(githubUpdateCheck.getCurrentVersion()), cleanVersion(githubUpdateCheck.getLatestVersion()));
		}
	}

	private void shutdownOnError() {
		error("The plugin has not started, fix the errors listed above");
		getServer().getPluginManager().disablePlugin(this);
	}

	private void loadExtensions() {
		if (this.getServer().getPluginManager().isPluginEnabled("Essentials")) {
			loadEssentialsExt().ifPresent(ext -> ext.init(this, this.injector));
		}
	}

	private Optional<AreashopExtension> loadEssentialsExt() {
		try {
			Class<?> clazz = Class.forName("me.wiefferink.areashop.adapters.plugins.essentials.EssentialsExtension");
			Class<? extends AreashopExtension> extClass = clazz.asSubclass(AreashopExtension.class);
			AreashopExtension extension = extClass.getConstructor().newInstance();
			return Optional.of(extension);
		} catch (ReflectiveOperationException ex) {
			error("Failed to initialize the essentials extension!");
			ex.printStackTrace();
		}
		return Optional.empty();
	}

	/**
	 * Cleanup a version number.
	 * @param version Version to clean
	 * @return Cleaned up version (removed prefixes and suffixes)
	 */
	private String cleanVersion(String version) {
		version = version.toLowerCase();

		// Strip 'v' as used on Github tags
		if(version.startsWith("v")) {
			version = version.substring(1);
		}

		// Strip build number as used by Jenkins
		if(version.contains("#")) {
			version = version.substring(0, version.indexOf("#"));
		}

		return version;
	}

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	/**
	 * Called on shutdown or reload of the server.
	 */
	@Override
	public void onDisable() {

		Bukkit.getServer().getScheduler().cancelTasks(this);

		// Cleanup managers
		for(Manager manager : managers) {
			manager.shutdown();
		}
		signErrorLogger.saveIfDirty();
		HandlerList.unregisterAll(this);
	}

	/**
	 * Indicates if the plugin will be using MiniMessage or not
	 * @return true if MiniMessage should be used, false otherwise
	 */
	public static boolean useMiniMessage() {
		return getInstance().getConfig().getBoolean("useMiniMessage");
	}

	/**
	 * Indicates if the plugin is ready to be used.
	 * @return true if the plugin is ready, false otherwise
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 * Set if the plugin is ready to be used or not (not to be used from another plugin!).
	 * @param ready Indicate if the plugin is ready to be used
	 */
	public void setReady(boolean ready) {
		this.ready = ready;
	}

	/**
	 * Set if the plugin should output debug messages (loaded from config normally).
	 * @param debug Indicates if the plugin should output debug messages or not
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	private void performLanguageMigrations() {
		boolean migrateExisting = this.getConfig().getBoolean("migrateLanguages", false);
		if (!migrateExisting) {
			return;
		}
		getLogger().info("Performing language migration");
		List<String> chatPrefix = getConfig().getStringList("chatPrefix");
		List<String> convertedChatPrefix = LanguageConverter.convertRawList(chatPrefix);
		if (convertedChatPrefix.size() == 1) {
			getConfig().set("mmChatPrefix", convertedChatPrefix.getFirst());
		} else {
			getConfig().set("mmChatPrefix", convertedChatPrefix);
		}
		saveConfig();
		File existingLanguages = getDataFolder().toPath().resolve(languageFolder).toFile();
		File[] langFiles = existingLanguages.listFiles(file -> file.getName().endsWith(".yml"));
		if (langFiles == null) {
			return;
		}
		for (File file : langFiles) {
			String name = file.getName();
			String langName = name.substring(0, name.length() - 4);
			if (langName.endsWith("-MM")) {
				debug("Skipping migration for " + langName + " as it already exists.");
				continue;
			}
			File toMigrate = new File(existingLanguages, langName + "-MM.yml");
			try {
				LanguageConverter.performConversion(file, toMigrate);
			} catch (IOException ex) {
				getLogger().log(Level.SEVERE, "Failed to perform migration for lang: " + file.getName(), ex);
			}
		}
		getLogger().info("Language migration complete!");
	}

	/**
	 * Setup a new LanguageManager.
	 */
	private void setupLanguageManager() {
		languageManager = new ASLanguageManager(
				this,
				languageFolder,
				useMiniMessage() ? getConfig().getString("mmLanguage") : getConfig().getString("language"),
				"EN",
				useMiniMessage() ? getConfig().getStringList("mmChatPrefix") : chatprefix,
				getConfig().getString("wgPrefix")
		);
	}

	/**
	 * Set the chatprefix to use in the chat (loaded from config normally).
	 * @param chatprefix The string to use in front of chat messages (supports formatting codes)
	 */
	public void setChatprefix(List<String> chatprefix) {
		this.chatprefix = chatprefix;
	}

	public SignErrorLogger getSignErrorLogger() {
		return signErrorLogger;
	}

	/**
	 * Function to get the WorldGuard plugin.
	 * @return WorldGuardPlugin
	 */
	@Override
	public WorldGuardPlugin getWorldGuard() {
		return worldGuard;
	}

	/**
	 * Function to get WorldGuardInterface for version dependent things.
	 * @return WorldGuardInterface
	 */
	public WorldGuardInterface getWorldGuardHandler() {
		return this.worldGuardInterface;
	}


	@Nonnull
	public SignManager getSignManager() {
		return signManager;
	}

	/**
	 * Get the RegionManager.
	 * @param world World to get the RegionManager for
	 * @return RegionManager for the given world, if there is one, otherwise null
	 */
	public RegionManager getRegionManager(World world) {
		return this.worldGuardInterface.getRegionManager(world);
	}

	/**
	 * Function to get the WorldEdit plugin.
	 * @return WorldEditPlugin
	 */
	@Override
	public WorldEditPlugin getWorldEdit() {
		return worldEdit;
	}

	@Override
	public Plugin pluginInstance() {
		return this;
	}

	/**
	 * Function to get WorldGuardInterface for version dependent things.
	 * @return WorldGuardInterface
	 */
	public WorldEditInterface getWorldEditHandler() {
		return this.worldEditInterface;
	}

	/**
	 * Function to get the LanguageManager.
	 * @return the LanguageManager
	 */
	public LanguageManager getLanguageManager() {
		return languageManager;
	}

	/**
	 * Get the SignLinkerManager.
	 * Handles sign linking mode.
	 * @return The SignLinkerManager
	 */
	@Nonnull
	@Override
	public SignLinkerManager getSignlinkerManager() {
		return signLinkerManager;
	}

	/**
	 * Get the FeatureManager.
	 * Manages region specific features.
	 * @return The FeatureManager
	 */
	@Nonnull
	@Override
	public FeatureManager getFeatureManager() {
		return featureManager;
	}

	@NotNull
	@Override
	public ServiceManager getServiceManager() {
		return this.serviceManager;
	}

	/**
	 * Function to get the Vault plugin.
	 * @return Economy
	 */
	private Economy getEconomy() {
		RegisteredServiceProvider<Economy> economy = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if(economy == null) {
			error("There is no economy provider to support Vault, make sure you installed an economy plugin");
			return null;
		}
		return economy.getProvider();
	}

	/**
	 * Get the Vault permissions provider.
	 * @return Vault permissions provider
	 */
	private net.milkbowl.vault.permission.Permission getPermissionProvider() {
		RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider == null) {
			return null;
		}
		return permissionProvider.getProvider();
	}

	/**
	 * Check for a permission of a (possibly offline) player.
	 * @param offlinePlayer OfflinePlayer to check
	 * @param permission Permission to check
	 * @return true if the player has the permission, false if the player does not have permission or, is offline and there is not Vault-compatible permission plugin
	 */
	public boolean hasPermission(OfflinePlayer offlinePlayer, String permission) {
		// Online, return through Bukkit
		if(offlinePlayer.getPlayer() != null) {
			return offlinePlayer.getPlayer().hasPermission(permission);
		}

		// Resolve while offline if possible
		net.milkbowl.vault.permission.Permission permissionProvider = getPermissionProvider();
		if(permissionProvider != null) {
			// TODO: Should we provide a world here?
			return permissionProvider.playerHas(null, offlinePlayer, permission);
		}

		// Player offline and no offline permission provider available, safely say that there is no permission
		return false;
	}

	/**
	 * Method to get the FileManager (loads/save regions and can be used to get regions).
	 * @return The fileManager
	 */
	public IFileManager getFileManager() {
		return fileManager;
	}

	/**
	 * Register dynamic permissions controlled by config settings.
	 */
	private void registerDynamicPermissions() {
		// Register limit groups of amount of regions a player can have
		ConfigurationSection section = getConfig().getConfigurationSection("limitGroups");
		if(section == null) {
			return;
		}
		for(String group : section.getKeys(false)) {
			if(!"default".equals(group)) {
				Permission perm = new Permission("areashop.limits." + group);
				try {
					Bukkit.getPluginManager().addPermission(perm);
				} catch(IllegalArgumentException e) {
					warn("Could not add the following permission to be used as limit: " + perm.getName());
				}
			}
		}
		Bukkit.getPluginManager().recalculatePermissionDefaults(Bukkit.getPluginManager().getPermission("playerwarps.limits"));
	}

	/**
	 * Register all required tasks.
	 */
	private void setupTasks() {

		long signLogTicks = Utils.millisToTicks(TimeUnit.MINUTES.toMillis(5));
		Bukkit.getScheduler().runTaskTimerAsynchronously(this,
				() -> signErrorLogger.saveIfDirty(),
				signLogTicks,
				signLogTicks);

		// Rent expiration timer
		long expirationCheck = Utils.millisToTicks(Utils.getDurationFromSecondsOrString("expiration.delay"));
		final AreaShop finalPlugin = this;
		if(expirationCheck > 0) {
			Do.syncTimer(expirationCheck, () -> {
				if(isReady()) {
					finalPlugin.getFileManager().checkRents();
					AreaShop.debugTask("Checking rent expirations...");
				} else {
					AreaShop.debugTask("Skipped checking rent expirations, plugin not ready");
				}
			});
		}

		// Inactive unrenting/selling timer
		long inactiveCheck = Utils.millisToTicks(Utils.getDurationFromMinutesOrString("inactive.delay"));
		if(inactiveCheck > 0) {
			Do.syncTimer(inactiveCheck, () -> {
				if(isReady()) {
					finalPlugin.getFileManager().checkForInactiveRegions();
					AreaShop.debugTask("Checking for regions with players that are inactive too long...");
				} else {
					AreaShop.debugTask("Skipped checking for regions of inactive players, plugin not ready");
				}
			});
		}

		// Periodic updating of signs for timeleft tags
		long periodicUpdate = Utils.millisToTicks(Utils.getDurationFromSecondsOrString("signs.delay"));
		if(periodicUpdate > 0) {
			Do.syncTimer(periodicUpdate, () -> {
				if(isReady()) {
					finalPlugin.getFileManager().performPeriodicSignUpdate();
					AreaShop.debugTask("Performing periodic sign update...");
				} else {
					AreaShop.debugTask("Skipped performing periodic sign update, plugin not ready");
				}
			});
		}

		// Saving regions and group settings
		long saveFiles = Utils.millisToTicks(Utils.getDurationFromMinutesOrString("saving.delay"));
		if(saveFiles > 0) {
			Do.syncTimer(saveFiles, () -> {
				if(isReady()) {
					finalPlugin.getFileManager().saveRequiredFiles();
					AreaShop.debugTask("Saving required files...");
				} else {
					AreaShop.debugTask("Skipped saving required files, plugin not ready");
				}
			});
		}

		// Sending warnings about rent regions to online players
		long expireWarning = Utils.millisToTicks(Utils.getDurationFromMinutesOrString("expireWarning.delay"));
		if(expireWarning > 0) {
			Do.syncTimer(expireWarning, () -> {
				if(isReady()) {
					finalPlugin.getFileManager().sendRentExpireWarnings();
					AreaShop.debugTask("Sending rent expire warnings...");
				} else {
					AreaShop.debugTask("Skipped sending rent expire warnings, plugin not ready");
				}
			});
		}

		// Update all regions on startup
		if(getConfig().getBoolean("updateRegionsOnStartup")) {
			Do.syncLater(20, () -> {
				finalPlugin.getFileManager().updateAllRegions();
				AreaShop.debugTask("Updating all regions at startup...");
			});
		}
	}


	/**
	 * Return the config.
	 */
	@Override
	public YamlConfiguration getConfig() {
		return fileManager.getConfig();
	}

	/**
	 * Sends an debug message to the console.
	 * @param message The message that should be printed to the console
	 */
	public static void debug(Object... message) {
		if(AreaShop.getInstance().debug) {
			info("Debug: " + StringUtils.join(message, " "));
		}
	}

	/**
	 * Non-static debug to use as implementation of the interface.
	 * @param message Object parts of the message that should be logged, toString() will be used
	 */
	@Override
	public void debugI(Object... message) {
		AreaShop.debug(StringUtils.join(message, " "));
	}

	@Override
	public void debugI(Supplier<String> message) {
		if (debug) {
			info("Debug: " + message.get());
		}
	}

	/**
	 * Print an information message to the console.
	 * @param message The message to print
	 */
	public static void info(Object... message) {
		AreaShop.getInstance().getLogger().info(() -> StringUtils.join(message, " "));
	}

	/**
	 * Print a warning to the console.
	 * @param message The message to print
	 */
	public static void warn(Object... message) {
		AreaShop.getInstance().getLogger().warning(() -> StringUtils.join(message, " "));
	}

	/**
	 * Print an error to the console.
	 * @param message The message to print
	 */
	public static void error(Object... message) {
		AreaShop.getInstance().getLogger().severe(() -> StringUtils.join(message, " "));
	}

	/**
	 * Print debug message for periodic task.
	 * @param message The message to print
	 */
	public static void debugTask(Object... message) {
		if(AreaShop.getInstance().getConfig().getBoolean("debugTask")) {
			AreaShop.debug(StringUtils.join(message, " "));
		}
	}

	/**
	 * Reload all files of the plugin and update all regions.
	 * @param confirmationReceiver The CommandSender which should be notified when complete, null for nobody
	 */
	public void reload(final CommandSender confirmationReceiver) {
		setReady(false);
		fileManager.saveRequiredFilesAtOnce();
		fileManager.loadFiles(true);
		setupLanguageManager();
		messageBridge.message(confirmationReceiver, "reload-reloading");
		fileManager.checkRents();
		fileManager.updateAllRegions(confirmationReceiver);
	}

	/**
	 * Reload all files of the plugin and update all regions.
	 */
	public void reload() {
		reload(null);
	}

}




