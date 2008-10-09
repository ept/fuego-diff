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

package fc.raxs.test;

import static fc.xml.xmlr.test.RandomDirectoryTree.KEY_GEN;
import static fc.xml.xmlr.test.RandomDirectoryTree.permutateTree;
import static fc.xml.xmlr.test.RandomDirectoryTree.randomDirTree;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

import junit.framework.Assert;
import junit.framework.TestCase;
import fc.raxs.RandomAccessXmlStore;
import fc.raxs.Store;
import fc.util.CompareUtil;
import fc.util.Debug;
import fc.util.log.Log;
import fc.xml.xas.Item;
import fc.xml.xmlr.ChangeBuffer;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.XmlrDebug;
import fc.xml.xmlr.model.IdentificationModel;
import fc.xml.xmlr.model.KeyModel;
import fc.xml.xmlr.model.NodeModel;
import fc.xml.xmlr.model.TreeModel;
import fc.xml.xmlr.model.XasCodec;
import fc.xml.xmlr.test.DeweyKeyedRefTree;
import fc.xml.xmlr.test.RandomDirectoryTree;
import fc.xml.xmlr.test.XasTests;
import fc.xml.xmlr.test.RandomDirectoryTree.KeyGen;
import fc.xml.xmlr.xas.DeweyKey;
import fc.xml.xmlr.xas.DeweyRefNode;
import fc.xml.xmlr.xas.MutableDeweyRefTree;
import fc.xml.xmlr.xas.UniformXasCodec;

public class VersioningTest extends TestCase {

  public static UniformXasCodec DIR_TM = new XasTests.DirTreeModel(); 
  
  private static final long RND_SEED = 42L;

  public void testIdVersioning() throws IOException {
    String PDF="iiiiiiiiiIdddddduuuuuuuuummmm";
    long TSIZE=5000;long DSIZE=50;double DPROB=0.01;double VAR=5.0;double DVAR=2.0;
    Log.info("Testing ChangeTree by repeatedly making "+
        " random edits on tree of size "+TSIZE+". Deleting and the inserting " +
        " another node with the same id is also tested");
    Log.info("In reports below, changebuffer is tree 1, facit tree 2");

    int MAX_LAP=25;

    // Commit block
    Random oprnd = new Random(RND_SEED);
    KeyGen kg = new KeyGen( KEY_GEN );
    MutableRefTree dt = randomDirTree(TSIZE,DSIZE,DPROB,VAR,DVAR, 
        new Random(RND_SEED),kg);
    ((RandomDirectoryTree.MutableDirectoryTree ) dt).setOrdered(true);
    Store s = new FakeStore(dt);
    RandomAccessXmlStore raxs = RandomAccessXmlStore.open(s); 
    LinkedList<RefTree> facits=  new LinkedList<RefTree>();
    facits=null; // Disable this to enable test self-check
    LinkedList<Integer> vlist= new LinkedList<Integer>();
    vlist.add( raxs.commit(raxs.getEditableTree()) ); // Initial ver
    if( facits != null )
      facits.add( RandomDirectoryTree.treeCopy( dt ) );
    for( int lap = 0;lap < MAX_LAP;lap++) {
      Log.info("Lap "+(lap+1)+" of "+MAX_LAP+", keygen at "+
          (new KeyGen(kg)).next());
      RefTree baseTree = RandomDirectoryTree.treeCopy(dt);
      MutableRefTree ct = raxs.getEditableTree();
      //Log.info("Init test tree");XmlrDebug.dumpTree(dt,System.out);
      //RandomDirectoryTree.edstring="\n";
      //RandomDirectoryTree._verbose = true;
      permutateTree(ct,oprnd.nextInt(1+(int)TSIZE/20),
          PDF,DPROB,new Random(lap^RND_SEED),kg);
      //Log.info("Editstring for facit is ",RandomDirectoryTree.edstring);
      int prevVer = vlist.getLast();
      /*if( prevVer == 16 ) {
        Log.info("Dump of version-to-be "+(prevVer+1));
        XmlrDebug.dumpTree(ct);
      }*/
      int version=raxs.commit(ct);
      vlist.add(version);
      if( facits != null )
        facits.add( RandomDirectoryTree.treeCopy( dt ) );
      /*if( version == 17 ) {
        Log.info("Dump of version committed "+version);
        XmlrDebug.dumpTree(dt);
      }*/
      Log.info("Committed version "+version+", testing to go back to "+prevVer);
      // Test to go 1 back
      RefTree oneBack = raxs.getTree(prevVer);
      Assert.assertTrue("Could not go back 1 version", 
          XmlrDebug.treeComp(baseTree, oneBack));      
    }
    
    // Retrieve block
    oprnd = new Random(RND_SEED);
    kg = new KeyGen( KEY_GEN );
    dt = randomDirTree(TSIZE, DSIZE, DPROB, VAR, DVAR, new Random(RND_SEED),kg);
    ((RandomDirectoryTree.MutableDirectoryTree ) dt).setOrdered(true);

    for( int lap = -1;lap < MAX_LAP;lap++) {
      int version = vlist.removeFirst();
      Debug.Time.stamp();
      RefTree ot = raxs.getTree(version);
      Log.info("Retrieved "+ version+" in "+Debug.Time.sinceFmt());
      if( facits != null) {
        RefTree ft = facits.removeFirst();
        Assert.assertTrue("Mismatching trees, but problem is not with repo",
            XmlrDebug.treeComp(ft, dt,CompareUtil.OBJECT_EQUALITY,false));
      }
      if( !XmlrDebug.treeComp(dt, ot, CompareUtil.OBJECT_EQUALITY, false )) {
        Log.error("Mismatching trees. Dumping desired, retrieved");
        XmlrDebug.dumpTree(dt);
        XmlrDebug.dumpTree(ot);
      }
      Assert.assertTrue("Bad tree in repo", XmlrDebug.treeComp(dt, ot));
      // Build next tree
      permutateTree(dt,oprnd.nextInt(1+(int)TSIZE/20),
          PDF,DPROB,new Random((lap+1)^RND_SEED),kg);
    }
    System.runFinalizersOnExit(true);
  }
  
  public static class FakeStore extends Store  {
    IdAddressableRefTree t;

    public FakeStore(IdAddressableRefTree t) {
      this.t = t;
    }

    @Override
    public IdAddressableRefTree getTree() {
      return t;
    }

    @Override
    public TreeModel getTreeModel() {
      KeyModel longKeys = new KeyModel() {

        public Key makeKey(Object s) {
          if( s ==null )
            return null;
          return new RandomDirectoryTree.LongKey(Long.parseLong(s.toString()) );
        }
        
      };
      return new TreeModel(longKeys,IdentificationModel.ID_ATTRIBUTE,
          DIR_TM,NodeModel.DEFAULT);
    }

    public void close() {
	// EMPTY
    }

    public void open() throws IOException {
      // EMPTY
    }
  }

  public void testDeweyVersioning() throws IOException, NodeNotFoundException {
    DeweyKey ROOT_KEY = DeweyKey.ROOT_KEY; //.child(0);
    String PDF="iiiiiiiiiIdddddduuuuuuuuummmm";
    int MIN_OP=3;
    long TSIZE=2500;long DSIZE=20;double DPROB=0.01;double VAR=5.0;double DVAR=2.0;
    Log.info("Testing versioning of Dewey tree of size "+TSIZE+".");

    int MAX_LAP=25;

    // Commit block
    Random oprnd = new Random(RND_SEED);
    KeyGen kg = new KeyGen( KEY_GEN );
    MutableRefTree idt = randomDirTree(TSIZE,DSIZE,DPROB,VAR,DVAR, 
        new Random(RND_SEED),kg);
    ((RandomDirectoryTree.MutableDirectoryTree ) idt).setOrdered(true);
    Store s = new DeweyStore( RefTrees.getAddressableTree(
        new DeweyKeyedRefTree( idt ) ) );
    RandomAccessXmlStore raxs = RandomAccessXmlStore.open(s); 
    LinkedList<RefTree> facits=  new LinkedList<RefTree>();
    facits=null; // Disable this to enable test self-check
    LinkedList<Integer> vlist= new LinkedList<Integer>();
    vlist.add( raxs.commit(raxs.getEditableTree()) ); // Initial ver
    if( facits != null )
      facits.add( RandomDirectoryTree.treeCopy( new DeweyKeyedRefTree( idt, ROOT_KEY ) ) );
    for( int lap = 0;lap < MAX_LAP;lap++) {
      Log.info("Lap "+(lap+1)+" of "+MAX_LAP+", keygen at "+
          (new KeyGen(kg)).next());
      RefTree baseTree = new DeweyKeyedRefTree(
          RandomDirectoryTree.treeCopy(raxs.getTree()), ROOT_KEY  );
      //Log.info("Full tree");XmlrDebug.dumpTree(raxs.getTree());
      MutableRefTree ct = raxs.getEditableTree();
      //Log.info("Init test tree");XmlrDebug.dumpTree(dt,System.out);
      //RandomDirectoryTree.edstring="\n";
      //RandomDirectoryTree._verbose = true;
      RandomDirectoryTree.useInsertAutoKey = true;
      permutateTree(ct,oprnd.nextInt(MIN_OP+(int)TSIZE/20),
          PDF,DPROB,new Random(lap^RND_SEED),kg,
          new RandomDirectoryTree.TransientNodePicker(ct,
              new Random(lap^RND_SEED)));
      RandomDirectoryTree.useInsertAutoKey = false;

      //Log.info("Editstring for facit is ",RandomDirectoryTree.edstring);
      int prevVer = vlist.getLast();
      /*if( prevVer == 16 ) {
        Log.info("Dump of version-to-be "+(prevVer+1));
        XmlrDebug.dumpTree(ct);
      }*/
      //Log.info("Dump of version-to-be "+(vlist.getLast()+1));
      //XmlrDebug.dumpTree(ct);
      
      int version=raxs.commit(ct);
      vlist.add(version);
      if( facits != null )
        facits.add( new DeweyKeyedRefTree(
            RandomDirectoryTree.treeCopy( raxs.getTree() ) ) );
      /*if( version == 17 ) {
        Log.info("Dump of version committed "+version);
        XmlrDebug.dumpTree(dt);
      }*/
      //Log.info("Committed version "+version+", testing to go back to "+prevVer);
      // Test to go 1 back
      RefTree oneBack = raxs.getTree(prevVer);
      //Log.info("Dump of one back ("+prevVer+")");
      //XmlrDebug.dumpTree(((ChangeBuffer) oneBack).getChangeTree());
      //XmlrDebug.dumpTree(oneBack);
      //XmlrDebug.dumpTree(baseTree);
      
      Assert.assertTrue("Could not go back 1 version", 
          XmlrDebug.treeComp(baseTree, oneBack));      
    }
    
    // Retrieve block
    oprnd = new Random(RND_SEED);
    kg = new KeyGen( KEY_GEN );
    idt = randomDirTree(TSIZE, DSIZE, DPROB, VAR, DVAR, new Random(RND_SEED),kg);
    ((RandomDirectoryTree.MutableDirectoryTree ) idt).setOrdered(true);
    MutableDeweyRefTree dt = new MutableDeweyRefTree();
    dt.insert(null, MutableRefTree.AUTO_KEY, idt.getRoot().getContent());
    RefTrees.apply(new DeweyKeyedRefTree( idt ), dt );
    
    
    for( int lap = -1;lap < MAX_LAP;lap++) {
      //RefTree dt = new DeweyKeyedRefTree(idt);
      int version = vlist.removeFirst();
      Debug.Time.stamp();
      RefTree ot = raxs.getTree(version);
      Log.info("Retrieved "+ version+" in "+Debug.Time.sinceFmt());
      if( facits != null) {
        RefTree ft = facits.removeFirst();
        Assert.assertTrue("Mismatching trees, but problem is not with repo",
            XmlrDebug.treeComp(ft, dt,CompareUtil.OBJECT_EQUALITY,false));
      }
      if( !XmlrDebug.treeComp(dt, ot, CompareUtil.OBJECT_EQUALITY, false )) {
        Log.error("Mismatching trees at version "+version+
            ". Dumping desired, retrieved");
        XmlrDebug.dumpTree(dt);
        XmlrDebug.dumpTree(ot);
      } 
      Assert.assertTrue("Bad tree in repo", XmlrDebug.treeComp(dt, ot));
      // Build next tree
      /*permutateTree(idt,oprnd.nextInt(1+(int)TSIZE/20),
          PDF,DPROB,new Random((lap+1)^RND_SEED),kg);*/
      RandomDirectoryTree.useInsertAutoKey = true;
      permutateTree(dt,oprnd.nextInt(MIN_OP+(int)TSIZE/20),
          PDF,DPROB,new Random((lap+1)^RND_SEED),kg,
          new RandomDirectoryTree.TransientNodePicker(dt,
              new Random((lap+1)^RND_SEED)));      
      RandomDirectoryTree.useInsertAutoKey = false;
    }
    System.runFinalizersOnExit(true);
    if(TSIZE > 999 && MAX_LAP > 20 )
      Log.info("That was froody. "+MAX_LAP+" Dewied versions, of "+TSIZE+
          " nodes each, and it works. ");
  }
  
  public static class DeweyStore extends Store  {
   
    MutableDeweyRefTree t;

    public DeweyStore(IdAddressableRefTree t) {
      this.t = new MutableDeweyRefTree();
      try {
        this.t.insert(null, MutableRefTree.AUTO_KEY, t.getRoot().getContent());
        RefTrees.apply(t, this.t);
        Log.debug("Initial DeweyStore tree");
        //XmlrDebug.dumpTree(this.t);
      } catch (NodeNotFoundException e) {
        Log.fatal("Can't init store",e);
      }
    }

    @Override
    public IdAddressableRefTree getTree() {
      return t;
    }

    @Override
    public TreeModel getTreeModel() {
      return tm;
    }

    public void close() {
	// EMPTY
    }

    public void open() throws IOException {
      // EMPTY
    }

    KeyModel longKeys = new KeyModel() {

      public Key makeKey(Object s) {
        if( s ==null )
          return null;
        return new RandomDirectoryTree.LongKey(Long.parseLong(s.toString()) );
      }
      
    };

    KeyModel deweyKeys = new KeyModel() {

      public Key makeKey(Object s) throws IOException {
        return s == null ? null : DeweyKey.createKey(s.toString());
      }        
    };
    
    IdentificationModel deweyId = new IdentificationModel() {

      public Key identify(Item i, KeyModel km) throws IOException {
        return null;
      }

      public Item tag(Item i, Key k, KeyModel km) {
        return i;
      }
      
    };
    
    XasCodec codec = new XasTests.DirTreeModel(longKeys);
    
    TreeModel tm = new TreeModel(deweyKeys,deweyId,
        codec,DeweyRefNode.NODE_MODEL);

    @Override
    public ChangeBuffer getChangeBuffer() {
      MutableDeweyRefTree mdrt = new MutableDeweyRefTree();
      mdrt.setForceAutoKey(true);
      return new ChangeBuffer(mdrt, getTree(), mdrt);
    }

    @Override
    public void apply(RefTree nt) throws NodeNotFoundException {
      t.apply(nt);
    }
  }
}


// arch-tag: a8ee0867-7b6b-42e8-b960-f415a255a993
