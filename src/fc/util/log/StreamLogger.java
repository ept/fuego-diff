/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util.log;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import fc.util.Util;

/**
 * A {@link Logger} implementation for logging into a stream. This class gets a {@link PrintStream}
 * (e.g. {@link System#out}) at construction time and uses that stream for logging thereafter. Each
 * logger has a name that will be output for every log message.
 * @author Jaakko Kangasharju
 * @author Tancred Lindholm
 */
public class StreamLogger extends AbstractLogger {

    private DateFormat format;

    private PrintStream out;


    /**
     * Construct a logger with the default level.
     * @param out
     *            the stream to use for log output
     */
    public StreamLogger(PrintStream out) {
        this.out = out;
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }


    /**
     * Construct a logger with a given level.
     * @param out
     *            the stream to use for log output
     * @param level
     *            the minimum level to log at
     */
    public StreamLogger(PrintStream out, int level) {
        super(level);
        this.out = out;
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }


    public void log(Object message, int level, Object data) {
        if (data == null && message instanceof Throwable) {
            data = message;
            message = "Exception";
        }
        if (isEnabled(level)) {
            String name = (level >= 0 && level < names.length) ? names[level] : "UNKNOWN";
            out.println(name + ": " + format.format(new Date()));
            out.println(" - " + callingMethod());
            out.println(" - " + message);
            if (data != null) {
                out.print(" - ");
                if (data instanceof Throwable) {
                    ((Throwable) data).printStackTrace(out);
                } else if (data instanceof Object[]) {
                    out.println(Arrays.deepToString((Object[]) data));
                } else if (data.getClass().isArray()) {
                    Class cl = data.getClass();
                    if (cl == byte[].class) {
                        out.println(Arrays.toString((byte[]) data));
                    } else if (cl == short[].class) {
                        out.println(Arrays.toString((short[]) data));
                    } else if (cl == int[].class) {
                        out.println(Arrays.toString((int[]) data));
                    } else if (cl == long[].class) {
                        out.println(Arrays.toString((long[]) data));
                    } else if (cl == char[].class) {
                        out.println(Arrays.toString((char[]) data));
                    } else if (cl == float[].class) {
                        out.println(Arrays.toString((float[]) data));
                    } else if (cl == double[].class) {
                        out.println(Arrays.toString((double[]) data));
                    } else if (cl == boolean[].class) {
                        out.println(Arrays.toString((boolean[]) data));
                    }
                } else {
                    out.println(data.toString()); // Put fancier toString here
                }
            }
            if (level >= FATALERROR) { // Errors that require immediate shutdown
                // It is not appropriate to call System.exit
                // out.println("TERMINATING VM to avoid any damage.");
                // System.exit(-1);
                if (level >= ASSERTFAILED) {
                    throw new AssertionError(message);
                } else {
                    if (data instanceof Throwable) {
                        throw new Error(String.valueOf(message), (Throwable) data);
                    } else {
                        throw new Error(String.valueOf(message));
                    }
                }
            }
            out.flush();
        }
    }


    @Override
    public OutputStream getLogStream(int level) {

        if (isEnabled(level)) {

            return new FilterOutputStream(out) {

                public void close() throws IOException {
                    flush();
                    StreamLogger.this.out.println("<<EOS");
                }

            };
        } else return Util.SINK;
    }

}

// arch-tag: f2755a5b-ae47-471c-9bf8-f070c5d0499e
