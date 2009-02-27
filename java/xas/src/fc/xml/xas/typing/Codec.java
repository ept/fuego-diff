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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fc.util.log.Log;
import fc.xml.xas.Qname;
import fc.xml.xas.Verifier;
import fc.xml.xas.XasUtil;

/**
 * A store for application-specific value and primitive codecs. An application
 * that wishes to use its own types needs to register the appropriate codecs
 * using the static methods of this class.
 */
public class Codec {

    private static List<ValueCodec> valueCodecs = new ArrayList<ValueCodec>();
    private static Map<String, List<PrimitiveCodec>> primitiveCodecs = new HashMap<String, List<PrimitiveCodec>>();
    private static Map<Class, Qname> valueTypeNames = new HashMap<Class, Qname>();
    private static Map<Class, Qname> primitiveTypeNames = new HashMap<Class, Qname>();

    static {
	primitiveTypeNames.put(Integer.class, XasUtil.INT_TYPE);
	primitiveTypeNames.put(Long.class, XasUtil.LONG_TYPE);
	primitiveTypeNames.put(String.class, XasUtil.STRING_TYPE);
	primitiveTypeNames.put(Qname.class, XasUtil.QNAME_TYPE);
    }

    private Codec () {
    }

    public static void registerValueType (Class type, Qname name) {
	valueTypeNames.put(type, name);
    }

    public static Qname getValueType (Class type) {
	return valueTypeNames.get(type);
    }

    public static void registerValueCodec (ValueCodec codec) {
	Verifier.checkNotNull(codec);
	if (Log.isEnabled(Log.DEBUG)) {
	    Log.debug("Registering value codec", codec);
	}
	valueCodecs.add(codec);
    }

    public static void deregisterValueCodec (ValueCodec codec) {
	Verifier.checkNotNull(codec);
	valueCodecs.remove(codec);
    }

    public static ValueCodec getValueCodec (Qname name) {
	for (int i = valueCodecs.size() - 1; i >= 0; i--) {
	    ValueCodec codec = valueCodecs.get(i);
	    if (codec.isKnown(name)) {
		return codec;
	    }
	}
	return null;
    }

    public static void registerPrimitiveType (Class type, Qname name) {
	primitiveTypeNames.put(type, name);
    }

    public static Qname getPrimitiveType (Class type) {
	return primitiveTypeNames.get(type);
    }

    public static void registerPrimitiveCodec (PrimitiveCodec codec) {
	Verifier.checkNotNull(codec);
	String type = codec.getType();
	if (Log.isEnabled(Log.DEBUG)) {
	    Log.debug("Registering primitive codec for type " + type, codec);
	}
	List<PrimitiveCodec> list = primitiveCodecs.get(type);
	if (list == null) {
	    list = new ArrayList<PrimitiveCodec>();
	    primitiveCodecs.put(type, list);
	}
	list.add(codec);
    }

    public static void deregisterPrimitiveCodec (PrimitiveCodec codec) {
	Verifier.checkNotNull(codec);
	String type = codec.getType();
	List<PrimitiveCodec> list = primitiveCodecs.get(type);
	if (list != null) {
	    list.remove(codec);
	}
    }

    public static PrimitiveCodec getPrimitiveCodec (String type, Qname name) {
	List<PrimitiveCodec> list = primitiveCodecs.get(type);
	if (list != null) {
	    for (int i = list.size() - 1; i >= 0; i--) {
		PrimitiveCodec codec = list.get(i);
		if (codec.isKnown(name)) {
		    return codec;
		}
	    }
	}
	return null;
    }

}

// arch-tag: 80f523b2-cd93-452e-b5be-7921748aad1f
