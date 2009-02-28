/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.model;

import fc.xml.xmlr.Key;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTreeNodeImpl;

/** Model for adding nodes to a tree. */
public interface NodeModel {

    /**
     * Add a new node to a tree.
     * @param parent
     *            parent node, or <code>null</code> if this is a new root
     * @param key
     *            key of the node
     * @param content
     *            content of the node. May be an instance of {@link fc.xml.xmlr.Reference}, in which
     *            case the node should be a reference node
     * @param pos
     *            position in the child list of <i>parent</i>. May be
     *            {@link MutableRefTree#DEFAULT_POSITION}.
     * @return new node
     */
    public RefTreeNode build(RefTreeNode parent, Key key, Object content, int pos);

    /**
     * Default node model. Builds {@link RefTreeNodeImpl} nodes.
     */

    public static final NodeModel DEFAULT = new NodeModel() {

        public RefTreeNode build(RefTreeNode parent, Key key, Object content, int pos) {
            assert parent == null || parent instanceof RefTreeNodeImpl;
            RefTreeNodeImpl p = (RefTreeNodeImpl) parent;
            RefTreeNodeImpl n = new RefTreeNodeImpl(p, key, content);
            if (p == null) return n; // Inserting root
            if (pos == MutableRefTree.DEFAULT_POSITION) p.addChild(n);
            else p.addChild(pos, n);
            return n;
        }

    };
}

// arch-tag: c1b243d9-7156-4599-bab2-68f8fb7ed7af
