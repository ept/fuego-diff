/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.xas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fc.util.log.Log;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.Reference;
import fc.xml.xmlr.model.NodeModel;

// The impl is not very efficient with ins/delete in the middle of a
// childList (due to ArrayList & explicit child positions
/**
 * Reftree node keyed by Dewey keys. Nodes know their position in the parent's child list, so keys
 * are very fast to compute. Child lists are implemented using {@link java.util.ArrayList}, with the
 * performance implications of that list.
 */
public class DeweyRefNode implements RefTreeNode {

    private Object content;
    private DeweyRefNode parent;
    private int childPos = -1;
    private List<DeweyRefNode> children = null;


    /** Create unattached node. */
    public DeweyRefNode(Object content) {
        this.content = content;
    }


    /**
     * Create node.
     * @param parent
     *            parent node
     * @param childPos
     *            position in child list of parent
     * @param content
     *            node content.
     */

    public DeweyRefNode(DeweyRefNode parent, int childPos, Object content) {
        assert childPos >= 0;
        assert parent != null : "The root should have a PrefixNode, not null!";
        this.content = content;
        this.parent = parent;
        this.childPos = childPos;
    }


    public Key getId() {
        if (this.parent == null) return null;
        DeweyRefNode prefix = this;
        int depth = -1;
        for (; prefix.parent != null; prefix = prefix.parent)
            depth++;
        assert prefix instanceof DeweyRefNode.PrefixNode : "Expected a prefix here, got " +
                                                           prefix.getClass() + "childPos is " +
                                                           childPos;
        int[] key = new int[depth];
        // Below: p.parent is a pathological case (detached node)
        for (DeweyRefNode p = this; p.parent != null && p.parent.parent != null; p = p.parent)
            key[--depth] = p.childPos;
        return ((DeweyKey) prefix.getId()).append(key);
    }


    public RefTreeNode getParent() {
        return parent;
    }


    public Object getContent() {
        return isReference() ? null : content;
    }


    public Iterator getChildIterator() {
        return ensureChildren().iterator();
    }


    public boolean isReference() {
        return content instanceof Reference;
    }


    public boolean isTreeRef() {
        return isReference() && ((Reference) content).isTreeReference();
    }


    public boolean isNodeRef() {
        return isReference() && !((Reference) content).isTreeReference();
    }


    public Reference getReference() {
        return isReference() ? (Reference) content : null;
    }


    protected List<DeweyRefNode> ensureChildren() {
        if (children == null) children = new ArrayList<DeweyRefNode>();
        return children;
    }


    public int childCount() {
        return ensureChildren().size();
    }


    public RefTreeNode get(int i) {
        return ensureChildren().get(i);
    }


    public void addChild(DeweyRefNode child) {
        assert child != null;
        assert child != this; // Circular relationship!
        child.childPos = ensureChildren().size();
        children.add(child);
        child.parent = this;
    }


    public void addChild(int ix, DeweyRefNode child) {
        assert child != null;
        assert child != this; // Circular relationship!
        ensureChildren().add(ix, child);
        // Shift childpos of successors
        for (int i = ix + 1; i < children.size(); i++)
            children.get(i).childPos = i;
        child.childPos = ix;
        child.parent = this;
    }


    public boolean removeChild(DeweyRefNode child) {
        assert ensureChildren().get(child.childPos) == child;
        ensureChildren().remove(child.childPos);
        for (int i = child.childPos; i < children.size(); i++)
            children.get(i).childPos = i;
        child.parent = null;
        child.childPos = -1;
        return true;
    }


    public int getPosition() {
        return childPos;
    }


    public void setContent(Object content) {
        this.content = content;
    }


    void detach() {
        if (parent != null) {
            parent.children.set(childPos, null);
            parent = null;
            childPos = -1;
        }
    }


    void attach(DeweyRefNode parent) {
        if (parent != null) {
            parent.addChild(this);
        } else {
            assert false : "Cannot attach to null";
            // parent = null;
            // childPos=0;
        }
    }

    public static class PrefixNode extends DeweyRefNode {

        private DeweyKey prefix;


        public PrefixNode(DeweyKey prefix) {
            super(null);
            // Log.debug("The prefix key is "+prefix, new Throwable());
            this.prefix = prefix;
        }


        @Override
        public Key getId() {
            return prefix;
        }

    }

    /**
     * Node model for this class. Root will be keyed with <code>/</code>
     */

    public static NodeModel NODE_MODEL = new DeweyNodeModel(DeweyKey.ROOT_KEY);

    /**
     * Node model for this class, with root keyed as <code>/0</code>. In many cases (XML documents
     * with document root as tree root), it is useful to key the root as <code>/0</code>.
     */

    public static NodeModel NODE_MODEL_ALT = new DeweyNodeModel(DeweyKey.ROOT_KEY.down());

    private static class DeweyNodeModel implements NodeModel {

        private DeweyKey pk;


        public DeweyNodeModel(DeweyKey rootKey) {
            pk = rootKey;
        }


        public RefTreeNode build(RefTreeNode parent, Key key, Object content, int pos) {
            DeweyRefNode dp = (DeweyRefNode) parent;
            DeweyRefNode n = new DeweyRefNode(dp != null ? dp : new DeweyRefNode.PrefixNode(pk), 0,
                                              content);
            if (dp != null) {
                if (pos == MutableRefTree.DEFAULT_POSITION) dp.addChild(n);
                else dp.addChild(pos, n);
            }
            return n;
        }

    }

}

// arch-tag: 0228f1b4-4efe-4927-853d-c8c71a4f2409
