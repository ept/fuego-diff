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
import java.io.IOException;
import java.util.Iterator;

import fc.util.IOExceptionTrap;
import fc.util.Debug.Time;
import fc.util.log.Log;
import fc.xml.xas.FragmentPointer;
import fc.xml.xas.Item;
import fc.xml.xas.StartDocument;
import fc.xml.xas.index.Index;
import fc.xml.xas.index.LazyFragment;
import fc.xml.xas.index.SeekableKXmlSource;
import fc.xml.xas.index.SeekableSource;
import fc.xml.xmlr.ChangeBuffer;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.model.KeyIdentificationModel;
import fc.xml.xmlr.model.TreeModel;
import fc.xml.xmlr.xas.DeweyKey;
import fc.xml.xmlr.xas.MutableDeweyRefTree;
import fc.xml.xmlr.xas.UniformXasCodec;
import fc.xml.xmlr.xas.XasRefTree;

/** XAS item store keyed by Dewey XPaths. This store provides random access
 * by Dewey XPaths to an underlying XML file. The file is indexed on store 
 * open. The in-memory representation is a tree of 
 * {@link fc.xml.xas.index.LazyFragment LazyFragments} for the
 * document. Whenever a key is looked-up, the corresponding 
 * <code>LazyFragments</code>
 * are forced.
 */

public class DeweyStore extends XasStore implements IOExceptionTrap {

  /** Create store by configuration.
   * 
   * @param sc Configuration of store.
   * @throws IOException 
   */
  
  public DeweyStore(StoreConfiguration sc ) throws IOException {
    this(sc.getStoreFile(), sc.getModel() == null ? XAS_ITEM_TREE :
      sc.getModel(),null);
  }

  /** Create store by file and tree model.
   * 
   * @param f XML file
   * @param tm tree model. Currently only {@link XasStore#XAS_ITEM_TREE} is
   * allowed.
   */
  
  public DeweyStore(File f, TreeModel tm ) {
    this(f,tm,null);
  }

  /**  Create store by file and tree model.
   * 
   * @param f XML file
   * @param tm tree model. Currently only {@link XasStore#XAS_ITEM_TREE} is
   * allowed.
   * @param trap trap for non-reportable exceptions, <code>null</code> means
   * use this object.
   */
  public DeweyStore(File f, TreeModel tm, IOExceptionTrap trap) {
    this.f = f;
    this.tm = tm;
    writable = !f.exists() || f.canWrite();
    this.trap = trap == null ? this : trap;
  }

  /** @inheritDoc
   */

  @Override
  public ChangeBuffer getChangeBuffer() {
    if( !writable )
      return null;
    MutableDeweyRefTree changeTree = new MutableDeweyRefTree();
    changeTree.setForceAutoKey(true);
    return new ChangeBuffer(changeTree,t,changeTree);
  }

  protected void init(File f) {
    if( Measurements.STORE_TIMINGS )
      Time.stamp( Measurements.H_STORE_INIT );
    try {
      ls = new DeweySource(f); 
    } catch (IOException e) {
      trap(e);
    }
    t = new XasRefTree(ls,(UniformXasCodec) tm.getCodec());
    Log.debug("Tree root is "+t.getRoot());
    if( Measurements.STORE_TIMINGS )
      Log.debug("Init took",Time.sinceFmt(Measurements.H_STORE_INIT));
  }

    
  protected class DeweySource extends LazySource {

    private LazyFragment r=null; 
    private SeekableSource source;
    private Index index;
    private FragmentPointer currentPos; 

    public DeweySource(File f) throws IOException {
      init(f);
    }
    
    public DeweyKey getRoot() {
      return DeweyKey.ROOT_KEY;
    }

    public DeweyKey getParent(Key k) throws NodeNotFoundException {
      if( k == null || !(k instanceof DeweyKey))
        throw new NodeNotFoundException(k); 
      return ((DeweyKey) k).up();
    }

    public boolean contains(Key dk) {
      if( r ==  null || dk == null || !( dk instanceof DeweyKey ) )
        return false;
      fc.xml.xas.index.DeweyKey k =  ((DeweyKey) dk).getXasDeweyKey();
      boolean exists = safeFind(k) != null;
      //Log.debug("Exist test for "+dK+" (native="+k+") is",exists);
      return exists;
    }

    public Iterator<Key> getChildKeys(Key dk) 
      throws NodeNotFoundException {
      if( r ==  null || dk == null || !( dk instanceof DeweyKey ) )
        throw new NodeNotFoundException(dk);
      final fc.xml.xas.index.DeweyKey p =  ((DeweyKey) dk).getXasDeweyKey();
      Index.Entry e = safeFind(p);
      if( e == null  )
        throw new NodeNotFoundException(dk);
      return new Iterator<Key>() {

        fc.xml.xas.index.DeweyKey pos = p.down();

        public boolean hasNext() {
          return index.find(pos)!=null;
        }

        public DeweyKey next() {
          DeweyKey dk = new DeweyKey(pos);
          pos = pos.next();
          return dk;
        }

        public void remove() {
          throw new UnsupportedOperationException(); 
        }
      };
    }

    public void seek(Key dk) throws NodeNotFoundException {
      iseek(dk);
    }

    public KeyIdentificationModel getKeyIdentificationModel() {
      return tm;
    }

    public Item next() throws IOException {
	if (currentPos != null) {
	    //Log.debug("Current store position is",currentPos);
        Item item = currentPos.get();
        currentPos.advance();
	    return item;
	} else
	    return null;
    }

    public void close() throws IOException {
      if( r != null )
        source.close();
    }

    public Item getLazyTree(Key dk) throws NodeNotFoundException, 
      IOException {
      Log.log("Making lazy tree for key ",Log.TRACE, dk);
      fc.xml.xas.index.DeweyKey k =  iseek(dk);
      return new LazyFragment(index,k,next());      
    }

    protected Index.Entry safeFind(fc.xml.xas.index.DeweyKey k) {
      Index.Entry e = index.find(k);
      return e;
    }
    
    protected fc.xml.xas.index.DeweyKey iseek(Key k1) throws NodeNotFoundException {
      if( r == null || !(k1 instanceof DeweyKey))
        throw new NodeNotFoundException(k1);
      DeweyKey dk = (DeweyKey) k1;
      fc.xml.xas.index.DeweyKey k =  dk.getXasDeweyKey();
      try {
        if (k.isRoot()) {
          r.force(1);
          currentPos = r.pointer();
        } else {
          currentPos = r.query(k.deconstruct());
        }
      } catch (IOException e) {
        trap(e);
      }
      if (currentPos != null) {
        return k;
      } else {
        throw new NodeNotFoundException(dk);
      }
    }
    
    protected void buildIndex() throws IOException {
      if( Measurements.STORE_TIMINGS )
        Time.stamp( Measurements.H_STORE_INDEX_INIT );
      index = Index.buildFull(source);
      Log.debug("Index built. Size="+index.size()/*,index*/);
    }  
    
    protected void init(File f) throws IOException {
      if( f.length() == 0 ) {
        // BUGFIX-20061127-1: Properly decode empty file as empty store
        r = null; // Store is empty
        return;
      }
      source = new SeekableKXmlSource(f.getPath());
      buildIndex();
      r = new LazyFragment(index, fc.xml.xas.index.DeweyKey.root(), StartDocument.instance());
      //Log.debug("Root set up. Root is",r);
    }
    
  }
}
