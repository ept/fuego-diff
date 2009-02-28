/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import java.io.IOException;

import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.XasUtil;

/**
 * A content encoder implementing chaining of encoders. The typical usage pattern for
 * {@link ContentEncoder} implementations is to chain them, i.e. an encoder is given a pre-existing
 * encoder, which it delegates to if it does not recognize the type to encode. This class contains a
 * field for this chained encoder, as well as a method to insert the XML Schema type attribute.
 */
public abstract class ChainedContentEncoder implements ContentEncoder {

    protected ContentEncoder chain;


    /**
     * Insert an appropriate XML Schema type attribute. It is the responsibility of the
     * {@link ContentEncoder#encode} method to insert the <code>type</code> attribute for the object
     * to encode. Since this code is practically always the same, it is included as a common part of
     * this class. This method invokes the {@link TypedXmlSerializer#attribute} method with the
     * supplied type name and other arguments.
     * @param namespace
     *            the namespace URI of the type
     * @param name
     *            the local name of the type
     * @param ser
     *            the serializer to use for outputting the type attribute
     */
    protected void putTypeAttribute(String namespace, String name, TypedXmlSerializer ser)
            throws IOException {
        String prefix = ser.getPrefix(namespace, false);
        if (prefix != null) {
            ser.attribute(XasUtil.XSI_NAMESPACE, "type", prefix + ":" + name);
        } else {
            throw new IOException("Type namespace " + namespace + " not recognized");
        }
    }

}
