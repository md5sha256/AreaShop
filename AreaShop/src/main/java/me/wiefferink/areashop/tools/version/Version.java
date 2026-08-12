package me.wiefferink.areashop.tools.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Version(String original, VersionData versionData) {

    /**
     * Semver pattern, cg1 = major, cg2 = minor, cg3 = patch, cg4 = prerelease and cg5 = buildmetadata
     * Taken from <a href="https://semver.<a href="org/">...</a>">and https://regex1</a>01.com/r/vkijKf/1/
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:\\.(0|[1-9]\\d*))?" +
                    "(?:-(.+?))?(?:\\+.*)?$");

    public static Version parse(String versionStr) {
        if (versionStr == null || versionStr.isBlank()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
        String v = versionStr.trim();
        v = v.replaceAll("-R0\\.1-SNAPSHOT.*$", "")
                .replaceAll("\\.build\\..+$", "");


        Matcher matcher = VERSION_PATTERN.matcher(v);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid version: " + versionStr);
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));

        String patchGroup = matcher.group(3);
        int patch = patchGroup != null ? Integer.parseInt(patchGroup) : 0;

        String preRelease = matcher.group(4);
        PreReleaseType preReleaseType = null;
        if (preRelease != null && !preRelease.isBlank()) {
            preReleaseType = PreReleaseType.parse(preRelease);
        }

        return new Version(versionStr, new VersionData(major, minor, patch, preReleaseType));
    }

    @Override
    public String toString() {
        return original;
    }

}
