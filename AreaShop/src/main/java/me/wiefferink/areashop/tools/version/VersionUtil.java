package me.wiefferink.areashop.tools.version;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class VersionUtil {
    public static final VersionData MINIMUM_VERSION = new VersionData(1, 21);

    @NotNull
    public static Version getCurrentServerVersion() {
        String version = Bukkit.getServer().getVersion();
        return Version.parse(version);
    }

    public static boolean isSupported() {
        Version current = getCurrentServerVersion();
        return !current.versionData().isOlderThan(MINIMUM_VERSION);
    }

    public static boolean isAtLeast(VersionData required) {
        Version current = getCurrentServerVersion();
        return !current.versionData().isOlderThan(required);
    }

}
