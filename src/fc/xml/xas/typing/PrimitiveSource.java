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

package fc.xml.xas.typing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import fc.xml.xas.AttributeNode;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.Text;
import fc.xml.xas.Verifier;
import fc.xml.xas.XasUtil;

public class PrimitiveSource implements ItemSource {

    private ItemSource source;
    private String type;
    private String encoding;
    private boolean looking = false;
    private StartTag lookingTag = null;
    private Qname lookingType = null;
    private Item cachedItem = null;
    private ByteArrayOutputStream bout = new ByteArrayOutputStream();

    public PrimitiveSource (ItemSource source, String type, String encoding) {
	Verifier.checkNotNull(source);
	this.source = source;
	this.type = type;
	this.encoding = encoding;
    }

    public Item next () throws IOException {
	if (cachedItem != null) {
	    Item item = cachedItem;
	    cachedItem = null;
	    return item;
	} else {
	    Item item = source.next();
	    if (Item.isStartTag(item)) {
		StartTag st = (StartTag) item;
		AttributeNode an = st.getAttribute(XasUtil.XSI_TYPE);
		if (an != null) {
		    looking = true;
		    lookingTag = st;
		    Object value = an.getValue();
		    if (value instanceof String) {
			byte[] bytes = ((String) value).getBytes(encoding);
			UnparsedPrimitive up =
			    new UnparsedPrimitive(type, bytes, encoding);
			ParsedPrimitive pp = up.convert(XasUtil.QNAME_TYPE, st);
			lookingType = (Qname) pp.getValue();
			an.setValue(pp);
		    } else if (value instanceof UnparsedPrimitive) {
			UnparsedPrimitive up = (UnparsedPrimitive) value;
			ParsedPrimitive pp = up.convert(XasUtil.QNAME_TYPE, st);
			lookingType = (Qname) pp.getValue();
			an.setValue(pp);
		    } else if (value instanceof ParsedPrimitive) {
			lookingType =
			    (Qname) ((ParsedPrimitive) value).getValue();
		    } else if (value instanceof Qname) {
			lookingType = (Qname) value;
		    } else {
			throw new IllegalStateException("Value of attribute "
			    + an + " not convertible to Qname");
		    }
//		    st.removeAttribute(XasUtil.XSI_TYPE);
		}
	    } else if (looking) {
		if (!UnparsedPrimitive.isUnparsedPrimitive(item)) {
		    while (Item.isText(item)) {
			Text t = (Text) item;
			bout.write(t.getData().getBytes(encoding));
			item = source.next();
		    }
		    if (Item.isEndTag(item)) {
			cachedItem = item;
			UnparsedPrimitive up =
			    new UnparsedPrimitive(type, bout.toByteArray(),
				encoding);
			item = up.convert(lookingType, lookingTag);
		    } else if (bout.size() > 0) {
			cachedItem = item;
			item =
			    new Text(new String(bout.toByteArray(), encoding));
		    }
		} else {
		    UnparsedPrimitive up = (UnparsedPrimitive) item;
		    item = up.convert(lookingType, lookingTag);
		}
		bout.reset();
		looking = false;
		lookingTag = null;
	    }
	    return item;
	}
    }

}

// arch-tag: 48f90544-684e-4d8a-9370-bd4d84147e49
