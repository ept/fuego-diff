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
import java.util.Enumeration;

import fuegocore.util.xas.CodecFactory;
import fuegocore.util.xas.TypedXmlParser;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.DefaultXmlParser;
import fuegocore.util.xas.DefaultXmlSerializer;
import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.ContentDecoder;
import fuegocore.util.xas.ContentCodecFactory;
import fuegocore.util.xas.XasUtil;

import org.xmlpull.v1.XmlPullParserException;

/**
 * A factory for textual XML serializers and parsers.  The serializers and
 * parsers produced by this factory use the standard textual format
 * of XML.  This is a Singleton object.
 */
public class XmlCodecFactory implements CodecFactory {

    private static XmlCodecFactory factory = null;
    private Vector factories = null;

    private XmlCodecFactory () {
	CodecIndustry.registerFactory("text/xml", this);
	CodecIndustry.registerFactory("application/soap+xml", this);
    }

    /**
     * Initialize and register XML codec factories.  Calling this
     * method will create an XML codec factory for the types
     * <code>"text/xml"</code> and <code>"application/soap+xml"</code>
     * and register the factory with the {@link CodecIndustry}.
     */
    public static void init () {
	if (factory == null) {
	    factory = new XmlCodecFactory();
	}
    }

    /**
     * Return the XML codec factory.  This method just returns the
     * Singleton object that is registered as the factory for XML
     * types.
     */
    public static XmlCodecFactory getInstance () {
	return factory;
    }

    /**
     * Return a new XML parser.  The returned parser reads textual
     * XML.  The token is ignored; a new parser, unrelated to any
     * previous parsers, is always created.
     */
    public TypedXmlParser getNewDecoder (Object token) {
	TypedXmlParser result = new DefaultXmlParser();
	if (factories != null && factories.size() > 0) {
	    ContentDecoder dec = (ContentDecoder)
		result.getProperty(XasUtil.PROPERTY_CONTENT_CODEC);
	    for (Enumeration e = factories.elements(); e.hasMoreElements(); ) {
		ContentCodecFactory fac = (ContentCodecFactory)
		    e.nextElement();
		dec = fac.getChainedDecoder(dec);
	    }
	    try {
		result.setProperty(XasUtil.PROPERTY_CONTENT_CODEC, dec);
	    } catch (XmlPullParserException ex) {
		/*
		 * If we got the property, we should be able to set it
		 * too.
		 */
	    }
	}
	return result;
    }

    /**
     * Return a new XML serializer.  The returned serializer writes textual
     * XML.  The token is ignored; a new serializer, unrelated to any
     * previous serializers, is always created.
     */
    public TypedXmlSerializer getNewEncoder (Object token) {
	TypedXmlSerializer result = new DefaultXmlSerializer();
	if (factories != null && factories.size() > 0) {
	    ContentEncoder enc = (ContentEncoder)
		result.getProperty(XasUtil.PROPERTY_CONTENT_CODEC);
	    for (Enumeration e = factories.elements(); e.hasMoreElements(); ) {
		ContentCodecFactory fac = (ContentCodecFactory)
		    e.nextElement();
		enc = fac.getChainedEncoder(enc);
	    }
	    result.setProperty(XasUtil.PROPERTY_CONTENT_CODEC, enc);
	}
	return result;
    }

    public void resetOutState (Object token) {
    }

    public void resetInState (Object token) {
    }

    public void installContentFactories (Vector list) {
	factories = list;
    }

}
