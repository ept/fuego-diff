/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

// $Id: Diff.java,v 1.13 2004/12/27 13:45:42 ctl Exp $
package fc.xml.xmlr.tdm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Stack;

import tdm.lib.DiffAlgorithm;
import fc.util.log.Log;
import fc.xml.xas.AttributeNode;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.TransformSource;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.NodeReference;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeImpl;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTreeNodeImpl;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.Reference;
import fc.xml.xmlr.TreeReference;
import fc.xml.xmlr.model.IdentificationModel;
import fc.xml.xmlr.model.KeyIdentificationModel;
import fc.xml.xmlr.model.KeyModel;
import fc.xml.xmlr.model.StringKey;
import fc.xml.xmlr.model.TransientKey;
import fc.xml.xmlr.model.TreeModel;
import fc.xml.xmlr.model.TreeModels;
import fc.xml.xmlr.model.XasCodec;
import fc.xml.xmlr.xas.DeweyKey;
import fc.xml.xmlr.xas.PeekableItemSource;
import fc.xml.xmlr.xas.RefItem;
import fc.xml.xmlr.xas.RefNodeItem;
import fc.xml.xmlr.xas.RefTreeItem;
import fc.xml.xmlr.xas.ReferenceItemTransform;

/**
 * Bridge between the reftrees and the <code>3dm</code> diff format. The <code>3dm</code> diff
 * format and reftrees have in common the encoding of subtrees by using references to other trees.
 * This class provides functionality for quickly converting between these formats without expensive
 * subtree expansion or node matching. There are, however, some limitations, stemming from the
 * differences in the formats. These limitations can always be bypassed by executing the standard
 * <code>3dm</code> diff algorithm on expanded or (better) normalized input reftrees.
 * <p>
 * <b>Note:</b> The implementation is somewhat hampered by the lack (as of 2004/11/23) of a fixed
 * <code>3dm</code> diff format, in the sense that the tags and namespaces outputted may not be
 * compatible with future versions of <code>3dm</code>.
 */

public class Diff implements RefTree {

    /** Default prefix for diff namespace. */
    public static final String DIFF_NS_PREFIX = "diff";

    /** Namespace for diff tags. */
    public static final String DIFF_NS = "http://www.hiit.fi/fc/xml/tdm/diff";

    /** Name of "copy tree" tag. */
    public static final Qname DIFF_COPY_TAG = new Qname(DIFF_NS, "copy");
    /** Name of "insert tree" tag. */
    public static final Qname DIFF_INS_TAG = new Qname(DIFF_NS, "insert");
    /** Name of the diff root tag. */
    public static final Qname DIFF_ROOT_TAG = new Qname(DIFF_NS, "diff");

    /** Name of the default root operation. */
    public static final String DIFF_ROOTOP_INS = "insert";

    /** Namespace of diff attributes. */
    public static final String ATT_NS = "";

    /** Name of the source address attribute. */
    public static final Qname DIFF_CPYSRC_ATTR = new Qname(ATT_NS, "src");
    /** Name of the destination address attribute. */
    public static final Qname DIFF_CPYDST_ATTR = new Qname(ATT_NS, "dst");
    /** Name of the copy run length. */
    public static final Qname DIFF_CPYRUN_ATTR = new Qname(ATT_NS, "run");
    /** Name of the root operation attribute. */
    public static final Qname DIFF_ROOTOP_ATTR = new Qname(ATT_NS, "op");

    protected RefTreeNode root = null;


    // hidden so far...
    protected Diff() {
    }


    /**
     * Encode a reftree as a diff. In essence, the algorithm replaces tree references with
     * corresponding diff copy tags. Child lists of copy tags that refer to consecutive nodes in a
     * child list in the base tree are run-length encoded using the <code>run</code> attribute.
     * <p>
     * Note that node references and expanded nodes will be copied verbatim to the output, i.e. the
     * size of the diff is highly dependent on the efficient use of tree references in the input
     * tree.
     * <p>
     * Note 2: all references in the refTree are assumed to be valid
     * </p>
     * @param refTree
     *            reftree to encode
     * @param base
     *            tree containing nodes referenced in <code>refTree</code>
     * @throws IOException
     *             if encoding fails
     * @return the encoded diff
     */
    public static Diff encode(IdAddressableRefTree base, RefTree refTree) throws IOException {
        Diff d = new Diff();
        (d.new RefTreeDiffer(base, null)).runDiff(refTree.getRoot());
        return d;
    }


    /**
     * Encode a reftree as a diff. In essence, the algorithm replaces tree references with
     * corresponding diff copy tags. Child lists of copy tags that refer to consecutive nodes in a
     * child list in the base tree are run-length encoded using the <code>run</code> attribute. This
     * variant checks for consecutiveness using a {@link SequenceTester} rather than by using the
     * actual base tree. Thus, this encoder does not need the base tree at all, as long as the
     * consecutiveness of ids may be determined by some other means.
     * <p>
     * Note that node references and expanded nodes will be copied verbatim to the output, i.e. the
     * size of the diff is highly dependent on the efficient use of tree references in the input
     * tree.
     * <p>
     * Note 2: all references in the refTree are assumed to be valid
     * </p>
     * @param refTree
     *            reftree to encode
     * @param st
     *            algorithm for testing if two nodes from <code>refTree</code> are in sequence in
     *            the base tree.
     * @return the encoded diff
     * @throws IOException
     *             if encoding fails
     */
    public static Diff encode(RefTree refTree, SequenceTester st) throws IOException {
        Diff d = new Diff();
        (d.new RefTreeDiffer(null, st)).runDiff(refTree.getRoot());
        return d;
    }


    /**
     * Decode diff. In essence, the algorithm replaces the diff <code>copy</code> tags in the input
     * with tree references to the base tree. This particular implementations constructs a reftree
     * which is lazily constructed, e.g. <code>&lt;copy src="xxx" run="nnn" /&gt;</code> wont be
     * expanded to a run of tree references until the corresponding nodes are visited.
     * <p>
     * Note that the algorithm cannot decode generic <code>3dm</code> diffs; <b>only those diffs
     * which copy full rather than truncated subtrees may be decoded</b>. In other words, the
     * <code>copy</code> tags in the diff may not have any children. Note that
     * {@link #encode(IdAddressableRefTree, RefTree) encode} never produces such undecodable diffs.
     * @param base
     *            base tree to resolve references against
     * @return RefTree decoded reftree
     * @throws IOException
     *             if decoding fails
     */

    public RefTree decode(IdAddressableRefTree base) throws IOException {
        Object c = getRoot().getContent();
        RefTreeNode rtroot = null;
        if (c instanceof DiffOperation &&
            ((DiffOperation) c).getOperation() == DiffOperation.ROOT_INSERT) {
            Iterator insTree = getRoot().getChildIterator();
            if (!insTree.hasNext())
                throw new DiffFormatException("Diff encodes empty XML document");
            RefTreeNode diffRoot = (RefTreeNode) insTree.next();
            if (diffRoot.getContent() instanceof DiffOperation) {
                // This takes care of the case <diff rootop=ins><copy id=nn
                // run=1></diff>
                DiffOperation ro = (DiffOperation) diffRoot.getContent();
                if (ro.getOperation() != DiffOperation.COPY ||
                    ((Number) ro.getRun()).longValue() != 1)
                // I guess we could allow diff:insert, but that is really
                    // redundant
                    throw new DiffFormatException("Invalid diff operation at XPath /0/0: ");
                diffRoot = new RefTreeNodeImpl(null, new TransientKey(),
                                               new TreeReference(ro.getSource()));
            }
            rtroot = new DelayedRefTreeNode(diffRoot, base);
        } else if (c instanceof DiffOperation &&
                   ((DiffOperation) c).getOperation() == DiffOperation.ROOT_COPY) {
            // Iterator tree = new CopySequenceIterator((DiffOperation)
            // c,null,base);
            rtroot = new RefTreeNodeImpl(null, new TransientKey(),
                                         new TreeReference(((DiffOperation) c).getSource()));
        } else throw new DiffFormatException("Invalid diff root tag");
        return new RefTreeImpl(rtroot);
    }


    /**
     * Write diff as XML. The method differs from
     * {@link fc.xml.xmlr.xas.XasSerialization#writeTree(RefTree, ItemTarget, TreeModel) writeTree}
     * in the respect that it knows how to serialize nodes that represent 3dm diff tags.
     * @param t
     *            item target to output the diff to
     * @param tm
     *            target tree model
     * @throws IOException
     *             if writing fails
     */

    // NOTE: Do not make other methods for convenience; it is better that
    // people use getDiffModel()...
    public void writeDiff(ItemTarget t, TreeModel tm) throws IOException {
        fc.xml.xmlr.xas.XasSerialization.writeTree(this, t,
                                                   new DiffTreeModel(tm, this.getRoot().getId()),
                                                   tm);
    }


    /**
     * Deserialize Fuego diff from XML. Deserializes the canonical Fuego Diff format, where keys are
     * Dewey keys, and may be relative (i.e. <code>&lt;copy src="./1" &gt;</code> could appear in
     * the diff), and where the key of the document start item is <code>/</code>.
     * <p>
     * The method is a wrapper to the more general {@link #readDiff(ItemSource, TreeModel, Key )
     * readDiff}, which it provides with the appropriate decoding of relative Dewey Keys and tree
     * model.
     * @param is
     *            item source to read diff from
     * @throws IOException
     * @return Diff
     */

    public static Diff readDiff(ItemSource is) throws IOException {
        return readDiff(new TransformSource(new TransformSource(is, new ReferenceItemTransform()),
                                            new RelativeDeweyKeyExpander()),
                        TreeModels.xasItemTree(), DeweyKey.ROOT_KEY);
    }


    /**
     * Deserialize diff from XML. Deserializes a diff generated by this class or a <code>3dm</code>
     * diff (with some restrictions, see {@link #decode decode}) as a {@link Diff} object. The
     * method differs from {@link fc.xml.xmlr.xas.XasSerialization#readTree(ItemSource, TreeModel)
     * readTree} in the respect that it knows how to deserialize nodes that represent diff tags.
     * @param is
     *            item source to read diff from
     * @param tm
     *            destination tree tree model
     * @param baseRootId
     *            key of base root tag
     * @throws IOException
     * @return Diff
     */

    public static Diff readDiff(ItemSource is, TreeModel tm, Key baseRootId) throws IOException {
        Diff d = new Diff();
        TreeModel dtm = new TreeModel(tm.getKeyModel(),
                                      getDiffIdentificationModel(tm.getIdentificationModel()),
                                      getDiffCodec(tm.getCodec(), baseRootId), tm.getNodeModel());
        RefTree t = fc.xml.xmlr.xas.XasSerialization.readTree(is, dtm);
        d.root = t.getRoot();
        return d;
    }


    /**
     * Get codec that handles diff tags.
     * @param codec
     *            underlying codec for non-diff tags
     * @return diff codec
     */
    public XasCodec getDiffCodec(XasCodec codec) {
        return getDiffCodec(codec, getRoot().getId());
    }


    /**
     * Get codec that handles diff tags.
     * @param codec
     *            underlying codec for non-diff tags
     * @param rootId
     *            key of root tag
     * @return diff codec
     */
    public static XasCodec getDiffCodec(XasCodec codec, Key rootId) {
        return new DiffTreeModel(codec, rootId);
    }


    private static IdentificationModel getDiffIdentificationModel(final IdentificationModel m) {
        return new IdentificationModel() {

            public Key identify(Item i, KeyModel km) throws IOException {
                if (i.getType() == Item.START_TAG &&
                    DIFF_NS.equals(((StartTag) i).getName().getNamespace())) return new TransientKey();
                else return m.identify(i, km);
            }


            public Item tag(Item i, Key k, KeyModel km) {
                if (i.getType() == Item.START_TAG &&
                    DIFF_NS.equals(((StartTag) i).getName().getNamespace())) return i;
                else return m.tag(i, k, km);
            }

        };

    }


    // Reftree iface ----

    public RefTreeNode getRoot() {
        return root;
    }

    // Get id's for RefTreeNodes and Diffops (by srcid)
    // no src -> null (should never occur in the simpler diff format we use
    // here,
    // where <ins> tags are never used

    /** Differencing algorithm implementation */
    protected class RefTreeDiffer extends DiffAlgorithm {

        private RefTreeNodeImpl currentNode = null;
        private IdAddressableRefTree base;
        private SequenceTester st;
        private Map successors = new HashMap();


        public RefTreeDiffer(IdAddressableRefTree base, SequenceTester st) {
            if (base != null && st != null) Log.log("Either must be null", Log.ASSERTFAILED);
            this.base = base;
            this.st = st;
        }


        public void runDiff(RefTreeNode root) throws IOException {
            diff(root);
        }


        protected List getStopNodes(Object changeNode) {
            RefTreeNode n = ((RefTreeNode) changeNode);
            List l = null;
            if (n.isTreeRef()) l = Collections.EMPTY_LIST;
            else {
                l = new ArrayList(1);
                l.add(changeNode);
            }
            return l;
        }


        protected Object lookupBase(Object changeNode) {
            RefTreeNode n = ((RefTreeNode) changeNode);
            if (n.isTreeRef()) {
                Key target = n.getReference().getTarget();
                return base.getNode(target);
            }
            return null;
            /*
             * Key id = n.getId(); /// Is it really OK to return a node in the same tree? // should
             * be, if all <refs> exist in base, which the must do if // change refs base. But what
             * if they don't? // Maybe a base.getNode() would prevent errors here // OTOH, that
             * slows things down.... return n.isTreeRef() ? changeNode : null;
             */
        }


        protected void content(Object node, boolean start) {
            RefTreeNodeImpl n = null;
            if (node instanceof DiffAlgorithm.DiffOperation) {
                DiffAlgorithm.DiffOperation op = (DiffAlgorithm.DiffOperation) node;
                Diff.DiffOperation c = new Diff.DiffOperation(op.getOperation(),
                                                              identify(op.getSource()),
                                                              identify(op.getDestination()),
                                                              op.getRun(), identify(op.getSource())); // id
                // by
                // src
                n = new RefTreeNodeImpl(currentNode, new TransientKey(), c);
            } else n = new RefTreeNodeImpl(currentNode, ((RefTreeNode) node).getId(),
                                           (RefTreeNode) node);
            if (start) {
                if (currentNode != null) currentNode.addChild(n);
                else root = n;
            }
            if (start) currentNode = n;
            else currentNode = (RefTreeNodeImpl) currentNode.getParent();
        }


        protected Iterator getChildIterator(Object changeNode) {
            return ((RefTreeNode) changeNode).getChildIterator();
        }


        protected boolean appends(Object baseTailO, Object baseNextO) {
            RefTreeNode baseTail = (RefTreeNode) baseTailO;
            RefTreeNode baseNext = (RefTreeNode) baseNextO;
            if (st != null) return st.inSequence(baseTail, baseNext);
            Key baseId = ((RefTreeNode) baseTail).getId();
            Key succ = ensureSuccessor(baseId, successors, base);
            /*
             * if( succ != NO_SUCCESSOR && !succ.equals(baseNext.getId() ) )
             * Log.debug("Break in sequence. (prev,next)="+baseNext.getId()+ ","+succ);
             */
            return succ != NO_SUCCESSOR && succ.equals(baseNext.getId());
        }


        protected Key identify(Object node) {
            if (node == DiffAlgorithm.DiffOperation.NO_VALUE) return null;
            if (!(node instanceof RefTreeNode)) {
                Log.log("Erroneous node class", Log.ASSERTFAILED);
                return StringKey.createKey("ERROR:" + node.toString());
            }
            return ((RefTreeNode) node).getId();
        }

    }

    private static final Key NO_SUCCESSOR = StringKey.createUniqueKey();


    protected static Key ensureSuccessor(Key baseId, Map<Key, Key> succcessors,
                                         IdAddressableRefTree base) {
        final Key NO_PREDECESSOR = StringKey.createUniqueKey();
        Key succ = succcessors.get(baseId);
        try {
            if (succ == null) {
                Key pid = base.getParent(baseId);
                Key prev = NO_PREDECESSOR;
                for (Iterator<Key> i = base.childIterator(pid); i.hasNext();) {
                    Key nid = i.next();
                    if (prev != NO_PREDECESSOR) succcessors.put(prev, nid);
                    prev = nid;
                }
                succcessors.put(prev, NO_SUCCESSOR);
                succ = succcessors.get(baseId);
                if (succ == null) Log.log("Broken parent/child relationship", Log.ASSERTFAILED);
            }
        } catch (NodeNotFoundException x) {
            Log.log("Broken reftree", Log.FATALERROR, x); // Not sure how to
            // report this...
        }
        return succ;
    }

    /** Exception signaling an invalid diff. */

    public static class DiffFormatException extends IOException {

        /**
         * Create a new exception.
         * @param msg
         *            message
         */
        public DiffFormatException(String msg) {
            super(msg);
        }
    }

    /**
     * Interface for class implementing node a sequence test.
     */

    public interface SequenceTester {

        /**
         * Tests if two reftree nodes follow each other. Should return <code>true</code> if and only
         * if <i>a</i> and <i>b</i> have a common parent <i>p</i>, and the child list iterator of
         * <i>p</i> would return <i>b</i> immediately after <i>b</i>; i.e. <i>b</i> follows <i>a</i>
         * immediately in the child list of <i>p</i>.
         * @param a
         *            First Node
         * @param b
         *            Second Node
         * @return <code>true</code> if b follows a
         */
        public boolean inSequence(RefTreeNode a, RefTreeNode b);
    }

    /**
     * Diff operation node content. Used in the Diff reftree nodes to encode diff operations.
     */
    protected static class DiffOperation implements RefTrees.IdentifiableContent {

        public static final int ROOT_COPY = DiffAlgorithm.DiffOperation.ROOT_COPY;
        public static final int ROOT_INSERT = DiffAlgorithm.DiffOperation.ROOT_INSERT;
        public static final int COPY = DiffAlgorithm.DiffOperation.COPY;
        public static final int INSERT = DiffAlgorithm.DiffOperation.INSERT;

        private int operation;
        private Key source;
        private Key destination;
        private Key id;
        private Long run;


        protected DiffOperation(int aOperation, Key aSource, Key aDestination, Long aRun, Key aid) {
            operation = aOperation;
            source = aSource;
            destination = aDestination;
            run = aRun;
            id = aid;
        }


        public int getOperation() {
            return operation;
        }


        public Key getSource() {
            return source;
        }


        public Key getDestination() {
            return destination;
        }


        public Long getRun() {
            return run;
        }


        public Key getId() {
            return id;
        }


        public String toString() {
            return "diff{" +
                   (operation == COPY ? "cpy" : (operation == INSERT ? "ins"
                           : (operation == ROOT_COPY ? "rcpy" : "rins"))) + ",src=" + source +
                   ",dst=" + destination + ",run=" + run + "}";
        }

    }

    /**
     * RefTreeNode that know how to expand a sequence of copy ops as its childlist.
     */

    protected static class DelayedRefTreeNode implements RefTreeNode {

        final RefTreeNode node;
        final IdAddressableRefTree base;


        public DelayedRefTreeNode(RefTreeNode node, IdAddressableRefTree base) {
            this.node = node;
            this.base = base;
        }


        protected void trap(NodeNotFoundException ex) {
            // FIXME: Add real trap
            Log.fatal("Untrapped missing node " + ex.getId(), ex);
        }


        public Iterator getChildIterator() {
            final Iterator baseIterator = node.getChildIterator();
            return new Iterator() {

                Iterator copySequenceIterator = null; // != null when active


                public void remove() {
                    throw new UnsupportedOperationException();
                }


                public boolean hasNext() {
                    return (copySequenceIterator != null && copySequenceIterator.hasNext()) ||
                           baseIterator.hasNext();
                }


                public Object next() {
                    if (copySequenceIterator != null) {
                        if (copySequenceIterator.hasNext()) return copySequenceIterator.next();
                        else copySequenceIterator = null;
                    }
                    RefTreeNode next = (RefTreeNode) baseIterator.next();
                    if (next.getContent() instanceof DiffOperation) {
                        if (next.getChildIterator().hasNext())
                            throw new IllegalStateException("diffops may not have children in"
                                                            + "this version of the algo");
                        try {
                            copySequenceIterator = new CopySequenceIterator(
                                                                            (DiffOperation) next.getContent(),
                                                                            DelayedRefTreeNode.this,
                                                                            base);
                        } catch (NodeNotFoundException e) {
                            trap(e);
                        }
                        return copySequenceIterator.next();
                    }
                    return new DelayedRefTreeNode(next, base);
                }
            };
        }


        public Object getContent() {
            return node.getContent();
        }


        public Key getId() {
            return node.getId();
        }


        public RefTreeNode getParent() {
            RefTreeNode n = node.getParent();
            return n != null ? new DelayedRefTreeNode(n, base) : null;
        }


        public boolean isNodeRef() {
            return node.isNodeRef();
        }


        public boolean isReference() {
            return node.isReference();
        }


        public boolean isTreeRef() {
            return node.isTreeRef();
        }


        public Reference getReference() {
            return node.getReference();
        }

    }

    /** Iterator that expands diff copy tags to tree references. */
    protected static class CopySequenceIterator implements Iterator {

        protected Map<Key, Key> successors = new HashMap(); // = new HashMap();
        // // Move out of
        // iterator for more
        // speed!!!

        Key id;
        long left;
        DelayedRefTreeNode parent;


        public CopySequenceIterator(DiffOperation op, DelayedRefTreeNode parent,
                                    IdAddressableRefTree baseTree) throws NodeNotFoundException {
            if (op.getOperation() != DiffOperation.ROOT_COPY &&
                op.getOperation() != DiffOperation.COPY)
                throw new IllegalStateException("No inserts should be seen here");
            id = op.getSource();
            if (!baseTree.contains(id)) throw new NodeNotFoundException(id);
            ensureSuccessor(id, successors, baseTree);
            left = op.getRun() != null ? op.getRun().longValue() : 1;
            if (left < 1) Log.log("CS iter should never map to <1 elems.", Log.ASSERTFAILED);
            this.parent = parent;
        }


        public void remove() {
            throw new UnsupportedOperationException();
        }


        public boolean hasNext() {
            return left > 0 && id != NO_SUCCESSOR;
        }


        // NOTE: Always has a first element
        public Object next() {
            if (left <= 0 || id == NO_SUCCESSOR) throw new NoSuchElementException();
            Key current = id;
            id = successors.get(id);
            left--;
            return /* new DelayedRefTreeNode( */
            new RefTreeNodeImpl(parent, current, new TreeReference(current));/* ) */
        }
    }

    public static class DiffTreeModel implements XasCodec {

        private Key rootId;
        private XasCodec codec;


        public DiffTreeModel(XasCodec wtm, Key rootId) {
            this.codec = wtm;
            this.rootId = rootId;
        }


        public Object decode(PeekableItemSource is, KeyIdentificationModel kim) throws IOException {
            Item i = is.peek();
            Object diffTag = null;
            do_default: if (i.getType() == Item.START_TAG) {
                StartTag st = (StartTag) i;
                // -------------- Copy tag?
                if (DIFF_COPY_TAG.equals(st.getName())) {
                    is.next();
                    String src = null, dst = null;
                    Long run = null;
                    for (Iterator<AttributeNode> it = st.attributes(); it.hasNext();) {
                        AttributeNode an = it.next();
                        if (DIFF_CPYSRC_ATTR.equals(an.getName())) src = an.getValue().toString();
                        else if (DIFF_CPYDST_ATTR.equals(an.getName())) dst = an.getValue().toString();
                        else if (DIFF_CPYRUN_ATTR.equals(an.getName())) {
                            try {
                                if (an.getValue() instanceof Number) run = ((Number) an.getValue()).longValue();
                                else run = new Long(an.getValue().toString());
                            } catch (NumberFormatException x) {
                                throw new DiffFormatException("Non-numeric run: " + an.getValue());
                            }
                        } else throw new DiffFormatException("Unknown attribute: " + an.getName());
                    }
                    diffTag = new DiffOperation(DiffOperation.COPY, kim.makeKey(src),
                                                kim.makeKey(dst), run, kim.makeKey(src));
                    return diffTag;

                } else // ------------- Insert tag?
                if (DIFF_INS_TAG.equals(st.getName())) {
                    is.next();
                    // FIXME-W actually, this code should not just be
                    // for reftrees; the ex below should occur on
                    // decodeTorefTree
                    // -> we should parse ins tags
                    throw new DiffFormatException(
                                                  "Diffs with inserts cannot be decoded to reftrees");
                } else // ------------- Root tag?
                if (DIFF_ROOT_TAG.equals(st.getName())) {
                    is.next();
                    boolean rootIsIns = false;
                    for (Iterator<AttributeNode> it = st.attributes(); it.hasNext();) {
                        AttributeNode an = it.next();
                        if (DIFF_ROOTOP_ATTR.equals(an.getName())) {
                            if (!DIFF_ROOTOP_INS.equals(an.getValue()))
                                throw new DiffFormatException("Unknown root operation " +
                                                              an.getValue());
                            rootIsIns = true;
                        }
                    }
                    RefTrees.IdentifiableContent c = new DiffOperation(
                                                                       rootIsIns ? DiffOperation.ROOT_INSERT
                                                                               : DiffOperation.ROOT_COPY,
                                                                       rootId, null, null, rootId);
                    if (!rootIsIns) {
                        diffTag = c;
                    } else diffTag = null;
                    return c;
                } else break do_default;
            }
            return codec.decode(is, kim);
        }


        public void encode(ItemTarget t, RefTreeNode n, StartTag context) throws IOException {
            Object o = n.getContent();
            if (!(o instanceof DiffOperation)) {
                codec.encode(t, n, context);
                return;
            }
            DiffOperation op = (DiffOperation) o;
            StartTag rt;
            switch (op.getOperation()) {
                case DiffOperation.ROOT_INSERT:
                case DiffOperation.ROOT_COPY:
                    rt = new StartTag(DIFF_ROOT_TAG, context);
                    rt.ensurePrefix(DIFF_NS, DIFF_NS_PREFIX);
                    if (op.getOperation() == DiffOperation.ROOT_INSERT)
                        rt.addAttribute(DIFF_ROOTOP_ATTR, DIFF_ROOTOP_INS);
                    break;
                case DiffOperation.COPY:
                    rt = new StartTag(DIFF_COPY_TAG, context);
                    rt.ensurePrefix(DIFF_NS, DIFF_NS_PREFIX);
                    addAttributes(op, rt);
                    break;
                case DiffOperation.INSERT:
                    rt = new StartTag(DIFF_INS_TAG, context);
                    rt.ensurePrefix(DIFF_NS, DIFF_NS_PREFIX);
                    addAttributes(op, rt);
                    break;
                default:
                    throw new IOException("Unknown diffop: " + op.getOperation());
            }
            t.append(rt);
        }


        protected void addAttributes(DiffOperation op, StartTag t) throws IOException {
            if (op.getDestination() != null)
                t.addAttribute(DIFF_CPYDST_ATTR, op.getDestination().toString());
            if (op.getSource() != null)
                t.addAttribute(DIFF_CPYSRC_ATTR, op.getSource().toString());
            if (op.getRun() != null) t.addAttribute(DIFF_CPYRUN_ATTR, op.getRun().toString());
        }

    }

    /**
     * Expand relative Dewey keys in a diff item sequence. Reference items need to have been decoded
     * previous to this step.
     */

    public static class RelativeDeweyKeyExpander implements ItemTransform {

        private Stack<String> parentPaths = new Stack<String>();
        protected Queue<Item> queue = new LinkedList<Item>();


        public RelativeDeweyKeyExpander() {
        }


        // Generates ./n1/[..]/n2 or full path
        private String getAbsolutePath(String parent, String path) {
            if (path.startsWith("/")) // BUGFIX070619-10
                return path;
            assert parent != null;
            assert (path.startsWith("."));
            return parent + path.substring(1); // E.g. "/1/2"+"./3"
        }


        public boolean hasItems() {
            return !queue.isEmpty();
        }


        public Item next() {
            return queue.poll();
        }


        public void append(Item ev) throws IOException {
            switch (ev.getType()) {
                case Item.START_DOCUMENT:
                    queue.add(ev);
                    parentPaths.push(null); // For convenicence
                    break;
                case RefTreeItem.TREE_REFERENCE:
                    // Actually, these may not even occur in diffs
                    RefItem ri = (RefItem) ev;
                    String target = ri.getTarget().toString();
                    if (target.startsWith(".")) {
                        // FIXME-20061212-2
                        ev = RefItem.makeStartItem(
                                                   new TreeReference(
                                                                     StringKey.createKey(getAbsolutePath(
                                                                                                         parentPaths.peek(),
                                                                                                         target))),
                                                   null);
                    }
                    queue.add(ev);
                    break;
                case Item.START_TAG:
                    StartTag st = (StartTag) ev;
                    if (DIFF_COPY_TAG.equals(st.getName())) {
                        // The tricky case: fix diff:copy src attribute
                        StartTag xst = new StartTag(st.getName());
                        for (Iterator<AttributeNode> ai = st.attributes(); ai.hasNext();) {
                            AttributeNode an = ai.next();
                            if (DIFF_CPYSRC_ATTR.equals(an.getName()))
                            // BUGFIX-20070628-1: exception on absolute path &
                            // empty stack
                            xst.addAttribute(
                                             an.getName(),
                                             getAbsolutePath(parentPaths.isEmpty() ? null
                                                     : parentPaths.peek(), an.getValue().toString()));
                            else xst.addAttribute(an.getName(), an.getValue());
                        }
                        ev = xst;
                    }
                    queue.add(ev);
                    break;
                case RefNodeItem.NODE_REFERENCE: // ReferenceItem.START_REF_NODE:
                    RefNodeItem rin = (RefNodeItem) ev;
                    if (!rin.isEndTag()) {
                        target = rin.getTarget().toString();
                        if (target.startsWith(".")) {
                            // FIXME-20061212-2
                            target = getAbsolutePath(parentPaths.peek(), target); // BUGFIX070619-10
                            ev = RefItem.makeStartItem(
                                                       new NodeReference(
                                                                         StringKey.createKey(target)),
                                                       null);
                        }
                        queue.add(ev);
                        parentPaths.push(target);
                    } else {
                        parentPaths.pop();
                        queue.add(ev);
                    }
                    break;
                case Item.END_DOCUMENT:
                    queue.add(ev);
                    parentPaths.pop();
                    break;
                default:
                    queue.add(ev);
            }

        }
    }

}
// arch-tag: 87c0c79f182ec345fb03c5b197031c02 *-
