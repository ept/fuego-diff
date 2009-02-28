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

import fuegocore.util.xas.ContentDecoder;
import fuegocore.util.xas.ChainedContentDecoder;
import fuegocore.util.xas.Event;
import fuegocore.util.xas.EventList;
import fuegocore.util.xas.XmlReader;
import fuegocore.util.xas.XasUtil;
import fuegocore.util.Util;

/**
 * A default typed content decoder for FCME.  This decoder collects
 * into one class the decoding of all additional types that are
 * recognized by default on FCME.
 */
public class DefaultDecoder extends ChainedContentDecoder {

    public DefaultDecoder (ContentDecoder chain) {
	super(null);
	if (chain == null) {
	    throw new IllegalArgumentException("Chained decoder must be"
					       + " non-null");
	}
	this.chain = chain;
    }

    public Object decode (String namespace, String name, XmlReader reader,
			  EventList attributes) {
	Object result = null;
	if (Util.equals(namespace, XasUtil.XAS_NAMESPACE)) {
	    if (Util.equals(name, "XmlEventSequence")) {
		//System.out.println("Decoding event sequence");
		int pos = reader.getCurrentPosition();
		EventList e = new EventList();
		int depth = 0;
		Event ev = reader.advance();
		while (ev != null && (depth > 0
				      || ev.getType() != Event.END_ELEMENT)) {
		    //System.out.println("Adding event " + ev);
		    e.add(ev);
		    if (ev.getType() == Event.START_ELEMENT) {
			depth += 1;
		    } else if (ev.getType() == Event.END_ELEMENT) {
			depth -= 1;
		    }
		    ev = reader.advance();
		}
		if (ev != null && depth == 0
		    && ev.getType() == Event.END_ELEMENT) {
		    reader.backup();
		    result = e;
		} else {
		    reader.setCurrentPosition(pos);
		}
	    } else if (Util.equals(name, "vector")) {
		Vector v = new Vector();
		Object item;
		while ((item = expect(XasUtil.XAS_NAMESPACE, "item", reader))
		       != null) {
		    v.addElement(item);
		}
		result = v;
	    } else if (Util.equals(name, "hashtable")) {
		Hashtable h = new Hashtable();
		Object key;
		while ((key = expect(XasUtil.XAS_NAMESPACE, "key", reader))
		       != null) {
		    Object value = expect(XasUtil.XAS_NAMESPACE, "value",
					  reader);
		    if (value != null) {
			h.put(key, value);
		    }
		}
		result = h;
	    }
	}
	if (result == null && chain != null) {
	    result = chain.decode(namespace, name, reader, attributes);
	}
	return result;
    }

}
