/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.xas;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xmlr.AbstractMutableRefTree;
import fc.xml.xmlr.BatchMutable;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.KeyMap;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.NodeReference;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.Reference;

// Special features: getParent and childIterator are very lightweight
// In map, backTree = ref'd tree; frontTree = this
/**
 * Mutable Dewey-keyed reftree that maintains key mapping. The nodes in the tree are of class
 * {@link fc.xml.xmlr.xas.DeweyRefNode}. The {@link #getParent(Key)} and {@link #childIterator(Key)}
 * methods are very lightweight, due to the structure of Dewey keys.
 * <p>
 * The tree maintains a key map for Dewey keys based on the references present in the tree. The back
 * tree of the map is the referenced tree. For instance, if there is a tree reference to
 * <code>/0</code> in the tree at key <code>/0/2</code>, then <code>getFrontKey(/0) = /0/2</code>,
 * and <code>getBackKey(/0/2/1) = /0/1</code>. The feature is very useful when working with Dewey
 * keyed trees.
 */
public class MutableDeweyRefTree extends AbstractMutableRefTree implements KeyMap, BatchMutable {

    private DeweyRefNode root;
    private DeweyKey rootKey;
    private int rootKeyLen;
    private boolean forceAutoKey = false;

    // the tabs are keyed by reference TARGET, i.e. back key
    private Map<DeweyKey, DeweyRefNode> nodeRefTab = new HashMap<DeweyKey, DeweyRefNode>();
    private Map<DeweyKey, DeweyRefNode> treeRefTab = new HashMap<DeweyKey, DeweyRefNode>();


    // FIXME: This simple apply will not minimize the amount of work done
    // by comparing subtrees of t with the current state
    // Actually, there should probably be a RefTrees.compact(RefTree t,RefTree
    // base) which returns a minimized reftree w.r.t. the current state of base
    /** @inheritDoc */
    public void apply(RefTree t) throws NodeNotFoundException {
        Map<DeweyKey, DeweyRefNode> oldNRT = nodeRefTab;
        nodeRefTab = new HashMap<DeweyKey, DeweyRefNode>();
        Map<DeweyKey, DeweyRefNode> oldTRT = treeRefTab;
        treeRefTab = new HashMap<DeweyKey, DeweyRefNode>();
        try {
            setRoot(apply(t.getRoot(), new DeweyRefNode.PrefixNode(forceAutoKey ? DeweyKey.ROOT_KEY
                    : (DeweyKey) t.getRoot().getId())));
            buildRefTab(root);
        } catch (NodeNotFoundException e) {
            // Rollback = restore refTabs = easy, as they were backed up
            // + restore detached reftrees (We need to keep a list of those)
            Log.error("Apply failed, and rollback not coded.  :(");
            throw e;
        }
    }


    /** @inheritDoc */
    public void delete(Key id) throws NodeNotFoundException {
        DeweyRefNode n = findExist((DeweyKey) id);
        // KEYMAP CODE ------------------
        removeFromRefTabs(n);
        assert n.getParent() != null : "Trying to delete the prefix node?";
        if (n.getParent().getParent() == null) {
            // Deleting root
            setRoot(null);
        } else ((DeweyRefNode) n.getParent()).removeChild(n);
    }


    /**
     * Insert a new non-reference node.
     * @param parentId
     *            id of parent to the new node
     * @param pos
     *            position in child list, before which the new node is inserted (i.e. 0 implies at
     *            the start of the list). {@link #DEFAULT_POSITION DEFAULT_POSITION} inserts at the
     *            end of the child list.
     * @param newId
     *            id of the new node, must not already exist in the tree.
     *            {@link fc.xml.xmlr.MutableRefTree#AUTO_KEY} is highly recommended for this tree,
     *            since the parent and position will imply the added key. This parameter is ignored
     *            if {@link #isForceAutoKey()} is enabled.
     * @param content
     *            content object of new node
     * @throws NodeNotFoundException
     *             if the <code>parentId</code> node is not in the tree.
     * @return Key of inserted node
     */
    public Key insert(Key parentId, long pos, Key newId, Object content)
            throws NodeNotFoundException {
        if (newId != AUTO_KEY && find((DeweyKey) newId) != null)
            throw new IllegalArgumentException("key already exists " + newId);
        DeweyRefNode p = parentId == null ? null : findExist((DeweyKey) parentId);
        DeweyRefNode n = null;
        if (p == null) {
            DeweyKey rootKey = (!forceAutoKey && newId != AUTO_KEY) ? (DeweyKey) newId
                    : DeweyKey.ROOT_KEY;
            n = new DeweyRefNode(new DeweyRefNode.PrefixNode(rootKey), 0, content);
            setRoot(n);
        } else {
            n = new DeweyRefNode(content);
            if (pos == DEFAULT_POSITION) p.addChild(n);
            else p.addChild((int) pos, n);
        }
        if (!forceAutoKey && newId != AUTO_KEY && !n.getId().equals(newId)) {
            // Bad new key given, rollback and except
            Key realId = n.getId();
            if (p != null) p.removeChild(n);
            else {
                setRoot(null);
            }
            throw new NodeNotFoundException("The key " + newId +
                                            " does not agree with the insert position", realId);
        }
        // KEYMAP CODE ------------------
        if (content instanceof Reference) {
            Reference r = (Reference) content;
            (r.isTreeReference() ? treeRefTab : nodeRefTab).put((DeweyKey) r.getTarget(), n);
        }
        // Log.debug("Inserted at "+n.getId(),content);
        // BUGFIX-20070507-1: Return real key if AUTO or forced, not AUTO &
        // !force
        return newId == AUTO_KEY || forceAutoKey ? n.getId() : newId;
    }


    private void setRoot(DeweyRefNode n) {
        if (n == null) {
            rootKey = null;
            rootKeyLen = -1;
            root = null;
            nodeRefTab.clear();
            treeRefTab.clear();
        } else {
            rootKey = (DeweyKey) n.getId(); // Also checks key type before
            // root is set
            rootKeyLen = rootKey.length();
            root = n;
        }
        // Log.debug("Rootkey set to "+rootKey);
    }


    /** @inheritDoc */
    public Key move(Key nodeId, Key parentId, long pos) throws NodeNotFoundException {
        DeweyRefNode n = findExist((DeweyKey) nodeId);
        DeweyRefNode p = findExist((DeweyKey) parentId);
        if (n.getParent() == null) throw new IllegalArgumentException("Tried to move root");
        DeweyRefNode np = (DeweyRefNode) n.getParent();
        int origPos = np.getPosition();
        boolean removeOk = np.removeChild(n);
        assert removeOk;
        if (pos == DEFAULT_POSITION) p.addChild(n);
        else {
            try {
                p.addChild((int) pos, n); // BUGFIX-061005-3: Added p, not n, as
                // child
            } catch (RuntimeException e) {
                np.addChild(origPos, n); // Re-attach node to parent
                throw e;
            }
        }
        return n.getId();
    }


    /** @inheritDoc */
    public boolean update(Key nodeId, Object content) throws NodeNotFoundException {
        DeweyRefNode n = findExist((DeweyKey) nodeId);
        if (!Util.equals(content, n.getContent())) {
            if (n.isTreeRef() && n.childCount() > 0)
                throw new IllegalArgumentException("Tried to make inner node a ref-node, key: " +
                                                   nodeId);
            if (n.isReference()) // Take care of ref being deleted
                (n.isTreeRef() ? treeRefTab : nodeRefTab).remove(n.getReference().getTarget());
            n.setContent(content);
            if (n.isReference()) // Take care of added ref
                (n.isTreeRef() ? treeRefTab : nodeRefTab).put(
                                                              (DeweyKey) n.getReference().getTarget(),
                                                              n);
            return true;
        }
        return false;
    }


    /** @inheritDoc */

    public RefTreeNode getNode(Key id) {
        return id instanceof DeweyKey ? find((DeweyKey) id) : null; // FIXME:
        // slow-instanceof
    }


    /** @inheritDoc */
    public boolean contains(Key id) {
        return getNode(id) != null;
    }


    /** @inheritDoc */
    public Key getParent(Key nid) throws NodeNotFoundException {
        if (nid.equals(rootKey)) return null;
        return ((DeweyKey) nid).up();
    }


    /** @inheritDoc */
    public Iterator<Key> childIterator(Key id) throws NodeNotFoundException {
        final DeweyKey root = (DeweyKey) id;
        DeweyRefNode n = findExist(root);
        final int count = n.childCount();
        return new Iterator<Key>() {

            int pos = 0;


            public boolean hasNext() {
                return pos < count;
            }


            public Key next() {
                return root.child(pos++); // BUGFIX-22060926-1: Missing
                // increment
            }


            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }


    /** @inheritDoc */
    public RefTreeNode getRoot() {
        return root;
    }


    /** Dump debug info. */
    public void dumpDebug() {
        StringBuilder sb = new StringBuilder();
        // for( Map.Entry<DeweyKey,DeweyRefNode> e : treeRefTab.entrySet() )
        // sb.append(e.getKey()+"->"+e.getValue().getId()+", ");
        Log.debug(String.valueOf(treeRefTab.size()) + " key prefix mappings", sb);
        sb.setLength(0);
        // for( Map.Entry<DeweyKey,DeweyRefNode> e : nodeRefTab.entrySet() )
        // sb.append(e.getKey()+"->"+e.getValue().getId()+", ");
        Log.debug(String.valueOf(nodeRefTab.size()) + " exact key mappings", sb);
    }


    protected DeweyRefNode apply(RefTreeNode n, DeweyRefNode p) throws NodeNotFoundException {
        DeweyRefNode m = null;
        if (n.isReference()) {
            Reference r = n.getReference();
            DeweyRefNode rt = findExist((DeweyKey) r.getTarget());
            if (r.isTreeReference()) {
                rt.detach();
                m = rt;
            } else {
                // BUGFIX 20060928-1: NodeRef got turned into treeref by
                // careless
                // copy of Reference object
                m = new DeweyRefNode(rt.isReference() ? (rt.isNodeRef() ? rt.getReference()
                        : new NodeReference(rt.getReference().getTarget())) : rt.getContent());
            }
        } else m = new DeweyRefNode(n.getContent());
        m.attach(p);
        for (Iterator<RefTreeNode> ci = n.getChildIterator(); ci.hasNext();)
            apply(ci.next(), m);
        return m;
    }


    protected void buildRefTab(DeweyRefNode n) {
        if (n.isReference()) {
            Reference r = n.getReference();
            (r.isTreeReference() ? treeRefTab : nodeRefTab).put((DeweyKey) r.getTarget(), n);
        }
        for (Iterator<DeweyRefNode> ci = n.getChildIterator(); ci.hasNext();)
            buildRefTab(ci.next());
    }


    protected void removeFromRefTabs(RefTreeNode n) {
        if (n.isReference())
            (n.isTreeRef() ? treeRefTab : nodeRefTab).remove(n.getReference().getTarget());
        for (Iterator i = n.getChildIterator(); i.hasNext();)
            removeFromRefTabs((RefTreeNode) i.next());
    }


    protected DeweyRefNode find(DeweyKey k) {
        // Log.debug("Find "+k+" is "+(k== null ? null :
        // find(k.getXasDeweyKey(),
        // k.length()-rootKeyLen)));
        // BUGFIX-061005-2: getNode NPREx if there is no root yet
        return k == null || rootKey == null ? null : find(k.getXasDeweyKey(), k.length() -
                                                                              rootKeyLen);
    }


    // Recursive find; slightly slower than fasterFind(); we use it because it's
    // code is simpler
    protected DeweyRefNode find(fc.xml.xas.index.DeweyKey k, int left) {
        if (left < 1) {
            if (Util.equals(rootKey.getXasDeweyKey(), k)) return root;
            else return null;
        } else {
            DeweyRefNode p = find(k.up(), left - 1);
            int step = p == null ? -1 : k.getLastStep();
            return p == null || step >= p.childCount() ? null : (DeweyRefNode) p.get(step);
        }
    }


    /*
     * This find is slightly faster, but more gc-intensive Seems like deconstructing the key into an
     * array really speeds up traversal
     * 
     * protected DeweyRefNode fasterFind(DeweyKey k ) { // NOTE: any changes here go to findExists
     * too! DeweyRefNode pos = root; if( k == null ) return null; // FIX: make faster; point is to
     * strip root prefix /x/y/z... k = k.replaceAncestorSelf(rootKey, DeweyKey.ROOT_KEY); int []
     * path = k.deconstruct(); for(int i=0;i<path.length;i++) { int step = path[i]; if( pos == null
     * || step >= pos.childCount() ) // not testing for step < 0, as this is an erroneous key!
     * return null; pos=(DeweyRefNode) pos.get(path[i]); } return pos; }
     */

    protected DeweyRefNode findExist(DeweyKey k) throws NodeNotFoundException {
        DeweyRefNode n = k == null ? null : find(k.getXasDeweyKey(), k.length() - rootKeyLen);
        if (n == null) throw new NodeNotFoundException(k);
        return n;
    }


    // Returns null if frontKey isn't in frontTree, or if it isn't in backTree.
    // i.e. if it is unmappable.
    /** @inheritDoc */
    public Key getBackKey(Key frontKey) {
        Key bk = _getBackKey(frontKey);
        // Log.debug("Map: F->b: "+frontKey+"->"+bk);
        return bk;
    }


    /** @inheritDoc */
    public Key getFrontKey(Key backKey) {
        Key fk = _getFrontKey(backKey);
        // Log.debug("Map: b->F: "+backKey+"->"+fk);
        return fk;
    }


    /**
     * Check if the tree forces auto keys.
     * @return <code>true</code> if the tree forces auto keys.
     */
    public boolean isForceAutoKey() {
        return forceAutoKey;
    }


    /**
     * Set auto key force. If auto key force is enabled, given insertion keys will be ignored.
     * @param forceAutoKey
     *            <code>true</code> if auto key is forced
     */
    public void setForceAutoKey(boolean forceAutoKey) {
        this.forceAutoKey = forceAutoKey;
    }


    private Key _getBackKey(Key frontKey) {
        DeweyRefNode pos = root;
        DeweyKey k = (DeweyKey) frontKey;
        if (k == null) return null;
        // Handle root refs
        if (pos != null && pos.isTreeRef() || (pos != null && pos.isNodeRef() && rootKey.equals(k))) {
            // Log.debug("replaceAncestorSelf(key="+k+" ,anc="+(DeweyKey)
            // root.getId()+
            // " ,repl="+((DeweyKey) pos.getReference().getTarget()));
            return k.replaceAncestorSelf((DeweyKey) rootKey,
                                         ((DeweyKey) pos.getReference().getTarget()));
        }
        // FIXME: check that k is a prefix of root, i.e. it is not /1/0 when
        // root is /0
        // Log.debug("Strip root prefix of "+k+"="+k.replaceAncestorSelf((DeweyKey)
        // root.getId(), DeweyKey.ROOT_KEY));
        k = k.replaceAncestorSelf(rootKey, DeweyKey.ROOT_KEY);
        int[] path = k.deconstruct();
        for (int i = 0; i < path.length; i++) {
            int step = path[i];
            if (pos == null || step >= pos.childCount())
            // not testing for step < 0, as this is an erroneous key!
                return null;
            pos = (DeweyRefNode) pos.get(path[i]);
            if (pos.isTreeRef() || (pos.isNodeRef() && i == path.length - 1)) {
                // Log.debug("Mapping prefix of "+k+" of length "+(i+1)+" to "+
                // pos.getReference().getTarget(),
                // "result="+((DeweyKey)
                // pos.getReference().getTarget()).append(path,i+1));
                return ((DeweyKey) pos.getReference().getTarget()).append(path, i + 1);
            }
        }
        return null;
    }


    private Key _getFrontKey(Key bk) {
        DeweyKey backKey = (DeweyKey) bk;
        if (backKey == null) return null;
        RefTreeNode mappedNode = nodeRefTab.get(backKey);
        DeweyKey mapped = mappedNode == null ? null : (DeweyKey) nodeRefTab.get(backKey).getId();
        if (mapped != null) {
            assert nodeRefTab.get(backKey).getReference().getTarget().equals(backKey);
            return mapped;
        }
        DeweyKey origBackKey = backKey;
        do {
            mappedNode = treeRefTab.get(backKey);
            DeweyKey mappedKey = mappedNode == null ? null : (DeweyKey) mappedNode.getId();
            if (mappedKey != null) {
                assert treeRefTab.get(backKey).getReference().getTarget().equals(backKey);
                // Log.debug("Replacing ancestor "+backKey+" of "+origBackKey+
                // " with "+mappedKey+", yielding "+
                // origBackKey.replaceAncestorSelf(backKey, mappedKey));
                return origBackKey.replaceAncestorSelf(backKey, mappedKey);
            }
        } while ( /* !DeweyKey.ROOT_KEY.equals(backKey) && */
        (backKey = backKey.up()) != null);
        return null;
    }

}
// arch-tag: a9aac846-f36c-480c-9c3a-4c19bde44dd1
