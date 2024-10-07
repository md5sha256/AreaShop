package me.wiefferink.areashop.tools.version;

import javax.annotation.Nullable;
import java.util.Locale;

public enum PreReleaseType {

    UNKNOWN("unknown"),

    ALPHA("alpha"),
    BETA("beta"),
    SNAPSHOT("snapshot"),
    RELEASE_CANDIDATE("rc");

    private static final PreReleaseType[] VALUES = values();

    private final String asString;

    PreReleaseType(String asString) {
        this.asString = asString;
    }

    @Nullable
    public static PreReleaseType parse(String release) {
        if (release.isEmpty()) {
            return null;
        }
        String sanitized = release.toLowerCase(Locale.ENGLISH);
        for (PreReleaseType preReleaseType : VALUES) {
            if (preReleaseType.asString.equals(sanitized)) {
                return preReleaseType;
            }
        }
        return UNKNOWN;
    }

    public String asString() {
        return this.asString;
    }
}
