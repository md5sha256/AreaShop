package me.wiefferink.areashop.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.UUID;

public class CacheWrapper {

    private static final Logger log = LoggerFactory.getLogger(CacheWrapper.class);
    private final UUID uuid;
    private final String name;
    private long lastUsed;

    public CacheWrapper(CacheWrapper other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.lastUsed = other.lastUsed;
    }

    public CacheWrapper(@Nonnull UUID uuid, @Nonnull String name) {
        this(uuid, name, -1);
    }

    public CacheWrapper(@Nonnull UUID uuid, @Nonnull String name, long lastUsed) {
        this.uuid = uuid;
        this.name = name;
        this.lastUsed = lastUsed;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getName() {
        this.lastUsed = System.currentTimeMillis();
        return name;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CacheWrapper that = (CacheWrapper) o;
        return lastUsed == that.lastUsed && Objects.equals(uuid,
                that.uuid) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name, lastUsed);
    }

    @Override
    public String toString() {
        return "CacheWrapper{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", lastUsed=" + lastUsed +
                '}';
    }
}
