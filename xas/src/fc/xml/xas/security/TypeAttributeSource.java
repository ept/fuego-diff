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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import fc.xml.xas.Item;
import fc.xml.xas.ParserSource;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.XasUtil;
import fc.xml.xas.typing.ParsedPrimitive;
import fc.xml.xas.typing.TypingUtil;

public class TypeAttributeSource implements ParserSource {

    protected static Map<Qname, ParsedPrimitive> types =
	new HashMap<Qname, ParsedPrimitive>();

    static {
	types.put(SecUtil.DS_SIGVALUE_NAME, TypingUtil.BASE64_BINARY_ATTRIBUTE);
	types.put(SecUtil.DS_DIGEST_VALUE_NAME,
	    TypingUtil.BASE64_BINARY_ATTRIBUTE);
	types.put(SecUtil.XENC_CVALUE_NAME, TypingUtil.BASE64_BINARY_ATTRIBUTE);
    }

    private ParserSource source;

    public TypeAttributeSource (ParserSource source) {
	this.source = source;
    }

    public StartTag getContext () {
	return source.getContext();
    }

    public String getEncoding () {
	return source.getEncoding();
    }

    public InputStream getInputStream () {
	return source.getInputStream();
    }

    public Item next () throws IOException {
	Item item = source.next();
	if (Item.isStartTag(item)) {
	    StartTag st = (StartTag) item;
	    ParsedPrimitive type = types.get(st.getName());
	    if (type != null) {
		st.ensureAttribute(XasUtil.XSI_TYPE, type);
	    }
	}
	return item;
    }

}

// arch-tag: ff942c94-6e35-40a1-9492-6f3cb209583a
