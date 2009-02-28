/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

// $Id: Merge.java,v 1.6 2004/11/24 16:08:21 ctl Exp $
package fc.xml.xmlr.tdm;

import java.io.Flushable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import tdm.lib.ConflictLog;
import tdm.lib.MatchArea;
import tdm.lib.MatchedNodes;
import tdm.lib.Node;
import fc.util.Stack;
import fc.util.log.Log;
import fc.util.log.LogLevels;
import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.ItemList;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.XasUtil;
import fc.xml.xas.compat.SaxToItems;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeImpl;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTreeNodeImpl;
import fc.xml.xmlr.model.StringKey;
import fc.xml.xmlr.model.XasCodec;
import fc.xml.xmlr.xas.RefItem;
import fc.xml.xmlr.xas.XasSerialization;

/**
 * Three-way merging of reftrees using the <code>3dm</code> algorithm. The class implements
 * three-way merging of reftrees. Merging is performed by the <code>tdm.lib.Merge</code> algorithm.
 * The input trees normally need to be normalized in order to ensure that all nodes that need to be
 * visited during the merge are expanded.
 */

public class Merge {

    /**
     * Default merger for nodes which always fails. Recall that the node merger only gets called
     * when both branches are modified, so this merger is in fact not as useless as it sounds.
     */
    public static final NodeMerger DEFAULT_NODE_MERGER = new NodeMerger() {

        public RefTreeNode merge(RefTreeNode base, RefTreeNode n1, RefTreeNode n2) {
            return null; // No dice
        }
    };

    private static RuntimeException NO_SUCH_OP = new UnsupportedOperationException();

    protected Map<Key, RefTreeNode> baseIx = new HashMap<Key, RefTreeNode>();
    protected Map<Key, BranchNode> leftIx = new HashMap<Key, BranchNode>();
    protected Map<Key, BranchNode> rightIx = new HashMap<Key, BranchNode>();
    protected BaseNode t0r;
    protected BranchNode t1r;
    protected BranchNode t2r;
    protected NodeMerger nm;


    private Merge() {
    }


    /**
     * Merge reftrees. Perform a 3dm three-way merge of the input reftrees.
     * @param base
     *            base tree
     * @param t1
     *            first changed tree
     * @param t2
     *            second changed tree
     * @param log
     *            XML serializer that receives the <code>3dm</code> conflict log
     * @param nodeMerger
     *            node merger
     * @return RefTree merged tree
     * @throws IOException
     *             if an I/O error occurs during merge
     */
    public static RefTree merge(IdAddressableRefTree base, IdAddressableRefTree t1,
                                IdAddressableRefTree t2, ItemTarget log, NodeMerger nodeMerger)
            throws IOException {
        Merge m = new Merge(base, t1, t2, null, null, nodeMerger);
        RefTreeExternalizer ext = new RefTreeExternalizer();
        m.merge(log, ext);
        return ext.getTree();
    }


    /**
     * Merge reftrees. Perform a 3dm three-way merge of the input reftrees. The merged tree
     * undergoes a XAS serialization and deserialization cycle. This cycle allows e.g. populating
     * the merged tree with suitable content objects (via the given codec).
     * @param base
     *            base tree
     * @param t1
     *            first changed tree
     * @param t2
     *            second changed tree
     * @param log
     *            XML serializer that receives the <code>3dm</code> conflict log
     * @param cc
     *            content codec for conversion back and forth between XAS event sequences.
     * @param nodeMerger
     *            node merger
     * @return RefTree merged tree
     * @throws IOException
     *             if an I/O error occurs during merge
     */

    public static RefTree merge(IdAddressableRefTree base, IdAddressableRefTree t1,
                                IdAddressableRefTree t2, ItemTarget log, XasCodec cc,
                                NodeMerger nodeMerger) throws IOException {
        ItemList mergedItems = new ItemList();
        // merge(base,t1,t2,XasDebug.itemDump(),log,cc,nodeMerger);
        merge(base, t1, t2, mergedItems, log, cc, nodeMerger);
        return XasSerialization.readTree(XasUtil.itemSource(mergedItems), cc);
    }


    /**
     * Merge reftrees. Perform a 3dm three-way merge of the input reftrees. The merged tree is
     * streamed as XAS events as it is being constructed, and is not instantiated in memory.
     * @param base
     *            base tree
     * @param t1
     *            first changed tree
     * @param t2
     *            second changed tree
     * @param tm
     *            serializer that receives the merged tree
     * @param log
     *            XML serializer that receives the <code>3dm</code> conflict log
     * @param cw
     *            content codec for conversion back and forth between XAS event sequences.
     * @param nodeMerger
     *            node merger
     * @throws IOException
     *             if an I/O error occurs during merge
     */
    public static void merge(IdAddressableRefTree base, IdAddressableRefTree t1,
                             IdAddressableRefTree t2, ItemTarget tm, ItemTarget log, XasCodec cw,
                             NodeMerger nodeMerger) throws IOException {
        Merge m = new Merge(base, t1, t2, null, null, nodeMerger);
        /*
         * { // DEBUG Log.info("============MERGE========"); m.merge( new ItemList(), new
         * XasExternalizer(XasDebug.itemDump(),cw)); Log.info("============EOM========"); }
         */
        m.merge(log, new XasExternalizer(tm, cw));
    }


    protected Merge(IdAddressableRefTree base, IdAddressableRefTree t1, IdAddressableRefTree t2,
                    ItemTarget tm, ItemTarget log, NodeMerger nm) {
        // Get an unique fake root id. A little convoluted, but should work.
        Key fakeRootId = StringKey.createUniqueKey();
        /*
         * This is not necessary with uniqueKey for (; base.contains(fakeRootId) ||
         * t1.contains(fakeRootId) || t2.contains(fakeRootId); fakeRootId +=
         * Integer.toHexString(rnd.nextInt()));
         */
        RefTreeNode fakeRoot = new RefTreeNodeImpl(null, fakeRootId, new Object());
        t0r = new BaseNode(fakeRoot, base.getRoot(), baseIx);
        t1r = new BranchNode(fakeRoot, t1.getRoot(), baseIx, true);
        // BUGFIX-20061017-2: t1r was assigned twice->probably busted merge of
        // root
        t2r = new BranchNode(fakeRoot, t2.getRoot(), baseIx, false);
        this.nm = nm;
    }


    protected void merge(ItemTarget log, tdm.lib.XMLNode.Externalizer ext) throws IOException {
        try {
            tdm.lib.Merge m = new tdm.lib.Merge(null);
            /*
             * { PrintWriter pw = new PrintWriter(System.out); pw.println("Base"); t0r.debugTree(pw,
             * 0); pw.println("T1"); t1r.debugTree(pw, 0); pw.println("T2"); t2r.debugTree(pw, 0);
             * pw.flush(); }
             */
            m.treeMerge(t1r, t2r, ext, new NodeMergerProxy(nm));
            ConflictLog cl = m.getConflictLog();
            if (cl.hasConflicts()) {
                cl.writeConflicts(new SaxToItems(log));
            }
        } catch (Exception x) {
            Log.log("Merge ex", LogLevels.ERROR, x);
            if (x instanceof IOException) throw (IOException) x;
        }
    }

    public static class XasExternalizer implements tdm.lib.XMLNode.Externalizer {

        TagCloser wr;
        XasCodec cw;


        public XasExternalizer(ItemTarget wr, XasCodec cw) {
            this.wr = new TagCloser(wr);
            this.cw = cw;
        }


        public void startNode(tdm.lib.XMLNode xn) throws IOException {
            // Log.info("Open", ((XMLNode) xn).getRefTreeNode());
            RefTreeNode n = ((XMLNode) xn).getRefTreeNode();
            if (n.isReference()) wr.append(RefItem.makeStartItem(n.getReference(), wr.getContext()));
            else {
                wr.mark();
                cw.encode(wr, n, wr.getContext());
            }
        }


        public void endNode(tdm.lib.XMLNode xn) throws IOException {
            // Log.info("Close"/*((XMLNode) xn).getRefTreeNode()*/);
            RefTreeNode n = ((XMLNode) xn).getRefTreeNode();
            if (n.isReference()) {
                if (n.isNodeRef()) wr.append(RefItem.makeEndItem(n.getReference()));
            } else wr.close();
        }

        private static class TagCloser implements ItemTarget {

            ItemTarget t;
            StartTag context = null;
            Qname MARKER = new Qname("", "");
            Stack<Item> closing = new Stack<Item>();


            public TagCloser(ItemTarget t) {
                this.t = t;
            }


            public void append(Item i) throws IOException {
                if (Item.isStartTag(i)) {
                    StartTag st = (StartTag) i;
                    context = st;
                    closing.push(st);
                } else if (RefItem.isRefItem(i)) {
                    RefItem ri = (RefItem) i;
                    context = ri.getContext();
                    closing.push(ri);
                }
                t.append(i);
                // Log.info("Emitting",i);
            }


            public void mark() {
                closing.push(new StartTag(MARKER, context));
            }


            public void close() throws IOException {
                for (Item i = null; !closing.isEmpty() && (i = closing.pop()) != null;) {
                    if (RefItem.isRefItem(i)) continue;
                    StartTag st = (StartTag) i;
                    if (st.getName() == MARKER) break;
                    t.append(new EndTag(st.getName()));
                }
                Item ci = !closing.isEmpty() ? closing.peek() : null;
                context = ci == null || Item.isStartTag(ci) ? (StartTag) ci
                        : ((RefItem) ci).getContext();
            }


            public void flush() throws IOException {
                if (t instanceof Flushable) ((Flushable) t).flush();
            }


            public StartTag getContext() {
                return context;
            }
        }
    }

    static class RefTreeExternalizer implements tdm.lib.XMLNode.Externalizer {

        RefTreeNode root;
        Stack parents = new Stack();


        public RefTreeExternalizer() {
        }


        public void startNode(tdm.lib.XMLNode xn) throws IOException {
            // Log.log("RTE-Open",Log.DEBUG,((XMLNode) xn).getRefTreeNode());
            RefTreeNodeImpl parent = parents.isEmpty() ? null : (RefTreeNodeImpl) parents.peek();
            RefTreeNode n = ((XMLNode) xn).getRefTreeNode();
            RefTreeNodeImpl emitted = new RefTreeNodeImpl(null, n.getId(), n);
            if (parent == null) root = emitted;
            else parent.addChild(emitted);
            parents.push(emitted);
        }


        public void endNode(tdm.lib.XMLNode xn) throws IOException {
            // Log.log("RTE-Close",Log.DEBUG,((XMLNode) xn).getRefTreeNode());
            parents.pop();
        }


        public RefTree getTree() {
            return new RefTreeImpl(root);
        }
    }

    /** Interface for custom node three-way mergers. */
    public interface NodeMerger {

        /**
         * Perform a three-way merge of reftree nodes. Only called if both <code>n1</code> and
         * <code>n2</code> differ from <code>base</code>.
         * @param base
         *            node in the base tree
         * @param n1
         *            node in the first variant tree (<code>t1</code> parameter of the various
         *            <code>merge</code> methods)
         * @param n2
         *            node in the second variant tree (<code>t2</code> parameter of the various
         *            <code>merge</code> methods)
         * @return merged node, or <code>null</code> if the node cannot be merged
         */
        RefTreeNode merge(RefTreeNode base, RefTreeNode n1, RefTreeNode n2);
    }

    class NodeMergerProxy implements tdm.lib.XMLNode.Merger {

        private NodeMerger nm;


        public NodeMergerProxy(NodeMerger nm) {
            this.nm = nm;
        }


        public tdm.lib.XMLNode merge(tdm.lib.XMLNode baseNode, tdm.lib.XMLNode aNode,
                                     tdm.lib.XMLNode bNode) {
            RefTreeNode merged = nm.merge(((XMLNode) baseNode).getRefTreeNode(),
                                          ((XMLNode) aNode).getRefTreeNode(),
                                          ((XMLNode) bNode).getRefTreeNode());
            return merged == null ? null : new XMLNode(merged);
        }
    }

    class BranchNode extends tdm.lib.BranchNode {

        protected boolean isLeft;
        protected RefTreeNode n;


        // base ix is map of id->baseNode
        public BranchNode(RefTreeNode root, RefTreeNode onlyChild, Map baseIx, boolean isLeft) {
            this.n = root;
            this.childPos = 0;
            this.parent = null;
            this.content = new XMLNode(n);
            this.baseMatch = (BaseNode) baseIx.get(n.getId());
            this.isLeft = isLeft;
            children.add(new BranchNode(onlyChild, 0, this, baseIx, isLeft));
            (isLeft ? leftIx : rightIx).put(n.getId(), this);
        }


        public BranchNode(RefTreeNode n, int childPos, BranchNode parent, Map baseIx, boolean isLeft) {
            this.n = n;
            this.childPos = childPos;
            this.parent = parent;
            this.content = new XMLNode(n);
            this.baseMatch = (BaseNode) baseIx.get(n.getId());
            this.isLeft = isLeft;
            int pos = 0;
            for (Iterator i = n.getChildIterator(); i.hasNext();) {
                children.add(new BranchNode((RefTreeNode) i.next(), pos++, this, baseIx, isLeft));
            }
            (isLeft ? leftIx : rightIx).put(n.getId(), this);
        }


        @Override
        public tdm.lib.BaseNode getBaseMatch() {
            return baseMatch;
        }


        @Override
        public int getBaseMatchType() {
            return getBaseMatch() == null ? 0 : MATCH_FULL;
        }


        @Override
        public tdm.lib.BranchNode getFirstPartner(int typeFlags) {
            // BUGFIX 20060920-4: returning partners when there is no base
            // match is not really correct (The idea was to merge identical
            // inserts, but that isn't really supported by the merge...)
            return !hasBaseMatch() ? null
                    : (BranchNode) (!isLeft ? leftIx : rightIx).get(n.getId());
        }


        @Override
        public MatchedNodes getPartners() {
            tdm.lib.MatchedNodes partners = new tdm.lib.MatchedNodes(baseMatch);
            if (hasBaseMatch()) partners.addMatch(getFirstPartner(MATCH_FULL));
            return partners;
        }


        @Override
        public boolean hasBaseMatch() {
            // BUGFIX 20060920-3: hasBaseMatch() returned true on same-id
            // inserts in both trees, although no base match, because we
            // checked the partner instead of the base match for null here.
            return getBaseMatch() != null;
        }


        @Override
        public boolean isLeftTree() {
            return isLeft;
        }


        /*
         * Base impl is ok for these: public Node getChildAsNode(int ix) public int getChildCount()
         * public int getChildPos() public tdm.lib.XMLNode getContent() public Node getLeftSibling()
         * public Node getRightSibling() public Node getParentAsNode() public boolean
         * hasLeftSibling() public boolean hasRightSibling() public tdm.lib.BranchNode getChild(int
         * ix) public tdm.lib.BranchNode getParent() public boolean isMatch(int type)
         */

        // Not needed
        @Override
        public void addChild(Node n) {
            throw NO_SUCH_OP;
        }


        @Override
        public void addChild(int ix, Node n) {
            throw NO_SUCH_OP;
        }


        @Override
        public void debug(PrintWriter pw, int indent) {
            throw NO_SUCH_OP;
        }


        @Override
        public void debugTree(PrintWriter pw, int indent) {
            /*
             * pw.println("                ".substring(15-indent)
             * +n+", "+n.getContent()+", basem="+getBaseMatch()); for(int i=0;i<getChildCount();i++)
             * getChild(i).debugTree(pw,indent+1);
             */
            throw NO_SUCH_OP;
        }


        @Override
        public MatchArea getMatchArea() {
            throw NO_SUCH_OP;
        }


        @Override
        public void removeChild(int ix) {
            throw NO_SUCH_OP;
        }


        @Override
        public void removeChildren() {
            throw NO_SUCH_OP;
        }


        @Override
        public void replaceChild(int ix, Node n) {
            throw NO_SUCH_OP;
        }


        @Override
        public void setContent(tdm.lib.XMLNode aContent) {
            throw NO_SUCH_OP;
        }


        @Override
        public void setMatchArea(MatchArea anArea) {
            throw NO_SUCH_OP;
        }


        @Override
        public void delBaseMatch() {
            throw NO_SUCH_OP;
        }


        @Override
        public void setBaseMatch(tdm.lib.BaseNode p, int amatchType) {
            throw NO_SUCH_OP;
        }


        @Override
        public void setMatchType(int amatchType) {
            throw NO_SUCH_OP;
        }


        @Override
        public void setPartners(MatchedNodes p) {
            throw NO_SUCH_OP;
        }

    }

    class BaseNode extends tdm.lib.BaseNode {

        RefTreeNode n;


        public BaseNode(RefTreeNode root, RefTreeNode onlyChild, Map baseIx) {
            this.n = root;
            this.childPos = 0;
            this.parent = null;
            this.content = new XMLNode(n);
            children.add(new BaseNode(onlyChild, 0, this, baseIx));
            baseIx.put(n.getId(), this);
        }


        // base ix is map of id->baseNode, filled in on create
        public BaseNode(RefTreeNode n, int childPos, BaseNode parent, Map baseIx) {
            this.n = n;
            this.childPos = childPos;
            this.parent = parent;
            this.content = new XMLNode(n);
            int pos = 0;
            for (Iterator i = n.getChildIterator(); i.hasNext();) {
                children.add(new BaseNode((RefTreeNode) i.next(), pos++, this, baseIx));
            }
            baseIx.put(n.getId(), this);
        }


        @Override
        public MatchedNodes getLeft() {
            tdm.lib.MatchedNodes matches = new tdm.lib.MatchedNodes(this);
            if (leftIx.containsKey(n.getId())) matches.addMatch(leftIx.get(n.getId()));
            return matches;
        }


        @Override
        public MatchedNodes getRight() {
            tdm.lib.MatchedNodes matches = new tdm.lib.MatchedNodes(this);
            if (rightIx.containsKey(n.getId())) matches.addMatch(rightIx.get(n.getId()));
            return matches;
        }


        /*
         * Base impl is ok for these: public Node getChildAsNode(int ix) public int getChildCount()
         * public int getChildPos() public tdm.lib.XMLNode getContent() public Node getLeftSibling()
         * public Node getRightSibling() public Node getParentAsNode() public boolean
         * hasLeftSibling() public boolean hasRightSibling() public tdm.lib.BaseNode getChild(int
         * ix) public tdm.lib.BaseNode getParent()
         */

        // Not needed
        @Override
        public void addChild(Node n) {
            throw NO_SUCH_OP;
        }


        @Override
        public void addChild(int ix, Node n) {
            throw NO_SUCH_OP;
        }


        @Override
        public void debug(PrintWriter pw, int indent) {
            throw NO_SUCH_OP;
        }


        @Override
        public void debugTree(PrintWriter pw, int indent) {
            /*
             * pw.println("                ".substring(15-indent)+ this+": "
             * +n+", "+n.getContent()); for(int i=0;i<getChildCount();i++)
             * getChild(i).debugTree(pw,indent+1);
             */
            throw NO_SUCH_OP;
        }


        @Override
        public MatchArea getMatchArea() {
            throw NO_SUCH_OP;
        }


        @Override
        public void removeChild(int ix) {
            throw NO_SUCH_OP;
        }


        @Override
        public void removeChildren() {
            throw NO_SUCH_OP;
        }


        @Override
        public void replaceChild(int ix, Node n) {
            throw NO_SUCH_OP;
        }


        @Override
        public void setContent(tdm.lib.XMLNode aContent) {
            throw NO_SUCH_OP;
        }


        @Override
        public void setMatchArea(MatchArea anArea) {
            throw NO_SUCH_OP;
        }


        @Override
        public void swapLeftRightMatchings() {
            throw NO_SUCH_OP;
        }
    }

    class XMLNode extends tdm.lib.XMLNode {

        RefTreeNode n;


        public XMLNode(RefTreeNode n) {
            this.n = n;
        }


        @Override
        public boolean contentEquals(Object a) {
            return n.equals(((XMLNode) a).n);
        }


        @Override
        public int getContentHash() {
            // Not needed in 3dm merge phase
            throw new UnsupportedOperationException();
        }


        @Override
        public int getInfoSize() {
            // Not needed in 3dm merge phase
            throw new UnsupportedOperationException();
        }


        public RefTreeNode getRefTreeNode() {
            return n;
        }

        /*
         * public String toString() { return "XN: "+n+", n.c="+n.getContent(); }
         */
    }
}
// arch-tag: 0e4e430cd54276cf7046828184d8e8b8 *-
