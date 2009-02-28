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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import fc.util.IOExceptionTrap;
import fc.util.IOUtil;
import fc.util.Debug.Time;
import fc.util.log.Log;
import fc.xml.xas.Item;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.StartTag;
import fc.xml.xas.TransformTarget;
import fc.xml.xas.XasUtil;
import fc.xml.xas.XmlOutput;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTreeNodeImpl;
import fc.xml.xmlr.Reference;
import fc.xml.xmlr.XmlrDebug;
import fc.xml.xmlr.model.TreeModel;
import fc.xml.xmlr.model.TreeModels;
import fc.xml.xmlr.model.XasCodec;
import fc.xml.xmlr.xas.PeekableItemSource;
import fc.xml.xmlr.xas.RandomAccessSource;
import fc.xml.xmlr.xas.UniformXasCodec;
import fc.xml.xmlr.xas.XasSerialization;

/** Store whose content is a tree of XAS items.
 */

public abstract class XasStore extends Store {

  protected TreeModel tm = null;
  protected File f;
  protected boolean writable = true;
  protected IOExceptionTrap trap;
  protected IdAddressableRefTree t;
  protected LazySource ls;

  /** Store XAS item tree model. Equivalent to {@link TreeModels#xasItemTree()}.  
   */  
  public static TreeModel XAS_ITEM_TREE = TreeModels.xasItemTree();

  public static TreeModel XMLR1_ITEM_TREE = TreeModels.xmlr1Model();

  /** Get XML file as a store. 
   * 
   * @param f File
   * @param model tree model. Currently, only {@link #XAS_ITEM_TREE} is supported.
   * @return opened store
   */
  public static Store getStoreForFile(File f, TreeModel model ) {
    if( model == XAS_ITEM_TREE )
      return new DeweyStore(f,model);
    else if ( model == XMLR1_ITEM_TREE )
      return new IdStore(f,model);
    else
      throw new IllegalArgumentException("As of yet unsupported model"+model);
  }

  /** @inheritDoc
   * 
   */
  @Override
  public void open() throws IOException {
    init(f);    
  }

  /** Apply a new tree to the store. Tree references are resolved from
   * the old tree using XAS steam copying, thus yielding maximum performance.
   */
  
  @Override
  public void apply(RefTree nt) throws NodeNotFoundException {
    if( Measurements.STORE_TIMINGS )
      Time.stamp( Measurements.H_STORE_APPLY );
    Log.debug("Applying tree");
    //XmlrDebug.dumpTree(nt);
    File newFile = null;
    FileOutputStream os = null;
    Log.debug("Store file is",f);
    try {
      if( !f.exists() && !f.createNewFile() )
        throw new IOException("Cannot create "+f);
      if( !writable )
        throw new IllegalStateException("Store not writable");
      newFile = IOUtil.createTempFile(f);
      os = new FileOutputStream(newFile)/* {
        // DEBUG code to catch extra flushes.
        int fc = 0;
        @Override
        public void flush() throws IOException {
          fc++;
          //if( fc > 2)
          //  Log.debug("Flush "+fc, new Throwable());
        }
        
      }*/;
      XmlOutput xmlOut = new XmlOutput(os,"UTF-8");
      ItemTarget out = xmlOut;
      ItemTransform ot = getOutTransform();
      if( ot != null )
        out = new TransformTarget(out,ot);
      if( Measurements.STORE_TIMINGS )
        Time.stamp( Measurements.H_STORE_APPLY_WRITETREE );
      XasSerialization.writeTree(nt, out, tm.swapCodec(new 
          LazyCodec(tm.getCodec(), ls )),
          // BUGFIX-20061018-1:Do not use default SD()/ED() wrapper in this case
          XasUtil.EMPTY_SOURCE,XasUtil.EMPTY_SOURCE);
      if( Measurements.STORE_TIMINGS )
        Time.stamp( Measurements.H_STORE_APPLY_WRITETREE_END );
      ls.close();
      xmlOut.flush();
      os.close();
      os = null;
      IOUtil.replace(newFile,f);
      newFile = null;
    } catch( IOException x) {
      trap(x);
    } finally {
      if( os != null )
        try {
          os.close();
        } catch (IOException e) {
          Log.fatal("Could not close stream.");
        }
      if( newFile != null && newFile.exists() && !newFile.delete() )
        Log.fatal("Unable to clean away temporary file "+newFile);
    }
    if( Measurements.STORE_TIMINGS )
      Log.info("Tree apply took",Time.sinceFmt(Measurements.H_STORE_APPLY));
    init(f);
    //Log.debug("The re-read tree is");
    //XmlrDebug.dumpTree(t);
  }

  protected ItemTransform getOutTransform() {
    return null;
  }
  
  /** @inheritDoc
   */

  @Override
  public boolean isWritable() {
    return writable;
  }

  /** @inheritDoc
   */
  @Override
  public IdAddressableRefTree getTree() {
    return t;
  }
  
  /** @inheritDoc
   */
  @Override
  public TreeModel getTreeModel() {
    return tm;
  }

  /** @inheritDoc
   */
  @Override
  public void close() throws IOException {
    ls.close();
    ls = null;
    t = null;
  }

  /** Default exception trap. Re-throws the exception as an
   * {@link fc.util.IOExceptionTrap.RuntimeIOException}.
   */
  
  public void trap(IOException ex) {
    Log.error("Untrapped I/O error",ex);
    throw new IOExceptionTrap.RuntimeIOException(ex);
  }

  protected abstract void init(File f);
  
  public String toString() {
    return "XasStore("+f.getName()+")";
  }

  abstract class LazySource implements RandomAccessSource<Key> {
    abstract Item getLazyTree(Key k) throws NodeNotFoundException, IOException;
  }
  
  // Codec that encodes the ref'd items to the output. Uses lazy fragments
  // for stream copy. 
  
  class LazyCodec extends XasCodec.EncoderOnly 
      implements XasCodec.ReferenceCodec {
    
    private XasCodec c;
    private LazySource ls;
    private int codecItems = -1;
    
    public LazyCodec(XasCodec c, LazySource ls) {
      if( c instanceof UniformXasCodec )
        codecItems = ((UniformXasCodec) c).size();
      this.c =  c instanceof XasCodec.ReferenceCodec ? c :
        new XasCodec.DefaultReferenceCodec(c);
      this.ls = ls;
    }

    public void encode(ItemTarget t, RefTreeNode n, StartTag context) throws IOException {
      //Log.debug("Lazy codec of ",n.getId()+" getref="+n.getReference());
      if( !n.isReference() ) {
        c.encode(t, n, context);
        return;
      }
      try {
        Reference r = n.getReference();
        Key k = r.getTarget();
        if( r.isTreeReference() ) {
          Item lazyTree = ls.getLazyTree(k);
          t.append(lazyTree);
        } else {
          ls.seek(k);
          if( codecItems != -1 ) {
            XasUtil.copy(ls, t, codecItems);
          } else {
            // We need to do a full decode+encode cycle as the number of 
            // XAS items per node content is not known, or varies.
            Log.warning("Using slower codec nodeRef copy");
            Object data = c.decode( new PeekableItemSource( ls ), getTreeModel());
            RefTreeNode tn = new RefTreeNodeImpl(n.getParent(),n.getId(),data);
            c.encode(t, tn, context);
          }
        }
      } catch (NodeNotFoundException e) {
        throw e.makeIOException();
      } 
    }

  }
  
}
// arch-tag: 4e7d0b07-5f7c-4a44-b023-f9fbd0c3e456
