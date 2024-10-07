package me.wiefferink.areashop.tools.version;

import org.jetbrains.annotations.NotNull;

public record VersionData(int major, int minor, int patch, PreReleaseType preReleaseType)
        implements Comparable<VersionData> {


    public VersionData(int major, int minor, int patch) {
        this(major, minor, patch, null);
    }

    public VersionData(int major, int minor) {
        this(major, minor, 0);
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }

    public boolean isNewerThan(VersionData other) {
        return compareTo(other) > 0;
    }

    public boolean isOlderThan(VersionData other) {
        return compareTo(other) < 0;
    }


    @Override
    public int compareTo(@NotNull VersionData o) {
        int majorComp = Integer.compare(this.major, o.major);
        if (majorComp != 0) {
            return majorComp;
        }
        int minorComp = Integer.compare(this.minor, o.minor);
        if (minorComp != 0) {
            return minorComp;
        }
        int patchComp = Integer.compare(this.patch, o.patch);
        if (patchComp != 0) {
            return patchComp;
        }
        if (this.preReleaseType == null && o.preReleaseType == null) {
            return 0;
        }
        if (this.preReleaseType == null) {
            return 1;
        }
        return this.preReleaseType.compareTo(o.preReleaseType);
    }
}

