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

import java.io.Flushable;
import java.io.OutputStream;

/**
 * An {@link ItemTarget} that outputs to a byte stream. This interface gives
 * direct access to the underlying stream for efficiency in certain cases.
 * Callers need to be careful to not change the processing context while
 * reading.
 * 
 * @see ParserSource
 */
public interface SerializerTarget extends ItemTarget, Flushable {

    OutputStream getOutputStream ();

    /**
         * Get the encoding used for characters in the underlying byte stream.
         * Any written bytes that represent characters must be encoded according
         * to this encoding.
         * 
         * @return the name of the encoding
         */
    String getEncoding ();

    /**
         * Get the current processing context. If a user of this interface
         * writes any bytes to the output stream returned by
         * {@link #getOutputStream()}, the processing context after writing
         * those bytes must be the same as the one returned by this method
         * before the writing.
         * 
         * @return the current processing context
         */
    StartTag getContext ();

}

// arch-tag: adbc7328-7d9b-4104-8f5e-c9eb5b9fc817
