package dev.w1zzrd.logging;

import java.util.logging.Logger;

public class LoggerFactory {
    public static Logger getLogger(Class<?> cls) {
        return Logger.getLogger(cls.getName());
    }

    public static Logger getLogger(String name) {
        return Logger.getLogger(name);
    }
}
