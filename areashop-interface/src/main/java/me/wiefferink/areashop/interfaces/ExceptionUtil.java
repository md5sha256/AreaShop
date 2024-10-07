package me.wiefferink.areashop.interfaces;

import javax.annotation.Nonnull;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {

    private ExceptionUtil() {
        throw new IllegalStateException("Cannot instantiate static utility class");
    }

    @Nonnull
    public static String getStackTrace(@Nonnull Throwable throwable) {
        // Taken from Commons lang 3
        final StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer, true));
        return writer.toString();
    }

    @Nonnull
    public static String dumpCurrentStack() {
        return getStackTrace(new Exception());
    }

}
