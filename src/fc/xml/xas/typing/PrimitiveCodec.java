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

import fc.xml.xas.Qname;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.StartTag;

/**
 * An encoder-decoder pair for primitive typed data.
 */
public interface PrimitiveCodec {

    /**
         * Get the MIME type that this codec understands.
         */
    String getType ();

    /**
         * Query whether a type is understood
         * 
         * @param typeName the name of the type to query
         * @return <code>true</code> if this codec understands the type
         *         <code>typeName</code>, <code>false</code> otherwise
         */
    boolean isKnown (Qname typeName);

    void encode (Qname typeName, Object value, SerializerTarget target)
	    throws IOException;

    Object decode (Qname typeName, byte[] value, int offset, int length,
	    String encoding, StartTag context) throws IOException;

}

// arch-tag: 2c9a18e0-a56c-4a0c-ba9e-f664c6f666ff
