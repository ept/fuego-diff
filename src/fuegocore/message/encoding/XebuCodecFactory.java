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

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import fuegocore.util.xas.CodecIndustry;
import fuegocore.util.xas.TypedXmlParser;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.ContentDecoder;
import fuegocore.util.xas.ContentCodecFactory;
import fuegocore.util.xas.XasUtil;

/**
 * A factory for Xebu serializers and parsers.  The serializers and
 * parsers produced by this factory use the Xebu format, the precise
 * features of which are determined by the name of the format.  There
 * are currently six different factories, one for each feature turned
 * on or off.  The features are item caching, content caching, and
 * element caching (content caching implies item caching, so there are
 * not eight factories).
 *
 * <p>The names of the formats and associated features are: <ul>
 * <li><code>application/x-ebu+none</code>: no caching</li>
 * <li><code>application/x-ebu+item</code>: item caching</li>
 * <li><code>application/x-ebu+data</code>: item and content
 * caching</li> <li><code>application/x-ebu+elem</code>: element
 * caching</li> <li><code>application/x-ebu+elit</code>: element and
 * item caching</li> <li><code>application/x-ebu+elid</code>: element,
 * item, and content caching</li> </ul> The features are meaningful
 * only for the serializers; parsers determine the needed caching from
 * each document separately.
 */
public class XebuCodecFactory implements TokenCacheCodecFactory {

    private static final int NONE = 0;
    private static final int ITEM = 1;
    private static final int DATA = 2;
    private static final int ELEM = 3;
    private static final int ELIT = 4;
    private static final int ELID = 5;
    private static final int LAST = 6;
    private static final String[] types
	= { "application/x-ebu+none", "application/x-ebu+item",
	    "application/x-ebu+data", "application/x-ebu+elem",
	    "application/x-ebu+elit", "application/x-ebu+elid" };

    private static XebuCodecFactory[] factories = new XebuCodecFactory[LAST];

    private int type;
    private Hashtable outCaches = new Hashtable();
    private Hashtable inCaches = new Hashtable();
    private Hashtable outSeqCaches = new Hashtable();
    private Hashtable inSeqCaches = new Hashtable();
    private Vector codecFactories = null;

    static {
	for (int i = 0; i < factories.length; i++) {
	    factories[i] = new XebuCodecFactory(i);
	}
    }

    private XebuCodecFactory (int type) {
	CodecIndustry.registerFactory(types[type], this);
	this.type = type;
    }

    private boolean cacheItem (int type) {
	return type != NONE && type != ELEM;
    }

    private boolean cacheContent (int type) {
	return type == DATA || type == ELID;
    }

    private boolean cacheSequence (int type) {
	return type == ELEM || type == ELIT || type == ELID;
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
	XebuParser result;
	Object[][] caches = (Object[][]) inCaches.get(token);
	Vector sequenceCache = null;
	if (cacheSequence(type)) {
	    sequenceCache = (Vector) inSeqCaches.get(token);
	}
	if (caches == null && sequenceCache == null) {
	    result = new XebuParser();
	} else if (caches == null) {
	    result = new XebuParser(sequenceCache);
	} else if (sequenceCache == null) {
	    result = new XebuParser(caches);
	} else {
	    result = new XebuParser(caches, sequenceCache);
	}
	if (caches == null && cacheItem(type)) {
	    inCaches.put(token, result.getProperty(XebuConstants.PROPERTY_INITIAL_CACHES));
	}
	if (sequenceCache == null && cacheSequence(type)) {
	    inSeqCaches.put(token, result.getProperty(XebuConstants.PROPERTY_INITIAL_SEQUENCE_CACHE));
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
	OutCache[] caches = (OutCache[]) outCaches.get(token);
	SequenceCache sequenceCache = null;
	if (cacheSequence(type)) {
	    sequenceCache = (SequenceCache) outSeqCaches.get(token);
	}
	if (caches == null && sequenceCache == null) {
	    result = new XebuSerializer();
	} else if (caches == null) {
	    result = new XebuSerializer(sequenceCache);
	} else if (sequenceCache == null) {
	    result = new XebuSerializer(caches);
	} else {
	    result = new XebuSerializer(caches, sequenceCache);
	}
	if (caches == null && cacheItem(type)) {
	    outCaches.put(token, result.getProperty(XebuConstants.PROPERTY_INITIAL_CACHES));
	}
	if (sequenceCache == null && cacheSequence(type)) {
	    outSeqCaches.put(token, result.getProperty(XebuConstants.PROPERTY_INITIAL_SEQUENCE_CACHE));
	}
	result.setFeature(XebuConstants.FEATURE_ITEM_CACHING, cacheItem(type));
	result.setFeature(XebuConstants.FEATURE_CONTENT_CACHING,
			  cacheContent(type));
	result.setFeature(XebuConstants.FEATURE_SEQUENCE_CACHING,
			  cacheSequence(type));
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
	outSeqCaches.remove(token);
    }

    public void resetInState (Object token) {
	inCaches.remove(token);
	inSeqCaches.remove(token);
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

    /**
     * Set the initial output sequence cache for a specified token.
     *
     * @param token a token identifying the cache to set
     * @param cache the initial cache to use
     * @return <code>true</code> if the cache of <code>token</code>
     * was set to <code>cache</code>, <code>false</code> otherwise
     */
    public boolean setInitialOutSeqCache (Object token, SequenceCache cache) {
	boolean result = false;
	if (cache != null && !outSeqCaches.containsKey(token)) {
	    outSeqCaches.put(token, cache);
	    result = true;
	}
	return result;
    }

    /**
     * Set the initial input sequence cache for a specified token.
     *
     * @param token a token identifying the cache to set
     * @param cache the initial cache to use
     * @return <code>true</code> if the cache of <code>token</code>
     * was set to <code>cache</code>, <code>false</code> otherwise
     */
    public boolean setInitialInSeqCache (Object token, Vector cache) {
	boolean result = false;
	if (cache != null && !inSeqCaches.containsKey(token)) {
	    inSeqCaches.put(token, cache);
	    result = true;
	}
	return result;
    }

    public void installContentFactories (Vector list) {
	codecFactories = list;
    }

}
