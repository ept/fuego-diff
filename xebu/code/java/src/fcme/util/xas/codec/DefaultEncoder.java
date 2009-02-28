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

package fcme.util.xas.codec;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;

import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.ChainedContentEncoder;
import fuegocore.util.xas.ContentCodecFactory;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.EventSequence;
import fuegocore.util.xas.XmlWriter;
import fuegocore.util.xas.Qname;
import fuegocore.util.xas.XasUtil;
import fuegocore.util.Util;

/**
 * A default typed content encoder for FCME.  This encoder collects
 * into one class the encoding of all additional types that are
 * recognized by default on FCME.
 */
public class DefaultEncoder extends ChainedContentEncoder {

    public DefaultEncoder (ContentEncoder chain) {
	this.chain = chain;
    }


    public boolean encode (Object o, String namespace, String name,
			   TypedXmlSerializer ser)
	throws IOException {
	boolean result = false;
	if (Util.equals(namespace, XasUtil.XAS_NAMESPACE)) {
	    if (Util.equals(name, "XmlEventSequence")) {
		if (o instanceof EventSequence) {
		    //System.out.println("Encoding event sequence " + o);
		    putTypeAttribute(namespace, name, ser);
		    EventSequence e = (EventSequence) o;
		    XasUtil.outputSequence(e, ser);
		    result = true;
		}
	    } else if (Util.equals(name, "vector")) {
		if (o instanceof Vector) {
		    putTypeAttribute(namespace, name, ser);
		    Vector v = (Vector) o;
		    XmlWriter xw = new XmlWriter(ser);
		    for (Enumeration e = v.elements(); e.hasMoreElements(); ) {
			Object i = e.nextElement();
			Qname qname =
			    ContentCodecFactory.getXmlName(i.getClass());
			if (qname != null) {
			    xw.typedElement(XasUtil.XAS_NAMESPACE, "item",
					    qname.getNamespace(),
					    qname.getName(), i);
			} else {
			    throw new IOException("Unknown type of object "
						  + i);
			}
		    }
		    result = true;
		}
	    } else if (Util.equals(name, "hashtable")) {
		if (o instanceof Hashtable) {
		    putTypeAttribute(namespace, name, ser);
		    Hashtable h = (Hashtable) o;
		    XmlWriter xw = new XmlWriter(ser);
		    for (Enumeration e = h.keys(); e.hasMoreElements(); ) {
			Object k = e.nextElement();
			Object v = h.get(k);
			Qname kname =
			    ContentCodecFactory.getXmlName(k.getClass());
			if (kname != null) {
			    xw.typedElement(XasUtil.XAS_NAMESPACE, "key",
					    kname.getNamespace(),
					    kname.getName(), k);
			} else {
			    throw new IOException("Unknown type of object "
						  + k);
			}
			Qname vname =
			    ContentCodecFactory.getXmlName(v.getClass());
			if (vname != null) {
			    xw.typedElement(XasUtil.XAS_NAMESPACE, "value",
					    vname.getNamespace(),
					    vname.getName(), v);
			} else {
			    throw new IOException("Unknown type of object "
						  + v);
			}
		    }
		    result = true;
		}
	    }
	}
	if (!result && chain != null) {
	    result = chain.encode(o, namespace, name, ser);
	}
	return result;
    }

}
