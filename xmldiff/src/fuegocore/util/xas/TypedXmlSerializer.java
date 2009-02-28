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

import org.xmlpull.v1.XmlSerializer;

/**
 * An extended interface to permit the output of typed XML content.
 * Normally a legacy application would output both typed and untyped
 * content encoded as a text {@link String}, making it impossible for
 * an alternative encoder to determine whether the content should be
 * encoded specially or it already is.  This interface is intended for
 * use when the content has already been encoded and must not be
 * encoded further.  The normal <code>text()</code> methods are to be
 * used when default (or no) encoding is appropriate.
 */
public interface TypedXmlSerializer extends XmlSerializer {

    /**
     * Output the given content as the specified type.  This method
     * will output an {@link Object} having the given XML Schema
     * datatype.  If the datatype has no special encoding known to
     * this serializer, it will fall back on the
     * <code>toString()</code> method of the supplied object.
     *
     * @param content the object to output
     * @param namespace the namespace URI of the XML Schema type
     * @param name the local name of the XML Schema type
     * @return this encoder for the purposes of chaining
     *
     * @throws IllegalArgumentException if the given type name was not
     * introduced by the preceding start element event
     */
    TypedXmlSerializer typedContent (Object content, String namespace,
				     String name)
	throws IOException;

}
