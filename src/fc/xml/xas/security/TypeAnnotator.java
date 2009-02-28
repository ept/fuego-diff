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

package fc.xml.xas.security;

import java.util.HashMap;
import java.util.Map;

import fc.xml.xas.IdentityTransform;
import fc.xml.xas.Item;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.XasUtil;
import fc.xml.xas.typing.ParsedPrimitive;

public class TypeAnnotator extends IdentityTransform {

    private static Map<Qname,ParsedPrimitive> types =
	new HashMap<Qname,ParsedPrimitive>();

    static {
	Qname name = new Qname(XasUtil.XSD_NS, "QName");
	ParsedPrimitive value =
	    new ParsedPrimitive(name, XasUtil.BASE64_BINARY_TYPE);
	types.put(SecUtil.DS_SIGVALUE_NAME, value);
	types.put(SecUtil.DS_DIGEST_VALUE_NAME, value);
	types.put(SecUtil.XENC_CVALUE_NAME, value);
    }

    public void append (Item item) {
	if (Item.isStartTag(item)) {
	    StartTag tag = (StartTag) item;
	    ParsedPrimitive type = types.get(tag.getName());
	    if (type != null) {
		tag.setAttribute(XasUtil.XSI_TYPE, type);
	    }
	}
	queue.offer(item);
    }

}

// arch-tag: 1b6ba603-0a89-40df-af58-b0493075aee3
