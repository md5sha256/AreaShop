package me.wiefferink.areashop.features.signs;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.bakedlibs.dough.blocks.ChunkPosition;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public class SignCache {

    private final Map<Long, RegionSign> allSigns = new HashMap<>();
    private final Map<Long, Collection<RegionSign>> signsByChunk = new HashMap<>();

    public Optional<RegionSign> signAtLocation(long position) {
        return Optional.ofNullable(this.allSigns.get(position));
    }

    public Collection<RegionSign> signsAtChunk(long position) {
        return Collections.unmodifiableCollection(this.signsByChunk.getOrDefault(position, Collections.emptyList()));
    }

    public Collection<RegionSign> allSigns() {
        return Collections.unmodifiableCollection(this.allSigns.values());
    }

    public Collection<BlockPosition> allSignLocations() {
        Collection<BlockPosition> positions = new ArrayList<>(this.signsByChunk.size());
        for (RegionSign sign : this.allSigns.values()) {
            positions.add(sign.getPosition());
        }
        return positions;
    }

    public void addSign(RegionSign regionSign) {
        BlockPosition blockPosition = regionSign.getPosition();
        this.allSigns.put(blockPosition.getPosition(), regionSign);
        ChunkPosition chunkPosition = new ChunkPosition(blockPosition.getChunk());
        Collection<RegionSign> signs = this.signsByChunk.computeIfAbsent(chunkPosition.getPosition(), x -> new HashSet<>());
        signs.add(regionSign);
    }

    public Optional<RegionSign> removeSign(Location location) {
        return removeSign(new BlockPosition(location));
    }

    public Optional<RegionSign> removeSign(BlockPosition position) {
        RegionSign removed = this.allSigns.remove(position.getPosition());
        if (removed != null) {
            World world = position.getWorld();
            ChunkPosition chunkPosition = new ChunkPosition(world, position.getChunkX(), position.getChunkZ());
            this.signsByChunk.remove(chunkPosition.getPosition());
            // Remove the sign from the region
        }
        return Optional.ofNullable(removed);
    }

    public void removeSign(RegionSign regionSign) {
        BlockPosition blockPosition = regionSign.getPosition();
        this.allSigns.remove(blockPosition.getPosition());
        ChunkPosition chunkPosition = new ChunkPosition(blockPosition.getChunk());
        long packedChunkPos = chunkPosition.getPosition();
        Collection<RegionSign> signs = this.signsByChunk.get(packedChunkPos);
        if (signs != null) {
            signs.remove(regionSign);
            if (signs.isEmpty()) {
                this.signsByChunk.remove(packedChunkPos);
            }
        }
    }

    public void clear() {
        this.allSigns.clear();
        this.signsByChunk.clear();
    }

}
