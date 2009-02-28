/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import fc.util.Util;
import fc.xml.xmlr.model.NodeModel;

/**
 * Default implementation of {@link RefTreeNode}. The class provides a space-efficient default
 * implementation for reftree nodes. The implementation does not allow non-reference nodes to have a
 * <code>null</code> content.
 */

public class RefTreeNodeImpl implements RefTreeNode {

    private Object content = null;
    private Key id;
    private RefTreeNode parent;

    protected List children = null; // new LinkedList();


    /**
     * Create a new node from template.
     * @param parent
     *            parent reftree node, or <code>null</code> if none
     * @param id
     *            unique id of the node
     * @param values
     *            template reftreenode to copy (shallow)
     */

    public RefTreeNodeImpl(RefTreeNode parent, Key id, RefTreeNode values) {
        // BUGFIX-20060928-2: bad init in case values was a ref
        this.parent = parent;
        this.id = id;
        this.content = values.isReference() ? values.getReference() : values.getContent();
    }


    /**
     * Create a new ordinary (non-reference) node.
     * @param parent
     *            parent reftree node, or <code>null</code> if none
     * @param id
     *            unique id of the node
     * @param content
     *            content object for the node, must not be <code>null</code>
     */

    public RefTreeNodeImpl(RefTreeNode parent, Key id, Object content) {
        assert content != null;
        this.id = id;
        this.parent = parent;
        this.content = content;
    }


    /**
     * Create a new node. A <code>null</code> content implies that the node is a reference, in which
     * case the <code>isTreeRef</code> flag determines if the reference is a node or tree reference.
     * @deprecated content should give ref status
     * @param parent
     *            parent reftree node, or <code>null</code> if none
     * @param id
     *            unique id of the node
     * @param isTreeRef
     *            <code>true</code> if the node is a tree reference
     * @param content
     *            content of node, or <code>null</code> if the node is a reference node.
     */
    @Deprecated
    public RefTreeNodeImpl(RefTreeNode parent, Key id, boolean isTreeRef, Object content) {
        // Below: die if TreeRef content but isTreeRef=false
        assert !(content instanceof NodeReference);
        assert !(content instanceof TreeReference) || isTreeRef;
        assert !isTreeRef || content == null;
        this.id = id;
        this.parent = parent;
        if (isTreeRef) content = new TreeReference(id);
        else if (content == null) content = new NodeReference(id);
        this.content = content;
    }


    // RefTreeNode

    public Iterator getChildIterator() {
        return children == null ? Collections.EMPTY_LIST.iterator() : children.iterator();
    }


    public Object getContent() {
        return content instanceof Reference ? null : content;
    }


    public Key getId() {
        return id;
    }


    public RefTreeNode getParent() {
        return parent;
    }


    public boolean isNodeRef() {
        return content instanceof NodeReference;
    }


    public Reference getReference() {
        return content instanceof Reference ? (Reference) content : null;
    }


    public boolean isReference() {
        return content instanceof Reference;
    }


    public boolean isTreeRef() {
        return content instanceof TreeReference;
    }


    // Extra stuff

    /**
     * Set content of node.
     * @param content
     *            content of node, or <code>null</code> to make the node a reference node, in which
     *            case the <code>isTreeRef</code> flag determines if the reference is a node or tree
     *            reference.
     */

    public void setContent(Object content) {
        this.content = content;
    }


    /**
     * Set to <code>true</code> to make the node a tree reference.
     * @param treeRef
     *            boolean
     */

    public void setTreeRef(boolean treeRef) {
        content = new TreeReference(id);
    }


    /**
     * Set node parent.
     * @param parent
     *            new parent of the node, or <code>null</code> if none.
     */
    public void setParent(RefTreeNode parent) {
        this.parent = parent;
    }


    /**
     * Append node to the child list. As a side effect, the parent of the child is set to this node.
     * @param n
     *            child node to append
     */
    public void addChild(RefTreeNodeImpl n) {
        ensureChildList().add(n);
        n.setParent(this);
    }


    public void addChild(int ix, RefTreeNodeImpl child) {
        ensureChildList().add(ix, child);
        child.parent = this;
    }


    /**
     * Remove node from child list. As a side effect, the parent of the node is set to
     * <code>null</code>.
     * @param n
     *            node to remove
     * @return <code>true</code> if the node was found and removed from the child list
     */
    public boolean removeChild(RefTreeNodeImpl n) {
        ensureChildList();
        if (children.remove(n)) {
            n.setParent(null);
            return true;
        }
        return false;
    }


    /**
     * Get first child in child list.
     * @return first child in child list, or <code>null</code> if the node has no children.
     */

    public RefTreeNodeImpl firstChild() {
        if (children == null || children.size() == 0) return null;
        return (RefTreeNodeImpl) children.get(0);
    }


    /**
     * Get last child in child list.
     * @return last child in child list, or <code>null</code> if the node has no children.
     */
    /*
     * public RefTreeNodeImpl lastChild() { if( children == null || children.size() == 0 ) return
     * null; return (RefTreeNodeImpl) children.get(children.size()-1); }
     */

    /**
     * Return number of children.
     * @return number of children
     */

    public int childCount() {
        return children == null ? 0 : children.size();
    }


    public RefTreeNode getChild(int i) {
        return (RefTreeNode) ensureChildList().get(i);
    }


    /**
     * Compare nodes for equality. Requires the content object to implement
     * {@link java.lang.Object#equals} properly.
     */

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RefTreeNode)) return false;
        RefTreeNode n = (RefTreeNode) obj;
        return Util.equals(n.getId(), id) && n.isNodeRef() == isNodeRef() &&
               n.isTreeRef() == isTreeRef() && Util.equals(n.getContent(), getContent());
    }


    @Override
    public int hashCode() {
        return (id == null ? 0 : id.hashCode()) ^ (content == null ? 0 : content.hashCode());
    }


    protected final List ensureChildList() {
        if (children == null) children = makeList();
        return children;
    }


    protected List makeList() {
        return new LinkedList();
    }

    public static final NodeModel NODE_MODEL = new NodeModel() {

        public RefTreeNode build(RefTreeNode parent, Key key, Object content, int pos) {
            RefTreeNodeImpl n = new RefTreeNodeImpl(null, key, content);
            if (parent != null) {
                if (pos != MutableRefTree.DEFAULT_POSITION) ((RefTreeNodeImpl) parent).addChild(
                                                                                                pos,
                                                                                                n);
                else ((RefTreeNodeImpl) parent).addChild(n);
            }
            return n;
        }
    };

    /*
     * public String toString() { String hashStr = Long.toString( 0x0ffffffffl&hashCode(),16);
     * hashStr = "00000000".substring(hashStr.length())+hashStr; return "[RNI"+hashStr+"]"
     * +String.valueOf(id)+": "+getContent(); }
     */
}
// arch-tag: 63bbd5d1c5ea7345aa8a040d30d38304 *-
