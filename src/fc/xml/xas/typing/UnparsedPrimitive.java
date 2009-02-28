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

import fc.util.Measurer;
import fc.xml.xas.Item;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.Verifier;

/**
 * A primitive typed value item in serialized form. An item of this type needs to be converted
 * before it can be used by programs. This kind of an item is the result of parsing, as the parser
 * is decoupled from the data types, so it cannot decode typed data, but rather only provides the
 * bytes.
 */
public class UnparsedPrimitive extends Item {

    public static final int UNPARSED = 0x1902;

    private String type;
    private byte[] value;
    private int offset;
    private int length;
    private String encoding;


    public static boolean isUnparsedPrimitive(Item i) {
        return hasType(i, UNPARSED);
    }


    public UnparsedPrimitive(String type, byte[] value, String encoding) {
        this(type, value, 0, value.length, encoding);
    }


    public UnparsedPrimitive(String type, byte[] value, int offset, int length, String encoding) {
        super(UNPARSED);
        Verifier.checkNotNull(type);
        Verifier.checkNotNull(value);
        Verifier.checkOffsetLength(0, offset, length, value.length);
        this.type = type;
        this.value = value;
        this.offset = offset;
        this.length = length;
        this.encoding = encoding;
    }


    /**
     * Convert this item into a parsed primitive. This method will acquire a suitable primitive
     * codec from {@link Codec} and attempt to convert the bytes of this item into a value.
     * @param typeName
     *            the name of the type of this item
     * @param context
     *            the processing context in effect at this item
     * @return a {@link ParsedPrimitive} representing the parsed value of this item
     * @throws IOException
     *             if the decoding fails, either due to a missing codec or a failing decode
     */
    public ParsedPrimitive convert(Qname typeName, StartTag context) throws IOException {
        PrimitiveCodec codec = Codec.getPrimitiveCodec(type, typeName);
        if (codec != null) {
            Object token = Measurer.get(Measurer.TIMING).start();
            Object result = codec.decode(typeName, value, offset, length, encoding, context);
            Measurer.get(Measurer.TIMING).finish(token, "Primitive type decoding");
            if (result != null) {
                return new ParsedPrimitive(typeName, result);
            } else {
                throw new IOException("Value " + new String(value, offset, length, encoding) +
                                      " not convertable to type " + typeName);
            }
        } else {
            throw new IOException("No decoder found for format " + type + " and type " + typeName);
        }
    }

}

// arch-tag: 6b74d1be-4c24-4e94-858c-c2ba246eeec7
