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
 * A fragment containing a single item. This wrapper is useful in cases where an
 * existing fragment is replaced by a single item, since the replacing item
 * needs to inherit the replaced fragment's size.
 */
public class WrapperItem extends FragmentItem {

    public static final int WRAPPER = 0x4202;

    public static boolean isWrapper (Item i) {
	return hasType(i, WRAPPER);
    }

    public WrapperItem (Item item) {
	super(WRAPPER, 1);
	this.firstItem = item;
    }

    public String toString () {
	return "W(" + firstItem + ")";
    }

}

// arch-tag: efac70cc-c790-4470-8e4f-9ff3bc9a0fc2
