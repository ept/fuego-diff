/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.index;

import fc.xml.xas.Item;
import fc.xml.xas.Verifier;

/**
 * A class for iterating over a stream of items while maintaining a key. In indexing applications it
 * is often necessary to iterate over an item stream and to mark the Dewey keys of (some)
 * encountered items somehow. This class handles the maintenance of the key during the iteration, so
 * the application code only needs to contain the application logic.
 */
public class KeyIterator {

    private DeweyKey key;
    private boolean isText = false;


    /**
     * Construct a key iterator starting from the root.
     */
    public KeyIterator() {
        this(DeweyKey.root());
    }


    /**
     * Construct a key iterator starting from the specified key.
     * @param key
     *            the key to start from
     */
    public KeyIterator(DeweyKey key) {
        this.key = key;
    }


    /**
     * Get the current key of the iteration.
     */
    public DeweyKey current() {
        return key;
    }


    /**
     * Update the state of the iterator during iteration.
     * @param item
     *            the current item from the processed stream
     * @throws IllegalStateException
     *             if <code>item</code> is a document delimiter and the current key is not root
     *             outside the delimiter
     */
    public void update(Item item) {
        if (isText && !Item.isContent(item)) {
            key = key.next();
            isText = false;
        }
        if (Item.isStartTag(item)) {
            key = key.down();
        } else if (Item.isEndTag(item)) {
            key = key.up();
            key = key.next();
        } else if (Item.isContent(item)) {
            isText = true;
        } else if (Item.isStartDocument(item)) {
            Verifier.check(key.isRoot(), "Key is root");
            key = key.down();
        } else if (Item.isEndDocument(item)) {
            key = key.up();
            Verifier.check(key.isRoot(), "Key is root");
        } else {
            key = key.next();
        }
    }


    public String toString() {
        return "KI(" + key + ")";
    }

}

// arch-tag: a55baa4c-3127-4712-bad6-0e8c13d7ccb6
