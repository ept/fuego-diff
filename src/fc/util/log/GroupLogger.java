/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util.log;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import fc.util.Util;

/**
 * A class for logging by thread group.  The methods in this class
 * provide a way to direct the loggings of different thread groups to
 * different loggers.
 *
 * @author Jaakko Kangasharju
 */
public class GroupLogger extends AbstractLogger {

    private static Map<ThreadGroup,Logger> loggers
	= new HashMap<ThreadGroup,Logger>();

    /**
     * Install a logger logging to the given stream.  This class is a
     * convenience method that installs a {@link StreamLogger} to log
     * to the given stream at the default level.  The group to
     * associate the new logger with is the current thread's group.
     *
     * @param out the stream to log into
     */
    public void install (PrintStream out) {
	install(new StreamLogger(out));
    }

    /**
     * Install a logger logging to the given stream.  This class is a
     * convenience method that installs a {@link StreamLogger} to log
     * to the given stream at the given level.  The group to associate
     * the new logger with is the current thread's group.
     *
     * @param out the stream to log into
     * @param level the level to log at
     */
    public void install (PrintStream out, int level) {
	install(new StreamLogger(out, level));
    }

    /**
     * Install a logger for this thread group.  The group to associate
     * the new logger with is the current thread's group.
     *
     * @param logger the logger to use for this thread group
     */
    public void install (Logger logger) {
	ThreadGroup group = Thread.currentThread().getThreadGroup();
	loggers.put(group, logger);
    }

    /**
     * Query whether a logger has been installed.
     *
     * @return whether this thread group already has an associated
     * logger
     */
    public boolean isInstalled () {
	ThreadGroup group = Thread.currentThread().getThreadGroup();
	return loggers.containsKey(group);
    }

    /**
     * Call the current group's logger's corresponding method.
     */
    public boolean isEnabled (int level) {
	ThreadGroup group = Thread.currentThread().getThreadGroup();
	Logger logger = loggers.get(group);
	if (logger != null) {
	    return logger.isEnabled(level);
	} else {
	    return false;
	}
    }

    /**
     * Call the current group's logger's corresponding method.
     */
    public void log (Object message, int level) {
	ThreadGroup group = Thread.currentThread().getThreadGroup();
	Logger logger = loggers.get(group);
	if (logger != null) {
	    logger.log(message, level);
	}
    }

    /**
     * Call the current group's logger's corresponding method.
     */
    public void log (Object message, int level, Object data) {
	ThreadGroup group = Thread.currentThread().getThreadGroup();
	Logger logger = loggers.get(group);
	if (logger != null) {
	    logger.log(message, level, data);
	}
    }

    public OutputStream getLogStream(int level) {
	ThreadGroup group = Thread.currentThread().getThreadGroup();
	Logger logger = loggers.get(group);
	return logger == null ? Util.SINK : logger.getLogStream(level);
    }

}

// arch-tag: cc0234ff-012c-490b-9357-4a37f570d0cb
