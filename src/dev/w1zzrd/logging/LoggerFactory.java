package dev.w1zzrd.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerFactory {
    public static Logger getLogger(final Class<?> cls) {
        return getLogger(cls, Level.ALL);
    }

    public static Logger getLogger(final Class<?> cls, final Level level) {
        final Logger l = Logger.getLogger(cls.getName());
        l.setLevel(level);
        return l;
    }

    public static Logger getLogger(final String name) {
        return getLogger(name, Level.ALL);
    }

    public static Logger getLogger(final String name, final Level level) {
        final Logger l = Logger.getLogger(name);
        l.setLevel(level);
        return l;
    }
}
