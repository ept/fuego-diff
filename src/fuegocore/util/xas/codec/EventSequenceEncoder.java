/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas.codec;

import java.io.IOException;

import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.ChainedContentEncoder;
import fuegocore.util.xas.EventSequence;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.XasUtil;
import fuegocore.util.Util;

/**
 * A typed content encoder for the {@link EventSequence} type. This class implements the
 * {@link ContentEncoder} interface for encoding {@link EventSequence} objects into XML.
 */
public class EventSequenceEncoder extends ChainedContentEncoder {

    public EventSequenceEncoder(ContentEncoder chain) {
        this.chain = chain;
    }


    public boolean encode(Object o, String namespace, String name, TypedXmlSerializer ser)
            throws IOException {
        boolean result = false;
        if (Util.equals(namespace, XasUtil.XAS_NAMESPACE) && Util.equals(name, "XmlEventSequence")) {
            if (o instanceof EventSequence) {
                // System.out.println("Encoding event sequence " + o);
                putTypeAttribute(namespace, name, ser);
                EventSequence e = (EventSequence) o;
                XasUtil.outputSequence(e, ser);
                result = true;
            }
        } else if (chain != null) {
            result = chain.encode(o, namespace, name, ser);
        }
        return result;
    }

}
