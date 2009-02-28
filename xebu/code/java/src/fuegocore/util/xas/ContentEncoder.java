/*
 * Copyright 2006 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import java.io.IOException;

/**
 * An interface for encoding data specially.  This interface will be
 * invoked by a {@link TypedXmlSerializer} whenever it encounters a
 * datatype that it does not know how to encode.  The interface is
 * intentionally constructed to permit mutual recursion between this
 * encoder and the {@link TypedXmlSerializer} that invoked it, which
 * allows for intuitive handling of structured datatypes.
 *
 * <p>This interface should be implemented by applications outputting
 * structured data as XML using the XML Schema datatypes.  An
 * application is free to use only the {@link TypedXmlSerializer#text}
 * methods, but this may compromise on encoding performance by not
 * permitting direct encoding.
 */
public interface ContentEncoder {

    /**
     * Encode an object of the specified type.  This method outputs a
     * given object with a given XML Schema type to the given
     * serializer object.  The return value indicates whether the type
     * was known.
     *
     * <p>This method may add any attributes that modify the decoding
     * of the encoded content by calling the {@link
     * TypedXmlSerializer#attribute} method on <code>ser</code> before
     * outputting anything else.  The {@link ContentDecoder#decode}
     * method will receive all attributes output to the element.
     *
     * @param o the object to encode
     * @param namespace the namespace URI of the object's type
     * @param name the local name of the object's type
     * @param ser the serializer to output the encoded form to
     * @return whether anything was output; <code>false</code> is
     * returned only if, for some reason, no calls to <code>ser</code>
     * were made.
     */
    boolean encode (Object o, String namespace, String name,
		    TypedXmlSerializer ser) throws IOException;

}
