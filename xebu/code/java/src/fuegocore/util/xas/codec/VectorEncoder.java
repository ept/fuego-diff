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

import java.util.Vector;
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
 * A typed content encoder for the {@link Vector} type.  This class
 * implements the {@link ContentEncoder} interface for encoding {@link
 * Vector} objects into XML.
 */
public class VectorEncoder extends ChainedContentEncoder {

    public VectorEncoder (ContentEncoder chain) {
	this.chain = chain;
    }

    public boolean encode (Object o, String namespace, String name,
			   TypedXmlSerializer ser)
	throws IOException {
	boolean result = false;
	if (Util.equals(namespace, XasUtil.XAS_NAMESPACE)
	    && Util.equals(name, "vector")) {
	    if (o instanceof Vector) {
		putTypeAttribute(namespace, name, ser);
		Vector v = (Vector) o;
		XmlWriter xw = new XmlWriter(ser);
		for (Enumeration e = v.elements(); e.hasMoreElements(); ) {
		    Object i = e.nextElement();
		    Qname qname = ContentCodecFactory.getXmlName(i.getClass());
		    if (qname != null) {
			xw.typedElement(XasUtil.XAS_NAMESPACE, "item",
					qname.getNamespace(), qname.getName(),
					i);
		    } else {
			throw new IOException("Unknown type of object " + i);
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
