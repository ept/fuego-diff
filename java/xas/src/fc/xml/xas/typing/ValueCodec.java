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

import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;

/**
 * An encoder-decoder pair for complex typed data.
 */
public interface ValueCodec {

    /**
         * Query whether a type is understood
         * 
         * @param typeName the name of the type to query
         * @return <code>true</code> if this codec understands the type
         *         <code>typeName</code>, <code>false</code> otherwise
         */
    boolean isKnown (Qname typeName);

    void encode (Qname typeName, Object value, ItemTarget target,
	    StartTag parent) throws IOException;

    Object decode (Qname typeName, ItemSource source) throws IOException;

}

// arch-tag: 6b77cfa1-0c7f-4e39-966e-12f927c2d26b
