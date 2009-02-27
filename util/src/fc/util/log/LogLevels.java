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

/** Pre-defined logging levels. Note: These are split into an interface of their
 * own to enable easy inclusion into static logger implementations (such as
 * {@link Log}).
 *
 * Each log level also has a human-readable string name.  For {@link
 * Logger} implementations derived from {@link AbstractLogger} setting
 * the system property {@value fc.util.log.AbstractLogger#PROPERTY_LEVEL}
 * to that string name causes the corresponding level to be set as the
 * minimum level.  The string names are documented at each level
 * constant.
 *
 * @author Tancred Lindholm
 * @author Jaakko Kangasharju
 */

public interface LogLevels {

    /**
     * Log level for assertion failures.  String name: {@code
     * "ASSERT"}.
     */
    static final int ASSERTFAILED = 18;

    /**
     * Log level for fatal errors.  String name: {@code "FATAL"}.
     */
    static final int FATALERROR = 15;

    /**
     * Log level for normal errors.  String name: {@code "ERROR"}.
     */
    static final int ERROR = 12;

    /**
     * Log level for warnings.  String name: {@code "WARN"}.
     */
    static final int WARNING = 9;

    /**
     * Log level for informative messages.  String name: {@code
     * "INFO"}.
     */
    static final int INFO = 6;

    /**
     * Log level for debugging output.  String name: {@code "DEBUG"}.
     */
    static final int DEBUG = 3;

    /**
     * Log level for tracing output.  String name: {@code "TRACE"}.
     */
    static final int TRACE = 0;

    /**
     * Human-readable names for the log levels.  In the absence of
     * enums, this is the best that can be done.
     */
    static final String[] names = { "TRACE", "", "", "DEBUG", "", "", "INFO",
				    "", "", "WARN", "", "", "ERROR",
				    "", "", "FATAL", "", "", "ASSERT" };

}

// arch-tag: 2721cfbf-7753-48f4-92aa-4aa914103e63

