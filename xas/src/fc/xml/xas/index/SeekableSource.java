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

package fc.xml.xas.index;

import java.io.IOException;

import fc.xml.xas.ItemSource;
import fc.xml.xas.StartTag;

/**
 * An {@link ItemSource} that allows seeking. To support indexing and
 * out-of-order parsing, it is necessary to support sources with offsets and
 * settable positions.
 */
public interface SeekableSource extends ItemSource {

    /**
         * Get the position at which the underlying source is currently.
         */
    int getCurrentPosition ();

    /**
         * Get the position at which the underlying source was before the latest
         * item. This method can not be called immediately after a call to
         * {@link #setPosition(int, StartTag)}. Rather, there must be a call to
         * {@link #next()} in between.
         * 
         * The code sequences
         * 
         * <pre>
         * int pos = source.getCurrentPosition();
         * Item i = source.next();
         * </pre>
         * 
         * and
         * 
         * <pre>
         * Item i = source.next();
         * int pos = source.getPreviousPosition();
         * </pre>
         * 
         * have equivalent effects, assuming that <code>i != null</code>.
         */
    int getPreviousPosition ();

    /**
         * Set the current position. The position to set must be a result of
         * previously calling {@link #getCurrentPosition()} or
         * {@link #getPreviousPosition()}, and the provided processing context
         * must be correct (this is usually accomplished by using a source that
         * also implements {@link fc.xml.xas.ParserSource}).
         * 
         * @param pos the new position
         * @param context the processing context at the new position
         * 
         * @throws IOException if setting the position fails
         */
    void setPosition (int pos, StartTag context) throws IOException;

    public void close () throws IOException;

}

// arch-tag: 0417e875-ca97-425f-9318-b41c354dd703
