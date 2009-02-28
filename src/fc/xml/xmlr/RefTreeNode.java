/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

// $Id: RefTreeNode.java,v 1.4 2004/11/22 12:45:09 ctl Exp $
package fc.xml.xmlr;

import java.util.Iterator;

/**
 * Base interface for a node in a reftree. Nodes in reftrees are of three kinds:
 * <ul>
 * <li>Ordinary nodes that have an arbitrary {@linkplain #getContent content}</li>
 * <li>Nodes referencing a single node from another tree. For these nodes, {@link #isNodeRef} is
 * <code>true</code></li>
 * <li>Nodes referencing a subtree from another tree. For these nodes, {@link #isTreeRef} is
 * <code>true</code></li>
 * </ul>
 * <p>
 * The interface provides the operations on reftree nodes that are required by the algorithms in
 * this package. The tree addressing methods were deliberately kept as simple as possible in order
 * to put minimum restrictions on the actual implementations.
 */
// FIXME: is Reference() allowed as content or not? I.e. can getContent()
// return a Reference()?
public interface RefTreeNode /* extends Iterable<RefTreeNode> */{

    /**
     * Return node id/reference target. This fields serves double duty as unique id of the node as
     * well as target id, in the case when the node is a reference.
     * @return node unique id.
     */

    public Key getId();


    /**
     * Get parent of node.
     * @return parent of this node, or <code>null</code> if this is the root.
     */
    public RefTreeNode getParent();


    /**
     * Get content of node. The content may be any object the XMLR API user wishes to attach to the
     * node. The content of reference nodes is ignored.
     * @return Object content of the node
     */
    public Object getContent();


    /**
     * Get children of the node.
     * @return iterator over the children of the node
     */

    public Iterator getChildIterator();


    /**
     * Get node reference. Returns a non-<code>null</code> reference object if this node is a
     * reference node. If the node is not a reference node, <code>null</code> is returned.
     * @return reference, or <code>null</code> if not a reference.
     */
    public Reference getReference();


    /**
     * Check if node is reference.
     */
    public boolean isReference();


    /**
     * Returns true if this node references a subtree. Subtree references must not have any
     * children.
     * @return <code>true</code> if this node references a subtree.
     */

    public boolean isTreeRef();


    /**
     * Returns true if this node references a single element.
     * @return <code>true</code> if this node references a single element.
     */
    public boolean isNodeRef();

}
// arch-tag: 1db6efd61cfd135d3ba783e189fa4f54 *-
