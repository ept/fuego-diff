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

package fcme.message.encoding;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import fuegocore.message.encoding.XebuConstants;
import fuegocore.message.encoding.OutTokenCache;
import fuegocore.message.encoding.TokenCacheCodecFactory;
import fuegocore.util.xas.TypedXmlParser;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.ContentDecoder;
import fuegocore.util.xas.ContentCodecFactory;
import fuegocore.util.xas.XasUtil;
import fcme.util.xas.CodecIndustry;

/**
 * A factory for Xebu serializers and parsers.  The serializers and
 * parsers produced by this factory use the Xebu format, the precise
 * features of which are determined by the name of the format.  There
 * are currently three different factories, one for each feature
 * turned on or off.  The features are item caching and content
 * caching (content caching implies item caching, so there are not
 * four factories).
 *
 * <p>The names of the formats and associated features are: <ul>
 * <li><code>application/x-ebu+none</code>: no caching</li>
 * <li><code>application/x-ebu+item</code>: item caching</li>
 * <li><code>application/x-ebu+data</code>: item and content
 * caching</li> </ul> The features are meaningful only for the
 * serializers; parsers determine the needed caching from each document
 * separately.
 */
public class XebuCodecFactory implements TokenCacheCodecFactory {

    private static final int NONE = 0;
    private static final int ITEM = 1;
    private static final int DATA = 2;
    private static final int LAST = 3;

    private static final String[] types
	= { "application/x-ebu+none", "application/x-ebu+item",
	    "application/x-ebu+data" };

    private static XebuCodecFactory[] factories = new XebuCodecFactory[LAST];
    private static boolean isReady = false;

    private int type;
    private Hashtable outCaches = new Hashtable();
    private Hashtable inCaches = new Hashtable();
    private Vector codecFactories = null;

    private XebuCodecFactory (int type) {
	CodecIndustry.registerFactory(types[type], this);
	this.type = type;
    }

    private boolean cacheItem (int type) {
	return type != NONE;
    }

    private boolean cacheContent (int type) {
	return type == DATA;
    }

    /**
     * Initialize and register Xebu codec factories.  Calling this
     * method will create Xebu codec factories for each of the
     * supported Xebu types and register the factories with the {@link
     * CodecIndustry}.
     */
    public static void init () {
	if (!isReady) {
	    for (int i = 0; i < factories.length; i++) {
		factories[i] = new XebuCodecFactory(i);
	    }
	    isReady = true;
	}
    }

    /**
     * Return a new Xebu parser.  The parser is able to parse any
     * Xebu document independently of whether the creating factory has
     * caching features set.  The argument token is used to associate
     * previously-used caches with new parsers.
     *
     * @param token a token identifying the caches to use
     */
    public TypedXmlParser getNewDecoder (Object token) {
	//System.out.println("Returning new decoder of type " + type
	//+ " for token " + token);
	XebuParser result;
	if (cacheItem(type)) {
	    //System.out.println("Item caching requested");
	    Object caches = inCaches.get(token);
	    //System.out.println("Got caches " + caches);
	    if (caches != null) {
		result = new XebuParser((Object[][]) caches);
	    } else {
		result = new XebuParser();
		caches =
		    result.getProperty(XebuConstants.PROPERTY_INITIAL_CACHES);
		//System.out.println("Entering caches " + caches);
		inCaches.put(token, caches);
	    }
	} else {
	    result = new XebuParser();
	}
	if (codecFactories != null && codecFactories.size() > 0) {
	    ContentDecoder dec = (ContentDecoder)
		result.getProperty(XasUtil.PROPERTY_CONTENT_CODEC);
	    for (Enumeration e = codecFactories.elements();
		 e.hasMoreElements(); ) {
		ContentCodecFactory fac = (ContentCodecFactory)
		    e.nextElement();
		dec = fac.getChainedDecoder(dec);
	    }
	    result.setProperty(XasUtil.PROPERTY_CONTENT_CODEC, dec);
	}
	return result;
    }

    /**
     * Return a new Xebu serializer.  The returned serializer will use
     * caches according to the features of the creating factory and
     * write the corresponding Xebu format.  The argument token is
     * used to associate previously-used caches with new serializers.
     *
     * @param token a token identifying the caches to use
     */
    public TypedXmlSerializer getNewEncoder (Object token) {
	XebuSerializer result;
	if (cacheItem(type)) {
	    Object caches = outCaches.get(token);
	    if (caches != null) {
		result = new XebuSerializer((OutCache[]) caches);
	    } else {
		result = new XebuSerializer();
		outCaches.put(token, result.getProperty(XebuConstants.PROPERTY_INITIAL_CACHES));
	    }
	    result.setFeature(XebuConstants.FEATURE_ITEM_CACHING, true);
	    if (cacheContent(type)) {
		result.setFeature(XebuConstants.FEATURE_CONTENT_CACHING, true);
	    } else {
		result.setFeature(XebuConstants.FEATURE_CONTENT_CACHING,
				  false);
	    }
	} else {
	    result = new XebuSerializer();
	    result.setFeature(XebuConstants.FEATURE_ITEM_CACHING, false);
	}
	if (codecFactories != null && codecFactories.size() > 0) {
	    ContentEncoder enc = (ContentEncoder)
		result.getProperty(XasUtil.PROPERTY_CONTENT_CODEC);
	    for (Enumeration e = codecFactories.elements();
		 e.hasMoreElements(); ) {
		ContentCodecFactory fac = (ContentCodecFactory)
		    e.nextElement();
		enc = fac.getChainedEncoder(enc);
	    }
	    result.setProperty(XasUtil.PROPERTY_CONTENT_CODEC, enc);
	}
	return result;
    }

    public void resetOutState (Object token) {
	outCaches.remove(token);
    }

    public void resetInState (Object token) {
	inCaches.remove(token);
    }

    public boolean setInitialOutCache (Object token, OutTokenCache[] caches) {
	boolean result = false;
	if (caches != null && !outCaches.containsKey(token)) {
	    outCaches.put(token, caches);
	    result = true;
	}
	return result;
    }

    public boolean setInitialInCache (Object token, Object[][] caches) {
	boolean result = false;
	if (caches != null && !inCaches.containsKey(token)) {
	    inCaches.put(token, caches);
	    result = true;
	}
	return result;
    }

    public void installContentFactories (Vector list) {
	codecFactories = list;
    }

}
