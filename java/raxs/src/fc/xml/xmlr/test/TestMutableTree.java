/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-raxs-users@hoslab.cs.helsinki.fi.
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
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.MutableRefTreeImpl;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.test.RandomDirectoryTree.KeyGen;
import fc.xml.xmlr.test.RandomDirectoryTree.LongKey;

public class TestMutableTree extends TestCase {

  private static final long RND_SEED = 42L;

  protected MutableRefTree buildCandidateInstance(Key rootId) {
    return new MutableRefTreeImpl(rootId,
        MutableRefTreeImpl.ID_AS_STRINGS_BY_LENGTH_ALPHA);
    
  }
  
  public void testMutableTree() throws NodeNotFoundException {
    String PDF="iiiiiiiiiIdddddduuuuuuuuummmm";
    double DIRP=0.05;
    long TSIZE=5000;long DSIZE=5;double DPROB=0.01;double VAR=5.0;double DVAR=2.0;
    Log.info("Testing MutableRefTree");
    Log.info("In reports below, changebuffer is tree 1, facit tree 2");
    KeyGen baseKg = KEY_GEN;
    int MAX_LAP=25;
    for( int lap = 0;lap < MAX_LAP;lap++) {
      Log.info("Lap "+(lap+1)+" of "+MAX_LAP);
      
      MutableRefTree dt = randomDirTree(TSIZE,DSIZE,DPROB,VAR,DVAR, 
          new Random(1^RND_SEED),baseKg);
      ((RandomDirectoryTree.MutableDirectoryTree) dt).setOrdered(true);
      // Copy it
      MutableRefTree tt = buildCandidateInstance(dt.getRoot().getId());
      tt.insert(null, dt.getRoot().getId(), dt.getRoot().getContent());
      RefTrees.apply(dt, tt);
      Assert.assertTrue("Differing tree after copy",treeComp(dt,tt));        

      IdAddressableRefTree baseTree = RefTrees.getAddressableTree(treeCopy(dt));

      // Change it
      KeyGen kg = new KeyGen(baseKg);
      //RandomDirectoryTree.edstring = "";
      permutateTree(dt,10*lap,PDF,DPROB,new Random(lap^RND_SEED),kg);
      //Log.debug("DirTree edits",RandomDirectoryTree.edstring);
      //RandomDirectoryTree.edstring = "";
      kg = new KeyGen(baseKg); 
      permutateTree(tt,10*lap,PDF,DPROB,new Random(lap^RND_SEED),kg);
      //Log.debug("TestTree edits",RandomDirectoryTree.edstring);

      //TreeUtil.dumpTree(dt, System.out);
      //TreeUtil.dumpTree(tt, System.out);
      Assert.assertTrue("Differing tree after edit",treeComp(dt,tt));        

      
      Set<Key> baseSet = buildSet(baseTree.getRoot(),new HashSet<Key>());
      Set<Key> newSet = buildSet(dt.getRoot(),new HashSet<Key>());
      Set<Key> newSetCt = buildSet(tt.getRoot(),new HashSet<Key>());
      Assert.assertEquals("Node sets not equal",newSet,newSetCt);
      Set<Key> deletia = new HashSet<Key>(baseSet);
      deletia.removeAll(newSet);
      for( Key k : newSet )
        Assert.assertTrue("Missing key in changetree: "+k,tt.contains(k));
      for( Key k : deletia )
        Assert.assertFalse("Key should be deleted in changetree: "+k,tt.contains(k));
      int resurrects = 20;
      for( Key k : deletia ) {
        kg = new KeyGen((LongKey) k,1);
        permutateTree(dt,1,"iI",DPROB,new Random(lap^resurrects^RND_SEED),kg);
        assert dt.contains(k) : "Node did not appear in facit!"+k;
        kg = new KeyGen((LongKey) k,1);
        permutateTree(tt,1,"iI",DPROB,new Random(lap^resurrects^RND_SEED),kg);
        Assert.assertTrue("Re-inserted node is missing: "+k,tt.contains(k));
        if( --resurrects < 0 )
          break;
      }
      Assert.assertTrue("Differing tree after delete resurrect",treeComp(tt,dt));        
    }
    //Log.info("Congratulations; you are a spiffy hacker.");
  }

  private Set<Key> buildSet(RefTreeNode n, Set<Key> s) {
    s.add(n.getId());
    for( Object c: Util.iterable( n.getChildIterator() ) )
      buildSet((RefTreeNode) c,s);
    return s;
  }

}

// arch-tag: 56fe4e15-254f-47eb-a01b-f32f8cafaa97

