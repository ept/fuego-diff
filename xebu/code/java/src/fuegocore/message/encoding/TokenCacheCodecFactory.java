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

package fuegocore.message.encoding;

import fuegocore.util.xas.CodecFactory;

/**
 * An interface for token caching codec factories.  Some XML
 * serialization formats are able to map names in XML documents to
 * binary tokens.  This interface provides an extension to the
 * standard {@link CodecFactory} interface for setting initial values
 * for those caches, often useful when some information on the syntax
 * of the XML documents is available.
 */
public interface TokenCacheCodecFactory extends CodecFactory {

    /**
     * Set the initial output token caches for a specified token.  
     *
     * @param token a token identifying the cache to set
     * @param caches the initial caches to use
     * @return <code>true</code> if the caches of <code>token</code>
     * were set to <code>caches</code>, <code>false</code> otherwise
     */
    boolean setInitialOutCache (Object token, OutTokenCache[] caches);

    /**
     * Set the initial input token caches for a specified token.  
     *
     * @param token a token identifying the cache to set
     * @param caches the initial caches to use
     * @return <code>true</code> if the caches of <code>token</code>
     * were set to <code>caches</code>, <code>false</code> otherwise
     */
    boolean setInitialInCache (Object token, Object[][] caches);

}
