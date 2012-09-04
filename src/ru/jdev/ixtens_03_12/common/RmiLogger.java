package ru.jdev.ixtens_03_12.common;

import org.apache.log4j.Logger;

public class RmiLogger {

    private final Logger logger;

    public RmiLogger(Logger logger) {
        this.logger = logger;
    }

    public void trace(String message, Object... params) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format(message, params));
        }
    }

    public void info(String message, Object... params) {
        if (logger.isTraceEnabled()) {
            logger.info(String.format(message, params));
        }
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String message, Throwable throwable, Object... params) {
        logger.warn(String.format(message, params), throwable);
    }

    public void fatal(String message, Throwable t) {
        logger.fatal(message, t);
    }

    public void fatal(String message) {
        logger.fatal(message);
    }

}
