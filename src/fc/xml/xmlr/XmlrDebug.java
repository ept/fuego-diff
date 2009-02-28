/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fc.util.CompareUtil;
import fc.util.Debug;
import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xmlr.model.TransientKey;

/** Various utility functions to aid debugging. */

public class XmlrDebug {

    /**
     * Compare two reftrees.
     * @param t1
     *            tree 1
     * @param t2
     *            tree 2
     * @return <code>false</code> if trees do not match
     */
    public static boolean treeComp(RefTree t1, RefTree t2) {
        return treeComp(t1, t2, CompareUtil.OBJECT_EQUALITY, true);
    }


    /**
     * Compare two reftrees.
     * @param t1
     *            tree 1
     * @param t2
     *            tree 2
     * @param flagErrors
     *            <code>true</code> if errors are logged
     * @return <code>false</code> if trees do not match
     */
    public static boolean treeComp(RefTree t1, RefTree t2, boolean flagErrors) {
        return treeComp(t1, t2, CompareUtil.OBJECT_EQUALITY, flagErrors);
    }


    /**
     * Compare two reftrees.
     * @param t1
     *            tree 1
     * @param t2
     *            tree 2
     * @param idcmp
     *            comparator for keys
     * @param flagErrors
     *            <code>true</code> if errors are logged
     * @return <code>false</code> if trees do not match
     */
    public static boolean treeComp(RefTree t1, RefTree t2, Comparator idcmp, boolean flagErrors) {
        try {
            return treeComp(t1.getRoot(), t2.getRoot(), RefTrees.getIdentityIdMap(), 0, idcmp,
                            false, flagErrors) == 0;
        } catch (NodeNotFoundException ex) {
            Log.error("Exception", ex);
        }
        return false;
    }


    /**
     * Compare two reftrees.
     * @param r1
     *            root of tree 1
     * @param r2
     *            root of tree 1
     * @param map
     *            NOT USED: key map
     * @param fc
     *            difference counter
     * @param idcmp
     *            comparator for keys
     * @param contentAsStrings
     *            <code>true</code> if content is compared as strings
     * @param flagerrors
     *            <code>true</code> if errors are logged
     * @return number of differences + <i>fc</i>
     * @throws NodeNotFoundException
     *             if an error occurs
     */
    // identical = 0
    public static int treeComp(RefTreeNode r1, RefTreeNode r2, RefTrees.IdMap map, int fc,
                               Comparator idcmp, boolean contentAsStrings, boolean flagerrors)
            throws NodeNotFoundException {
        if (r1 == null || r2 == null) {
            Log.log("A node is missing, id=" + (r1 == null ? r2.getId() : r1.getId()) + " tree " +
                    (r1 == null ? "1" : "2"), Log.ERROR);
            return fc + 1;
        }
        if (idcmp.compare(r1.getId(), r2.getId()) != 0) {
            if (flagerrors) {
                Log.log("ID mismatch: " + r1.getId() + ", " + r2.getId(), Log.ERROR);
                Log.log("ID classes: " + r1.getId().getClass() + ", " + r2.getId().getClass(),
                        Log.FATALERROR);
            }

            fc++;
        }
        if (r1.isTreeRef() || r2.isTreeRef()) { return fc; // Matching tree refs
        }
        if (!r1.isNodeRef() && !r2.isNodeRef()) {
            Object c1 = r1.getContent();
            Object c2 = c1 == r1.getContent() ? r2.getContent() : r1.getContent();
            boolean badContent = (contentAsStrings && !Util.equals(c1.toString(), c2.toString())) ||
                                 (!contentAsStrings && !Util.equals(c1, c2));
            if (badContent) {
                if (flagerrors)
                    Log.log("Content mismatch at key " + r1.getId() + ": " +
                            Debug.toString(r1.getContent()) + "<->" +
                            Debug.toString(r2.getContent()), Log.FATALERROR);
                fc++;
            }
        }
        int r1cc = 0; // r1.getChildCount();
        int r2cc = -1; // r2.getChildCount();
        Iterator i2 = r2.getChildIterator();
        Map r2Index = new HashMap();
        for (Iterator i = r2.getChildIterator(); i.hasNext();) {
            RefTreeNode n = (RefTreeNode) i.next();
            r2Index.put(n.getId(), n);
        }
        r2cc = r2Index.size();
        for (Iterator i = r1.getChildIterator(); i.hasNext();) {
            RefTreeNode n = (RefTreeNode) i.next();
            Key mappedid = (Key) map.getDestId(n);
            fc = treeComp(n, (RefTreeNode) r2Index.get(mappedid), map, fc, idcmp, contentAsStrings,
                          flagerrors);
            r1cc++;
        }
        if (r1cc != r2cc) {
            if (flagerrors)
                Log.log("Childcount mismatch below " + r1.getId() + " (counts are " + r1cc + "," +
                        r2cc + ")", Log.FATALERROR);
            return fc++;
        }
        return fc;
    }


    /**
     * Compares two reftrees by node equality. Also requires child order to match exactly. Note: any
     * references are <b>not</b> expanded, i.e., if there is a reference in one tree, the exact same
     * kind of reference must be in the other tree.
     * @param t1
     *            first tree
     * @param t2
     *            second tree
     * @return <code>true</code> if equals
     */
    public static boolean equalityTreeComp(RefTree t1, RefTree t2) {
        return equalityTreeComp(t1.getRoot(), t2.getRoot());
    }


    public static boolean equalityTreeComp(RefTreeNode n, RefTreeNode m) {
        if (!Util.equals(n, m)) {
            Log.error("Nodes do not match");
            Log.error("First node ", n);
            Log.error("Second node ", m);
            return false;
        }
        int cc = 0;
        for (Iterator<RefTreeNode> in = n.getChildIterator(); in.hasNext();) {
            in.next();
            cc++;
        }
        Iterator<RefTreeNode> in = n.getChildIterator();
        boolean ok = true;
        for (Iterator<RefTreeNode> im = m.getChildIterator(); im.hasNext() && ok;) {
            if (!in.hasNext()) {
                Log.error("Child list of tree 1 ended before tree 2 at id2=" + im.next().getId());
                return false;
            }
            ok = ok & equalityTreeComp(in.next(), im.next());
            cc--;
        }
        if (ok && cc > 0) {
            Log.error("Tree1 has " + cc + " nodes more in child list of id1=" + n.getId());
            return false;
        }
        return ok;
    }


    /**
     * Debug dump reftree. Tree is dumped to <code>Log.getLogStream(Log.DEBUG)</code>.
     * @param t
     *            tree
     */

    public static void dumpTree(RefTree t) {
        if (!Log.isEnabled(Log.DEBUG)) return;
        dumpTree(t, new PrintStream(Log.getLogStream(Log.DEBUG)));
    }


    public static void dumpTree(RefTree t, Class keyClass) {
        if (!Log.isEnabled(Log.DEBUG)) return;
        dumpTree(t.getRoot(), keyClass, 0, new PrintStream(Log.getLogStream(Log.DEBUG)));
    }


    /**
     * Debug dump reftree.
     * @param t
     *            tree
     * @param out
     *            dump target
     */

    public static void dumpTree(RefTree t, PrintStream out) {
        Class kc = t.getRoot() != null ? (t.getRoot().getId() != null ? t.getRoot().getId().getClass()
                : null)
                : null;
        dumpTree(t.getRoot(), kc, 0, out);
        out.flush();
    }


    /**
     * Debug dump reftree.
     * @param root
     *            root node
     * @param keyClass
     *            class of keys. Keys of other classes are annotated with their class in the dump.
     * @param level
     *            indentation level
     * @param out
     *            dump target
     */
    public static void dumpTree(RefTreeNode root, Class keyClass, int level, PrintStream out) {
        // if( !Log.isEnabled(Log.DEBUG) )
        // return;
        Object c = root == null ? "" : root.getContent();
        String cstr = "";
        if (root != null) {
            cstr = !root.isReference() ? Debug.toPrintable(c == null ? "<null>" : c.toString())
                    : fmtRef(root, keyClass);
        }
        // NOTE 1024 token limit is to avoid X crashing into oblivion on Ubuntu
        // 6.06 :)
        // (Guess: the nvidia graphics driver has a severe buffer overflow... )
        String idstr = root == null ? "<null node>" : (root.getId() == null ? null
                : (root.getId().getClass() == keyClass ? guardTransient(root.getId())
                        : guardTransient(root.getId()) + "[" + fmtClass(root.getId().getClass()) +
                          "]"));
        out.println("                                       ".substring(0, level) + idstr + ": " +
                    (cstr.length() > 1024 ? cstr.substring(0, 1024) + "..." : cstr) +
                    (root != null ? " " + fmtClass(root.getClass()) : ""));
        if (root != null) {
            for (Iterator i = root.getChildIterator(); i.hasNext();)
                dumpTree((RefTreeNode) i.next(), keyClass, level + 1, out);
        }
    }


    private static String fmtRef(RefTreeNode n, Class keyClass) {
        String s = (n.isTreeRef() ? "<T " : "<n ") + n.getReference().getTarget();
        Key target = n.getReference().getTarget();
        if (target != null && target.getClass() != keyClass) {
            s += "[" + fmtClass(target.getClass()) + "]";
        }
        return s + ">";
    }


    protected static String fmtClass(Class c) {
        if (c == null) return "<null>";
        String s = c.getCanonicalName();
        int split = Math.max(s.lastIndexOf('.'), s.lastIndexOf('$'));
        return split > -1 ? s.substring(split + 1) : s;
    }


    protected static String guardTransient(Key k) {
        return k instanceof TransientKey ? ((TransientKey) k).debugString() : k.toString();
    }


    /** Return string listing objects from iterator. */
    public static String toString(Iterator i) {
        return toString(i, Integer.MAX_VALUE);
    }


    /**
     * Return string listing objects from iterator.
     * @param i
     *            iterator
     * @param max
     *            maximum number of items
     * @return string listing objects from iterator.
     */
    public static String toString(Iterator i, int max) {
        StringBuilder sb = new StringBuilder('[');
        for (; i.hasNext() && max > 0; max--) {
            String step = i.next().toString();
            sb.append((sb.length() > 1 ? ", " : "") +
                      (step.length() > 1024 ? step.substring(0, 1024) + "..." : step));
        }
        return sb.append(']').toString();
    }
}
// arch-tag: b893e658-7c47-459e-b125-1e9ce7341412

