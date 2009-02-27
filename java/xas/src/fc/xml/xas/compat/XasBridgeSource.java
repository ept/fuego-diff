/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.compat;

import java.io.IOException;
import java.util.Enumeration;

import fc.xml.xas.Comment;
import fc.xml.xas.EndDocument;
import fc.xml.xas.EndTag;
import fc.xml.xas.EntityRef;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.Pi;
import fc.xml.xas.Qname;
import fc.xml.xas.StartDocument;
import fc.xml.xas.StartTag;
import fc.xml.xas.Text;
import fc.xml.xas.typing.TypedItem;
import fuegocore.util.xas.Event;
import fuegocore.util.xas.EventList;
import fuegocore.util.xas.XmlReader;

public class XasBridgeSource implements ItemSource {

    private XmlReader reader;

    public XasBridgeSource (XmlReader reader) {
	this.reader = reader;
    }

    private StartTag convertStartTag (Event ev, EventList pms, EventList atts) {
	StartTag st = new StartTag(new Qname(ev.getNamespace(), ev.getName()));
	if (pms != null) {
	    for (Enumeration e = pms.events(); e.hasMoreElements();) {
		Event pm = (Event) e.nextElement();
		st.addPrefix(pm.getNamespace(), (String) pm.getValue());
	    }
	}
	for (Enumeration e = atts.events(); e.hasMoreElements();) {
	    Event att = (Event) e.nextElement();
	    st.addAttribute(new Qname(att.getNamespace(), att.getName()), att
		.getValue());
	}
	return st;
    }

    public static Item convert (Event ev) {
	switch (ev.getType()) {
	case Event.START_DOCUMENT:
	    return StartDocument.instance();
	case Event.END_DOCUMENT:
	    return EndDocument.instance();
	case Event.START_ELEMENT:
	    return new StartTag(new Qname(ev.getNamespace(), ev.getName()));
	case Event.END_ELEMENT:
	    return new EndTag(new Qname(ev.getNamespace(), ev.getName()));
	case Event.CONTENT:
	    return new Text((String) ev.getValue());
	case Event.TYPED_CONTENT:
	    return new TypedItem(new Qname(ev.getNamespace(), ev.getName()), ev
		.getValue());
	case Event.ENTITY_REFERENCE:
	    return new EntityRef(ev.getName());
	case Event.COMMENT:
	    return new Comment((String) ev.getValue());
	case Event.PROCESSING_INSTRUCTION: {
	    String value = (String) ev.getValue();
	    int index = value.indexOf(' ');
	    if (index > 0) {
		return new Pi(value.substring(0, index), value
		    .substring(index + 1));
	    } else {
		return null;
	    }
	}
	default:
	    return null;
	}
    }

    public Item next () throws IOException {
	Event ev = reader.advance();
	EventList pms = null;
	while (ev.getType() == Event.NAMESPACE_PREFIX) {
	    if (pms == null) {
		pms = new EventList();
	    }
	    pms.add(ev);
	    ev = reader.advance();
	}
	if (ev.getType() == Event.START_ELEMENT) {
	    Event st = ev;
	    EventList atts = new EventList();
	    while ((ev = reader.advance()).getType() == Event.ATTRIBUTE) {
		atts.add(ev);
	    }
	    reader.backup();
	    return convertStartTag(st, pms, atts);
	} else {
	    return convert(ev);
	}
    }

}

// arch-tag: 3cee3f24-4ab5-4778-8e9a-9ce32aea03ce
