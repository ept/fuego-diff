/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;

/**
 * A utility class collecting various useful <code>static</code> methods. This class can only be
 * used for its static methods; no instances can be created.
 * <p>
 * The difference between this class and the {@link Util} class is that this class is allowed to
 * contain methods depending on libraries outside the MIDP and CLDC specifications.
 * @author Jaakko Kangasharju
 */
public class ExtUtil {

    /*
     * Private constructor to prevent instantiation.
     */
    private ExtUtil() {
    }


    /**
     * Stringize an internet address reasonably. The normal stringization of an
     * <code>InetSocketAddress</code> does not produce addresses suitable for the host:port part of
     * a URL. This method returns a string of the form <code>addr:port</code> where
     * <code>addr</code> is either a hostname or a dotted-decimal IP address. IP address is
     * preferred if available.
     */
    public static String addressToString(InetSocketAddress a) {
        String value;
        InetAddress addr = a.getAddress();
        if (addr != null) {
            value = addr.getHostAddress();
        } else {
            value = a.getHostName();
        }
        value += ":" + a.getPort();
        return value;
    }


    /**
     * Return a stack trace as a string. Especially when logging it is not convenient to use the
     * {@link Throwable#printStackTrace()} methods, since they mess up the logging output. So this
     * method constructs a string similar to what would be printed by
     * {@link Throwable#printStackTrace()} and returns that.
     * @param t
     *            the {@link Throwable} of which the stack trace is desired
     * @return a string representation of <code>t</code>'s stack trace.
     */
    public static String stackTraceString(Throwable t) {
        StringBuffer val = new StringBuffer();
        boolean first = true;
        while (t != null) {
            if (first) {
                val.append(t.toString() + "\n");
            } else {
                val.append("Cause: " + t + "\n");
            }
            StackTraceElement[] stackTrace = t.getStackTrace();
            for (int j = 0; j < stackTrace.length; j++) {
                val.append("\tat " + stackTrace[j] + "\n");
            }
            t = t.getCause();
        }
        return val.toString();
    }


    /**
     * Open a named resource. This method does essentially the same thing as
     * {@link ClassLoader#getResource} except that the using code is spared from having to create a
     * {@link ClassLoader} instance.
     * @param name
     *            the name of the resource
     * @return a {@link URL} pointing to the resource or <code>null</code> is the resource was not
     *         found
     */
    public static URL getResource(String name) {
        URL url = null;
        ClassLoader loader = ExtUtil.class.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        if (loader != null) {
            url = loader.getResource(name);
        }
        return url;
    }


    /**
     * Open an input stream to either a file or a resource. This method opens an input stream to the
     * file named <code>name</code>. If the file is not found, it will attempt to locate a resource
     * named <code>dir/name</code> and return an input stream for that resource.
     * @param dir
     *            the initial part of the resource name
     * @param name
     *            the name of the file or the rest of the resource name
     * @return an {@link InputStream} for reading from the file or resource, or <code>null</code> if
     *         access failed
     */
    public static InputStream getFileOrResource(String dir, String name) {
        InputStream in = null;
        try {
            in = new FileInputStream(name);
        } catch (FileNotFoundException ex) {
            URL url = getResource(dir + "/" + name);
            if (url != null) {
                try {
                    in = url.openStream();
                } catch (IOException e) {
                    // So we failed
                }
            }
        }
        return in;
    }

}

// arch-tag: 9edf811b-ed5f-4602-ad6c-b72816f7b2fc
