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

package fuegocore.message.encoding;

import fuegocore.util.xas.Event;

/**
 * A class for associating an output and an input token cache.  An
 * object of this class holds a pair consisting of a {@link
 * OutTokenCache} array and a {@link Object} array of arrays.  These
 * are suitable as arguments to setting initial caches for Xebu
 * encoding (e.g. through methods in a {@link TokenCacheCodecFactory}
 * object).  The class is provided so that the association between
 * complementary caches is easily preservable in systems.
 */
public class CachePair {

    private OutTokenCache[] out;
    private Object[][] in;

    /**
     * Insert a namespace into in and out caches.  This method is
     * intended for use by methods generating <code>CachePair</code>
     * objects.  It inserts the supplied namespace into both supplied
     * caches in such a way that the caches are consistent.
     *
     * @param outCaches the output caches to insert into
     * @param inCaches the input caches to insert into
     * @param namespace the namespace to insert
     */
    public static void putNamespace (OutTokenCache[] outCaches,
				     Object[][] inCaches, String namespace) {
	int index = outCaches[XebuConstants.NAMESPACE_INDEX].insert(namespace);
	inCaches[XebuConstants.NAMESPACE_INDEX][index] = namespace;
    }

    /**
     * Insert a name into in and out caches.  This method is intended
     * for use by methods generating <code>CachePair</code> objects.
     * It inserts the supplied name into both supplied caches in such
     * a way that the caches are consistent.
     *
     * @param outCaches the output caches to insert into
     * @param inCaches the input caches to insert into
     * @param namespace the namespace URI of the name to insert
     * @param name the local name of the name to insert
     */
    public static void putName (OutTokenCache[] outCaches, Object[][] inCaches,
				String namespace, String name) {
	Event ev = Event.createStartElement(namespace, name);
	int index = outCaches[XebuConstants.NAME_INDEX].insert(ev);
	inCaches[XebuConstants.NAME_INDEX][index] = ev;
    }

    /**
     * Insert a value into in and out caches.  This method is intended
     * for use by methods generating <code>CachePair</code> objects.
     * It inserts the supplied value into both supplied caches in such
     * a way that the caches are consistent.
     *
     * @param outCaches the output caches to insert into
     * @param inCaches the input caches to insert into
     * @param namespace the namespace URI of the value to insert
     * @param name the local name of the value to insert
     * @param value the actual value of the value to insert
     */
    public static void putValue (OutTokenCache[] outCaches,
				 Object[][] inCaches, String namespace,
				 String name, Object value) {
	Event ev = Event.createTypedContent(namespace, name, value);
	int index = outCaches[XebuConstants.VALUE_INDEX].insert(ev);
	inCaches[XebuConstants.VALUE_INDEX][index] = ev;
    }

    /**
     * Insert content into in and out caches.  This method is intended
     * for use by methods generating <code>CachePair</code> objects.
     * It inserts the supplied content into both supplied caches in
     * such a way that the caches are consistent.
     *
     * @param outCaches the output caches to insert into
     * @param inCaches the input caches to insert into
     * @param content the content to insert
     */
    public static void putContent (OutTokenCache[] outCaches,
				   Object[][] inCaches, String content) {
	int index = outCaches[XebuConstants.CONTENT_INDEX].insert(content);
	inCaches[XebuConstants.CONTENT_INDEX][index] = content;
    }

    /**
     * Construct a new token cache pair.
     */
    public CachePair (OutTokenCache[] out, Object[][] in) {
	this.out = out;
	this.in = in;
    }

    /**
     * Return the output token cache part of this pair.
     */
    public OutTokenCache[] getOutCache () {
	return out;
    }

    /**
     * Return the input token cache part of this pair.
     */
    public Object[][] getInCache () {
	return in;
    }

}
