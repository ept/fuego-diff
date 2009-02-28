/*
 * Copyright 2006 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fcme.util.xas;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import fuegocore.util.xas.CodecFactory;
import fuegocore.util.xas.ContentCodecFactory;

import fcme.util.xas.XmlCodecFactory;
import fcme.util.xas.codec.DefaultCodecFactory;
import fcme.message.encoding.XebuCodecFactory;

/**
 * A central repository for {@link CodecFactory} objects.  An
 * application may wish to use several different encodings for XML due
 * to different characteristics of mediums (e.g. a binary format in
 * low-bandwidth networks, normal XML for interoperability).  This
 * class provides for registering serializer/parser factories for each
 * type beforehand and then later fetching a factory for a specific
 * type.
 *
 * <p>By default the {@link XmlCodecFactory} is registered at load
 * time.  Other factories that wish to register themselves need to
 * invoke the {@link #registerFactory} method at some point,
 * preferable as early as possible.
 */
public class CodecIndustry {

    private static Hashtable factories = new Hashtable();
    private static Hashtable existFactories = new Hashtable();
    private static Vector codecFactories = new Vector();

    /*
     * A private constructor to prevent instantiation.
     */
    private CodecIndustry () {
    }

    static {
	registerContentFactory(new DefaultCodecFactory());
	XmlCodecFactory.init();
	XebuCodecFactory.init();
    }

    /**
     * Register a factory for a specified type.  This method will
     * cause the given factory to be returned for future calls of
     * {@link #getFactory} with the same type.  Typically the given
     * type is a string representation of a MIME type,
     * e.g. <code>"text/xml"</code>.  Registering a <code>null</code>
     * factory has the effect of deregistering factories for the given
     * type.  The same factory object may be registered for multiple
     * types.
     *
     * @param type the type to register a factory for
     * @param factory the factory to register
     *
     * @see #getFactory
     */
    public static void registerFactory (String type, CodecFactory factory) {
	if (factory == null) {
	    factories.remove(type);
	} else {
	    factories.put(type, factory);
	    factory.installContentFactories(codecFactories);
	}
    }

    /**
     * Return a factory associated with a specified type.  This method
     * will return a factory previously registered with {@link
     * #registerFactory} for the specified type.
     *
     * @param type the type whose associated factory to return
     * @return the factory associated with <code>type</code>, or
     * <code>null</code> if no factory has been registered
     *
     * @see #registerFactory
     */
    public static CodecFactory getFactory (String type) {
	return (CodecFactory) factories.get(type);
    }

    /**
     * Register a factory for typed content codecs.  This will
     * register globally a {@link ContentCodecFactory} that will be
     * used to add {@link fuegocore.util.xas.ContentEncoder} and
     * {@link fuegocore.util.xas.ContentDecoder} objects to {@link
     * fuegocore.util.xas.TypedXmlSerializer} and {@link
     * fuegocore.util.xas.TypedXmlParser} objects later generated
     * through any registered {@link CodecFactory}.
     *
     * <p>The factory given to this method will always override any
     * previously registered factories for the types that it knows
     * about.
     *
     * <p>The registration does not affect already-existing {@link
     * fuegocore.util.xas.TypedXmlSerializer} and {@link
     * fuegocore.util.xas.TypedXmlParser} objects, so it is
     * recommended that applications register all their {@link
     * ContentCodecFactory} objects as close to startup as possible.
     *
     * @param factory the factory to register
     */
    public static void registerContentFactory (ContentCodecFactory factory) {
	//System.out.println("Registering content factory " + factory);
	if (!existFactories.containsKey(factory.getClass())) {
	    existFactories.put(factory.getClass(), factories);
	    codecFactories.addElement(factory);
	    factory.register();
	}
    }

    /**
     * Return all available types.  This method will return an
     * enumeration of the {@link String} objects for all types
     * registered with {@link #registerFactory}.  If a type is
     * deregistered by passing a <code>null</code> factory to {@link
     * #registerFactory}, the returned set will not contain that type.
     *
     * @return an enumeration for {@link String} objects containing
     * the types for which {@link #getFactory} will return a
     * non-<code>null</code> object
     */
    public static Enumeration availableTypes () {
	return factories.keys();
    }

}
