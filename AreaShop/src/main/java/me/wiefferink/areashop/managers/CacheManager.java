package me.wiefferink.areashop.managers;

import jakarta.inject.Inject;
import me.wiefferink.areashop.tools.CacheWrapper;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;

public class CacheManager extends Manager {

    private final Plugin plugin;
    private final Map<UUID, CacheWrapper> cache;

    private File cacheFile;
    private Duration expiryDuration;

    @Inject
    CacheManager(@Nonnull Plugin plugin) {
        this.plugin = plugin;
        this.cache = new HashMap<>();
    }


    @Override
    public void shutdown() {
        if (this.expiryDuration != null) {
            trimCache(expiryDuration);
        }
        if (this.cacheFile != null) {
            saveCache(cacheFile);
        }
    }

    public void initialize(@Nonnull File cacheFile, @Nonnull Duration expiryDuration) {
        this.cacheFile = cacheFile;
        this.expiryDuration = expiryDuration;
    }

    public void loadCache() {
        if (this.cacheFile != null) {
            loadCache(this.cacheFile);
        }
    }

    /**
     * Load the cache from the file
     */
    public void loadCache(@Nonnull File cacheFile) {
        if (Files.notExists(cacheFile.toPath())) {
            this.plugin.getLogger().info("uuid cache file does not exist, loaded 0 entries");
            return;
        }
        this.cache.clear();

        try (FileInputStream input = new FileInputStream(cacheFile)) {
            byte[] bytes = input.readAllBytes();
            ByteBuffer buffer = ByteBuffer.wrap(bytes).asReadOnlyBuffer();
            while (buffer.hasRemaining()) {
                long lastUsed = buffer.getLong();
                long lsb = buffer.getLong();
                long msb = buffer.getLong();
                int stringLen = buffer.getInt();
                byte[] stringBytes = new byte[stringLen];
                buffer.get(stringBytes);
                String name = new String(stringBytes, StandardCharsets.UTF_8);
                UUID uuid = new UUID(msb, lsb);
                CacheWrapper wrapper = new CacheWrapper(uuid, name, lastUsed);
                this.cache.put(wrapper.getUuid(), wrapper);
            }
            this.plugin.getLogger()
                    .info(String.format("Loaded %d cached uuid name entries", this.cache.size()));
        } catch (IOException ex) {
            ex.printStackTrace();
            this.plugin.getLogger().warning("Failed to load the uuid cache!");
        }
    }

    public void saveCache() {
        if (this.cacheFile != null) {
            saveCache(this.cacheFile);
        }
    }

    public void saveCacheAsync() {
        if (this.cacheFile != null) {
            saveCacheAsync(this.cacheFile);
        }
    }


    /**
     * Save the cache to the file
     */
    public void saveCache(@Nonnull File cacheFile) {
        List<CacheWrapper> copy = this.cache.values().stream().map(CacheWrapper::new).toList();
        try {
            // Ensure parent directory exists
            Path parentDir = cacheFile.toPath().getParent();
            if (parentDir != null) { // parent might be null if cacheFile is relative with no parent
                Files.createDirectories(parentDir);
            }
            byte[] bytes = serialize(copy);
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                fos.write(bytes);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save cache.json", e);
        }
    }

    public void saveCacheAsync(@Nonnull File cacheFile) {
        List<CacheWrapper> copy = this.cache.values().stream().map(CacheWrapper::new).toList();
        CompletableFuture.runAsync(() -> {
            try {
                // Ensure parent directory exists
                Path parentDir = cacheFile.toPath().getParent();
                if (parentDir != null) { // parent might be null if cacheFile is relative with no parent
                    Files.createDirectories(parentDir);
                }
                byte[] bytes = serialize(copy);
                try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                    fos.write(bytes);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save cache.json", e);
            }
        });
    }

    private byte[] serialize(List<CacheWrapper> wrappers) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(256 * wrappers.size());
        for (CacheWrapper wrapper : wrappers) {
            ByteBuffer buffer = ByteBuffer.allocate(256);
            byte[] stringBytes = wrapper.getName().getBytes(StandardCharsets.UTF_8);
            int stringLen = stringBytes.length;
            long lsb = wrapper.getUuid().getLeastSignificantBits();
            long msb = wrapper.getUuid().getMostSignificantBits();
            long lastUsed = wrapper.getLastUsed();
            buffer.putLong(lastUsed);
            buffer.putLong(lsb);
            buffer.putLong(msb);
            buffer.putInt(stringLen);
            buffer.put(stringBytes);

            buffer.flip();

            bos.write(buffer.array(), buffer.position(), buffer.remaining());
        }
        return bos.toByteArray();
    }

    /**
     * Get a cache entry
     */
    public CacheWrapper get(UUID uuid) {
        return cache.get(uuid);
    }

    public CacheWrapper computeIfAbsent(@Nonnull UUID uuid, Function<UUID, CacheWrapper> function) {
        return this.cache.computeIfAbsent(uuid, function);
    }

    /**
     * Put a cache entry
     */
    public void put(UUID uuid, CacheWrapper wrapper) {
        cache.put(uuid, wrapper);
    }

    /**
     * Remove a cache entry
     */
    public CacheWrapper remove(UUID uuid) {
        return cache.remove(uuid);
    }

    /**
     * Check if cache contains a UUID
     */
    public boolean contains(UUID uuid) {
        return cache.containsKey(uuid);
    }

    /**
     * Get all cache entries
     */
    public Map<UUID, CacheWrapper> getAllEntries() {
        return new HashMap<>(cache);
    }

    /**
     * Clear all cache entries
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Get the size of the cache
     */
    public int size() {
        return cache.size();
    }

    public void trimCache() {
        if (this.expiryDuration != null) {
            trimCache(this.expiryDuration);
        }
    }

    /**
     * Trim the cache by removing entries older than 1 week
     */
    public void trimCache(@Nonnull Duration expiryDuration) {
        long expiryMillis = expiryDuration.toMillis();
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, CacheWrapper>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, CacheWrapper> entry = iterator.next();
            CacheWrapper wrapper = entry.getValue();

            if (wrapper.getLastUsed() != -1 && (currentTime - wrapper.getLastUsed()) >= expiryMillis) {
                iterator.remove();
            }
        }

    }
}