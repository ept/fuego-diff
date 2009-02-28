/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.xas;

import java.io.IOException;
import java.util.Iterator;

import fc.xml.xas.ItemSource;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.model.KeyIdentificationModel;

// TODO: It really looks like Key should be replaced with any generic Object
// type in order to avoid wrapping keys back and forth.

/**
 * Item source that can be navigated by reftree keys.
 */
public interface RandomAccessSource<E extends Key> extends ItemSource {

    /**
     * Get source root key.
     * @return root key
     */
    public E getRoot();


    /**
     * Get parent key.
     * @param k
     *            key to get parent for
     * @return parent key of <i>k</i>
     * @throws NodeNotFoundException
     *             if <i>k</i> is not present
     */
    public E getParent(Key k) throws NodeNotFoundException;


    /**
     * Check if source contains key.
     * @param k
     *            key to check for
     * @return <code>true</code> if source contains key
     */
    public boolean contains(Key k);


    /**
     * Get child keys for a key.
     * @param k
     *            key to get child keys for
     * @return iterator of child keys
     * @throws NodeNotFoundException
     *             if <i>k</i> is not present
     */
    public Iterator<E> getChildKeys(Key k) throws NodeNotFoundException;


    /**
     * Seek source to a given key.
     * @param k
     *            key to seek to
     * @throws NodeNotFoundException
     *             if <i>k</i> is not present
     */
    public void seek(E k) throws NodeNotFoundException;


    // Later: public void forget(E k); // Mark subtree not needed

    /**
     * Get source key identification model.
     * @return source key identification model
     */
    public KeyIdentificationModel getKeyIdentificationModel();


    public void close() throws IOException;

}

// arch-tag: 41c788a7-8511-4e4d-97b2-2d6434d2c119
