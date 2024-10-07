package me.wiefferink.areashop.tools.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Version(String original, VersionData versionData) {

    /**
     * Semver pattern, cg1 = major, cg2 = minor, cg3 = patch, cg4 = prerelease and cg5 = buildmetadata
     * Taken from <a href="https://semver.<a href="org/">...</a>">and https://regex1</a>01.com/r/vkijKf/1/
     */
    public static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");


    public static Version parse(String version) {
        Matcher matcher = SEMVER_PATTERN.matcher(version);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }
        String majorRaw = matcher.group(1);
        String minorRaw = matcher.group(2);
        String patchRaw = matcher.group(3);
        String preReleaseRaw = matcher.group(4);
        int major;
        int minor;
        try {
            major = Integer.parseInt(majorRaw);
            minor = Integer.parseInt(minorRaw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }
        if (patchRaw == null || patchRaw.isEmpty()) {
            return new Version(version, new VersionData(major, minor, 0, null));
        }
        int patch;
        try {
            patch = Integer.parseInt(patchRaw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }
        if (preReleaseRaw == null || preReleaseRaw.isEmpty()) {
            return new Version(version, new VersionData(major, minor, patch, null));
        }
        PreReleaseType releaseType = PreReleaseType.parse(preReleaseRaw);
        return new Version(version, new VersionData(major, minor, patch, releaseType));
    }

    @Override
    public String toString() {
        return this.original;
    }

}
