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
import static fc.xml.xmlr.test.RandomDirectoryTree.randomDirTree;
import static fc.xml.xmlr.test.RandomDirectoryTree.treeCopy;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;
import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xmlr.ChangeBuffer;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.XmlrDebug;
import fc.xml.xmlr.test.RandomDirectoryTree.KeyGen;
import fc.xml.xmlr.test.RandomDirectoryTree.LongKey;
import fc.xml.xmlr.xas.MutableDeweyRefTree;

public class TestDeweyTrees extends TestCase {

    private static final long RND_SEED = 42L;


    protected MutableRefTree makeTestTree(IdAddressableRefTree baseTree) {
        MutableDeweyRefTree mdrt = new MutableDeweyRefTree();
        return new ChangeBuffer(mdrt, baseTree, mdrt);
    }


    public void testChangeTree() {
        // NOTE This code is 2x slower with DSIZE=50 compared to DSIZE=5
        // Why is this?? Child list sorting woes?
        int OPS = 200;
        String PDF = "iiiiiiiiiIdddddduuuuuuuuummmm";
        double DIRP = 0.05;
        long TSIZE = 1000;
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
        // baseKg = new KeyGen(4905981);
        for (int lap = 0; lap < MAX_LAP; lap++) {
            Log.info("Lap " + (lap + 1) + " of " + MAX_LAP + ", keygen at " +
                     (new KeyGen(baseKg)).next());
            MutableRefTree dt = randomDirTree(TSIZE, DSIZE, DPROB, VAR, DVAR, new Random(lap ^
                                                                                         RND_SEED),
                                              baseKg);
            RefTree baseTree = treeCopy(dt);
            IdAddressableRefTree baseTreeD = RefTrees.getAddressableTree(new DeweyKeyedRefTree(
                                                                                               baseTree));

            MutableRefTree ct = makeTestTree(baseTreeD); // new
            // ChangeTree(baseTree);
            // Log.info("Init test tree");
            // XmlrDebug.dumpTree(baseTree,System.out);
            /*
             * Testing the transientNodePicker RandomDirectoryTree.NodePicker np = new
             * RandomDirectoryTree.TransientNodePicker(dt, new Random(lap^RND_SEED)); String ids
             * =""; for(int i=0;i<1000;i++) ids+=np.nextNode().getId().toString()+", ";
             * Log.debug("Some ids are ",ids);
             */
            KeyGen kg = new KeyGen(baseKg);
            RandomDirectoryTree.edstring = null;
            // RandomDirectoryTree.edstring="\n";
            permutateTree(dt, OPS, PDF, DPROB, new Random(lap ^ RND_SEED), kg,
                          new RandomDirectoryTree.TransientNodePicker(dt,
                                                                      new Random(lap ^ RND_SEED)));
            if (RandomDirectoryTree.edstring != null &&
                RandomDirectoryTree.edstring.trim().length() == 0) {
                Log.info("Empty edit; going to next lap");
                continue;
            }

            // Log.info("Edit string for facit is ",RandomDirectoryTree.edstring);
            // RandomDirectoryTree.edstring+="------\n";
            kg = new KeyGen(baseKg); //
            RandomDirectoryTree.useInsertAutoKey = true;
            permutateTree(ct, OPS, PDF, DPROB, new Random(lap ^ RND_SEED), kg,
                          new RandomDirectoryTree.TransientNodePicker(ct,
                                                                      new Random(lap ^ RND_SEED)));
            RandomDirectoryTree.useInsertAutoKey = false;
            DeweyKeyedRefTree deweyDt = new DeweyKeyedRefTree(dt);
            // Log.info("Edit string is ",RandomDirectoryTree.edstring);
            /*
             * Log.info("Back tree"); XmlrDebug.dumpTree(baseTree,System.out);
             * Log.info("Facit tree (Deweyed)"); XmlrDebug.dumpTree(deweyDt,System.out);
             * Log.info("Test tree"); XmlrDebug.dumpTree(ct,System.out);
             */
            // ((MutableDeweyRefTree) ((ChangeBuffer)
            // ct).getChangeTree()).dumpDebug();
            /*
             * Log.info("Change tree"); XmlrDebug.dumpTree( ((ChangeBuffer) ct).getChangeTree() );
             */
            Assert.assertTrue(treeComp(ct, deweyDt));
            // Check contains() status of nodes
            Set<Key> baseSetD = buildSet(baseTreeD.getRoot(), new HashSet<Key>());
            Set<Key> newSetD = buildSet(deweyDt.getRoot(), new HashSet<Key>());
            Set<Key> baseSet = buildSet(baseTree.getRoot(), new HashSet<Key>());
            Set<Key> newSet = buildSet(dt.getRoot(), new HashSet<Key>());
            Set<Key> newSetCtD = buildSet(ct.getRoot(), new HashSet<Key>());
            Assert.assertEquals("Node sets not equal", newSetD, newSetCtD);
            Set<Key> deletiaD = new HashSet<Key>(baseSetD);
            deletiaD.removeAll(newSetD);
            Set<Key> deletia = new HashSet<Key>(baseSet);
            deletia.removeAll(newSet);
            for (Key k : newSetD)
                Assert.assertTrue("Missing key in changeTree: " + k, ct.contains(k));
            for (Key k : deletiaD)
                Assert.assertFalse("Key should be deleted in changeTree: " + k, ct.contains(k));
            int resurrects = 20;
            for (Key k : deletia) {
                kg = new KeyGen((LongKey) k, 1);
                permutateTree(dt, 1, "iI", DPROB, new Random(lap ^ resurrects ^ RND_SEED), kg);
                assert dt.contains(k) : "Node did not appear in facit!" + k;
                kg = new KeyGen((LongKey) k, 1);
                RandomDirectoryTree.useInsertAutoKey = true;
                permutateTree(ct, 1, "iI", DPROB, new Random(lap ^ resurrects ^ RND_SEED), kg);
                RandomDirectoryTree.useInsertAutoKey = false;
                // k is wrong key:
                // Assert.assertTrue("Re-inserted node is missing: "+k,ct.contains(k));
                if (--resurrects < 0) break;
            }
            Assert.assertTrue("Differing tree after delete resurrect",
                              treeComp(ct, new DeweyKeyedRefTree(dt)));
        }
        Log.info("Congratulations; you are a spiffy hacker.");
    }


    private Set<Key> buildSet(RefTreeNode n, Set<Key> s) {
        s.add(n.getId());
        for (Object c : Util.iterable(n.getChildIterator()))
            buildSet((RefTreeNode) c, s);
        return s;
    }

}
// arch-tag: d138fb94-3364-4053-b391-3d769f8612af
