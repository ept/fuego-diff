/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import fc.util.Util;
import fc.util.log.Log;
import fc.util.log.LogLevels;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.AbstractMutableRefTree;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeImpl;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTreeNodeImpl;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.XmlrDebug;
import fc.xml.xmlr.model.StringKey;

public class RandomDirectoryTree {

    public static KeyGen KEY_GEN = new KeyGen(0L);

    static String edstring = null; // "";
    public static boolean useInsertAutoKey = false;


    public static void permutateTreeStringOrder(MutableRefTree t, long ops, String pdf,
                                                double deleteTreeProb, final Random rnd, KeyGen kg) {
        permutateTree(t, ops, pdf, deleteTreeProb, rnd, kg,
                      new IdNodePicker(new RandomizedSetByString(rnd.nextInt()), t));
    }


    public static void permutateTree(MutableRefTree t, long ops, String pdf, double deleteTreeProb,
                                     final Random rnd, KeyGen kg) {
        permutateTree(t, ops, pdf, deleteTreeProb, rnd, kg, new SafeIdNodePicker(t, rnd));
    }


    /**
     * Permutate a reftree by random edits. The edits and their symbols are insert leaf (
     * <code>i</code>), insert directory (<code>I</code>), delete leaf (<code>d</code>), update node
     * (<code>u</code>), and move node ( <code>m</code>).
     * @param t
     *            Tree to edit
     * @param ops
     *            number of edits
     * @param pdf
     *            relative frequency of edits. Each type of edit has a symbol, and the probability
     *            of the edit type is the relative frequency of that symbol in this string. E.g. "
     *            <code>mmmu</code>" = 75% moves, 25% updates
     * @param deleteTreeProb
     *            probability that a leaf delete deletes is parent (recursively applied towards
     *            root)
     * @param rnd
     *            random generator
     * @param kg
     *            id generator for inserted nodes
     * @param np
     *            randomized set of editable nodes. Should be initialized to the nodes allowed to
     *            edited in t.
     */

    public static void permutateTree(MutableRefTree t, long ops, String pdf, double deleteTreeProb,
                                     final Random rnd, KeyGen kg, NodePicker np) {
        int redoFails = 0;
        Key rootId = t.getRoot().getId();
        RefTreeNode pos = np.nextNode();
        // Log.debug("The set of nodes is below; selection="+pos,treeNodes);
        int redos = 0;
        boolean redo = false;
        char op = 'Q';
        Key nId = null;
        top: for (; ops > 0; ops--) {
            // Log.debug("The set of nodes is",treeNodes);
            nId = redo ? nId : kg.next();
            // Log.info("Using key "+nId);
            op = redo ? op : pdf.charAt(rnd.nextInt(pdf.length()));
            redo = false;
            // The selection procedure below may choose a file only once, but a
            // dir as
            // many times as it has files. Also, the probability of a dir is
            // weighted by the numbers of files in it
            RefTreeNode rndNode = pos;
            assert rndNode != null : "Node disappeared " + rndNode.getId();
            RefTreeNode rndDirNode = rndNode;
            int dscan = np.size();
            assert rndDirNode.getContent() != null;
            while (((DirectoryEntry) rndDirNode.getContent()).getType() == DirectoryEntry.FILE) {
                pos = np.nextNode();
                rndDirNode = pos;
                if (rndDirNode == null) {
                    Log.fatal("Tried to select non-existing node " + pos.getId(), new Throwable());
                    redo = true;
                    break top;
                }
                dscan--;
                if (dscan < 0) {
                    // XmlrDebug.dumpTree(t,System.out);
                    redo = true;
                    Log.log("No dir found", LogLevels.ERROR);
                    break top;
                }
                assert rndDirNode.getContent() != null;
            }
            Key preDirNodeId = rndDirNode.getId();
            // treeNodes.remove(rndId);
            // Log.log("Picked "+rndId,Log.INFO);
            // Get next pos...what a kludge
            try {
                redo: switch (op) {
                    case 'i': { // File ins
                        Key iKey = makeInsertId(t, nId);
                        Object nc = new DirectoryEntry(nId, fileName(nId, rnd), DirectoryEntry.FILE);
                        Key nKey = t.insert(rndDirNode.getId(), iKey, nc);
                        // Log.log("ins-f @ "+rndDirNode.getId()+" ",Log.INFO,
                        // t.getNode(nId).getContent());
                        if (edstring != null) edstring += "i(" + preDirNodeId + "," + nc + ")\n";
                        np.addNode(t.getNode(nKey), false); // treeNodes.add(nId);
                        break;
                    }
                    case 'I': { // Dir ins
                        Key iKey = makeInsertId(t, nId);
                        Object nc = new DirectoryEntry(nId, dirName(nId, rnd), DirectoryEntry.DIR);
                        Key nKey = t.insert(rndDirNode.getId(), iKey, nc);
                        // Log.log("ins-d @ "+rndDirNode.getId()+" ",Log.INFO,t.getNode(nId).getContent());
                        if (edstring != null) edstring += "I(" + preDirNodeId + "," + nc + ")\n";
                        np.addNode(t.getNode(nKey), false); // treeNodes.add(nId);
                        break;
                    }
                    case 'd': // delete
                        // Travel to bottom
                        // FIXME 120906-1 This selects a node which is
                        // dependent on child order!
                        // Log.debug("Delete init node=",rndNode);
                        for (Iterator i = rndNode.getChildIterator(); i.hasNext() &&
                                                                      (rndNode = (RefTreeNode) i.next()) == null;)
                            ;
                        // / while (rndNode.firstChild() != null)
                        // rndNode = rndNode.firstChild();
                        // Travel upwards with delTree probability
                        // Log.debug("Delete bottom node=",rndNode);
                        while (rnd.nextDouble() < deleteTreeProb)
                            rndNode = rndNode == null ? null : rndNode.getParent();
                        // Log.debug("Delete chosen node=",rndNode);
                        if (rndNode == null || rootId.equals(rndNode.getId())) {
                            // Log.debug("BREAK: Tried to delete root ");
                            break; // Never delete root
                        }
                        // Log.log("Del ",Log.INFO,rndNode.getContent());
                        if (rndNode.getParent() == null) {
                            Log.debug("REDO: Tried to delete root ", rndNode.getId());
                            break redo; // Tried to delete root
                        }
                        preDirNodeId = rndNode.getId();
                        {
                            Set deletia = new HashSet();
                            np.removeNode(rndNode, true);
                            // treeSet(t, rndNode, deletia, '+');
                            // Log.debug("Deletia below id "+rndNode.getId()+
                            // " at opcount "+ops,deletia);
                            // Sweep delete
                            /*
                             * for( Iterator i = deletia.iterator(); i.hasNext(); ) { t.delete((Key)
                             * i.next()); }
                             */
                            t.delete(rndNode.getId());
                        }
                        if (edstring != null) edstring += "d(" + preDirNodeId + ")\n";
                        break;
                    case 'u':
                        preDirNodeId = rndNode.getId();
                        DirectoryEntry c = (DirectoryEntry) rndNode.getContent();
                        String newName = c.getType() == DirectoryEntry.FILE ? fileName(kg.next(),
                                                                                       rnd)
                                : dirName(kg.next(), rnd);
                        // Log.log("Upd "+newName+", old=",Log.INFO,rndNode.getContent());
                        // /c.setName(newName);
                        DirectoryEntry nc = new DirectoryEntry(c);
                        nc.setName(newName);
                        t.update(rndNode.getId(), nc);
                        if (edstring != null) edstring += "u(" + preDirNodeId + "," + nc + ")\n";
                        break;

                    case 'm':
                        Key moveNode = np.nextNode().getId(); // treeNodes.first();
                        if (rootId.equals(moveNode)) {
                            // Log.debug("REDO: Tried to move root ",rootId);
                            redo = true;
                            break; // Never move root
                        }
                        RefTreeNode newParent = rndDirNode;
                        // Check for cyclic move
                        while (newParent != null) {
                            if (newParent.getId().equals(moveNode)) {
                                // Log.debug("REDO: Tried to do cyclic move of ",moveNode);
                                redo = true;
                                break redo;
                            }
                            newParent = newParent.getParent();
                        }
                        // Log.log("mov "+moveNode+" below "+rndDirNode.getId(),Log.INFO);
                        if (edstring != null)
                            edstring += "m(" + moveNode + "," + rndDirNode.getId() + ")\n";
                        t.move(moveNode, rndDirNode.getId());
                        break;
                    default:
                        Log.log("Invalid op " + op, LogLevels.ASSERTFAILED);
                }
                if (redo) {
                    // Log.log("Redoing failed "+op,Log.INFO);
                    ops++;
                    redos++;
                    if (redos > 2) {
                        redo = false;
                        redos = 0;
                        redoFails++;
                        if (redoFails < 2) // Only report no 1
                            Log.log("10 redos failed, giving up", LogLevels.INFO);
                    }
                }
                pos = np.nextNode();
            } catch (NodeNotFoundException ex) {
                Log.log(
                        "Selected nonexisting node " + ex.getId() + ", edstring so far=" + edstring,
                        LogLevels.FATALERROR, ex);
            }
        } // End for
    }


    protected static Key makeInsertId(IdAddressableRefTree t, Key kg) {
        if (useInsertAutoKey) return fc.xml.xmlr.MutableRefTree.AUTO_KEY;
        return kg;
    }

    /**
     * Interface for picking nodes to edit.
     */
    public interface NodePicker {

        /**
         * Get next pick.
         */
        public RefTreeNode nextNode();


        /**
         * Add pick candidate.
         * @param root
         * @param recurse
         */
        public void addNode(RefTreeNode root, boolean recurse);


        /**
         * Remove pick candidates
         * @param root
         * @param recurse
         */
        public void removeNode(RefTreeNode root, boolean recurse);


        /** Size of picklist. */
        public int size();

    }

    static boolean _verbose = false;

    public static class SafeIdNodePicker implements NodePicker {

        ArrayList<Key> queue = new ArrayList<Key>();
        int length;
        Random rnd;
        int pos = 0;
        IdAddressableRefTree target;


        public SafeIdNodePicker(IdAddressableRefTree target, Random rnd) {
            this(target, rnd, null);
        }


        public SafeIdNodePicker(IdAddressableRefTree target, Random rnd, Key root) {
            this.target = target;
            this.rnd = rnd;
            addNode(root == null ? target.getRoot() : target.getNode(root), true);
            if (_verbose) Log.debug("Queue after init", queue.subList(0, length));
        }


        private void addOne(RefTreeNode n) {
            Key k = n.getId();
            assert n != null;
            int where = rnd.nextInt(length + 1);
            if (where < length) {
                queue.add(length, queue.get(where));
                queue.set(where, k);
            } else queue.add(length, k);
            length++;
        }


        private void remove(Set<Key> s) {
            int initLen = length;
            assert !s.contains(null);
            if (_verbose) Log.debug("Set before remove of " + s, queue.subList(0, length));
            for (int i = 0; i < length; i++) {
                if (s.contains(queue.get(i))) {
                    if (length > 1) {
                        queue.set(i, queue.get(--length));
                        i--; // NOTE: It may be that the one we swapped at the
                        // delpos should be deleted!
                        if (queue.set(length, null) == null) {
                            Log.warning("Queue is", queue.subList(0, length));
                            Log.warning("Deletia is", s);
                        }
                        // assert queue.set(length, null)!=null; // Sets unused
                        // to null
                    } else {
                        queue.clear();
                        length = 0;
                    }
                }
            }
            if (_verbose) Log.debug("Set after remove", queue.subList(0, length));
            assert length == initLen - s.size();

            // assert (new HashSet(queue.subList(0, length))).size() == length;
        }


        public RefTreeNode nextNode() {
            if (_verbose) Log.debug("Picking @ " + (pos + 1), queue.get(((pos + 1) % length)));
            return target.getNode(queue.get(((++pos) % length)));
        }


        public void addNode(RefTreeNode root, boolean recurse) {
            addOne(root);
            if (!recurse) return;
            for (Iterator i = root.getChildIterator(); i.hasNext();) {
                addNode((RefTreeNode) i.next(), recurse);
            }
        };


        public void removeNode(RefTreeNode root, boolean recurse) {
            Set<Key> s = new HashSet<Key>();
            removeNode(root, recurse, s);
            remove(s);
        }


        private void removeNode(RefTreeNode root, boolean recurse, Set<Key> s) {
            s.add(root.getId());
            if (!recurse) return;
            for (Iterator i = root.getChildIterator(); i.hasNext();) {
                removeNode((RefTreeNode) i.next(), recurse, s);
            }
        };


        public int size() {
            return length;
        };

    }

    public static class IdNodePicker implements NodePicker {

        Key pos = null;
        SortedSet<Key> s = null;
        IdAddressableRefTree target = null;;


        public IdNodePicker(Comparator c, IdAddressableRefTree target) {
            this.target = target;
            s = new TreeSet<Key>(c);
            addNode(target.getRoot(), true);
            pos = s.last();
        }


        public RefTreeNode nextNode() {
            pos = nextKey();
            return pos == null ? null : target.getNode(pos);
        }


        protected Key nextKey() {
            try {
                SortedSet<Key> tmp = s.subSet(s.first(), pos);
                // Log.log("tmp="+tmp,Log.INFO);
                Key k = tmp.size() == 0 ? (s.size() > 0 ? s.last() : null) : tmp.last();
                return k;
            } catch (IllegalArgumentException ex) {
                // Happens when old s.first() was removed so pos becomes first
            }
            return s.first();
        }


        public void addNode(RefTreeNode root, boolean recurse) {
            s.add(root.getId());
            if (!recurse) return;
            for (Iterator i = root.getChildIterator(); i.hasNext();) {
                addNode((RefTreeNode) i.next(), recurse);
            }
        };


        public void removeNode(RefTreeNode root, boolean recurse) {
            s.remove(root.getId());
            if (!recurse) return;
            for (Iterator i = root.getChildIterator(); i.hasNext();) {
                removeNode((RefTreeNode) i.next(), recurse);
            }
        };


        public int size() {
            return s.size();
        };
    }

    public static class TransientNodePicker implements NodePicker {

        IdAddressableRefTree target = null;
        Key root;
        Random rnd;
        int size = 0;


        public TransientNodePicker(IdAddressableRefTree target, Random rnd) {
            this(target, rnd, null);
        }


        public TransientNodePicker(IdAddressableRefTree target, Random rnd, Key root) {
            this.rnd = rnd;
            this.target = target;
            this.root = root;
            nextNode(); // Ensure size ok
        }


        public RefTreeNode nextNode() {
            RefTreeNode[] picks = new RefTreeNode[32];
            size = nextNode(root == null ? target.getRoot() : target.getNode(root), picks, 0);
            for (int i = picks.length - 1; i >= 0; --i) {
                if (picks[i] != null) {
                    // Log.debug("Picked "+picks[i].getId());
                    return picks[i];
                }
            }
            // assert false;
            return size > 0 ? nextNode() : null; // Retry... (Mental note: this
            // is going to bite me
            // as infinite recursion
        }


        public int nextNode(RefTreeNode n, RefTreeNode[] picks, int counted) {
            int bin = 0, mask;
            for (int i = counted; i > 2; i >>>= 1)
                bin++;
            mask = (4 << bin) - 1;
            if ((rnd.nextInt() & mask) == 0) {
                // Log.debug("Pick into bin "+bin+", mask="+mask+", count="+counted+
                // " n="+n.getId());
                picks[bin] = n;
            }
            for (Iterator i = n.getChildIterator(); i.hasNext();)
                counted = nextNode((RefTreeNode) i.next(), picks, counted);
            return counted + 1;
        }


        public void addNode(RefTreeNode root, boolean recurse) {
        }


        public void removeNode(RefTreeNode root, boolean recurse) {
        }


        public int size() {
            return size;
        }

    }


    public static MutableDirectoryTree randomDirTree(long nodes, long nodesPerDir, double dirProb,
                                                     double variance, double dirVariance,
                                                     Random rnd, KeyGen kg) {
        Key id = kg.next();
        MutableDirectoryTree t = new MutableDirectoryTree(id,
                                                          new DirectoryEntry(id, null,
                                                                             DirectoryEntry.TREE));
        try {
            LinkedList<Key> roots = new LinkedList<Key>();
            roots.add(t.getRoot().getId());
            while (nodes > 0 && roots.size() > 0) {
                Key parentId = roots.removeFirst();
                long dents = (long) (rnd.nextGaussian() * variance) + nodesPerDir;
                dents = Math.max(1, dents);
                long dirs = (long) (rnd.nextGaussian() * dirVariance + dents * dirProb);
                dirs = Math.max(1, dirs);
                long files = dents - dirs;
                if (files + dirs > nodes) dirs = 0;
                if (files > nodes) files = nodes;
                // Add dirs
                nodes -= (dirs + files);
                for (; dirs > 0; dirs--) {
                    Key nId = kg.next();
                    t.insert(parentId, nId, new DirectoryEntry(nId, dirName(nId, rnd),
                                                               DirectoryEntry.DIR));
                    roots.addLast(nId);
                }
                // Add files
                for (; files > 0; files--) {
                    Key nId = kg.next();
                    t.insert(parentId, nId, new DirectoryEntry(nId, fileName(nId, rnd),
                                                               DirectoryEntry.FILE));
                }
            }
        } catch (NodeNotFoundException ex) {
            Log.log("Tree construction error", LogLevels.ASSERTFAILED, ex);
        }
        return t;
    }


    public static String fileName(Key id, Random rnd) {
        final String[] fnames = { "foo", "bar", "baz", "quup", "ding", "dong", "jabber", "wocky",
                                 "armadillo", "gnu", "gnat" };
        final String[] exts = { "c", "java", "txt", "h", "doc", "xml", "gif", "jpg", "tmp",
                               "class", "ps", "tex" };
        return fnames[rnd.nextInt(fnames.length)] + "-" + id + "." + exts[rnd.nextInt(exts.length)]; // +rnd.nextInt();
    }


    public static String dirName(Key id, Random rnd) {
        final String[] dnames = { "bin", "share", "doc", "linux", "src", "cache", "sbin", "home",
                                 "texmf" };
        return dnames[rnd.nextInt(dnames.length)] + "-" + id;
    }


    public static Key nextId(long id) {
        return StringKey.createKey(id);
    }


    public static RefTree treeCopy(RefTree t) {
        RefTree copy = new RefTreeImpl(treeCopy(t.getRoot(), null));
        assert XmlrDebug.treeComp(t, copy) : "treeCopy broken";
        return copy;
    }


    public static RefTreeNodeImpl treeCopy(RefTreeNode root, RefTreeNode parent) {
        RefTreeNodeImpl newRoot = new RefTreeNodeImpl(parent, root.getId(), root.isTreeRef(), null);
        if (root.getContent() != null)
            newRoot.setContent(new DirectoryEntry((DirectoryEntry) root.getContent()));
        for (Iterator i = root.getChildIterator(); i.hasNext();)
            newRoot.addChild(treeCopy((RefTreeNode) i.next(), newRoot));
        return newRoot;
    }

    public static class RandomizedSetByString implements Comparator {

        int seed;


        public RandomizedSetByString(int seed) {
            this.seed = seed;
        }


        // NOTE: The toString() things are to order the selection by the
        // string rep of the type, not by its real hash code
        public boolean equals(Object o1, Object o2) {
            return compare(o1.toString(), o2.toString()) == 0;
        }


        public int compare(Object o1, Object o2) {
            return (o1.toString().hashCode() ^ seed) - (o2.toString().hashCode() ^ seed);
        }
    }

    public static class RandomizedSet implements Comparator {

        int seed;


        public RandomizedSet(int seed) {
            this.seed = seed;
        }


        // NOTE: The toString() things are to order the selection by the
        // string rep of the type, not by its real hash code
        public boolean equals(Object o1, Object o2) {
            return compare(o1, o2) == 0;
        }


        public int compare(Object o1, Object o2) {
            return (o1.hashCode() ^ seed) - (o2.hashCode() ^ seed);
        }
    }

    public static class DirectoryEntry implements RefTrees.IdentifiableContent {

        /** Constant indicating missing type. */
        public static final int NONE = -1;

        /** Constant indicating directory tree root entry. */
        public static final int TREE = 1;

        /** Constant indicating directory type entry. */
        public static final int DIR = 2;

        /** Constant indicating file type entry. */
        public static final int FILE = 3;

        private Key id;
        private String name;
        private int type;


        public DirectoryEntry() {
            this(null, null, -1);
        }


        public DirectoryEntry(DirectoryEntry src) {
            id = src.id;
            name = src.name;
            type = src.type;
        }


        public DirectoryEntry(Key aId, String aName, int aType) {
            id = aId;
            name = aName;
            type = aType;
        }


        public void setId(Key id) {
            this.id = id;
        }


        public void setName(String name) {
            this.name = name;
        }


        public void setType(int type) {
            this.type = type;
        }


        public Key getId() {
            return id;
        }


        public String getName() {
            if (type == DirectoryEntry.TREE) // The tree has no name attr!
                return null;
            return name;
        }


        public int getType() {
            return type;
        }


        @Override
        public boolean equals(Object o) {
            return o instanceof DirectoryEntry &&
                   (Util.equals(((DirectoryEntry) o).getId(), getId()) &&
                    Util.equals(((DirectoryEntry) o).getName(), getName()) && ((DirectoryEntry) o).getType() == getType());
        }


        @Override
        public int hashCode() {
            return type ^ (name == null ? 0 : name.hashCode()) ^ id.hashCode();
        }


        // dummies
        public String getLocationId() {
            return "#lid#";
        }


        public String getNextId() {
            return "#nextid#";
        }


        public String getUid() {
            return "#uid#-" + getId();
        }


        public int getVersion() {
            return 0;
        }


        public String getLinkNextId() {
            return "#lnextid#";
        }


        public String getLinkUid() {
            return "#luid#";
        }


        public int getLinkVersion() {
            return 1;
        }


        public String getLinkId() {
            return id.toString();
        }


        @Override
        public String toString() {
            return (type == FILE ? "file" : (type == DIR ? "dir" : "tree ")) + "{name=" + name +
                   ",...}";
        }
    }

    public static class MutableDirectoryTree extends AbstractMutableRefTree {

        private Map<Key, RefTreeNode> index = new HashMap<Key, RefTreeNode>();
        private boolean ordered = false;

        private RefTreeNodeImpl root = null;


        public MutableDirectoryTree(Key rootId, Object content) {
            root = new RefTreeNodeImpl(null, rootId, false, content);
            index.put(rootId, root);
        }


        public MutableDirectoryTree(RefTree initTree) {
            root = (RefTreeNodeImpl) initTree.getRoot();
            init(root);
        }


        // The MutableDirectoryTree iface ------
        @Override
        public RefTreeNode getNode(Key id) {
            return index.get(id);
        }


        @Override
        public void delete(Key id) throws NodeNotFoundException {
            // Log.debug("Delete",id);
            RefTreeNodeImpl n = (RefTreeNodeImpl) index.get(id);
            if (n == null) throw new NodeNotFoundException(id);
            RefTreeNodeImpl p = (RefTreeNodeImpl) n.getParent();
            if (p == null) throw new IllegalArgumentException("Trying to delete root");
            if (index.get(p.getId()) == null) throw new NodeNotFoundException(id); // Parent was
            // previously
            // deleted!
            if (p == null) root = null;
            else {
                cleanIndex(n);
                boolean deleted = p.removeChild(n);
                assert deleted;
            }
        }


        private void cleanIndex(RefTreeNode n) {
            // Log.info("Removing id ",n.getId());
            index.remove(n.getId());
            for (Object c : Util.iterable(n.getChildIterator()))
                cleanIndex((RefTreeNode) c);
        }


        @Override
        public Key insert(Key parentId, long pos, Key newId, Object content)
                throws NodeNotFoundException {
            // Log.debug("Insert @"+parentId,newId);
            if (content == null || pos != MutableRefTree.DEFAULT_POSITION)
                Log.log("Invalid op", LogLevels.ASSERTFAILED);
            RefTreeNodeImpl n = (RefTreeNodeImpl) index.get(parentId);
            if (n == null) throw new NodeNotFoundException(parentId);
            RefTreeNodeImpl newNode = new RefTreeNodeImpl(n, newId, false, content);
            if (index.put(newId, newNode) != null)
                Log.log("Duplicate id " + newId, LogLevels.ASSERTFAILED);
            if (!ordered) n.addChild(newNode);
            else {
                n.addChild(getTargetPos(n, newNode), newNode);
            }
            return newId;
        }


        @Override
        public Key move(Key nodeId, Key parentId, long pos) throws NodeNotFoundException {
            // Log.debug("Move "+nodeId+" to"+parentId);
            if (pos != MutableRefTree.DEFAULT_POSITION)
                Log.log("Invalid op", LogLevels.ASSERTFAILED);
            RefTreeNodeImpl n = (RefTreeNodeImpl) index.get(nodeId);
            if (n == null) throw new NodeNotFoundException(nodeId);
            RefTreeNodeImpl pNew = (RefTreeNodeImpl) index.get(parentId);
            if (pNew == null) throw new NodeNotFoundException(parentId);
            RefTreeNodeImpl p = (RefTreeNodeImpl) n.getParent();
            if (p == null) Log.log("Tried to move root", Log.ASSERTFAILED);
            p.removeChild(n);
            if (!ordered) pNew.addChild(n);
            else {
                pNew.addChild(getTargetPos(pNew, n), n);
            }
            return n.getId();
        }


        public boolean update(Key nodeId, Object content) throws NodeNotFoundException {
            // Log.debug("Update",nodeId);
            if (content == null) Log.log("Invalid op", Log.ASSERTFAILED);
            RefTreeNodeImpl n = (RefTreeNodeImpl) index.get(nodeId);
            if (n == null) throw new NodeNotFoundException(nodeId);
            if (!content.equals(n.getContent())) {
                n.setContent(content);
                return true;
            }
            return false;
        }


        public RefTreeNode getRoot() {
            return root;
        }


        private int getTargetPos(RefTreeNode parent, RefTreeNode n) {
            int pos = 0;
            for (Iterator i = parent.getChildIterator(); i.hasNext();) {
                if (ID_BY_ORDER.compare(n, i.next()) > 0) pos++;
                else return pos;
            }
            return pos;

        }


        private void init(RefTreeNode root) {
            if (index.put(root.getId(), root) != null) Log.log("Duplicate ids", Log.ASSERTFAILED);
            for (Iterator i = root.getChildIterator(); i.hasNext();)
                init((RefTreeNode) i.next());
        }

        public static final Comparator ID_BY_ORDER = new Comparator() {

            public int compare(Object o1, Object o2) {
                Key id1 = ((RefTreeNode) o1).getId();
                Key id2 = ((RefTreeNode) o2).getId();
                return id1 == null ? (id1 == id2 ? 0 : -1) : ((Comparable) id1).compareTo(id2);
            }
        };


        public void setOrdered(boolean ordered) {
            this.ordered = ordered;
        }

    }

    public static class KeyGen {

        long id = 0l;
        long max = Long.MAX_VALUE;


        public KeyGen(long start) {
            id = start;
        }


        public KeyGen(KeyGen start) {
            id = start.id;
        }


        public KeyGen(LongKey first, long keys) {
            id = first.id;
            max = id + keys;
        }


        public Key next() {
            assert id < max;
            return new LongKey(id++);
        }
    }

    public static class LongKey implements Key, Comparable {

        long id;


        public LongKey(long id) {
            this.id = id;
        }


        public String toString() {
            return /* "lk-"+ */String.valueOf(id);
        }


        public boolean equals(Object obj) {
            return obj instanceof LongKey && obj != null && ((LongKey) obj).id == id;
        }


        public int hashCode() {
            return (int) ((id & 0xfffff) ^ (id >>> 32));
        }


        public int compareTo(Object o) {
            return (int) (id - ((LongKey) o).id);
        }
    }
}
// arch-tag: ade0a69d-59ab-4c9d-87ca-f82c4bab6a13
