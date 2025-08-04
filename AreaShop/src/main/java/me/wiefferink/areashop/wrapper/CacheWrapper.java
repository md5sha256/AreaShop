package me.wiefferink.areashop.wrapper;

public class CacheWrapper {

    private String name;
    private long lastUsed;

    public CacheWrapper() {
    }

    public CacheWrapper(String name, long lastUsed) {
        this.name = name;
        this.lastUsed = lastUsed;
    }

    public String getName() {
        this.lastUsed = System.currentTimeMillis();
        return name;

    }

    public void setName(String name) {
        this.lastUsed = System.currentTimeMillis();
        this.name = name;
    }

    public Long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }
}
