package me.wiefferink.areashop.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import jakarta.inject.Inject;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.wrapper.CacheWrapper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class CacheManager extends Manager {

    private final AreaShop plugin;
    private final Gson gson;
    private final File cacheFile;
    private Map<UUID, CacheWrapper> cache;

    @Inject
    CacheManager(AreaShop plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cacheFile = new File(plugin.getDataFolder(), "cache.json");
        this.cache = new HashMap<>();

        loadCache();
    }


    @Override
    public void shutdown() {
        trimCache();
        saveCache();
    }

    /**
     * Load the cache from the file, create if it doesn't exist
     */
    public void loadCache() {
        if (!cacheFile.exists()) {
            saveCache();
            return;
        }

        try (FileReader reader = new FileReader(cacheFile)) {
            Type type = new TypeToken<Map<UUID, CacheWrapper>>() {
            }.getType();
            Map<UUID, CacheWrapper> loadedCache = gson.fromJson(reader, type);

            if (loadedCache != null) {
                cache = loadedCache;
                plugin.getLogger().info("Loaded " + cache.size() + " cache entries from cache.json");
            } else {
                cache = new HashMap<>();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load cache.json", e);
            cache = new HashMap<>();
        }
    }


    /**
     * Save the cache to the file
     */
    public void saveCache() {

        CompletableFuture.runAsync(() -> {
            try {
                // Ensure parent directory exists
                if (!cacheFile.getParentFile().exists()) {
                    cacheFile.getParentFile().mkdirs();
                }

                try (FileWriter writer = new FileWriter(cacheFile)) {
                    gson.toJson(cache, writer);
                    plugin.getLogger().fine("Saved " + cache.size() + " cache entries to cache.json");
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save cache.json", e);
            }
        });
    }

    /**
     * Get a cache entry
     */
    public CacheWrapper get(UUID uuid) {
        return cache.get(uuid);
    }

    public Map<UUID, CacheWrapper> cache() {
        return cache;
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

    /**
     * Trim the cache by removing entries older than 1 week
     */
    public void trimCache() {
        long timeInMillis = 30L * 24L * 60L * 60L * 1000L;
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, CacheWrapper>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, CacheWrapper> entry = iterator.next();
            CacheWrapper wrapper = entry.getValue();

            if (wrapper.getLastUsed() != null && (currentTime - wrapper.getLastUsed()) > timeInMillis) {
                iterator.remove();
            }
        }

    }
}