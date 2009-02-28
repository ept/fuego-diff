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
 * A sink for a stream of items. An {@link ItemTarget} acts as a sink for the
 * Pipes and Filters pattern. Push filters can be implemented by composing
 * targets on top of each other.
 */
public interface ItemTarget {

    /**
         * Add an item to the end of the stream.
         * 
         * @param item the item to add to the stream
         * @throws IOException if writing the item fails for some reason
         */
    void append (Item item) throws IOException;

}

// arch-tag: 5951d6eb-bc2d-4a48-9636-53ebdf78bdbf
