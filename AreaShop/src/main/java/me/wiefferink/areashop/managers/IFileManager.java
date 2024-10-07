package me.wiefferink.areashop.managers;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.wiefferink.areashop.events.ask.AddingRegionEvent;
import me.wiefferink.areashop.events.ask.DeletingRegionEvent;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.IRegionContainer;
import me.wiefferink.areashop.regions.RegionGroup;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collection;
import java.util.List;

public interface IFileManager extends IRegionContainer {

    /**
     * Get the folder where schematics are stored.
     *
     * @return The folder where schematics are stored
     */
    String getSchematicFolder();

    /**
     * Get a group.
     *
     * @param name The name of the group to get (will be normalized)
     * @return The group if found, otherwise null
     */
    RegionGroup getGroup(String name);

    /**
     * Get all groups.
     *
     * @return Collection with all groups (safe to modify)
     */
    Collection<RegionGroup> getGroups();

    /**
     * Get the default region settings as provided by the user (default.yml).
     *
     * @return YamlConfiguration with the settings (might miss settings, which should be filled in with {@link #getFallbackRegionSettings()})
     */
    YamlConfiguration getRegionSettings();

    /**
     * Get the default regions settings as provided by AreaShop (default.yml).
     *
     * @return YamlConfiguration with the default settings
     */
    YamlConfiguration getFallbackRegionSettings();

    /**
     * Get the config file (config.yml).
     *
     * @return YamlConfiguration with the settings of users, with fallback to the settings provided by AreaShop
     */
    YamlConfiguration getConfig();

    /**
     * Get a list of names of all groups.
     *
     * @return A String list with all the names
     */
    List<String> getGroupNames();

    /**
     * Add a region to the list and mark it as to-be-saved.
     *
     * @param region Then region to add
     * @return true when successful, otherwise false (denied by an event listener)
     */
    AddingRegionEvent addRegion(GeneralRegion region);

    /**
     * Add a region to the list without saving it to disk (useful for loading at startup).
     *
     * @param region The region to add
     * @return true when successful, otherwise false (denied by an event listener)
     */
    AddingRegionEvent addRegionNoSave(GeneralRegion region);

    /**
     * Mark all RegionGroups that they should regenerate regions.
     */
    void markGroupsAutoDirty();

    /**
     * Add a RegionGroup.
     *
     * @param group The RegionGroup to add
     */
    void addGroup(RegionGroup group);

    /**
     * Check if a player can add a certain region as rent or buy region.
     *
     * @param sender The player/console that wants to add a region
     * @param region The WorldGuard region to add
     * @param world  The world the ProtectedRegion is located in
     * @param type   The type the region should have in AreaShop
     * @return The result if a player would want to add this region
     */
    AddResult checkRegionAdd(CommandSender sender,
                             ProtectedRegion region,
                             World world,
                             GeneralRegion.RegionType type);

    /**
     * Remove a region from the list.
     *
     * @param region        The region to remove
     * @param giveMoneyBack use true to give money back to the player if someone is currently holding this region, otherwise false
     * @return true if the region has been removed, false otherwise
     */
    DeletingRegionEvent deleteRegion(GeneralRegion region, boolean giveMoneyBack);

    /**
     * Remove a group.
     *
     * @param group Group to remove
     */
    void removeGroup(RegionGroup group);

    /**
     * Update all signs that need periodic updating.
     */
    void performPeriodicSignUpdate();

    /**
     * Send out rent expire warnings.
     */
    void sendRentExpireWarnings();

    /**
     * Update regions in a task to minimize lag.
     *
     * @param regions              Regions to update
     * @param confirmationReceiver The CommandSender that should be notified at completion
     */
    void updateRegions(Collection<GeneralRegion> regions, CommandSender confirmationReceiver);

    /**
     * Update a list of regions.
     *
     * @param regions The list of regions to update.
     */
    void updateRegions(Collection<GeneralRegion> regions);

    /**
     * Update all regions, happens in a task to minimize lag.
     */
    void updateAllRegions();

    /**
     * Update all regions.
     *
     * @param confirmationReceiver Optional CommandSender that should receive progress messages
     */
    void updateAllRegions(CommandSender confirmationReceiver);

    /**
     * Save the group file to disk.
     */
    void saveGroupsIsRequired();

    /**
     * Check if saving the groups file is required.
     *
     * @return true if changes are made and saving is required, otherwise false
     */
    boolean isSaveGroupsRequired();

    /**
     * Save the groups file to disk synchronously.
     */
    void saveGroupsNow();

    /**
     * Save all region related files spread over time (low load).
     */
    void saveRequiredFiles();

    /**
     * Save all region related files directly (only for cases like onDisable()).
     */
    void saveRequiredFilesAtOnce();

    /**
     * Indicates that a/multiple WorldGuard regions need to be saved.
     *
     * @param worldName The world where the regions that should be saved is in
     */
    void saveIsRequiredForRegionWorld(String worldName);

    /**
     * Save all worldGuard regions that need saving.
     */
    void saveWorldGuardRegions();

    /**
     * Get the folder the region files are located in.
     *
     * @return The folder where the region.yml files are in
     */
    String getRegionFolder();

    /**
     * Check if a region is on the adding blacklist.
     *
     * @param region The region name to check
     * @return true if the region may not be added, otherwise false
     */
    boolean isBlacklisted(String region);

    /**
     * Unrent regions that have no time left, regions to check per tick is in the config.
     */
    void checkRents();

    /**
     * Check all regions and unrent/sell them if the player is inactive for too long.
     */
    void checkForInactiveRegions();

    /**
     * Load the file with the versions, used to check if the other files need conversion.
     */
    @SuppressWarnings("unchecked")
    void loadVersions();

    /**
     * Save the versions file to disk.
     */
    void saveVersions();

    /**
     * Load all files from disk.
     *
     * @param thisTick Load files in the current tick or a tick later
     * @return true if the files are loaded correctly, otherwise false
     */
    boolean loadFiles(boolean thisTick);

    /**
     * Load the default.yml file
     *
     * @return true if it has been loaded successfully, otherwise false
     */
    boolean loadDefaultFile();

    /**
     * Load the default.yml file
     *
     * @return true if it has been loaded successfully, otherwise false
     */
    boolean loadConfigFile();

    /**
     * Load the groups.yml file from disk
     *
     * @return true if succeeded, otherwise false
     */
    boolean loadGroupsFile();

    /**
     * Load all region files.
     */
    void loadRegionFiles();

    /**
     * Get the settings of a group.
     *
     * @param groupName Name of the group to get the settings from
     * @return The settings of the group
     */
    ConfigurationSection getGroupSettings(String groupName);

    /**
     * Set a setting for a group.
     *
     * @param group   The group to set it for
     * @param path    The path to set
     * @param setting The value to set
     */
    void setGroupSetting(RegionGroup group, String path, Object setting);

    // Enum for region types
    enum AddResult {
        BLACKLISTED("blacklisted"),
        NOPERMISSION("nopermission"),
        ALREADYADDED("alreadyadded"),
        ALREADYADDEDOTHERWORLD("alreadyaddedotherworld"),
        SUCCESS("success");

        private final String value;

        AddResult(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
