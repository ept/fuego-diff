/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas.codec;

import fuegocore.util.xas.ContentCodecFactory;
import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.ContentDecoder;

/**
 * A default factory for typed content encoders and decoders. An object of this class is suitable to
 * use as the default {@link ContentCodecFactory} implementation. It recognizes some standard
 * structured Java types.
 */
public class DefaultCodecFactory extends ContentCodecFactory {

    private static ContentCodecFactory factory = new HashtableCodecFactory(
                                                                           new VectorCodecFactory(
                                                                                                  new EventSequenceCodecFactory()));


    public ContentEncoder getChainedEncoder(ContentEncoder chain) {
        return factory.getChainedEncoder(chain);
    }


    public ContentDecoder getChainedDecoder(ContentDecoder chain) {
        return factory.getChainedDecoder(chain);
    }

}
