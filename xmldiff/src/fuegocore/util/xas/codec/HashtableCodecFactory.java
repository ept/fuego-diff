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

package fuegocore.util.xas.codec;

import fuegocore.util.xas.ContentCodecFactory;
import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.ContentDecoder;
import fuegocore.util.xas.Qname;
import fuegocore.util.xas.XasUtil;

/**
 * A typed content codec factory for the {@link java.util.Hashtable}
 * type.  This {@link ContentCodecFactory} builds encoders and
 * decoders recognizing the {@link java.util.Hashtable} Java type.
 * The XML name given for this type has namespace {@link
 * XasUtil#XAS_NAMESPACE} and local name <code>"hashtable"</code>.
 * This type mapping is registered during initialization of the class.
 */
public class HashtableCodecFactory extends ContentCodecFactory {

    private ContentCodecFactory factory;

    static {
	try {
	    ContentCodecFactory.addTypeMapping
		(Class.forName("java.util.Hashtable"),
		 new Qname(XasUtil.XAS_NAMESPACE, "hashtable"));
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

    public HashtableCodecFactory () {
	this(null);
    }

    public HashtableCodecFactory (ContentCodecFactory factory) {
	this.factory = factory;
    }

    public ContentEncoder getChainedEncoder (ContentEncoder chain) {
	if (factory != null) {
	    chain = factory.getChainedEncoder(chain);
	}
	return new HashtableEncoder(chain);
    }

    public ContentDecoder getChainedDecoder (ContentDecoder chain) {
	if (factory != null) {
	    chain = factory.getChainedDecoder(chain);
	}
	return new HashtableDecoder(chain);
    }

}
