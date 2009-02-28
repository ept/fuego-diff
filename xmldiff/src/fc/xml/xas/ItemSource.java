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

import java.io.IOException;

/**
 * A source for a stream of items. An {@link ItemSource} acts as a source for
 * the Pipes and Filters pattern. Pull filters can be implemented by composing
 * sources on top of each other.
 */
public interface ItemSource {

    /**
         * Return the next item from the source.
         *
         * @return the next item in the stream or <code>null</code> if the
         *         stream is exhausted
         * @throws IOException if acquiring the next item fails
         */
    Item next () throws IOException;

}

// arch-tag: ebeafa60-711f-4f70-b55e-961926462cfb
