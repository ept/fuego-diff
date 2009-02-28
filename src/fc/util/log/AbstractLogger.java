/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util.log;

import java.io.OutputStream;

/**
 * A helper class for implementing loggers. This class provides a generic foundation that can be
 * used by several loggers. It implements the level checks of {@link Logger#isEnabled} and the
 * {@link Logger#log(Object,int)} as a convenience.
 * @author Jaakko Kangasharju
 */
public abstract class AbstractLogger implements Logger {

    /**
     * The name of the system property for setting the log level. This is marked {@code public} so
     * that Javadoc can find it in {@code @value} statements.
     */
    public static final String PROPERTY_LEVEL = "fc.log.level";

    /**
     * The minimum level at which to log. By default this is set to {@link LogLevels#INFO}, but can
     * be reset in a subclass constructor or using the system property {@value #PROPERTY_LEVEL}; the
     * values for this property are given by each log level in {@link LogLevels}. It may be freely
     * changed at run time with the changes being visible in the object's behavior.
     */
    protected int minLevel = LogLevels.INFO;


    protected AbstractLogger() {
        String level = System.getProperty(PROPERTY_LEVEL);
        if (level != null) {
            for (int i = 0; i < LogLevels.names.length; i++) {
                if (level.equals(LogLevels.names[i])) {
                    minLevel = i;
                    break;
                }
            }
        }
    }


    protected AbstractLogger(int level) {
        minLevel = level;
    }


    /**
     * Return the method name that called logging. This method will trawl through the current stack
     * trace and pick the first one the is not in this class's package.
     */
    protected String callingMethod() {
        StackTraceElement[] stack = (new Throwable()).getStackTrace();
        StackTraceElement frame = null;
        for (StackTraceElement f : stack) {
            try {
                if (!f.getClassName().startsWith(AbstractLogger.class.getPackage().getName()) &&
                    !Logger.class.isAssignableFrom(Class.forName(f.getClassName()))) {
                    frame = f;
                    break;
                }
            } catch (ClassNotFoundException e) {
                frame = f;
                break;
            }
        }
        return String.valueOf(frame);
    }


    public boolean isEnabled(int level) {
        return level >= minLevel;
    }


    public void log(Object message, int level) {
        if (isEnabled(level)) {
            log(message, level, null);
        }
    }


    public void log(Object message, int level, Throwable cause) {
        if (isEnabled(level)) {
            log(message, level, (Object) cause);
        }
    }


    public void debug(Object message) {
        log(message, LogLevels.DEBUG);
    }


    public void debug(Object message, Throwable cause) {
        log(message, LogLevels.DEBUG, cause);
    }


    public void debug(Object message, Object data) {
        log(message, LogLevels.DEBUG, data);
    }


    public void info(Object message) {
        log(message, LogLevels.INFO);
    }


    public void info(Object message, Throwable cause) {
        log(message, LogLevels.INFO, cause);
    }


    public void info(Object message, Object data) {
        log(message, LogLevels.INFO, data);
    }


    public void warning(Object message) {
        log(message, LogLevels.WARNING);
    }


    public void warning(Object message, Throwable cause) {
        log(message, LogLevels.WARNING, cause);
    }


    public void warning(Object message, Object data) {
        log(message, LogLevels.WARNING, data);
    }


    public void error(Object message) {
        log(message, LogLevels.ERROR);
    }


    public void error(Object message, Throwable cause) {
        log(message, LogLevels.ERROR, cause);
    }


    public void error(Object message, Object data) {
        log(message, LogLevels.ERROR, data);
    }


    public void fatal(Object message) {
        log(message, LogLevels.FATALERROR);
    }


    public void fatal(Object message, Throwable cause) {
        log(message, LogLevels.FATALERROR, cause);
    }


    public void fatal(Object message, Object data) {
        log(message, LogLevels.FATALERROR, data);
    }


    abstract public OutputStream getLogStream(int level);

}

// arch-tag: 779f2f6f-6ae4-4f97-af8a-0cbe96a22929
