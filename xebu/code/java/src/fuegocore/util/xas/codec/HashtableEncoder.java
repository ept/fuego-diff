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

package fuegocore.util.xas.codec;

import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;

import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.ChainedContentEncoder;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.ContentCodecFactory;
import fuegocore.util.xas.XmlWriter;
import fuegocore.util.xas.Qname;
import fuegocore.util.xas.XasUtil;
import fuegocore.util.Util;

/**
 * A typed content encoder for the {@link Hashtable} type.  This class
 * implements the {@link ContentEncoder} interface for encoding {@link
 * Hashtable} objects into XML.
 */
public class HashtableEncoder extends ChainedContentEncoder {

    public HashtableEncoder (ContentEncoder chain) {
	this.chain = chain;
    }

    public boolean encode (Object o, String namespace, String name,
			   TypedXmlSerializer ser)
	throws IOException {
	boolean result = false;
	if (Util.equals(namespace, XasUtil.XAS_NAMESPACE)
	    && Util.equals(name, "hashtable")) {
	    if (o instanceof Hashtable) {
		putTypeAttribute(namespace, name, ser);
		Hashtable h = (Hashtable) o;
		XmlWriter xw = new XmlWriter(ser);
		for (Enumeration e = h.keys(); e.hasMoreElements(); ) {
		    Object k = e.nextElement();
		    Object v = h.get(k);
		    Qname kname = ContentCodecFactory.getXmlName(k.getClass());
		    if (kname != null) {
			xw.typedElement(XasUtil.XAS_NAMESPACE, "key",
					kname.getNamespace(), kname.getName(),
					k);
		    } else {
			throw new IOException("Unknown type of object " + k);
		    }
		    Qname vname = ContentCodecFactory.getXmlName(v.getClass());
		    if (vname != null) {
			xw.typedElement(XasUtil.XAS_NAMESPACE, "value",
					vname.getNamespace(), vname.getName(),
					v);
		    } else {
			throw new IOException("Unknown type of object " + v);
		    }
		}
		result = true;
	    }
	} else if (chain != null) {
	    result = chain.encode(o, namespace, name, ser);
	}
	return result;
    }

}
