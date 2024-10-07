package me.wiefferink.areashop.adapters.plugins;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.EntityFunction;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.wiefferink.areashop.interfaces.AreaShopInterface;
import me.wiefferink.areashop.interfaces.ExceptionUtil;
import me.wiefferink.areashop.interfaces.GeneralRegionInterface;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public class FastAsyncWorldEditHandler extends WorldEditInterface {

    public FastAsyncWorldEditHandler(AreaShopInterface pluginInterface) {
        super(pluginInterface);
    }

    @Override
    public boolean supportsAsyncOperations() {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> restoreRegionBlocksAsync(File rawFile, GeneralRegionInterface regionInterface) {
        File targetFile = null;
        for (ClipboardFormat formatOption : ClipboardFormats.getAll()) {
            for (String extension : formatOption.getFileExtensions()) {
                File fileOption = new File(rawFile.getAbsolutePath() + "." + extension);
                if (fileOption.exists()) {
                    targetFile = fileOption;
                    break;
                }
            }
        }
        if (targetFile == null || !targetFile.exists() || !targetFile.isFile()) {
            pluginInterface.getLogger().info(() -> "Not restoring region. Schematic not found: " + rawFile);
            return CompletableFuture.completedFuture(false);
        }
        File finalFile = targetFile;
        ClipboardFormat format = ClipboardFormats.findByFile(targetFile);
        if (format == null) {
            pluginInterface.getLogger().warning(() -> "Could not find a clipboard format for file: " + finalFile.getAbsolutePath());
            return CompletableFuture.completedFuture(false);
        }
        BlockVector3 min = regionInterface.getRegion().getMinimumPoint();
        final World world = BukkitAdapter.adapt(regionInterface.getWorld());
        if (world == null) {
            pluginInterface.getLogger().warning(() -> "Did not restore region " + regionInterface.getName() + ", world not found: " + regionInterface.getWorldName());
            return CompletableFuture.completedFuture(false);

        }
        ProtectedRegion wgRegion = regionInterface.getRegion();
        long volume = wgRegion.volume();
        Region region = new CuboidRegion(wgRegion.getMinimumPoint(), wgRegion.getMaximumPoint());
        int maxBlocks = pluginInterface.getConfig().getInt("maximumBlocks", Integer.MAX_VALUE);
        if (volume > maxBlocks) {
            pluginInterface.getLogger().warning((() -> "Region is bigger than the max allowed block change size! Volume: " + volume + " Limit: " + maxBlocks));
            return CompletableFuture.completedFuture(false);
        }
        pluginInterface.debugI(() -> String.format("Attempting to restore using format: %s", format.getName()));
        // Bug in which schematic pasting doesn't clear tile entities properly
        // We manually remove tile entities in the destination before we paste the schematic
        pluginInterface.debugI(() -> "Clearing entities for region: " + regionInterface.getName());
        if (pluginInterface.getConfig().getBoolean("forceClearEntities", false) && failedClearEntities(world, region, regionInterface)) {
            pluginInterface.getLogger().warning(() -> "Failed to clear tile entities for region:  " + regionInterface.getName() + ". Will attempt to past anyway");
        }
        BlockVector3 dimensions = regionInterface.computeDimensions();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(pluginInterface.pluginInstance(), () -> {
            Clipboard clipboard;
            try (InputStream is = new FileInputStream(finalFile);
                 ClipboardReader reader = format.getReader(is)) {
                clipboard = reader.read();
                if (!clipboard.getDimensions().equals(dimensions)) {
                    pluginInterface.getLogger().warning(() -> "Size of the region " + regionInterface.getName() + " is not the same as the schematic to restore!");
                    pluginInterface.debugI(() -> "schematic|region, x:" + clipboard.getDimensions().getX() + "|" + regionInterface.getWidth() + ", y:" + clipboard.getDimensions().getY() + "|" + regionInterface.getHeight() + ", z:" + clipboard.getDimensions().getZ() + "|" + regionInterface.getDepth());
                    future.complete(false);
                    return;
                }
            } catch (IOException ex) {
                pluginInterface.getLogger().warning("An error occurred while restoring schematic of " + regionInterface.getName() + ", enable debug to see the complete stacktrace");
                pluginInterface.debugI(() -> ExceptionUtil.getStackTrace(ex));
                future.complete(false);
                return;
            } catch (Exception ex) {
                pluginInterface.getLogger().warning(() -> "crashed during restore of " + regionInterface.getName());
                pluginInterface.debugI(() -> ExceptionUtil.getStackTrace(ex));
                future.complete(false);
                return;
            }
            final EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit().newEditSessionBuilder()
                    .world(world)
                    .build();
            final Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
                    .to(min)
                    .copyEntities(false) // Don't copy entities as FAWE has bug which doesn't synchronize entity access
                    .build();
            future.complete(true);
            try (editSession; clipboard) {
                Operations.complete(operation);
                future.complete(true);
            } catch (WorldEditException ex) {
                pluginInterface.getLogger().warning(() -> "An error occurred while restoring schematic of " + regionInterface.getName() + ", enable debug to see the complete stacktrace");
                pluginInterface.debugI(() -> ExceptionUtil.getStackTrace(ex));
                future.complete(false);
            }
        });
        return future;

    }

    @Override
    public CompletableFuture<Boolean> saveRegionBlocksAsync(File file, GeneralRegionInterface regionInterface) {
        final ClipboardFormat format = BuiltInClipboardFormat.FAST;
        final File targetFile = new File(file.getAbsolutePath() + "." + format.getPrimaryFileExtension());
        ProtectedRegion wgRegion = regionInterface.getRegion();
        Region region = new CuboidRegion(wgRegion.getMinimumPoint(), wgRegion.getMaximumPoint());
        final World world = BukkitAdapter.adapt(regionInterface.getWorld());
        if (world == null) {
            pluginInterface.getLogger().warning(() -> "Did not save region " + regionInterface.getName() + ", world not found: " + regionInterface.getWorldName());
            return CompletableFuture.completedFuture(false);
        }
        int maxBlocks = pluginInterface.getConfig().getInt("maximumBlocks", Integer.MAX_VALUE);
        long volume = region.getVolume();
        if (volume > maxBlocks) {
            pluginInterface.getLogger().warning((() -> "Region is bigger than the max allowed block change size! Volume: " + volume + " Limit: " + maxBlocks));
            return CompletableFuture.completedFuture(false);
        }
        pluginInterface.debugI(() -> String.format("Trying to save region %s to file %s with format %s", regionInterface.getName(), file.getName(), format.getName()));
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(pluginInterface.pluginInstance(), () -> {
            try (OutputStream os = new FileOutputStream(targetFile);
                 ClipboardWriter writer = format.getWriter(os);
                 Clipboard clipboard = new BlockArrayClipboard(region)) {
                try (EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit().newEditSessionBuilder()
                        .world(world)
                        .build()) {
                    final ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
                    // Don't copy entities as FAWE has bug which doesn't synchronize entity access
                    copy.setCopyingEntities(false);
                    Operations.complete(copy);
                }
                writer.write(clipboard);
                future.complete(true);
                return;
            } catch (IOException | WorldEditException ex) {
                pluginInterface.getLogger().warning(() -> "An error occurred while saving schematic of " + regionInterface.getName() + ", enable debug to see the complete stacktrace");
                pluginInterface.debugI(() -> ExceptionUtil.getStackTrace(ex));
            } catch (Exception ex) {
                pluginInterface.getLogger().warning(() -> "crashed during save of " + regionInterface.getName());
                pluginInterface.debugI(() -> ExceptionUtil.getStackTrace(ex));
            }
            future.complete(false);
        });
        return future;
    }

    @Override
    public boolean restoreRegionBlocks(File rawFile, GeneralRegionInterface regionInterface) {
        File targetFile = null;
        for (ClipboardFormat formatOption : ClipboardFormats.getAll()) {
            for (String extension : formatOption.getFileExtensions()) {
                File fileOption = new File(rawFile.getAbsolutePath() + "." + extension);
                if (fileOption.exists()) {
                    targetFile = fileOption;
                    break;
                }
            }
        }
        if (targetFile == null || !targetFile.exists() || !targetFile.isFile()) {
            pluginInterface.getLogger().info(() -> "Not restoring region. Schematic not found: " + rawFile);
            return false;
        }
        File finalFile = targetFile;
        ClipboardFormat format = ClipboardFormats.findByFile(targetFile);
        if (format == null) {
            pluginInterface.getLogger().warning(() -> "Could not find a clipboard format for file: " + finalFile.getAbsolutePath());
            return false;
        }
        BlockVector3 min = regionInterface.getRegion().getMinimumPoint();
        final World world = BukkitAdapter.adapt(regionInterface.getWorld());
        if (world == null) {
            pluginInterface.getLogger().warning(() -> "Did not restore region " + regionInterface.getName() + ", world not found: " + regionInterface.getWorldName());
            return false;

        }
        long volume = regionInterface.getRegion().volume();
        int maxBlocks = pluginInterface.getConfig().getInt("maximumBlocks", Integer.MAX_VALUE);
        if (volume > maxBlocks) {
            pluginInterface.getLogger().warning((() -> "Region is bigger than the max allowed block change size! Volume: " + volume + " Limit: " + maxBlocks));
            return false;
        }
        pluginInterface.debugI(String.format("Attempting to restore using format: %s", format.getName()));
        // Bug in which schematic pasting doesn't clear tile entities properly
        // We manually remove tile entities in the destination before we paste the schematic
        pluginInterface.debugI(() -> "Clearing entities for region: " + regionInterface.getName());
        ProtectedRegion wgRegion = regionInterface.getRegion();
        Region region = new CuboidRegion(wgRegion.getMinimumPoint(), wgRegion.getMaximumPoint());
        if (pluginInterface.getConfig().getBoolean("forceClearEntities", false) && failedClearEntities(world, region, regionInterface)) {
            pluginInterface.getLogger().warning(() -> "Failed to clear tile entities for region:  " + regionInterface.getName() + ". Will attempt to past anyway");
        }
        BlockVector3 dimensions = regionInterface.computeDimensions();
        try (InputStream is = new FileInputStream(finalFile);
             ClipboardReader reader = format.getReader(is)) {
            Clipboard clipboard = reader.read();
            if (!clipboard.getDimensions().equals(dimensions)) {
                pluginInterface.getLogger().warning(() -> "Size of the region " + regionInterface.getName() + " is not the same as the schematic to restore!");
                pluginInterface.debugI(() -> "schematic|region, x:" + clipboard.getDimensions().getX() + "|" + regionInterface.getWidth() + ", y:" + clipboard.getDimensions().getY() + "|" + regionInterface.getHeight() + ", z:" + clipboard.getDimensions().getZ() + "|" + regionInterface.getDepth());
                return false;
            }
            final Operation operation = new ClipboardHolder(clipboard).createPaste(world)
                    .to(min)
                    .copyEntities(false)
                    .build();
            Operations.complete(operation);
            return true;
        } catch (IOException | WorldEditException ex) {
            pluginInterface.getLogger().warning(() -> "An error occurred while restoring schematic of " + regionInterface.getName() + ", enable debug to see the complete stacktrace");
            pluginInterface.debugI(() -> ExceptionUtil.getStackTrace(ex));
        } catch (Exception ex) {
            pluginInterface.getLogger().warning(() -> "crashed during restore of " + regionInterface.getName());
            pluginInterface.debugI(() -> ExceptionUtil.getStackTrace(ex));
        }
        return false;
    }

    @Override
    public boolean saveRegionBlocks(File file, GeneralRegionInterface regionInterface) {
        final ClipboardFormat format = BuiltInClipboardFormat.FAST;
        final File targetFile = new File(file.getAbsolutePath() + "." + format.getPrimaryFileExtension());
        ProtectedRegion wgRegion = regionInterface.getRegion();
        Region region = new CuboidRegion(wgRegion.getMinimumPoint(), wgRegion.getMaximumPoint());
        final World world = BukkitAdapter.adapt(regionInterface.getWorld());
        if (world == null) {
            pluginInterface.getLogger().warning("Did not save region " + regionInterface.getName() + ", world not found: " + regionInterface.getWorldName());
            return false;
        }
        int maxBlocks = pluginInterface.getConfig().getInt("maximumBlocks", Integer.MAX_VALUE);
        long volume = region.getVolume();
        if (volume > maxBlocks) {
            pluginInterface.getLogger().warning((() -> "Region is bigger than the max allowed block change size! Volume: " + volume + " Limit: " + maxBlocks));
            return false;
        }
        pluginInterface.debugI(() -> String.format("Trying to save region %s to file %s with format %s", regionInterface.getName(), file.getName(), format.getName()));
        try (OutputStream os = new FileOutputStream(targetFile);
             ClipboardWriter writer = format.getWriter(os);
             Clipboard clipboard = new BlockArrayClipboard(region)) {
            try (EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit().newEditSessionBuilder()
                    .world(world)
                    .build()) {
                final ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
                copy.setCopyingEntities(true);
                Operations.complete(copy);
            }
            writer.write(clipboard);
            return true;
        } catch (IOException | WorldEditException ex) {
            pluginInterface.getLogger().warning(() -> "An error occurred while saving schematic of " + regionInterface.getName() + ", enable debug to see the complete stacktrace");
            pluginInterface.debugI(() -> ExceptionUtil.getStackTrace(ex));
        } catch (Exception ex) {
            pluginInterface.getLogger().warning("crashed during save of " + regionInterface.getName());
            pluginInterface.debugI(() -> ExceptionUtil.getStackTrace(ex));
        }
        return false;
    }

    @Override
    public WorldEditSelection getPlayerSelection(Player player) {
        try {
            Region region = pluginInterface.getWorldEdit().getSession(player)
                    .getSelection(BukkitAdapter.adapt(player.getWorld()));
            return new WorldEditSelection(player.getWorld(),
                    BukkitAdapter.adapt(player.getWorld(), region.getMinimumPoint()),
                    BukkitAdapter.adapt(player.getWorld(), region.getMaximumPoint()));
        } catch (IncompleteRegionException e) {
            return null;
        }
    }

    private boolean failedClearEntities(World world, Region region, GeneralRegionInterface regionInterface) {
        try (EditSession editSession = pluginInterface.getWorldEdit().getWorldEdit().newEditSessionBuilder()
                .world(world)
                .build()) {
            EntityFunction function = entity -> {
                EntityType entityType = entity.getType();
                if (entityType != null && (entityType.equals(EntityTypes.ITEM_FRAME)
                        || entityType.equals(EntityTypes.PAINTING)
                        || entityType.equals(EntityTypes.LEASH_KNOT)) && !entity.remove()) {
                    pluginInterface.debugI("Could not remove entity: " + entityType.getName());
                }
                return false;
            };
            EntityVisitor visitor = new EntityVisitor(editSession.getEntities(region).iterator(), function);
            Operations.complete(visitor);
            return false;
        } catch (WorldEditException ex) {
            pluginInterface.getLogger().warning("crashed during save of " + regionInterface.getName());
            pluginInterface.debugI(ExceptionUtil.getStackTrace(ex));
            return true;
        }
    }
}
