/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.test;

import static fc.xml.xmlr.XmlrDebug.treeComp;
import static fc.xml.xmlr.test.RandomDirectoryTree.KEY_GEN;
import static fc.xml.xmlr.test.RandomDirectoryTree.permutateTree;
import static fc.xml.xmlr.test.RandomDirectoryTree.permutateTreeStringOrder;
import static fc.xml.xmlr.test.RandomDirectoryTree.randomDirTree;
import static fc.xml.xmlr.test.RandomDirectoryTree.treeCopy;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;
import fc.util.CompareUtil;
import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xmlr.ChangeBuffer;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.KeyMap;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.XmlrDebug;
import fc.xml.xmlr.model.StringKey;
import fc.xml.xmlr.test.RandomDirectoryTree.KeyGen;
import fc.xml.xmlr.test.RandomDirectoryTree.LongKey;

public class TestChangeTree extends TestCase {

    private static final long RND_SEED = 42L;


    protected MutableRefTree makeTestTree(IdAddressableRefTree baseTree) {
        return new ChangeBuffer(baseTree);
    }


    public void testChangeTree() {
        int OPS = 200;
        String PDF = "iiiiiiiiiIdddddduuuuuuuuummmm";
        double DIRP = 0.05;
        long TSIZE = 5000;
        long DSIZE = 20;
        double DPROB = 0.1;
        double VAR = 5.0;
        double DVAR = 2.0;
        Log.info("Testing ChangeTree by repeatedly making " + OPS +
                 " random edits on tree of size " + TSIZE + ". Deleting and the inserting " +
                 " another node with the same id is also tested");
        Log.info("In reports below, changebuffer is tree 1, facit tree 2");
        KeyGen baseKg = KEY_GEN;
        int MAX_LAP = 25;
        // baseKg = new KeyGen(33777);
        for (int lap = 0; lap < MAX_LAP; lap++) {
            Log.info("Lap " + (lap + 1) + " of " + MAX_LAP + ", keygen at " +
                     (new KeyGen(baseKg)).next());
            MutableRefTree dt = randomDirTree(TSIZE, DSIZE, DPROB, VAR, DVAR, new Random(lap ^
                                                                                         RND_SEED),
                                              baseKg);
            ((RandomDirectoryTree.MutableDirectoryTree) dt).setOrdered(true);
            IdAddressableRefTree baseTree = RefTrees.getAddressableTree(treeCopy(dt));
            MutableRefTree ct = makeTestTree(baseTree); // new
            // ChangeTree(baseTree);
            // Log.info("Init test tree");XmlrDebug.dumpTree(dt,System.out);
            KeyGen kg = new KeyGen(baseKg);
            // RandomDirectoryTree.edstring="\n";
            // RandomDirectoryTree._verbose = true;
            permutateTree(dt, OPS, PDF, DPROB, new Random(lap ^ RND_SEED), kg);
            // Log.info("Editstring for facit is ",RandomDirectoryTree.edstring);
            // RandomDirectoryTree.edstring+="------\n";
            kg = new KeyGen(baseKg); // 
            permutateTree(ct, OPS, PDF, DPROB, new Random(lap ^ RND_SEED), kg);
            /*
             * Log.info("Editstring is ",RandomDirectoryTree.edstring); Log.info("Back tree");
             * XmlrDebug.dumpTree(baseTree,System.out); Log.info("Facit tree");
             * XmlrDebug.dumpTree(dt,System.out); Log.info("Test tree");
             * XmlrDebug.dumpTree(ct,System.out);
             */
            // Log.info("Treecmp=",treeComp(dt,baseTree));
            Assert.assertTrue(treeComp(ct, dt));
            // Check contains() status of nodes
            Set<Key> baseSet = buildSet(baseTree.getRoot(), new HashSet<Key>());
            Set<Key> newSet = buildSet(dt.getRoot(), new HashSet<Key>());
            Set<Key> newSetCt = buildSet(ct.getRoot(), new HashSet<Key>());
            Assert.assertEquals("Node sets not equal", newSet, newSetCt);
            Set<Key> deletia = new HashSet<Key>(baseSet);
            deletia.removeAll(newSet);
            for (Key k : newSet)
                Assert.assertTrue("Missing key in changetree: " + k, ct.contains(k));
            for (Key k : deletia)
                Assert.assertFalse("Key should be deleted in changetree: " + k, ct.contains(k));
            int resurrects = 1;
            // RandomDirectoryTree.edstring="\n";
            // Log.debug("====> Resurrect phase  <==========");
            for (Key k : deletia) {
                // Log.debug("--FACIT TREE--");
                kg = new KeyGen((LongKey) k, 1);
                permutateTree(dt, 1, "iI", DPROB, new Random(lap ^ resurrects ^ RND_SEED), kg);
                assert dt.contains(k) : "Node did not appear in facit!" + k;
                // Log.debug("--TEST TREE--");
                kg = new KeyGen((LongKey) k, 1);
                permutateTree(ct, 1, "iI", DPROB, new Random(lap ^ resurrects ^ RND_SEED), kg);
                Assert.assertTrue("Re-inserted node is missing: " + k, ct.contains(k));
                if (--resurrects < 0) break;
            }
            /*
             * Log.debug("Resurrect edits were (edit pairs, <facit,test>)",
             * RandomDirectoryTree.edstring); RandomDirectoryTree.edstring=null;
             * Log.info("Facit tree"); XmlrDebug.dumpTree(dt,System.out); Log.info("Test tree");
             * XmlrDebug.dumpTree(ct,System.out);
             */

            Assert.assertTrue("Differing tree after delete resurrect", treeComp(ct, dt));
        }
        Log.info("Congratulations; you are a spiffy hacker.");
    }


    private Set<Key> buildSet(RefTreeNode n, Set<Key> s) {
        s.add(n.getId());
        for (Object c : Util.iterable(n.getChildIterator()))
            buildSet((RefTreeNode) c, s);
        return s;
    }


    public void testChangeTreeIdMap() throws NodeNotFoundException {
        // NOTE: This dies at lap 260 with the parameters below in backingTree
        // permutation, with a TreeSet.last() NoSuchElementException, which is
        // weird, since the tested sets should be nonempty... maybe a Java
        // TreeSet bug?
        int OPS = 1;
        String PDF = "iiiiiiiiiIIIIIdddddduuuuuuuuummmm";
        long TSIZE = 5000;
        long DSIZE = 5;
        double DPROB = 0.01;
        double VAR = 5.0;
        double DVAR = 2.0;
        Log.info("In reports below, changebuffer is tree 1, facit tree 2");
        KeyGen baseKg = KEY_GEN;
        int MAX_LAP = 25;
        MutableRefTree bt = null;
        long startKey = -1l;
        for (int lap = 0; lap < MAX_LAP; lap++) {
            Log.info("Lap " + (lap + 1) + " of " + MAX_LAP);
            if (lap % 4 == 0) {
                startKey = 1000000000l;
                bt = randomDirTree(TSIZE, DSIZE, DPROB, VAR, DVAR, new Random(1 ^ RND_SEED),
                                   new KeyGen(0l) {});
                ((RandomDirectoryTree.MutableDirectoryTree) bt).setOrdered(true);
            } else {
                startKey += 1000000l;
            }
            IdAddressableRefTree baseTree = RefTrees.getAddressableTree(treeCopy(bt));
            ChangeBuffer dt = new ChangeBuffer(baseTree, new KeyMap() {

                public Key getBackKey(Key fk) {
                    return fk == null ? null
                            : new LongKey(Long.parseLong(((StringKey) fk).toString()));
                }


                public Key getFrontKey(Key bk) {
                    return bk == null ? null : StringKey.createKey(((LongKey) bk).id);
                }
            });
            checkKeyType(dt.getRoot(), StringKey.class);
            // Log.debug("====> Permutate of backtree <==========");
            permutateTreeStringOrder(bt, OPS + lap, PDF, DPROB, new Random(lap ^ RND_SEED),
                                     new KeyGen(startKey) {});
            // RandomDirectoryTree.edstring="";
            // Log.debug("====> Permutate of changeTree <==========");
            permutateTreeStringOrder(dt, OPS + lap, PDF, DPROB, new Random(lap ^ RND_SEED),
                                     new KeyGen(startKey) {

                                         @Override
                                         public Key next() {
                                             assert id < max;
                                             return new StringKey(String.valueOf(id++));
                                         }
                                     });
            // Log.debug("Changetree edit string is",RandomDirectoryTree.edstring);
            // RandomDirectoryTree.edstring="";
            // Log.debug("Backtree edit string is",RandomDirectoryTree.edstring);
            // Log.debug("====> Permutate DONE <==========");
            checkKeyType(dt.getRoot(), StringKey.class);
            Set<Key> randomNodes = new HashSet<Key>();
            randomSelection(dt.getRoot(), randomNodes, 10, new Random(lap ^ RND_SEED));
            // Check return key type for random access pattern
            // Log.debug("Random id selection", randomNodes);
            for (Key k : randomNodes)
                Assert.assertEquals(dt.getNode(k).getId().getClass(), StringKey.class);
            /*
             * System.out.println("Initial Tree"); XmlrDebug.dumpTree(baseTree,System.out);
             * System.out.println("Facit Tree"); XmlrDebug.dumpTree(bt,System.out);
             * System.out.println("Tested Tree"); XmlrDebug.dumpTree(dt,System.out);
             * System.out.flush();
             */
            Assert.assertEquals(0, XmlrDebug.treeComp(bt.getRoot(), dt.getRoot(),
                                                      new LongKeyDestMap(), 0,
                                                      CompareUtil.AS_STRINGS, true, true));
            dt.reset();
            Assert.assertEquals(0, XmlrDebug.treeComp(baseTree.getRoot(), dt.getRoot(),
                                                      new LongKeyDestMap(), 0,
                                                      CompareUtil.AS_STRINGS, true, true));
        }
    }

    private static class LongKeyDestMap extends RefTrees.IdMap {

        @Override
        public Key getDestId(Key srcId, RefTreeNode src) throws NodeNotFoundException {
            return new StringKey(srcId.toString());
        }


        @Override
        public Key getSrcId(Key dstId, RefTreeNode dest) throws NodeNotFoundException {
            return null;
        }
    }


    private void checkKeyType(RefTreeNode n, Class cl) {
        Assert.assertEquals(cl, n.getId().getClass());
        for (Object c : Util.iterable(n.getChildIterator()))
            checkKeyType((RefTreeNode) c, cl);
    }


    private void randomSelection(RefTreeNode n, Set<Key> ks, int ppm, Random rnd) {
        if ((rnd.nextInt() & 1023) <= ppm) ks.add(n.getId());
        for (Object c : Util.iterable(n.getChildIterator()))
            randomSelection((RefTreeNode) c, ks, ppm, rnd);
    }

    /*
     * DEAD CODE static class IdMappedChangeTree extends ChangeTree {
     * 
     * public IdMappedChangeTree(IdAddressableRefTree backingTree) { super(backingTree); }
     * 
     * protected boolean frontkey(Key id) { return id==null || id instanceof StringKey; }
     * 
     * protected boolean backkey(Key id) { return id==null || id instanceof LongKey; }
     * 
     * protected void initKeyIndex() { ki = new IdMapKI(); }
     * 
     * // Frontkey is string, backkey is longkey static class IdMapKI implements
     * ChangeTree.MutableMappingIndex {
     * 
     * private Map nodeById = new HashMap();
     * 
     * 
     * public Key getBackKey(Key fk) { return fk == null ? null : new
     * LongKey(Long.parseLong(((StringKey) fk).toString())); }
     * 
     * public Key getFrontKey(Key bk) { return bk == null ? null : StringKey.createKey(((LongKey)
     * bk).id); }
     * 
     * public Key forge(Key insKey) { return insKey; }
     * 
     * public RefTreeNode get(Key fk) { return (RefTreeNode) nodeById.get((StringKey) fk); }
     * 
     * public void move(Key fk, RefTreeNode n) { nodeById.put((StringKey) fk, n); }
     * 
     * public RefTreeNode remove(Key fk) { return (RefTreeNode) nodeById.remove((StringKey) fk); }
     * 
     * } }
     */
}
// arch-tag: 44b8861d-3f46-4ac2-ac93-c544e2de9166
