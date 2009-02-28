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

package fcme.util.xas.codec;

import fuegocore.util.xas.ContentCodecFactory;
import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.ContentDecoder;
import fuegocore.util.xas.Qname;
import fuegocore.util.xas.XasUtil;

/**
 * A default factory for typed content encoders and decoders.  An
 * object of this class creates encoders and decoders that recognize
 * some basic structured types.
 */
public class DefaultCodecFactory extends ContentCodecFactory {

    static {
	try {
	    ContentCodecFactory.addTypeMapping
		(Class.forName("java.util.Hashtable"),
		 new Qname(XasUtil.XAS_NAMESPACE, "hashtable"));
	    ContentCodecFactory.addTypeMapping
		(Class.forName("java.util.Vector"),
		 new Qname(XasUtil.XAS_NAMESPACE, "vector"));
	    ContentCodecFactory.addTypeMapping
		(Class.forName("fuegocore.util.xas.EventSequence"),
		 new Qname(XasUtil.XAS_NAMESPACE, "XmlEventSequence"));
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

    public ContentEncoder getChainedEncoder (ContentEncoder chain) {
	return new DefaultEncoder(chain);
    }

    public ContentDecoder getChainedDecoder (ContentDecoder chain) {
	return new DefaultDecoder(chain);
    }

}
