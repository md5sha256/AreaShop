package me.wiefferink.areashop.tools.version;

import javax.annotation.Nonnull;
import java.util.StringJoiner;

public class VersionUtil {

    public static final VersionData MC_1_21 = new VersionData(1, 21);

    @Nonnull
    public static String padTrailingZero(@Nonnull String minecraftVersion) {
        int dotCount = 0;
        for (char c : minecraftVersion.toCharArray()) {
            if (c == '.') {
                dotCount += 1;
            }
        }
        StringJoiner joiner = new StringJoiner("");
        for (int i = 0; i < 2 - dotCount; i++) {
            joiner.add(".0");
        }
        return minecraftVersion + joiner;
    }

    @Nonnull
    public static Version parseMinecraftVersion(@Nonnull String minecraftVersion) {
        // Expecting 1.X.X-R0.1-SNAPSHOT
        int stripLength = "-R0.1-SNAPSHOT".length();
        int length = minecraftVersion.length();
        if (length <= stripLength) {
            throw new IllegalArgumentException("Invalid minecraft version: " + minecraftVersion);
        }
        String strippedVersion = minecraftVersion.substring(0, length - stripLength);
        try {
            return Version.parse(padTrailingZero(strippedVersion));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid minecraft version: " + minecraftVersion, ex);
        }
    }

}
