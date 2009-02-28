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

package fuegocore.message.encoding.coa;

import fuegocore.message.encoding.OutCache;
import fuegocore.message.encoding.CachePair;
import fuegocore.message.encoding.XebuConstants;
import fuegocore.util.xas.Event;
import fuegocore.util.xas.XasUtil;

/**
 * A class for creating token mappings for SOAP messages.  This class
 * provides class methods for creating {@link
 * fuegocore.message.encoding.CachePair} objects to be used as the
 * default token mappings for Xebu.  These mappings consist of tokens
 * specific to SOAP messages.
 */
public class SoapCachePair {

    /*
     * Private constructor to prevent instantiation.
     */
    private SoapCachePair () {
    }

    private static CachePair createPair () {
	OutCache[] outCaches = new OutCache[XebuConstants.INDEX_NUMBER];
	Object[][] inCaches =
	    new Object[XebuConstants.INDEX_NUMBER][XebuConstants.CACHE_SIZE];
	for (int i = 0; i < XebuConstants.INDEX_NUMBER; i++) {
	    outCaches[i] = new OutCache();
	}
	CachePair.putNamespace(outCaches, inCaches,
			       XasUtil.SOAP_NAMESPACE);
	CachePair.putNamespace(outCaches, inCaches, XasUtil.XSD_NAMESPACE);
	CachePair.putNamespace(outCaches, inCaches, XasUtil.XSI_NAMESPACE);
	CachePair.putName(outCaches, inCaches, XasUtil.SOAP_NAMESPACE,
			  "soapenv");
	CachePair.putName(outCaches, inCaches, XasUtil.XSD_NAMESPACE, "xsd");
	CachePair.putName(outCaches, inCaches, XasUtil.XSI_NAMESPACE, "xsi");
	CachePair.putName(outCaches, inCaches, XasUtil.SOAP_NAMESPACE,
			  "Envelope");
	CachePair.putName(outCaches, inCaches, XasUtil.SOAP_NAMESPACE,
			  "Header");
	CachePair.putName(outCaches, inCaches, XasUtil.SOAP_NAMESPACE,
			  "Body");
	CachePair.putName(outCaches, inCaches, XasUtil.XSI_NAMESPACE, "type");
	CachePair.putName(outCaches, inCaches, XasUtil.SOAP_NAMESPACE,
			  "mustUnderstand");
	CachePair.putName(outCaches, inCaches, XasUtil.SOAP_NAMESPACE,
			  "encodingStyle");
	CachePair.putValue(outCaches, inCaches, XasUtil.SOAP_NAMESPACE,
			   "encodingStyle", XasUtil.SOAP_ENCODING);
	return new CachePair(outCaches, inCaches);
    }

    /**
     * Create a new mapping.  This method creates a new {@link
     * CachePair} object containing mappings as described in the class
     * documentation.
     */
    public static CachePair getNewPair () {
	return createPair();
    }

}
