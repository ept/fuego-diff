/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

import fuegocore.util.xas.codec.DefaultCodecFactory;
import fuegocore.util.ExtUtil;

/**
 * A central repository for {@link CodecFactory} objects. An application may wish to use several
 * different encodings for XML due to different characteristics of mediums (e.g. a binary format in
 * low-bandwidth networks, normal XML for interoperability). This class provides for registering
 * serializer/parser factories for each type beforehand and then later fetching a factory for a
 * specific type.
 * <p>
 * By default the {@link XmlCodecFactory} is registered at load time. Other factories that wish to
 * register themselves need to have their class names inserted into a file whose name is specified
 * with the <code>xas.factories</code> system property (<code>"xas.factories"</code> by default).
 * They also need to arrange for the registration to happen at class loading time; the code of
 * {@link XmlCodecFactory} can be used as a model of the recommended way.
 */
public class CodecIndustry {

    private static Map factories = new HashMap();
    private static Set existFactories = new HashSet();
    private static Vector codecFactories = new Vector();


    /*
     * A private constructor to prevent instantiation.
     */
    private CodecIndustry() {
    }

    static {
        registerContentFactory(new DefaultCodecFactory());
        try {
            Class.forName("fuegocore.util.xas.XmlCodecFactory");
        } catch (ClassNotFoundException e) {
            // We know this succeeds
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(
                                                   new InputStreamReader(
                                                                         ExtUtil.getFileOrResource(
                                                                                                   "util",
                                                                                                   "xas.factories")));
        if (reader != null) {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        Class.forName(line);
                    } catch (ClassNotFoundException e) {
                        // No worries, mate
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                // Don't do a thing; we don't care
                e.printStackTrace();
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Failure to close
                }
            }
        }
    }


    /**
     * Register a factory for a specified type. This method will cause the given factory to be
     * returned for future calls of {@link #getFactory} with the same type. Typically the given type
     * is a string representation of a MIME type, e.g. <code>"text/xml"</code>. Registering a
     * <code>null</code> factory has the effect of deregistering factories for the given type. The
     * same factory object may be registered for multiple types.
     * @param type
     *            the type to register a factory for
     * @param factory
     *            the factory to register
     * @see #getFactory
     */
    public static void registerFactory(String type, CodecFactory factory) {
        if (factory == null) {
            factories.remove(type);
        } else {
            factories.put(type, factory);
            factory.installContentFactories(codecFactories);
        }
    }


    /**
     * Return a factory associated with a specified type. This method will return a factory
     * previously registered with {@link #registerFactory} for the specified type.
     * @param type
     *            the type whose associated factory to return
     * @return the factory associated with <code>type</code>, or <code>null</code> if no factory has
     *         been registered
     * @see #registerFactory
     */
    public static CodecFactory getFactory(String type) {
        return (CodecFactory) factories.get(type);
    }


    /**
     * Register a factory for typed content codecs. This will register globally a
     * {@link ContentCodecFactory} that will be used to add {@link ContentEncoder} and
     * {@link ContentDecoder} objects to {@link TypedXmlSerializer} and {@link TypedXmlParser}
     * objects later generated through any registered {@link CodecFactory}.
     * <p>
     * The factory given to this method will always override any previously registered factories for
     * the types that it knows about.
     * <p>
     * The registration does not affect already-existing {@link TypedXmlSerializer} and
     * {@link TypedXmlParser} objects, so it is recommended that applications register all their
     * {@link ContentCodecFactory} objects as close to startup as possible.
     * @param factory
     *            the factory to register
     */
    public static void registerContentFactory(ContentCodecFactory factory) {
        if (!existFactories.contains(factory.getClass())) {
            existFactories.add(factory.getClass());
            codecFactories.add(factory);
            factory.register();
        }
    }


    /**
     * Return all available types. This method will return a set consisting of {@link String}
     * objects for all types registered with {@link #registerFactory}. If a type is deregistered by
     * passing a <code>null</code> factory to {@link #registerFactory}, the returned set will not
     * contain that type.
     * @return a set of {@link String} objects containing the types for which {@link #getFactory}
     *         will return a non-<code>null</code> object
     */
    public static Set availableTypes() {
        return factories.keySet();
    }

}
