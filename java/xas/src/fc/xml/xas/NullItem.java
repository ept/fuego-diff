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
 * An item with no content. This item simply takes up space, it has no behavior
 * or serialized form. It is useful in cases where an item needs to be deleted
 * from an array without incurring a performance penalty.
 */
public class NullItem extends Item implements AppendableItem {

    private static final NullItem instance = new NullItem();

    public static final int NULL = 0x4203;

    private NullItem () {
	super(NULL);
    }

    public static boolean isNull (Item i) {
	return i == instance;
    }

    public static NullItem instance () {
	return instance;
    }

    public void appendTo (ItemTarget target) {
    }

    public boolean equals (Object o) {
	return o == instance;
    }

    public int hashCode () {
	return NULL;
    }

    public String toString () {
	return "NI()";
    }

}

// arch-tag: 57734a7d-d7a3-49a5-a2ed-e681db6503c9
