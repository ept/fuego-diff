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

/**
 * A reference to a specific location in an XML document. A pointer may point to
 * either a fragment or a complete document, but its target will always be an
 * item, called the pointed-to item.
 */
public interface Pointer {

    /**
         * Get the pointed-to item.
         */
    Item get ();

    /**
         * Perform a query from the pointer's location. This method will follow
         * a path starting from the pointed-to item, in effect performing a
         * relative query instead of an absolute one.
         * 
         * @param path the path to follow
         * @return a pointer pointing to the queried location, or
         *         <code>null</code> if the location does not exist
         */
    Pointer query (int[] path);

    /**
         * 
         * 
         */
    void canonicalize ();

}

// arch-tag: 18507ed8-73cb-4720-91b3-f1411b46c9e6
