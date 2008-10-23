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
 * A generic filter for item stream processing. On an abstract level, an
 * {@link ItemTransform} behaves like a queue, allowing additions at the end and
 * removals from the front. However, the items may be transformed arbitrarily,
 * so the removed items do not need to be the same as the added items.
 */
public interface ItemTransform extends ItemSource, ItemTarget {

    /**
         * Check whether there are items available.
         * 
         * @return <code>true</code> if it is known that a <code>next()</code>
         *         will return a non-<code>null</code> item,
         *         <code>false</code> otherwise
         */
    boolean hasItems ();

    Item next () throws IOException;

    void append (Item item) throws IOException;

}

// arch-tag: 0dae8ec9-93eb-4ee4-92d1-9620a76f78d0
