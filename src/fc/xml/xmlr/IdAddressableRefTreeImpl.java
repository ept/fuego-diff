/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

// $Id: IdAddressableRefTreeImpl.java,v 1.3 2005/02/10 10:22:07 ctl Exp $
package fc.xml.xmlr;

import java.util.Iterator;

/**
 * Abstract default implementation for {@link IdAddressableRefTree}. The class provides default
 * implementations for the {@link #contains contains}, {@link #getParent getParent} and
 * {@link #childIterator childIterator} methods using the {@link RefTreeNode} interface.
 * <p>
 * The user needs to implement {@link #getNode getNode} in a subclass. The other methods may be
 * overriden if there is a way to quickly navigate the tree ids without accessing the actual reftree
 * nodes.
 */

public abstract class IdAddressableRefTreeImpl implements IdAddressableRefTree {

    public abstract RefTreeNode getNode(Key id);


    public boolean contains(Key id) {
        return getNode(id) != null;
    }


    /**
     * Get parent id of node id. The parent id is in esssence obtained by
     * <code>getNode(nid).getParent().getId()</code>.
     * @param nid
     *            id of node, whose parent id is desired
     * @return id of parent of <code>nid</code>, or <code>null</code> if <code>nid</code> is root
     * @throws NodeNotFoundException
     *             if <code>nid</code> is not in the tree.
     */
    public Key getParent(Key nid) throws NodeNotFoundException {
        RefTreeNode n = getNode(nid);
        if (n == null) throw new NodeNotFoundException(nid);
        RefTreeNode parent = n.getParent();
        return parent == null ? null : parent.getId();
    }


    /**
     * Get child ids of node id. The child ids are obtained by accessing
     * <code>getNode(id).childIterator()</code>.
     * @param id
     *            id of node, whose child ids are desired
     * @return Iterator over the {@link java.lang.String} child ids
     * @throws NodeNotFoundException
     *             if <code>id</code> is not in the tree.
     */

    public Iterator<Key> childIterator(Key id) throws NodeNotFoundException {
        RefTreeNode n = getNode(id);
        if (n == null) throw new NodeNotFoundException(id);
        final Iterator niter = n.getChildIterator();
        return new Iterator() {

            public void remove() {
                niter.remove();
            }


            public boolean hasNext() {
                return niter.hasNext();
            }


            public Object next() {
                return ((RefTreeNode) niter.next()).getId();
            }
        };
    }

}
// arch-tag: f7a7e92bff9602818a6c150b11565738 *-
