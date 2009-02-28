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
 * A singleton-like class for one-stop logging. This class provides a way to use a single global
 * {@link Logger} for all logging needs. The methods are the same as in the {@link Logger} interface
 * and delegate to the underlying implementation. It is an error to call any other method of this
 * class prior to calling {@link #setLogger} with a non-{@code null} argument.
 * @author Jaakko Kangasharju
 * @author Tancred Lindholm
 */
public class Log implements LogLevels {

    // Log.log("ffo",Log.INFO)
    private static Logger logger = null;

    /**
     * The name of the system property for setting a logger. The value of this property needs to be
     * the name of a class having a no-argument constructor. This constructor is called to create a
     * default logger. Note that setting this property overrides any loggers set by the application.
     */
    public static final String PROPERTY_LOGGER = "fc.log.logger";

    static {
        String className = System.getProperty(PROPERTY_LOGGER);
        if (className != null) {
            try {
                Class loggerClass = Class.forName(className);
                Log.logger = (Logger) loggerClass.newInstance();
            } catch (Exception ex) {
                // XXX - how do you log errors when your logger is not
                // functional?
            }
        }
    }


    private Log() {
    }


    /**
     * Set the logger to use. After this method is called, any call to the other methods of this
     * class will delegate to {@code logger}. Only the first call of this method has any effect.
     * @param logger
     *            the logger to use from now on
     */
    public static void setLogger(Logger logger) {
        if (Log.logger == null && logger != null) {
            Log.logger = logger;
        }
    }


    /**
     * Call the current logger's corresponding method.
     * @see Logger#isEnabled
     */
    public static boolean isEnabled(int level) {
        if (logger != null) {
            return logger.isEnabled(level);
        } else {
            return false;
        }
    }


    /**
     * Call the current logger's corresponding method.
     * @see Logger#log(Object,int)
     */
    public static void log(Object message, int level) {
        if (logger != null) {
            logger.log(message, level);
        }
    }


    /**
     * Call the current logger's corresponding method.
     * @see Logger#log(Object,int,Throwable)
     */
    public static void log(Object message, int level, Throwable cause) {
        if (logger != null) {
            logger.log(message, level, cause);
        }
    }


    /**
     * Call the current logger's corresponding method.
     * @see Logger#log(Object,int,Object)
     */
    public static void log(Object message, int level, Object data) {
        if (logger != null) {
            logger.log(message, level, data);
        }
    }


    public static void trace(Object message) {
        log(message, LogLevels.TRACE);
    }


    public static void trace(Object message, Throwable cause) {
        log(message, LogLevels.TRACE, cause);
    }


    public static void trace(Object message, Object data) {
        log(message, LogLevels.TRACE, data);
    }


    public static void debug(Object message) {
        log(message, LogLevels.DEBUG);
    }


    public static void debug(Object message, Throwable cause) {
        log(message, LogLevels.DEBUG, cause);
    }


    public static void debug(Object message, Object data) {
        log(message, LogLevels.DEBUG, data);
    }


    public static void info(Object message) {
        log(message, LogLevels.INFO);
    }


    public static void info(Object message, Throwable cause) {
        log(message, LogLevels.INFO, cause);
    }


    public static void info(Object message, Object data) {
        log(message, LogLevels.INFO, data);
    }


    public static void warning(Object message) {
        log(message, LogLevels.WARNING);
    }


    public static void warning(Object message, Throwable cause) {
        log(message, LogLevels.WARNING, cause);
    }


    public static void warning(Object message, Object data) {
        log(message, LogLevels.WARNING, data);
    }


    public static void error(Object message) {
        log(message, LogLevels.ERROR);
    }


    public static void error(Object message, Throwable cause) {
        log(message, LogLevels.ERROR, cause);
    }


    public static void error(Object message, Object data) {
        log(message, LogLevels.ERROR, data);
    }


    public static void fatal(Object message) {
        log(message, LogLevels.FATALERROR);
    }


    public static void fatal(Object message, Throwable cause) {
        log(message, LogLevels.FATALERROR, cause);
    }


    public static void fatal(Object message, Object data) {
        log(message, LogLevels.FATALERROR, data);
    }


    public static OutputStream getLogStream(int level) {
        return logger.getLogStream(level);
    }

}

// arch-tag: 8aa10004-ab8d-4e62-b89a-027c150ab30e
