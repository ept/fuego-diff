/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util.log;

/**
 * @author Jaakko Kangasharju
 * @author Tancred Lindholm
 */
public class CommonsLogger implements org.apache.commons.logging.Log {

    public CommonsLogger(String name) {
        // Our logging system has no place for the name
    }


    public boolean isTraceEnabled() {
        return Log.isEnabled(LogLevels.TRACE);
    }


    public boolean isDebugEnabled() {
        return Log.isEnabled(LogLevels.DEBUG);
    }


    public boolean isInfoEnabled() {
        return Log.isEnabled(LogLevels.INFO);
    }


    public boolean isWarnEnabled() {
        return Log.isEnabled(LogLevels.WARNING);
    }


    public boolean isErrorEnabled() {
        return Log.isEnabled(LogLevels.ERROR);
    }


    public boolean isFatalEnabled() {
        return Log.isEnabled(LogLevels.FATALERROR);
    }


    public void trace(Object message) {
        Log.log(message, LogLevels.TRACE);
    }


    public void trace(Object message, Throwable t) {
        Log.log(message, LogLevels.TRACE, t);
    }


    public void debug(Object message) {
        Log.log(message, LogLevels.DEBUG);
    }


    public void debug(Object message, Throwable t) {
        Log.log(message, LogLevels.DEBUG, t);
    }


    public void info(Object message) {
        Log.log(message, LogLevels.INFO);
    }


    public void info(Object message, Throwable t) {
        Log.log(message, LogLevels.INFO, t);
    }


    public void warn(Object message) {
        Log.log(message, LogLevels.WARNING);
    }


    public void warn(Object message, Throwable t) {
        Log.log(message, LogLevels.WARNING, t);
    }


    public void error(Object message) {
        Log.log(message, LogLevels.ERROR);
    }


    public void error(Object message, Throwable t) {
        Log.log(message, LogLevels.ERROR, t);
    }


    public void fatal(Object message) {
        Log.log(message, LogLevels.FATALERROR);
    }


    public void fatal(Object message, Throwable t) {
        Log.log(message, LogLevels.FATALERROR, t);
    }

}

// arch-tag: 8ec1a0b3-4147-4177-985d-ce892f007c24
