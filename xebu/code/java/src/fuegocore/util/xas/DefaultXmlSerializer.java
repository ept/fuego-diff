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
 * Default implementation for typed XML output.  This is an
 * implementation for textual XML of the {@link TypedXmlSerializer}
 * interface.  By default it uses the {@link XmlSchemaContentEncoder}
 * for typed content.
 */
public class DefaultXmlSerializer extends org.kxml2.io.KXmlSerializer
    implements TypedXmlSerializer {

    private ContentEncoder encoder = new XmlSchemaContentEncoder();

    public void setProperty (String name, Object value) {
	if (XasUtil.PROPERTY_CONTENT_CODEC.equals(name)) {
	    if (value instanceof ContentEncoder) {
		encoder = (ContentEncoder) value;
	    } else {
		throw new IllegalArgumentException("Not a ContentEncoder: "
						   + value);
	    }
	} else {
	    super.setProperty(name, value);
	}
    }

    public Object getProperty (String name) {
	if (XasUtil.PROPERTY_CONTENT_CODEC.equals(name)) {
	    return encoder;
	} else {
	    return super.getProperty(name);
	}
    }

    public TypedXmlSerializer typedContent (Object content, String namespace,
					    String name)
	throws IOException {
	if (encoder == null
	    || !encoder.encode(content, namespace, name, this)) {
	    throw new IOException("Failed to encode value " + content
				  + " as type {" + namespace + "}" + name);
	}
	return this;
    }

}
