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

import java.io.IOException;

import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.Verifier;
import fc.xml.xas.XasUtil;

public class TypingUtil {

    public static final ParsedPrimitive INT_ATTRIBUTE =
	new ParsedPrimitive(XasUtil.QNAME_TYPE, XasUtil.INT_TYPE);
    public static final ParsedPrimitive STRING_ATTRIBUTE =
	new ParsedPrimitive(XasUtil.QNAME_TYPE, XasUtil.STRING_TYPE);
    public static final ParsedPrimitive BASE64_BINARY_ATTRIBUTE =
	new ParsedPrimitive(XasUtil.QNAME_TYPE, XasUtil.BASE64_BINARY_TYPE);
    public static final ParsedPrimitive QNAME_ATTRIBUTE =
	new ParsedPrimitive(XasUtil.QNAME_TYPE, XasUtil.QNAME_TYPE);
    public static final ParsedPrimitive DATETIME_ATTRIBUTE =
	new ParsedPrimitive(XasUtil.QNAME_TYPE, XasUtil.DATETIME_TYPE);
    public static final ParsedPrimitive DOUBLE_ATTRIBUTE =
	new ParsedPrimitive(XasUtil.QNAME_TYPE, XasUtil.DOUBLE_TYPE);

    private TypingUtil () {

    }

    public static void appendPrimitiveTo (Qname name, Qname type, Object value,
	    StartTag parent, ParsedPrimitive typeAttribute, EndTag end,
	    ItemTarget target) throws IOException {
	Verifier.checkNotNull(name);
	Verifier.checkNotNull(type);
	Verifier.checkNotNull(typeAttribute);
	Verifier.checkNotNull(end);
	Verifier.checkNotNull(target);
	StartTag st = new StartTag(name, parent);
	st.addAttribute(XasUtil.XSI_TYPE, typeAttribute);
	target.append(st);
	if (value != null) {
	    target.append(new ParsedPrimitive(type, value));
	}
	target.append(end);
    }

    public static void appendComplexTo (Qname name, Qname type, Object value,
	    StartTag parent, ParsedPrimitive typeAttribute, EndTag end,
	    ItemTarget target) throws IOException {
	Verifier.checkNotNull(name);
	Verifier.checkNotNull(type);
	Verifier.checkNotNull(typeAttribute);
	Verifier.checkNotNull(end);
	Verifier.checkNotNull(target);
	StartTag st = new StartTag(name, parent);
	st.addAttribute(XasUtil.XSI_TYPE, typeAttribute);
	target.append(st);
	if (value != null) {
	    target.append(new TypedItem(type, value));
	}
	target.append(end);
    }

    public static Object expect (Qname name, Qname type, ItemSource source)
	    throws IOException {
	Object result = null;
	Item item = source.next();
	if (Item.isStartTag(item)) {
	    StartTag st = (StartTag) item;
	    if (st.getName().equals(name)) {
		item = source.next();
		if (ParsedPrimitive.isParsedPrimitive(item)) {
		    ParsedPrimitive pp = (ParsedPrimitive) item;
		    if (pp.getTypeName().equals(type)) {
			result = pp.getValue();
			source.next();
		    }
		} else if (TypedItem.isTyped(item)) {
		    TypedItem ti = (TypedItem) item;
		    if (ti.getTypeName().equals(type)) {
			result = ti.getValue();
			source.next();
		    }
		}
	    }
	}
	return result;
    }

    public static ItemSource typedSource (ItemSource source, String type,
	    String encoding) {
	ItemSource primitiveSource =
	    new PrimitiveSource(source, type, encoding);
	ItemSource valueSource = new DecodeSource(primitiveSource);
	return valueSource;
    }

}

// arch-tag: db761479-1ce9-4097-949b-e6cb44dd853b
