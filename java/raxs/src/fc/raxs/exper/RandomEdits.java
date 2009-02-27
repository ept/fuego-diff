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

package fc.raxs.exper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import fc.raxs.IdStore;
import fc.raxs.Measurements;
import fc.raxs.RandomAccessXmlStore;
import fc.raxs.RaxsConfiguration;
import fc.raxs.Store;
import fc.raxs.VersionHistory;
import fc.raxs.exper.RandomEdits.Edit.Operation;
import fc.util.Debug;
import fc.util.IOUtil;
import fc.util.StringUtil;
import fc.util.Debug.Measure;
import fc.util.Debug.Time;
import fc.util.StringUtil.Options;
import fc.util.log.Log;
import fc.util.log.SysoutLogger;
import fc.xml.xmlr.AbstractMutableRefTree;
import fc.xml.xmlr.ChangeBuffer;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.XmlrDebug;
import fc.xml.xmlr.XmlrUtil;
import fc.xml.xmlr.model.KeyIdentificationModel;
import fc.xml.xmlr.model.KeyModel;
import fc.xml.xmlr.model.TreeModel;
import fc.xml.xmlr.model.TreeModels;
import fc.xml.xmlr.test.DeweyKeyedRefTree;
import fc.xml.xmlr.test.RandomDirectoryTree;
import fc.xml.xmlr.test.XasTests;
import fc.xml.xmlr.test.RandomDirectoryTree.DirectoryEntry;
import fc.xml.xmlr.test.RandomDirectoryTree.KeyGen;
import fc.xml.xmlr.test.RandomDirectoryTree.NodePicker;
import fc.xml.xmlr.xas.DeweyKey;
import fc.xml.xmlr.xas.MutableDeweyRefTree;

import static fc.raxs.Measurements.*;

public class RandomEdits {
  
  public static final long SEQ_READ = 0x00000001;
  public static final long RND_READ = 0x00000002;
  public static final long RND_EDIT = 0x00000004;
  public static final long COMMITS  = 0x00000008;
  public static final long VERS_GET = 0x00000010;

  public static final long CONTENT  = 0x80000000;
  public static final long LAST_EDITSTEP_EDITS  = 0x40000000;

  public static final Object M_INITIAL = new Object();
  public static final Object M_CURRENT = new Object();
  public static final Object M_PRE_COMMIT = new Object();
  public static final Object M_COMMITTED = new Object();
  
  public static final Object H_START_SCRIPT = new Object();
  public static final Object H_END_SCRIPT = new Object();

  static final KeyIdentificationModel idkm = TreeModels.xmlr1Model().swapKeyModel(
      new KeyModel() {

    public Key makeKey(Object s) {
      if( s ==null )
        return null;
      return new RandomDirectoryTree.LongKey(Long.parseLong(s.toString()) );
    }
    
  });
  
  public static final TreeModel DEWEY_DIRTREE_MODEL =
    TreeModels.xasItemTree().swapCodec(new XasTests.DirTreeModel(idkm));

  public static final TreeModel ID_DIRTREE_MODEL = 
    TreeModels.xmlr1Model().swapCodec(
        new XasTests.DirTreeModel(idkm)).swapKeyModel(idkm);

  public static void sequentialRead(PrintStream out, String[] args )  {
    run(out,args,SEQ_READ+CONTENT);
  }

  public static void randomRead(PrintStream out, String[] args )  {
    run(out,args,RND_READ+CONTENT);
  }

  public static void randomEdit(PrintStream out, String[] args )  {
    run(out,args,RND_EDIT+CONTENT);
  }

  public static void randomEditCycle(PrintStream out, String[] args )  {
    run(out,args,RND_EDIT+COMMITS+CONTENT);
  }
  
  public static void versionRecall(PrintStream out, String[] args )  {
    run(out,args,RND_EDIT+COMMITS+VERS_GET+CONTENT+LAST_EDITSTEP_EDITS);
  }
  
  public static void run(PrintStream out, String[] args )  {
    run(out,args,SEQ_READ+CONTENT);
  }
  // Test with fixed tree size and increasing number of edits
  // parameters:
  @SuppressWarnings("unchecked")
  public static void run(PrintStream out, String[] args, long tests) {
    // NOTE: Currently, some stuff differs between disk and memory store
    // implementations:
    // - The disk stores have / = SD, and /0 = first tag, the memory stores have
    // / as root
    // - The undo step is more quickly done by overwriting the content file
    // for disk stores. For memory stores, we just commit the initial tree

    // Parameters
    Options opts = StringUtil.getOptions(args);
    String PDF = opts.getOpt("pdf", "iiiiiiiiiIdddddduuuuuuuuummmm");
    String STORE = opts.getOpt("store", null);
    String EDIT_ROOT = opts.getOpt("editroot", null);
    long LSIZE = (int) opts.getOpt("localsize", 0);
    long TSIZE = (int) opts.getOpt("size", 5000) - LSIZE;
    long TMAX = (int) opts.getOpt("maxsize", TSIZE);
    long TSTEP = (int) opts.getOpt("sizestep", 100);
    long DSIZE = 50;
    int MIN_EDITS = (int) opts.getOpt("edits", 1);
    int MAX_EDITS = (int) opts.getOpt("maxedits", TMAX > TSIZE ? 0 : 25);
    int EDIT_STEP = (int) opts.getOpt("step", 1);
    int REPEATS = (int) opts.getOpt("repeats", 1);
    double DPROB = 0.01;
    double VAR = 5.0;
    double DVAR = 2.0;
    int RND_SEED = (int) opts.getOpt("seed", 1);
    boolean doMemory = opts.getOpt("memory", false);
    boolean incrementalTree = opts.getOpt("incremental", false);
    boolean repickEdit = opts.getOpt("repick-edits-src", false);
    boolean makeRealHistory = (tests & LAST_EDITSTEP_EDITS ) != 0;
    RandomDirectoryTree.useInsertAutoKey = false;

    Random lrnd = new Random(RND_SEED);
    if (doMemory)
      Measurements.MEMORY = true;
    int runs = (int) opts.getOpt("runs", 1);
    File RESULT_FILE = new File(opts.getOpt("file", "result-rnd-edits-time"));
    Class[] MARKER_CLASSES = new Class[] { Measurements.class,
        RandomEdits.class };
    Class<? extends Store> storeType = IdStore.class;
    if (STORE != null) {
      try {
        storeType = (Class<? extends Store>) Class.forName(STORE);
      } catch (Exception e) {
        Log.fatal("Cannot find store " + STORE, e);
      }
    }
    File dir = null;
    boolean isDeweyKeyed = storeType == fc.raxs.DeweyStore.class
        || storeType == DeweyStore.class;
    TreeModel DIRTREE_MODEL = isDeweyKeyed ? DEWEY_DIRTREE_MODEL : ID_DIRTREE_MODEL;
    // Log.info("Is Dewy keyed="+isDeweyKeyed,storeType.getName());
    boolean isMemoryStore = MemoryStore.class.isAssignableFrom(storeType);
    boolean bySize = TMAX > TSIZE;
    // INIT: Generate tree and scripts
    if (opts.getErrors() > 0)
      Log.fatal("Errors on command line");
    ResultLog.TeeStream ts = null;
    ResultLog log = null;
    RaxsConfiguration rc = null;
    Object[] T_POINTS = { H_RAXS_OPEN, H_START_SCRIPT, H_END_SCRIPT,
        H_RAXS_COMMIT, H_RAXS_COMMIT_APPLY, H_STORE_APPLY_WRITETREE,
        H_STORE_APPLY_WRITETREE_END, H_STORE_INIT, H_RAXS_CLOSE };

    Object[] M_POINTS = { M_INITIAL, M_STORE_PREOPEN, M_STORE_OPEN, M_CURRENT,
        M_STORE_PRECLOSE, M_STORE_CLOSE };

    Object[] POINTS = doMemory ? M_POINTS : T_POINTS;
    try {
      ts = new ResultLog.TeeStream(new FileOutputStream(RESULT_FILE), out,
          false);
      log = new ResultLog(getTestName(tests) + (LSIZE > 0 ? "+local" : ""),
          storeType.getName(), bySize ? "size" : "edits", MARKER_CLASSES,
          POINTS, new PrintStream(ts));
      log.comment("Test-args: "+StringUtil.toString(args, " "));
      dir = new File(System.getProperty("fc.raxs.exper.dir", "+raxs-exper"));
      if (!dir.exists() && !dir.mkdir())
        Log.fatal("Cannot make " + dir);
      File f = new File(System.getProperty("fc.raxs.exper.file", "content.xml"));
      rc = RaxsConfiguration.getConfig(storeType, dir, f, DIRTREE_MODEL );
    } catch (Exception e) {
      Log.fatal("Exception creating initial store", e);
    }
    for (long tsize = TSIZE; tsize <= TMAX; tsize += TSTEP) {
      // SIZE-LOOP
      // Log.info("TSIZE="+TSIZE+", TMAX="+TMAX+", TSTEP="+TSTEP+",
      // tsize="+tsize);

      RandomDirectoryTree.KeyGen kg = new KeyGen(RandomDirectoryTree.KEY_GEN);
      List<List<Edit>> scripts = new LinkedList<List<Edit>>();
      MutableRefTree dt = RandomDirectoryTree.randomDirTree(tsize, DSIZE,
          DPROB, VAR, DVAR, new Random(RND_SEED), kg);
      Key localRoot = null;
      try {
        localRoot = EDIT_ROOT == null ? null : (isDeweyKeyed ? DeweyKey
            .createKey(EDIT_ROOT) : DIRTREE_MODEL.makeKey(EDIT_ROOT));
      } catch (IOException ex) {
        Log.fatal("Cannot parse edit root", ex);
      }
      if (LSIZE > 0) {
        Random rnd = new Random(RND_SEED);
        MutableRefTree focusTree = RandomDirectoryTree.randomDirTree(LSIZE,
            DSIZE, DPROB, VAR, DVAR, rnd, kg);
        NodePicker np = new RandomDirectoryTree.SafeIdNodePicker(dt, rnd);
        localRoot = focusTree.getRoot().getId();
        try {
          focusTree.update(localRoot, 
              new DirectoryEntry(localRoot,"grafted-"+localRoot,DirectoryEntry.DIR));
          Key dest = np.nextNode().getId();
        // Log.info("Inserting local tree at "+dest);
        // XmlrDebug.dumpTree(focusTree);
          insertTree(dt, dest, focusTree.getRoot(),false);
        } catch (NodeNotFoundException e) {
          Log.fatal(e);
        }
      }
      ChangeBuffer cb = null;
      if (isDeweyKeyed) {
        DeweyKeyedRefTree dewt = new DeweyKeyedRefTree(dt,
            isMemoryStore ? DeweyKey.ROOT_KEY : DeweyKey.ROOT_KEY.down());
        IdAddressableRefTree dtd = RefTrees.getAddressableTree(dewt);
        // Log.debug("DTD tree is");
        // XmlrDebug.dumpTree(dtD);
        MutableDeweyRefTree mt = new MutableDeweyRefTree();
        cb = new ChangeBuffer(mt, dtd, mt, dtd.getRoot().getId());
        if (localRoot != null && !(localRoot instanceof DeweyKey))
          localRoot = dewt.getFrontKey(localRoot);
        // Log.debug("change buffering tree is");
        // XmlrDebug.dumpTree(mT);
        /*
         * Log.debug("Front key of root "+dtd.getRoot().getId()+" is "+
         * mt.getFrontKey(dtd.getRoot().getId()));
         */
      } else
        cb = new ChangeBuffer(dt);

      final RefTree INITAL_TREE = cb;

      // Log.debug("Init tree is");
      // XmlrDebug.dumpTree(INITAL_TREE);
      for (int edits = MAX_EDITS; edits <= MAX_EDITS; edits += EDIT_STEP) {
        Log.info("Edits " + edits + " of " + MAX_EDITS + ", localRoot="
            + localRoot);
        // XmlrDebug.dumpTree(cb);
        KeyGen ekg = kg; //new KeyGen(kg);
        ArrayList<Edit> script = new ArrayList<Edit>();
        ScripterTree st = new ScripterTree(script, cb);

        RandomDirectoryTree.NodePicker np = null;
        Random rnd = new Random(edits ^ RND_SEED);
        if (isDeweyKeyed) {
          RandomDirectoryTree.useInsertAutoKey = true;
          np = new RandomDirectoryTree.TransientNodePicker(st, rnd, localRoot);
        } else {
          np = new RandomDirectoryTree.SafeIdNodePicker(st, rnd, localRoot);
        }
        RandomDirectoryTree.permutateTree(st, edits, PDF, DPROB, new Random(
            edits ^ RND_SEED), ekg, np);
        script.trimToSize();
        Log.debug("Script " + edits + " is", script);
        if (script.size() != edits)
          Log.fatal("Wrong no of edits emitted: " + script.size());
        try {
          cb.reset();
          Edit.apply(script, cb);
        } catch (NodeNotFoundException e) {
          Log.fatal("Script verify run failed", e);
        }
        scripts.add(script);
        cb.reset(); // Next lap starts from unmodified tree
      }

      for (long otsize=tsize; tsize <= (incrementalTree ? TMAX : otsize); 
        tsize += (incrementalTree ? TSTEP : 1 )) {
      // Steps
      RandomAccessXmlStore raxs = null;
      try {
        // Clean previous instance
        if (dir.exists())
          IOUtil.delTree(dir, false);

        // 1. Create a store with appropriate initial data
        raxs = RandomAccessXmlStore.open(rc);
        raxs.commit(INITAL_TREE);
        raxs.close();

        // 2. Open: start t, m
        int lap = 0;
        assert scripts.size() == 1;
        for (int run = 1; run <= runs; run++) {
          if (runs > 1)
            log.setRun(run);
          if (Measurements.MEMORY)
            Measure.set(M_INITIAL, Debug.usedMemory());
          for (List<Edit> script : scripts) {
            //Log.info("==Has a script");
            for (int i = (scripts.size() == 1 ? MIN_EDITS : script.size() - 1); i <= script
                .size(); i += EDIT_STEP) {
              for(int irep=0;irep<REPEATS;irep++) {
              //Log.info("==Looping edits");
              // Backup data file for store. This will allow a fast revert, but
              // at the cost of a nonsensical history
              File bakFile = null;
              if (!isMemoryStore && !makeRealHistory) {
                bakFile = new File(rc.getRoot(), "contents.bak");
                IOUtil.copyFile(rc.getStoreFile(), bakFile);
              }
              Log.info("Executing script " + (lap++) + " on tree ");
              Time.zeroIsNow();
              raxs.open();
              MutableRefTree et = null;
              if ((tests & RND_READ) != 0) {
                testRandomRead(raxs, script, i, tests);
              }
              if ((tests & RND_EDIT) != 0) {
                et = testRandomEdits(raxs, script, i, tests, EDIT_STEP);
              }
              if ((tests & COMMITS) != 0) {
                if (Measurements.MEMORY)
                  Measure.set(M_PRE_COMMIT, Debug.usedMemory());
                //XmlrDebug.dumpTree(raxs.getTree());
                int v =raxs.commit(et, false);
                Log.debug("Committed version "+v);
                if( (tests & VERS_GET) != 0)
                  log.comment("Built version "+v);
                //XmlrDebug.dumpTree(raxs.getTree());
                if (Measurements.MEMORY)
                  Measure.set(M_COMMITTED, Debug.usedMemory());
              }
              raxs.close();
              if( (tests & VERS_GET) == 0)
                log.result(bySize ? (int) (tsize + LSIZE) : i);
              else
              // Util.runGc();
              if( !makeRealHistory ) {
                if (isMemoryStore) {
                  raxs.open();
                  // Log.debug("Store and revert trees are");
                  // XmlrDebug.dumpTree(raxs.getTree());
                  // XmlrDebug.dumpTree(INITAL_TREE);
                  raxs.commit(INITAL_TREE);
                  raxs.close();
                } else
                  IOUtil.replace(bakFile, rc.getStoreFile());
              }
              if( !doMemory ) {
                // Allow some sleep between laps
                Debug.usedMemory();
                Debug.sleep(250);
                Debug.usedMemory();
              }
            } // Repeats
            }
          }
        }
        // Tests that only make sense outside the edit loop
        if ((tests & VERS_GET) != 0) {
          raxs.open();
          VersionHistory h = raxs.getVersionHistory();
          Log.info("Version history is", h);
          for (int v : h.listVersions()) {
            Time.zeroIsNow();
            Time.stamp(H_START_SCRIPT);
            raxs.getTree(v);
            Time.stamp(H_END_SCRIPT);
            log.result(v);
          }
        }
        if ((tests & SEQ_READ) != 0) {
          Time.zeroIsNow();
          raxs.open();
          testSeqRead(raxs, tests);
          raxs.close();
          log.result((int) (tsize + LSIZE));
        }
        // XmlrDebug.dumpTree(raxs.getTree());
      } catch (Exception e) {
        Log.error(e);
      } finally {
      }
      if (isMemoryStore)
        MemoryStore.forgetTrees();

      if( incrementalTree ) {
        MutableRefTree incTree = RandomDirectoryTree.randomDirTree(TSTEP,
            DSIZE, DPROB, VAR, DVAR, lrnd, kg);
        NodePicker np = isDeweyKeyed ? 
            new RandomDirectoryTree.TransientNodePicker(cb, lrnd) : 
              new RandomDirectoryTree.SafeIdNodePicker(cb, lrnd);
        localRoot = incTree.getRoot().getId();
        try {
          incTree.update(localRoot, 
              new DirectoryEntry(localRoot,"increment-"+localRoot,DirectoryEntry.DIR));
          Key dest = np.nextNode().getId();
          Log.debug("Grafting incremental tree at"+dest);
          //XmlrDebug.dumpTree(incTree);
          insertTree(cb, dest, incTree.getRoot(), isDeweyKeyed );
        } catch (NodeNotFoundException e) {
          Log.fatal(e);
        }
        Log.debug("Tree node count now ",XmlrUtil.countNodes(cb));  
      }
      if( repickEdit ) {
        NodePicker np = isDeweyKeyed ? 
            new RandomDirectoryTree.TransientNodePicker(cb, lrnd) : 
              new RandomDirectoryTree.SafeIdNodePicker(cb, lrnd);
        for( List<Edit> script : scripts) {
          for( Edit e : script ) {
            e.src = np.nextNode().getId();
          }
        }
      }
    } // Tree increment loop ends
    tsize -= incrementalTree ? 0 : 1;   
    } // SIZE-LOOP-ENDS
    log.finish();
    try {
      ts.close();
    } catch (IOException e) {
      Log.error(e);
    }
    Log.info("---> Done!");

  }

  private static String getTestName(long tests) {
    String test = "";
    long[] flags={SEQ_READ,RND_READ,RND_EDIT,COMMITS,VERS_GET, CONTENT};
    String[] names={"SEQ_READ","RND_READ","RND_EDIT","COMMITS","VERSION_GET", 
         "readContent"};
    for( int i=0;i<flags.length;i++) {
      if((tests & flags[i])!=0)
        test += (test.length()==0 ? "" : "+") + names[i];
    }
    return test;
  }

  private static MutableRefTree testRandomEdits(RandomAccessXmlStore raxs,
      List<Edit> script, int off, long tests, int len) throws IOException, NodeNotFoundException {
    MutableRefTree et = raxs.getEditableTree();
    // Log.debug("The editable tree is");
    // XmlrDebug.dumpTree(eT);
    Time.stamp(H_START_SCRIPT);
    if( (tests & LAST_EDITSTEP_EDITS) != 0)
      Edit.apply(script, et, Math.max(off-len, 0), off);
    else
      Edit.apply(script, et, 0, off);
    // Log.info("Edit script took",Debug.Time.sinceFmt());
    // XmlrDebug.dumpTree(((ChangeBuffer) eT).getChangeTree());
    if( Measurements.MEMORY)
      Measure.set( M_CURRENT, Debug.usedMemory());
    Time.stamp(H_END_SCRIPT);
    return et;
  }

  private static void testRandomRead(RandomAccessXmlStore raxs,
      List<Edit> script, int len, long tests) throws IOException, NodeNotFoundException {
    IdAddressableRefTree t = raxs.getTree();
    Time.stamp(H_START_SCRIPT);
    for( Edit e : script ) {
      RefTreeNode n = t.getNode(e.getSrc());
      if( ( tests & CONTENT ) != 0 )
        readContent(n);
      if( --len == 0 )
        break;
    }
    if( Measurements.MEMORY)
      Measure.set( M_CURRENT, Debug.usedMemory());
    Time.stamp(H_END_SCRIPT);
  }

  private static void readContent(RefTreeNode n) {
    Object o = n.getContent();
    if( o == n ) // This impossible test is just to ensure getContent()
                    // isn't optimized away
      Log.fatal("Got really weird root?");
  }

  private static void testSeqRead(RandomAccessXmlStore raxs, long tests) throws IOException {
    RefTree t = raxs.getTree();
    Time.stamp(H_START_SCRIPT);
    traverseTree(t.getRoot(),tests);
    Time.stamp(H_END_SCRIPT);
  }
  
  @SuppressWarnings("unchecked")
  private static void traverseTree(RefTreeNode root, long tests ) {
    if( ( tests & CONTENT ) != 0 )
      readContent(root);
    for( Iterator<RefTreeNode> in = root.getChildIterator();in.hasNext();)
      traverseTree(in.next(),tests);
  }

  public static void main(String[] args) {
    Log.setLogger(new SysoutLogger());
    String[] args2=new String[args.length+1];
    args2[0]="run";
    System.arraycopy(args, 0, args2, 1, args.length);
    run(System.out,args2);
  }
  
  public static class Edit {
    public enum Operation {INSERT,DELETE,UPDATE,MOVE};
    private Operation op;
    private Key src;
    private Key target;
    private long pos;
    private Object data;

    public Edit(Operation op, Key src, Key target, long pos, Object data) {
      this.op = op;
      this.src = src;
      this.target = target;
      this.pos = pos;
      this.data = data;
    }

    public Object getData() {
      return data;
    }

    public Operation getOp() {
      return op;
    }

    public long getPos() {
      return pos;
    }

    public Key getSrc() {
      return src;
    }

    public Key getTarget() {
      return target;
    }
    
    public void apply(MutableRefTree t) throws NodeNotFoundException {
      switch( op ) {
        case INSERT:
            t.insert(target, pos, src, data);
            break;
        case DELETE:
            t.delete(src);
            break;
        case UPDATE:
            t.update(src, data);
            break;
        case MOVE:
            t.move(src, target, pos);
          break;
        default:
          assert false;
      }
    }
    
    public static void apply(List<Edit> script, MutableRefTree t) throws NodeNotFoundException {
      for( Edit e: script)
        e.apply(t);
    }

    public static void apply(List<Edit> script, MutableRefTree t, int start, int end) throws NodeNotFoundException {
      for( int i =start;i<end;i++) {
        //XmlrDebug.dumpTree(t);
        //Log.debug("Applying "+script.get(i));
        script.get(i).apply(t);
      }
    }

    public String toString() {
      switch( op ) {
        case INSERT:
          return "INS("+src+","+target+","+(pos == -1 ? "" : ""+pos+",")+data+")";
        case DELETE:
          return "DEL("+src+")";
        case UPDATE:
          return "UPD("+src+","+data+")";
        case MOVE:
          return "MOV("+src+","+target+(pos == -1 ? "" : ""+pos)+")";
        default:
          assert false;
      }
      return null;
    }

  }
  
  public static class ScripterTree extends AbstractMutableRefTree {
    
    private List<Edit> edits;
    private MutableRefTree t;

    public ScripterTree(List<Edit> edits, MutableRefTree t) {
      this.edits = edits;
      this.t = t;
    }

    public Iterator<Key> childIterator(Key id) throws NodeNotFoundException {
      return t.childIterator(id);
    }

    public boolean contains(Key id) {
      return t.contains(id);
    }

    public void delete(Key id) throws NodeNotFoundException {
      t.delete(id);
      edits.add(new Edit(Operation.DELETE,id,null,-1,null));
    }

    public RefTreeNode getNode(Key id) {
      return t.getNode(id);
    }

    public Key getParent(Key nid) throws NodeNotFoundException {
      return t.getParent(nid);
    }

    public RefTreeNode getRoot() {
      return t.getRoot();
    }

    public Key insert(Key parentId, long pos, Key newId, Object content)
        throws NodeNotFoundException {
      Key k = t.insert(parentId, pos, newId, content);
      edits.add(new Edit(Operation.INSERT,newId,parentId,pos,content));
      return k;

    }

    public Key move(Key nodeId, Key parentId, long pos)
        throws NodeNotFoundException {
      Key k =t.move(nodeId, parentId, pos);
      edits.add(new Edit(Operation.MOVE,nodeId,parentId,pos,null) );
      return k;
    }

    public boolean update(Key nodeId, Object content)
        throws NodeNotFoundException {
      edits.add(new Edit(Operation.UPDATE,nodeId,null,-1,content));
      return t.update(nodeId, content);
    }
    
  }
  
  private static void insertTree(MutableRefTree t, Key pid, RefTreeNode r,
      boolean autoKey ) 
    throws NodeNotFoundException{
    Key nk = t.insert(pid, autoKey ? MutableRefTree.AUTO_KEY :  r.getId(),
        r.getContent());
    for( Iterator<RefTreeNode> i = r.getChildIterator();i.hasNext();)
      insertTree(t,nk,i.next(), autoKey);
  }
}
// arch-tag: 05da2ed8-188a-4ec1-ab28-6fbf5236cede
//
