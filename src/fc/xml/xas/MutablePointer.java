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
 * A pointer that allows mutations. This interface extends the {@link Pointer}
 * interface by adding mutation operations.
 */
public interface MutablePointer extends Pointer {

    /**
         * Change the pointed-to item.
         * 
         * @param item the new item to replace the pointed-to item
         */
    void set (Item item);

    /**
         * Insert a new item. The insertion is performed at the location right
         * after the pointed-to item, interpreted as a node.
         * 
         * @param item the item to insert
         */
    void insert (Item item);

    /**
         * Insert a new item as a child item. The {@link #insert(Item)} method
         * cannot be used to insert an item at the front of a child list, so
         * this operation is provided to do that. The pointed-to item must be a
         * {@link StartTag}.
         * 
         * @param item the item to insert
         * 
         * @throws IllegalStateException if the pointed-to item is not a
         *         {@link StartTag}
         */
    void insertFirstChild (Item item);

    /**
         * Delete an item. This method will delete the pointed-to item or the
         * node that it represents.
         * 
         * @return the deleted item, or, if it was a {@link StartTag}, it
         *         converted to a fragment
         * 
         * @throws IllegalStateException if the pointed-to item is an
         *         {@link EndTag}
         */
    Item delete ();

    /**
         * Move the pointed-to item to another location. The semantics of this
         * operation are <code>target.insert(delete())</code>, and exceptions
         * are thrown under the same conditions as those two operations would.
         * 
         * @param target the target of the move
         * 
         * @throws IllegalStateException if <code>this</code> and
         *         <code>target</code> point to different fragments, or if the
         *         pointed-to item is an {@link EndTag}
         *
         * @see #insert(Item)
         * @see #delete()
         */
    void move (MutablePointer target);

    /**
         * Move the pointed-to item to the first child of a node. The semantics
         * of this operation are <code>target.insertFirstChild(delete())</code>,
         * and exceptions are thrown under the same conditions as those two
         * operations would.
         * 
         * @param target the parent of the item after the move
         * 
         * @see #move(MutablePointer)
         * @see #insertFirstChild(Item)
         * @see #delete()
         */
    void moveFirstChild (MutablePointer target);

}

// arch-tag: 143f3515-2725-4b3a-aa06-79650cd98ca4
