/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

// $Id: MutableRefTree.java,v 1.4 2004/11/22 12:45:05 ctl Exp $
package fc.xml.xmlr;

/**
 * Base interface for mutable reftrees. Mutable reftrees are reftrees whose structure as well as
 * node content may be modified. The interface makes use of node ids rather than the
 * {@link RefTreeNode} nodes themselves.
 * <p>
 * The interface accommodates for ordered as well as unordered/automatically ordered child lists by
 * providing two variants of the <code>insert</code> and <code>move</code> methods.
 */

public interface MutableRefTree extends IdAddressableRefTree {

    /** Constant indicating default position in child list. */
    public static final long DEFAULT_POSITION = -1L;

    /**
     * Constant indicating that an automatic key assignment is desired. Used with
     * {@link #insert(Key, long, Key, Object)}. The value of this field is known to be
     * <code>null</code>, i.e., the field name is just to increase readability of the code.
     */

    public static final Key AUTO_KEY = null;


    /**
     * Delete a subtree. Deletes the subtree rooted at <code>id</code> from the tree.
     * @param id
     *            root of subtree to delete
     * @throws NodeNotFoundException
     *             if <code>id</code> is not in the tree
     */
    public void delete(Key id) throws NodeNotFoundException;


    // Ins at natural position.
    /**
     * Insert a new non-reference node. The insert happens at the default position in the child list
     * (as defined by the particular <code>MutableRefTree</code> implementation).
     * @param parentId
     *            id of parent to the new node
     * @param newId
     *            id of the new node, must not already exist in the tree
     * @param content
     *            content object of new node
     * @throws NodeNotFoundException
     *             if the <code>parentId</code> node is not in the tree.
     * @return Key of inserted node (useful with auto-key)
     */
    public Key insert(Key parentId, Key newId, Object content) throws NodeNotFoundException;


    // Ins at particular natural position. May not be supported

    /**
     * Insert a new non-reference node. Some implementations may not support insertion at other
     * positions than {@link #DEFAULT_POSITION DEFAULT_POSITION}.
     * @param parentId
     *            id of parent to the new node
     * @param pos
     *            position in child list, before which the new node is inserted (i.e. 0 implies at
     *            the start of the list). {@link #DEFAULT_POSITION DEFAULT_POSITION} inserts at the
     *            default position (as defined by the particular <code>MutableRefTree</code>
     *            implementation)
     * @param newId
     *            id of the new node, must not already exist in the tree
     * @param content
     *            content object of new node
     * @throws NodeNotFoundException
     *             if the <code>parentId</code> node is not in the tree.
     * @return Key of inserted node (useful with auto-key)
     */
    public Key insert(Key parentId, long pos, Key newId, Object content)
            throws NodeNotFoundException;


    /**
     * Move a node in the tree. The node target position among its siblings is the default position
     * in the child list (as defined by the particular <code>MutableRefTree</code> implementation).
     * <p>
     * <b>Note:</b> The result of a move that would make a node an ancestor of itself is undefined.
     * Some implementations may detect this condition and throw an
     * {@link java.lang.IllegalArgumentException}
     * @param nodeId
     *            node to move
     * @param parentId
     *            new parent of the node
     * @throws NodeNotFoundException
     *             if <code>nodeId</code> or <code>parentId</code> is missing from the tree
     * @return Key of the moved node (useful with position-dependent keys)
     */
    public Key move(Key nodeId, Key parentId) throws NodeNotFoundException;


    /**
     * Move a node in the tree. Some implementations may not support moves to other positions than
     * {@link #DEFAULT_POSITION DEFAULT_POSITION}. The 0-based target position denotes the position
     * in a child list from which the node has been removed. I.e., if the node <i>p</i> has the
     * child list <i>a,b,c</i>, then <code>move(<i>b</i>,<i>p</i>,1)</code> will yield the child
     * list <i>b,a,c</i>, and not <i>a,b,c</i>.
     * <p>
     * <b>Note:</b> The result of a move that would make a node an ancestor of itself is undefined.
     * Some implementations may detect this condition and throw an
     * {@link java.lang.IllegalArgumentException}
     * @param nodeId
     *            node to move
     * @param parentId
     *            new parent of the node
     * @param pos
     *            node target position among its siblings, with 0 signifying before the first child
     *            and {@link #DEFAULT_POSITION DEFAULT_POSITION} at the default position.
     * @return Key of the moved node (useful with position-dependent keys)
     * @throws NodeNotFoundException
     *             if <code>nodeId</code> or <code>parentId</code> is missing from the tree
     */

    public abstract Key move(Key nodeId, Key parentId, long pos) throws NodeNotFoundException;


    /**
     * Update content of a node.
     * @param nodeId
     *            id of node whose content is updated
     * @param content
     *            new content of the node
     * @return boolean <code>true</code> if the content was updated (in the case that
     *         <code>!oldContent.equals(content)</code>)
     * @throws NodeNotFoundException
     *             if <code>nodeId</code> is not in the tree
     */
    public abstract boolean update(Key nodeId, Object content) throws NodeNotFoundException;

}
// arch-tag: e748e93a331f0e85d49c98e1545f0e49 *-
