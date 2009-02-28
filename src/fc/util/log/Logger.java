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
 * A simple interface for logging. This interface is inspired by the Apache Commons Logging
 * interface, but is even more simplified. The purpose is to provide a common API so that the
 * logging in the system is consistent.
 * @author Jaakko Kangasharju
 * @author Tancred Lindholm
 */
public interface Logger extends LogLevels {

    /**
     * Return whether logging at the specified level is enabled. This method should always be called
     * before logging anything more complex than a simple string (e.g. when the string is
     * concatenated from several pieces at run time). Even though the logger will do this check,
     * checking this at the application level will avoid expensive needless processing.
     * @param level
     *            the level of logging to query
     * @return whether a <code>log</code> call at level {@code level} will log anything
     */
    boolean isEnabled(int level);


    /**
     * Log a message at a given level. If the given level is not enabled, nothing is done.
     * @param message
     *            the message to log
     * @param level
     *            the level at which to log
     */
    void log(Object message, int level);


    /**
     * Log a message at a given level with a cause. If the given level is not enabled, nothing is
     * done. This method takes as an additional argument a {@link Throwable} object that caused this
     * log message to be emitted; if this is not {@code null}, it should be logged, preferably with
     * a stack trace.
     * @param message
     *            the message to log
     * @param level
     *            the level at which to log
     * @param cause
     *            the {@link Throwable} that caused this logging
     */
    void log(Object message, int level, Throwable cause);


    /**
     * Log a message at a given level with additional data. If the given level is not enabled,
     * nothing is done. This method takes as an additional argument an object that contains debug
     * data relevant to this log message. If this is not {@code null}, it should be logged,
     * preferably as a human-readable string. Passing a separate data object, rather than
     * concatenating it to the message string allows for more readable formatting of e.g. String
     * arrays.
     * @param message
     *            the message to log
     * @param level
     *            the level at which to log
     * @param data
     *            debug data relevant to this log message
     */
    void log(Object message, int level, Object data);


    /**
     * Log a message at level debug. If the level is not enabled, nothing is done.
     * @param message
     *            the message to log
     */
    void debug(Object message);


    /**
     * Log a message at a debug level with a cause. If the debug level is not enabled, nothing is
     * done. This method takes as an additional argument a {@link Throwable} object that caused this
     * log message to be emitted; if this is not {@code null}, it should be logged, preferably with
     * a stack trace.
     * @param message
     *            the message to log
     * @param cause
     *            the {@link Throwable} that caused this logging
     */
    void debug(Object message, Throwable cause);


    /**
     * Log a message at debug level with additional data. If the debug level is not enabled, nothing
     * is done. This method takes as an additional argument an object that contains debug data
     * relevant to this log message. If this is not {@code null}, it should be logged, preferably as
     * a human-readable string. Passing a separate data object, rather than concatenating it to the
     * message string allows for more readable formatting of e.g. String arrays.
     * @param message
     *            the message to log
     * @param data
     *            debug data relevant to this log message
     */
    void debug(Object message, Object data);


    /**
     * Log a message at level info. If the level is not enabled, nothing is done.
     * @param message
     *            the message to log
     */
    void info(Object message);


    /**
     * Log a message at a info level with a cause. If the info level is not enabled, nothing is
     * done. This method takes as an additional argument a {@link Throwable} object that caused this
     * log message to be emitted; if this is not {@code null}, it should be logged, preferably with
     * a stack trace.
     * @param message
     *            the message to log
     * @param cause
     *            the {@link Throwable} that caused this logging
     */
    void info(Object message, Throwable cause);


    /**
     * Log a message at info level with additional data. If the info level is not enabled, nothing
     * is done. This method takes as an additional argument an object that contains info data
     * relevant to this log message. If this is not {@code null}, it should be logged, preferably as
     * a human-readable string. Passing a separate data object, rather than concatenating it to the
     * message string allows for more readable formatting of e.g. String arrays.
     * @param message
     *            the message to log
     * @param data
     *            info data relevant to this log message
     */
    void info(Object message, Object data);


    /**
     * Log a message at level warning. If the level is not enabled, nothing is done.
     * @param message
     *            the message to log
     */
    void warning(Object message);


    /**
     * Log a message at a warning level with a cause. If the warning level is not enabled, nothing
     * is done. This method takes as an additional argument a {@link Throwable} object that caused
     * this log message to be emitted; if this is not {@code null}, it should be logged, preferably
     * with a stack trace.
     * @param message
     *            the message to log
     * @param cause
     *            the {@link Throwable} that caused this logging
     */
    void warning(Object message, Throwable cause);


    /**
     * Log a message at warning level with additional data. If the warning level is not enabled,
     * nothing is done. This method takes as an additional argument an object that contains warning
     * data relevant to this log message. If this is not {@code null}, it should be logged,
     * preferably as a human-readable string. Passing a separate data object, rather than
     * concatenating it to the message string allows for more readable formatting of e.g. String
     * arrays.
     * @param message
     *            the message to log
     * @param data
     *            warning data relevant to this log message
     */
    void warning(Object message, Object data);


    /**
     * Log a message at level error. If the level is not enabled, nothing is done.
     * @param message
     *            the message to log
     */
    void error(Object message);


    /**
     * Log a message at a error level with a cause. If the error level is not enabled, nothing is
     * done. This method takes as an additional argument a {@link Throwable} object that caused this
     * log message to be emitted; if this is not {@code null}, it should be logged, preferably with
     * a stack trace.
     * @param message
     *            the message to log
     * @param cause
     *            the {@link Throwable} that caused this logging
     */
    void error(Object message, Throwable cause);


    /**
     * Log a message at error level with additional data. If the error level is not enabled, nothing
     * is done. This method takes as an additional argument an object that contains error data
     * relevant to this log message. If this is not {@code null}, it should be logged, preferably as
     * a human-readable string. Passing a separate data object, rather than concatenating it to the
     * message string allows for more readable formatting of e.g. String arrays.
     * @param message
     *            the message to log
     * @param data
     *            error data relevant to this log message
     */
    void error(Object message, Object data);


    /**
     * Log a message at level fatal. If the level is not enabled, nothing is done.
     * @param message
     *            the message to log
     */
    void fatal(Object message);


    /**
     * Log a message at a fatal level with a cause. If the fatal level is not enabled, nothing is
     * done. This method takes as an additional argument a {@link Throwable} object that caused this
     * log message to be emitted; if this is not {@code null}, it should be logged, preferably with
     * a stack trace.
     * @param message
     *            the message to log
     * @param cause
     *            the {@link Throwable} that caused this logging
     */
    void fatal(Object message, Throwable cause);


    /**
     * Log a message at fatal level with additional data. If the fatal level is not enabled, nothing
     * is done. This method takes as an additional argument an object that contains fatal data
     * relevant to this log message. If this is not {@code null}, it should be logged, preferably as
     * a human-readable string. Passing a separate data object, rather than concatenating it to the
     * message string allows for more readable formatting of e.g. String arrays.
     * @param message
     *            the message to log
     * @param data
     *            fatal data relevant to this log message
     */
    void fatal(Object message, Object data);


    public OutputStream getLogStream(int level);

}

// arch-tag: 8551f11f-2db5-4ad6-a806-f88001a3a0c6
