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

package fc.raxs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;


import org.kxml2.io.KXmlParser;

import fc.util.Debug;
import fc.util.NonListableSet;
import fc.util.Debug.Measure;
import fc.util.Debug.Time;
import fc.util.log.Log;
import fc.xml.xas.EndDocument;
import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.ItemList;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.ParserSource;
import fc.xml.xas.Qname;
import fc.xml.xas.StartDocument;
import fc.xml.xas.StartTag;
import fc.xml.xas.TransformSource;
import fc.xml.xas.TransformTarget;
import fc.xml.xas.XasUtil;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.XmlPullSource;
import fc.xml.xmlr.BatchMutable;
import fc.xml.xmlr.ChangeBuffer;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.KeyMap;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.RekeyedRefTree;
import fc.xml.xmlr.XmlrDebug;
import fc.xml.xmlr.model.TreeModel;
import fc.xml.xmlr.model.XasCodec;
import fc.xml.xmlr.tdm.Diff;
import fc.xml.xmlr.xas.XasSerialization;

/** Random-access, mutable, and versioned XML store (RAXS). A RAXS store 
 * provides lightweight read and write access to an underlying XML file 
 * or XAS Item Store. Access is provided through the XMLR RefTrees API, and 
 * read and write operations may address the document
 * in a random-access fashion. Furthermore, access to past revisions of a
 * document is provided.  
 */
public class RandomAccessXmlStore implements BatchMutable {

  /** First document version. Currently set to {@value}.
   */
  public static final int FIRST_VERSION = 1;
  /** First document version. Currently set to {@value}.
   */
  public static final int NO_VERSION = -1;
  /** Constant indicating an absent. Currently set to {@value}.
   */
  public static final int CURRENT_VERSION = -2;
  /** Maximum number of versions. Currently set to {@value}.
   */
  public static final int MAX_VERSION = 1<<20; // Max 1 million versions
    // Can be increased n times, at the expense of log2(n) file lookups
    // when opening a store
  
  // placeholders
  private static ItemList _HEADER = XasSerialization.DOC_HEADER_UTF8;
  private static ItemList _TRAILER = XasSerialization.DOC_TRAILER_UTF8;
  
  protected Store s;  
  protected RaxsConfiguration config;
  protected ChangeBuffer cb = null;
  protected Object parser = null; // No proper superclass for all XML parsers :(
  
  // State that needs to be restored
  protected int currentVersion = NO_VERSION;
  protected int oldestVersion = NO_VERSION;
  protected boolean currentVersionInited = false;
  protected boolean oldestVersionInited = false;
  protected boolean isOpened = false;
  
  /** Create store from configuration.
   * 
   * @param sc Store configuration
   * @throws ParseException
   */
  public RandomAccessXmlStore(RaxsConfiguration sc) throws ParseException {
    this( sc.createStore(), sc );
  }

  /** Create store from store instance and configuration.
   * 
   * @param s Underlying XAS Item store to 
   * @param config Store configuration (Item Store part ignored)
   */

  public RandomAccessXmlStore(Store s, RaxsConfiguration config) {
    this.s = s;
    this.config = config;
  }

  /** Create configuration from file and MIME type.
   * 
   * @param f file
   * @param mimeType MIME type of file
   * @return configuration
   * @throws IOException
   */
  public static RaxsConfiguration getConfiguration(File f, String mimeType) 
    throws IOException {
    // TODO: Add more advanced config detection, such as reading META-INF here
    if( f.isDirectory() ) {
      return RaxsConfiguration.getConfig(DeweyStore.class,f,
          new File("content.xml"));
    } else
      return RaxsConfiguration.getConfig(DeweyStore.class,f.getParentFile(),f);
  }

  /** Create and open store.
   * 
   * @param sc Store configuration
   * @return opened store
   * @throws IOException
   */
  public static RandomAccessXmlStore open( RaxsConfiguration sc ) 
    throws IOException {
    return new RandomAccessXmlStore( sc );
  }
  
  /** Create and open store.
   * 
   * @param s XAS Item Store
   * @return opened store
   * @throws IOException
   */
  public static RandomAccessXmlStore open( Store s ) throws IOException {
    return new RandomAccessXmlStore(s, 
        RaxsConfiguration.getConfig(s.getClass(), new File(".")));
  }
  
  /** Get store configuration. 
   * <p>NOTE: This reflects the configuration given to the store at 
   * instantiation, which may not be the actual configuration used. 
   * A typical example is the actual tree model used. (See {@link #getModel()})
   * @return Store configuration
   */
  public RaxsConfiguration getConfiguration() {
    return config;
  }
  
  /** Get store contents as reftree.
   * 
   * @return tree
   * @throws IOException
   */
  public IdAddressableRefTree getTree() throws IOException {
    open();
    return s.getTree();
  }
  
  /** Get editable reftree. To edit the store contents, obtain a mutable
   * reftree with this method. The changes may be committed to disk by passing
   * the tree returned by this method to {@link #commit(RefTree)}.
   * <p>Only one editable tree may be used at a time.
   * @return Mutable reftree for store
   * 
   * @throws IllegalStateException a tree obtained by this method previously is
   * still being edited.
   * @throws IOException
   */
  public synchronized MutableRefTree getEditableTree() 
    throws IllegalStateException, IOException {
    open();
    if( cb != null )
      throw new IllegalStateException(
          "An uncommitted edit session is in progress");
    return s.isWritable() ? cb = makeChangeBuffer() : null;
  }

  /** Commit a new tree. The tree to commit may either be a tree obtained
   * with {@link #getEditableTree()} or an arbitrary reftree that references
   * the current store tree. <i>t</i> in no longer editable nor valid
   * after a successful commit.
   * 
   * @param t reftree to commit
   * @return version number of committed data
   * @throws IllegalArgumentException if the store is being edited via another tree
   * than <i>t</i>
   */
  public int commit( RefTree t ) throws IllegalArgumentException {
    return commit(t,false);
  }
  
  /** Commit a new tree. The tree to commit may either be a tree obtained
   * with {@link #getEditableTree()} or an arbitrary reftree that references
   * the current store tree. In the former case, the <code>keepTree</code>
   * flag indicates whether the passed tree should still be valid and editable
   * after the commit.  
   * 
   * @param t Tree to commit
   * @param keepTree <code>true</code> means that editing on <i>t</i> may
   * continue. <code>false</code> invalidates <i>t</i>, and closes the editing
   * session on that tree. A new editable tree may then be obtained with
   * {@link #getEditableTree()}.
   * @return version number of committed data
   * @throws IllegalArgumentException if the store is being edited via another tree
   * than <i>t</i>
   */
  public synchronized int commit( RefTree t, boolean keepTree) throws IllegalArgumentException {
    if( Measurements.RAXS_TIMINGS )
      Time.stamp(Measurements.H_RAXS_COMMIT);
    if( !(cb == null || t == cb ) )
      throw new IllegalArgumentException("Another tree is active.");
    if( cb == null && keepTree )
      throw new IllegalArgumentException("Cannot keep an external tree active");
    try {
      //Log.debug("Dump of full new tree");
      //XmlrDebug.dumpTree(t);
      IdAddressableRefTree ct = cb != null ? 
        cb.getChangeTree() : RefTrees.getAddressableTree(t);
      open();
      if( s.getTree().getRoot() != null && // null root means store is empty
          RefTrees.isTreeRef(ct, s.getTree().getRoot().getId()) ) {
        Log.info("No changes, no commit needed");
        if( cb != null && !keepTree )
          cb = null;
        return getCurrentVersion();
      }
      int version = 
         commit(ct, ct instanceof KeyMap ? (KeyMap) ct : null, keepTree );
      if( cb != null ) {
        if( keepTree )
          cb.reset(); // Clean the changebuffer
        else
          cb = null;
      }
      return version;
    } catch (NodeNotFoundException e) {
      Log.error("Cannot commit -- missing node",e);
    } catch (IOException e) {
      Log.error("Cannot commit -- I/O error",e);
    }
    return NO_VERSION;
  }

  /** Open the store.
   * 
   * @throws IOException
   */
  public void open() throws IOException {
    if( !isOpened ) {
      if( Measurements.STORE_TIMINGS )
        Time.stamp(Measurements.H_RAXS_OPEN);
      if( Measurements.MEMORY )
        Measure.set(Measurements.M_STORE_PREOPEN,Debug.usedMemory());
      s.open();
      if( Measurements.MEMORY )
        Measure.set(Measurements.M_STORE_OPEN,Debug.usedMemory());
      isOpened = true;
    }
  }
  
  /** Close the store.
   * 
   * @throws IOException
   */
  public void close() throws IOException {
    if( Measurements.MEMORY )
      Measure.set(Measurements.M_STORE_PRECLOSE,Debug.usedMemory());
    if( cb != null )
      cb = null; // BUGFIX-20061214-11: Discard change buffer on close
    if( isOpened ) {
      if( Measurements.STORE_TIMINGS )
        Time.stamp( Measurements.H_RAXS_CLOSE );      
      s.close();
      isOpened = false;
    }
    if( Measurements.MEMORY )
      Measure.set(Measurements.M_STORE_CLOSE,Debug.usedMemory());
  }
  
  /** Get tree by version.
   *
   * @param version version to retrieve,
   * {@link RandomAccessXmlStore#CURRENT_VERSION} may be to get the
   * current tree.
   * @return the requested version of the tree
   * @throws IOException if an I/O error occurs.
   */
  
  public ChangeBuffer getTree(int version) throws IOException {
    
    ChangeBuffer tree = makeChangeBuffer(); 
    if( version == CURRENT_VERSION )
      return tree; // Current tree requested
    if( version > getCurrentVersion() || version == NO_VERSION || version < 0 )
      throw new NoSuchVersionException(version);
    int v = NO_VERSION;
    try {
      for (v = getPreviousVersion(getCurrentVersion()); v >= version;
      v = getPreviousVersion(v)) {
        // load olders-refs-tree
        Diff reverseDelta = null;
        PushbackInputStream din = new PushbackInputStream( getDeltaInStream(v) );
        try {
          // BUGFIX-20061221-1: Handle recall of empty version
          int t = din.read(); 
          if( t == -1 ) {
            // Delta to empty store
            tree.delete(tree.getRoot().getId());
            return tree;
          } 
          din.unread(t);
          ParserSource pr = getParser(din); 
          ItemSource in = new TransformSource(pr,new RemoveSafeXasItems());
          reverseDelta = Diff.readDiff(in, s.getTreeModel(),
              tree.getRoot().getId());
          // DEBUG: dump decoded delta
          /*{
           Log.debug("Base:");
           XmlrDebug.dumpTree(tree);
           Log.debug("Delta: "+(v+1)+"->"+v);
           XmlrDebug.dumpTree(reverseDelta);
           Log.debug("Delta as XMLR:");          
           RefTree r = reverseDelta.decode(tree);
           XmlrDebug.dumpTree(r);
           
           }*/
          tree.apply(reverseDelta.decode(tree));
        } finally {
          din.close();
        }
        // DEBUG: dump decoded delta
        /*{
          Log.debug("Change tree after apply:");
          XmlrDebug.dumpTree(tree.getChangeTree());
          Log.debug("Full tree after apply:");         
          XmlrDebug.dumpTree(tree);
        }*/
        
      }
    } catch (NodeNotFoundException ex) {
      Log.error("Broken delta references unknown node "+
          ex.getMessage()+" in version "+v,ex);
      throw new IOException("Broken data in rev "+v);
    }
    return tree;
  }

  /** Get reftree by version. Gets a reftree for version
   * <code>version</code> that refs nodes in version <code>refsVersion</code>.
   *
   * @param version version to retrieve,
   * {@link RandomAccessXmlStore#CURRENT_VERSION} may be to get the
   * current tree.
   * @param refsVersion version to reference,
   * {@link RandomAccessXmlStore#CURRENT_VERSION} may be used to
   * reference the current tree.
   * {@link RandomAccessXmlStore#NO_VERSION} may be used if there
   * may be no references in the returned tree.
   * @return the requested version of the tree as a reftree referencing
   *  <code>refsVersion</code>.
   * @throws IOException if an I/O error occurs.
   */
  
  public RefTree getRefTree(int version, int refsVersion) throws IOException {
    open();
    if( version == CURRENT_VERSION && refsVersion == NO_VERSION )
      return s.getTree(); // Refs nothing
    if (refsVersion == CURRENT_VERSION &&
        version == CURRENT_VERSION)
      return RefTrees.getRefTree(s.getTree()); // current-refs-current
    int x = version, y = refsVersion;
    RefTree cRy = getCurrentRefsOld(y);
    RefTree xRc = getTree(x).getChangeTree();
    
    try {
      // DEBUG 041216
      /*
       Log.log("xRc is: x=" + x, Log.INFO);
       try {
       XmlUtil.writeRefTree(xRc, System.err, DirectoryEntry.XAS_CODEC);
       } catch (Exception x2) {}
       System.err.flush();
       
       Log.log("cRy is: y=" + y, Log.INFO);
       try {
       XmlUtil.writeRefTree(cRy, System.err, DirectoryEntry.XAS_CODEC);
       } catch (Exception x2) {}
       System.err.flush();*/
      
      return RefTrees.combine(xRc,cRy,s.getTree());
    } catch (NodeNotFoundException ex) {
      Log.log("Broken repository, id=" + ex.getId(), Log.FATALERROR);
    }
    return null;
  }

  /** Applies a tree. Redirects to {@link RandomAccessXmlStore#commit(RefTree)}.
   */

  public void apply(RefTree t) throws NodeNotFoundException {
    commit(t);
  }
  
  /** Return store active tree model. 
   * @return store tree model
   */
  public TreeModel getModel() {
    return s.getTreeModel();
  }
  
  /** Get current version of store.
   * 
   */
  public int getCurrentVersion() {
    if( !currentVersionInited ) {
      currentVersionInited = true;
      File hdir = config.getHistoryFile();
      
      int high = MAX_VERSION;
      int low = FIRST_VERSION;
      int oldHigh = MAX_VERSION;
      int found = -1;
      while (hdir != null) {
        if (!getDeltaFile(hdir, high).exists()) {
          oldHigh = high;
          high = low + ((high + 1 - low) / 2);
          if (oldHigh == high) {
            found = high - 1;
            break;
          }
        } else {
          low = high;
          high += ((oldHigh - high) / 2);
          if (low == high) {
            found = low;
            break;
          }
        }
      }
      currentVersion=found > -1 ? found+1 : FIRST_VERSION;
      Log.debug("Store current version is ",currentVersion);
      assert !getDeltaFile(hdir, currentVersion).exists(): "Failed to get max ver";
    }
    return currentVersion;
  }

  /** Get oldest retrievable version of store. This version may be &gt; 
   * {@link #FIRST_VERSION} if the store has purged part of the revision
   * history in order to save space, etc. 
   * 
   * @return oldest version
   */
  public int getOldestVersion() {
    if( !oldestVersionInited ) {
      if( getCurrentVersion() == NO_VERSION )
        return NO_VERSION;
      oldestVersionInited = true;
      oldestVersion = FIRST_VERSION;      
    }
    return oldestVersion;
  }

  /*** Get history of retrievable store versions.
   * @return history
   */

  public VersionHistory getVersionHistory() {
    File hdir = config.getHistoryFile();
    if( hdir == null  )
      return VersionHistory.EMPTY_HISTORY;
    return new VersionHistoryImpl(this);
  }
  
  /** Get next version that will be assigned on a non-empty commit.
   * 
   * @return next version
   */
  public int getNextVersion() {
    return getCurrentVersion() < FIRST_VERSION ? FIRST_VERSION : 
      getCurrentVersion()+1;
  }

  /** get version preceding the given version.
   * 
   * @param v version
   * @return version immediately preceding <i>v</i>
   */
  public int getPreviousVersion(int v) {
    return --v >= FIRST_VERSION ? v : NO_VERSION;
  }
  
  protected int commit(final IdAddressableRefTree deltaRefTree,
      KeyMap newToCurr, boolean keepTree ) 
    throws NodeNotFoundException, IOException {
    if( Measurements.RAXS_TIMINGS )
      Time.stamp(Measurements.H_RAXS_STOREREVERSEDELTA);    
    storeReverseDelta(deltaRefTree, s.getTree(), newToCurr);
    // Commit!
    if( Measurements.RAXS_TIMINGS )
      Time.stamp(Measurements.H_RAXS_COMMIT_APPLY);
    try {
      s.apply(deltaRefTree);
    } catch (NodeNotFoundException e) {
      Log.error("Apply of new tree failed: Node not found "+e.getId());
      throw e;
    }
    return setCurrentVersion(getNextVersion()); // Increase version number
  }

  protected void storeReverseDelta(final IdAddressableRefTree deltaRefTree,
      IdAddressableRefTree currentTree,
      KeyMap newToCurr) throws NodeNotFoundException, IOException {
    if( currentTree.getRoot() == null) {
      // The current tree is the empty tree. We mark this with an empty 
      // reverse delta.
      Log.info("Special case: storing delta to empty tree");
      OutputStream emptyOut = getDeltaOutStream(getCurrentVersion());
      emptyOut.close();
      return;
    }
    if( newToCurr != null )
      Log.debug("Running algorithms with re-keyed trees");    
    
    final IdAddressableRefTree deltaRefTreeRK = newToCurr != null ?
        RekeyedRefTree.create(deltaRefTree, newToCurr) : deltaRefTree;
        
    RefTree currentAsRefTree = RefTrees.getRefTree(currentTree);
        
    Set[] usedRefs = RefTrees.normalize(currentTree,
        new RefTree[] { deltaRefTreeRK, currentAsRefTree });

    Log.debug("No of refs in new to current "+usedRefs.length);

    Set allowedContentRefs = new NonListableSet() {
      // The allowed content refs in currentRefsNew. A content ref is allowed if
      // the nodes have the same content. This is the case for a node n
      // if that node in the deltaTree refs to the backTree(=current version)

      public boolean contains(Object id) {
        RefTreeNode n = deltaRefTreeRK.getNode((Key) id);        
        return n==null ? false : n.isReference();
      }
    };

    Debug.Time.stamp();
    RefTree currentRefsNewRK= RefTrees.expandRefs(currentAsRefTree,usedRefs[0],
                                                 allowedContentRefs,
                                                 currentTree);
    Log.info("Reverse-delta expansion took",Debug.Time.sinceFmt());

    RefTree currentRefsNew = newToCurr != null ?
      new RekeyedRefTree.RemapRefsTree(currentRefsNewRK,newToCurr) :
        currentRefsNewRK;
    
    /*{
     // DEBUG CODE
      Log.debug("Tree dump of current:");
      XmlrDebug.dumpTree(currentTree);
      Log.debug("Tree dump of delta:");
      XmlrDebug.dumpTree(deltaRefTree);
      //Log.debug("Tree dump of current-refs-deltaRK:");
      //XmlrDebug.dumpTree(currentRefsNewRK);
      Log.debug("Tree dump of current-refs-delta:");
      XmlrDebug.dumpTree(currentRefsNew);
    }*/
    // Store the current-refs-new tree
    OutputStream deltaStream = getDeltaOutStream(getCurrentVersion());
    try {
      Diff reverseDiff = Diff.encode(deltaRefTree,currentRefsNew);
      //Log.debug("Committing diff");
      //XmlrDebug.dumpTree(reverseDiff);
      XmlOutput deltaTarget = new XmlOutput(deltaStream,"UTF-8");
      XasCodec diffModel = reverseDiff.getDiffCodec(s.getTreeModel());
      /*XasSerialization.writeTree(reverseDiff, XasDebug.itemDump() , diffModel, 
          _HEADER, _TRAILER);*/
      ItemTarget deltaOut = new TransformTarget(deltaTarget, new
          AddSafeXasItems());
      XasSerialization.writeTree(reverseDiff, deltaOut, diffModel,
          s.getTreeModel(), 
          XasUtil.itemSource( _HEADER ),
          XasUtil.itemSource(_TRAILER ) );
      deltaTarget.flush();
    } finally {
      deltaStream.close();
    }
  }

  protected RefTree getCurrentRefsOld(int oldVersion) throws IOException {
    final IdAddressableRefTree oRc = getTree(oldVersion).getChangeTree();
    // Make reverse-delta
    IdAddressableRefTree currentTree = s.getTree();
    RefTree currentAsRefTree = RefTrees.getRefTree(currentTree);
    RefTree cRo = null;
    try {
      Set[] allowedRefs =
        RefTrees.normalize(currentTree, new RefTree[] {oRc,currentAsRefTree});

      // allow content refs from current to old, if old refs the current
      // content (since in that case, they are equal)
      Set allowedContent = new NonListableSet() {
        public boolean contains(Object id) {
          RefTreeNode n = oRc.getNode((Key) id);
          return n==null ? false : n.isReference();
        }
      };
      cRo = RefTrees.expandRefs(currentAsRefTree, allowedRefs[0],
          allowedContent, currentTree);
    } catch (NodeNotFoundException ex) {
      Log.log("Broken repository, id="+ex.getId(),Log.FATALERROR);
    }
    return cRo;
  }
  
  protected ChangeBuffer makeChangeBuffer() {
    return s.getChangeBuffer() ;
  }
 
  protected int setCurrentVersion(int v) {
    return currentVersion=v;
  }

  protected InputStream getDeltaInStream(int version) throws IOException {
    File hdir = config.getHistoryFile();
    if( hdir == null )
      throw new IOException("No history location configured");
    return new FileInputStream(getDeltaFile(hdir,version));
  }

  protected OutputStream getDeltaOutStream(int version) throws IOException {
    File hdir = config.getHistoryFile();
    if( hdir == null )
      throw new IOException("No history location configured");
    if( !hdir.exists() && !hdir.mkdirs() ) {
      throw new IOException("Cannot make history folder "+hdir);
    }
    return new FileOutputStream(getDeltaFile(hdir, version));
  }

  // Get file expressing the reverse delta from version+1 to version
  private File getDeltaFile(File hdir, int version) {
    return new File(hdir,String.valueOf(version)+".xml");
  }  

  /** Get store XML parser for input stream. Gets the XML parser the store uses
   * to read byte-formatted data.
   * 
   * @param is
   * @return parser
   */
  public ParserSource getParser(InputStream is) throws IOException {
    if( parser == null)
      parser = new KXmlParser();
    KXmlParser kp = (KXmlParser) parser;
    return new XmlPullSource(kp,is);
  }

  protected void finalize() {
    _dumpHistory();
  }

  /** Item transform that encodes XAS items inside an XML fragment. 
   * This transform, in conjunction with its reverse, 
   * {@link RemoveSafeXasItems}, can be used to encode any XAS items as an
   * XML fragment. In particular, the <code>SD</code> and <code>ED</code>
   * items are encoded in a special manner, as these cannot be nested
   * inside an XML document. 
   */
  public static class AddSafeXasItems implements ItemTransform {
    
    private static final String XAS_NS_PREFIX = "xas";

    /** XAS Item namespace. Set to {@value}.
     */
    public static final String XAS_ITEM_NS 
    = "http://www.hiit.fi/fuego/fc/xml/xas";
    
    /** Tag for SD item. Set to <code>SD</code>.
     */

    public static Qname SD_TAGNAME = new Qname(XAS_ITEM_NS,"SD"); 

    /** Tag for SD item. Set to <code>ED</code>.
     */
    public static Qname ED_TAGNAME = new Qname(XAS_ITEM_NS,"ED");
    
    protected Queue<Item> queue = new LinkedList<Item>();
    protected int depth=-1; // 0 after SD, 1 after 2nd SD etc.
    
    public boolean hasItems() {
      return !queue.isEmpty();
    }
    
    public Item next() throws IOException {
      return queue.poll();
    }
    
    public void append(Item item) throws IOException {
      if( Item.isStartDocument(item)) {
        depth++;
        if( depth > 0) {
          StartTag st = new StartTag(SD_TAGNAME);
          st.ensurePrefix(XAS_ITEM_NS, XAS_NS_PREFIX);
          queue.add(st);
          queue.add(new EndTag(SD_TAGNAME));
          return;
        }
      } else if( Item.isEndDocument(item)) {
        depth--;
        if( depth > 0) {
          StartTag st = new StartTag(ED_TAGNAME);
          st.ensurePrefix(XAS_ITEM_NS, XAS_NS_PREFIX);
          queue.add(st);
          queue.add(new EndTag(ED_TAGNAME));
          return;
        }
      } else if ( Item.isStartTag(item)) {
        depth ++;
      } else if ( Item.isEndTag(item)) {
        depth--;
      }
      queue.add(item);
    }
  }

  /** Item transform that decodes XAS items inside an XML fragment. 
   * This transform, in conjunction with its reverse, 
   * {@link AddSafeXasItems}, can be used to decode any XAS items from an 
   * XML fragment. In particular, the <code>SD</code> and <code>ED</code>
   * items are encoded and decoded in a special manner, as these cannot 
   * be nested inside an XML document. 
   */

  public static class RemoveSafeXasItems implements ItemTransform {
        
    protected Queue<Item> queue = new LinkedList<Item>();
    
    public boolean hasItems() {
      return !queue.isEmpty();
    }
    
    public Item next() throws IOException {
      return queue.poll();
    }
    
    public void append(Item item) throws IOException {
      if( Item.isStartTag(item) && 
          AddSafeXasItems.SD_TAGNAME.equals( ((StartTag) item).getName() ) ) {
        queue.add(StartDocument.instance());
        return;
      } else if ( Item.isEndTag(item) && 
          AddSafeXasItems.SD_TAGNAME.equals( ((EndTag) item).getName() ) ) {
        queue.add(EndDocument.instance());
      }
      queue.add(item);
    }
  }
  
  // DEBUG Stuff

  public static Map<String,TreeMap<Integer,byte[]>> _repo =
    new HashMap<String,TreeMap<Integer,byte[]>>();
  
  private  TreeMap<Integer,byte[]> _deltas = null;
  
  private int _initStore(String key) {
    _deltas = _repo.get(key);
    if( _deltas==null) {
      _deltas = new TreeMap<Integer,byte[]>();
      _repo.put(key, _deltas);
      Log.info("New store "+key);
      return FIRST_VERSION;
    } else {
      if( _deltas.isEmpty() )
        return FIRST_VERSION;
      Log.info("Reusing store "+key+" at rev "+(1+_deltas.lastKey()));
      return _deltas.lastKey()+1;
    }
  }
  
  protected InputStream _getDeltaInStream(int version) throws IOException {
    byte[] data = _deltas.get(version);
    if( data == null )
      throw new IOException();
    return new ByteArrayInputStream(data);
  }
  
  protected OutputStream _getDeltaOutStream(final int version) {
    return new FilterOutputStream(new ByteArrayOutputStream()) {

      @Override
      public void close() throws IOException {
        out.close();
        byte[] data = ((ByteArrayOutputStream) out).toByteArray();
        _deltas.put(version, data);
        Log.debug("Delta " + version, data.length > 1500 ? "" + data.length
            + " bytes" : new String(data));
        // Log.debug("Delta "+version,new String(data));
      }
      
    };
  }
  
  private void _dumpHistory() {
    String vh = "";
    int size = 0;
    for( Map.Entry<Integer, byte[]> e : _deltas.entrySet() ) {
      size+=e.getValue().length;
      vh+=e.getKey().toString()+": "+e.getValue().length+" bytes ("+
        size+" bytes)\n";
    }
    Log.info("Version history",vh);
  }
}

// arch-tag: 3c96ad38-0505-4d87-9812-3ba375072e99
