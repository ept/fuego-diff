/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.typing;

import java.io.IOException;
import java.util.Arrays;

import fc.util.Measurer;
import fc.xml.xas.Item;
import fc.xml.xas.Qname;
import fc.xml.xas.SerializableItem;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.Verifier;

/**
 * A primitive typed value item. This class represents a primitive typed value in a form that is
 * directly usable by programs.
 */
public class ParsedPrimitive extends Item implements SerializableItem {

    public static final int PARSED = 0x1901;

    private Qname typeName;
    private Object value;


    public static boolean isParsedPrimitive(Item i) {
        return hasType(i, PARSED);
    }


    /**
     * Verify that an item is a parsed primitive of the correct type.
     * @param item
     *            the item to check
     * @param type
     *            the name of the type expected
     * @return <code>item</code> as a {@link ParsedPrimitive}
     * @throws IOException
     *             if <code>item</code> is not a {@link ParsedPrimitive} or does not have type
     *             <code>type</code>
     */
    public static ParsedPrimitive verifyParsedPrimitive(Item item, Qname type) throws IOException {
        if (!isParsedPrimitive(item)) { throw new IOException("Expected parsed primitive, got " +
                                                              item); }
        ParsedPrimitive pp = (ParsedPrimitive) item;
        if (type != null && !pp.getTypeName().equals(type)) { throw new IOException(
                                                                                    "Expected parsed primitive with type " +
                                                                                            type +
                                                                                            ", got " +
                                                                                            pp.getTypeName()); }
        return pp;
    }


    public ParsedPrimitive(Qname typeName, Object value) {
        super(PARSED);
        Verifier.checkNotNull(typeName);
        this.typeName = typeName;
        this.value = value;
    }


    public Qname getTypeName() {
        return typeName;
    }


    public Object getValue() {
        return value;
    }


    public void serialize(String type, SerializerTarget target) throws IOException {
        PrimitiveCodec codec = Codec.getPrimitiveCodec(type, typeName);
        if (codec != null) {
            Object token = Measurer.get(Measurer.TIMING).start();
            codec.encode(typeName, value, target);
            Measurer.get(Measurer.TIMING).finish(token, "Primitive type encoding");
        } else {
            throw new IOException("No encoder found for format " + type + " and type " + typeName);
        }
    }


    @Override
    public int hashCode() {
        int result = typeName.hashCode();
        if (value != null) {
            result ^= value.hashCode();
        }
        return result;
    }


    private static boolean valueEquals(Object o1, Object o2) {
        if (o1 instanceof byte[]) {
            return o2 instanceof byte[] && Arrays.equals((byte[]) o1, (byte[]) o2);
        } else {
            return o1.equals(o2);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ParsedPrimitive)) {
            return false;
        } else {
            ParsedPrimitive pp = (ParsedPrimitive) o;
            if (typeName.equals(pp.typeName)) {
                return value == null ? pp.value == null : valueEquals(value, pp.value);
            } else {
                return false;
            }
        }
    }


    @Override
    public String toString() {
        return "PP(" + String.valueOf(value) + ": " + typeName + ")";
    }

}

// arch-tag: 889cc955-71d3-440c-9c9f-d7bf643363b1
