package me.wiefferink.areashop.tools;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public record DurationInput(long duration, TimeUnit timeUnit) {

    public static final List<String> SUFFIXES;
    private static final Map<String, TimeUnit> SUFFIX_MAP = new HashMap<>();

    static {
        SUFFIX_MAP.put("d", TimeUnit.DAYS);
        SUFFIX_MAP.put("days", TimeUnit.DAYS);

        SUFFIX_MAP.put("h", TimeUnit.HOURS);
        SUFFIX_MAP.put("hrs", TimeUnit.HOURS);
        SUFFIX_MAP.put("hours", TimeUnit.HOURS);

        SUFFIX_MAP.put("m", TimeUnit.MINUTES);
        SUFFIX_MAP.put("mins", TimeUnit.MINUTES);
        SUFFIX_MAP.put("minutes", TimeUnit.MINUTES);

        SUFFIX_MAP.put("s", TimeUnit.SECONDS);
        SUFFIX_MAP.put("secs", TimeUnit.SECONDS);
        SUFFIX_MAP.put("seconds", TimeUnit.SECONDS);

        SUFFIX_MAP.put("ms", TimeUnit.MILLISECONDS);
        SUFFIX_MAP.put("millis", TimeUnit.MILLISECONDS);
        SUFFIX_MAP.put("milliseconds", TimeUnit.MILLISECONDS);

        SUFFIX_MAP.put("us", TimeUnit.MICROSECONDS);
        SUFFIX_MAP.put("micros", TimeUnit.MICROSECONDS);
        SUFFIX_MAP.put("microseconds", TimeUnit.MICROSECONDS);

        SUFFIX_MAP.put("ns", TimeUnit.NANOSECONDS);
        SUFFIX_MAP.put("nanos", TimeUnit.NANOSECONDS);
        SUFFIX_MAP.put("nanoseconds", TimeUnit.NANOSECONDS);

        SUFFIXES = List.copyOf(SUFFIX_MAP.keySet());
    }

    public static Optional<TimeUnit> getTimeUnit(@Nonnull String input) {
        return Optional.ofNullable(SUFFIX_MAP.get(input.toLowerCase(Locale.ENGLISH)));
    }

    @Nonnull
    public static String getTinySuffix(@Nonnull TimeUnit timeUnit) {
        return switch (timeUnit) {
            case DAYS -> "d";
            case HOURS -> "h";
            case MINUTES -> "m";
            case SECONDS -> "s";
            case MILLISECONDS -> "ms";
            case MICROSECONDS -> "us";
            case NANOSECONDS -> "ns";
        };
    }

    @Nonnull
    public static String getShortSuffix(@Nonnull TimeUnit timeUnit) {
        return switch (timeUnit) {
            case DAYS -> "days";
            case HOURS -> "hrs";
            case MINUTES -> "mins";
            case SECONDS -> "secs";
            case MILLISECONDS -> "millis";
            case MICROSECONDS -> "micros";
            case NANOSECONDS -> "nanos";
        };
    }

    @Nonnull
    public static String getSuffix(@Nonnull TimeUnit timeUnit) {
        return timeUnit.name().toLowerCase(Locale.ENGLISH);
    }

    @Nonnull
    public String toSpacedString() {
        return String.format("%d %s", duration(), getSuffix(timeUnit()));
    }

    @Nonnull
    public String toTinyString() {
        return String.format("%d%s", duration(), getTinySuffix(timeUnit()));
    }

    @Nonnull
    public String toTinySpacedString() {
        return String.format("%d %s", duration(), getTinySuffix(timeUnit()));
    }


    @Override
    public String toString() {
        return String.format("%d%s", duration(), getShortSuffix(timeUnit()));
    }

}
