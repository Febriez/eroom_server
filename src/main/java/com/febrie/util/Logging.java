package com.febrie.util;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Logging {


    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static void info(@NotNull Logger logger, String message, Object... args) {
        logger.info(message, args);
    }

    public static void warn(@NotNull Logger logger, String message, Object... args) {
        logger.warn(message, args);
    }

    public static void error(@NotNull Logger logger, String message, Object... args) {
        logger.error(message, args);
    }

    public static void debug(@NotNull Logger logger, String message, Object... args) {
        logger.debug(message, args);
    }

    public static void error(@NotNull Logger logger, String message, Throwable t) {
        logger.error(message, t);
    }
}
