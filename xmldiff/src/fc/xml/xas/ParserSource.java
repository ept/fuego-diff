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

package fc.xml.xas;

import java.io.InputStream;

/**
 * An {@link ItemSource} that gets its items from an underlying stream. This
 * interface permits direct access to the underlying bytes for efficiency in
 * certain cases. Callers need to be careful to not change the processing
 * context while reading.
 * 
 * @see SerializerTarget
 */
public interface ParserSource extends ItemSource {

    InputStream getInputStream ();

    /**
     * Get the encoding used for characters in the underlying byte stream.
     * 
     * @return the name of the encoding
     */
    String getEncoding ();

    /**
         * Get the current processing context. If a user of this interface reads
         * any bytes from the input stream returned by {@link #getInputStream()},
         * the processing context after reading those bytes must be the same as
         * the one returned by this method before the reading.
         * 
         * @return the current processing context
         */
    StartTag getContext ();

}

// arch-tag: d07b91ab-bb58-4f88-abdf-e1e1faf00ef0
