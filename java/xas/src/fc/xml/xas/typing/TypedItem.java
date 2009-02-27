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

import fc.util.Measurer;
import fc.xml.xas.Item;
import fc.xml.xas.Qname;
import fc.xml.xas.SerializableItem;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.StartTag;
import fc.xml.xas.Verifier;

/**
 * A complex typed value item. This class represents a complex typed value in a
 * form that is directly usable by programs.
 */
public class TypedItem extends Item implements SerializableItem {

    public static final int TYPED = 0x1900;

    private Qname typeName;
    private Object value;

    public static boolean isTyped (Item i) {
	return hasType(i, TYPED);
    }

    /**
         * Verify that an item is a typed item of the correct type.
         * 
         * @param item the item to check
         * @param type the name of the type expected
         * @return <code>item</code> as a {@link TypedItem}
         * @throws IOException if <code>item</code> is not a {@link TypedItem}
         *         or does not have type <code>type</code>
         */
    public static TypedItem verifyTypedItem (Item item, Qname type)
	    throws IOException {
	if (!isTyped(item)) {
	    throw new IOException("Expected typed item, got " + item);
	}
	TypedItem ti = (TypedItem) item;
	if (type != null && !ti.getTypeName().equals(type)) {
	    throw new IOException("Expected typed item with type " + type
		+ ", got " + ti.getTypeName());
	}
	return ti;
    }

    public TypedItem (Qname name, Object value) {
	super(TYPED);
	Verifier.checkNotNull(name);
	this.typeName = name;
	this.value = value;
    }

    public Qname getTypeName () {
	return typeName;
    }

    public Object getValue () {
	return value;
    }

    public void serialize (String type, SerializerTarget target)
	    throws IOException {
	ValueCodec codec = Codec.getValueCodec(typeName);
	if (codec != null) {
	    Object token = Measurer.get(Measurer.TIMING).start();
	    StartTag context = target.getContext();
	    codec.encode(typeName, value, target, context);
	    Measurer.get(Measurer.TIMING)
		.finish(token, "Complex type encoding");
	} else {
	    throw new IOException("No encoder found for type " + typeName);
	}
    }

    public int hashCode () {
	int result = typeName.hashCode();
	if (value != null) {
	    result ^= value.hashCode();
	}
	return result;
    }

    public boolean equals (Object o) {
	if (this == o) {
	    return true;
	} else if (!(o instanceof TypedItem)) {
	    return false;
	} else {
	    TypedItem ti = (TypedItem) o;
	    if (typeName.equals(ti.typeName)) {
		return value == null ? ti.value == null : value
		    .equals(ti.value);
	    } else {
		return false;
	    }
	}
    }

    public String toString () {
	return "TI(" + String.valueOf(value) + ": " + typeName + ")";
    }

}

// arch-tag: 2e4bc499-f7de-4fba-9a0f-82ec9d9aa51e
