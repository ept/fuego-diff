/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas.codec;

import java.util.Vector;

import fuegocore.util.xas.ContentDecoder;
import fuegocore.util.xas.ChainedContentDecoder;
import fuegocore.util.xas.XmlReader;
import fuegocore.util.xas.EventList;
import fuegocore.util.xas.XasUtil;
import fuegocore.util.Util;

/**
 * A typed content decoder for the {@link Vector} type. This class extends the
 * {@link ContentDecoder} abstraction for decoding {@link Vector} objects from XML.
 */
public class VectorDecoder extends ChainedContentDecoder {

    public VectorDecoder(ContentDecoder chain) {
        super(null);
        if (chain == null) { throw new IllegalArgumentException("Chained decoder must be"
                                                                + " non-null"); }
        this.chain = chain;
    }


    public Object decode(String namespace, String name, XmlReader reader, EventList attributes) {
        Object result = null;
        if (Util.equals(namespace, XasUtil.XAS_NAMESPACE) && Util.equals(name, "vector")) {
            Vector v = new Vector();
            Object item;
            while ((item = expect(XasUtil.XAS_NAMESPACE, "item", reader)) != null) {
                v.addElement(item);
            }
            result = v;
        } else if (chain != null) {
            result = chain.decode(namespace, name, reader, attributes);
        }
        return result;
    }

}
