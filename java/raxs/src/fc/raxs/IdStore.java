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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import fc.util.IOUtil;
import fc.util.RingBuffer;
import fc.util.Util;
import fc.util.Debug.Time;
import fc.util.log.Log;
import fc.xml.xas.FragmentItem;
import fc.xml.xas.Item;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.ParserSource;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.StartTag;
import fc.xml.xas.index.Index;
import fc.xml.xas.index.SeekableKXmlSource;
import fc.xml.xas.index.SeekableSource;
import fc.xml.xas.index.Index.Entry;
import fc.xml.xmlr.ChangeBuffer;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.MutableRefTreeImpl;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.model.KeyIdentificationModel;
import fc.xml.xmlr.model.TreeModel;
import fc.xml.xmlr.xas.UniformXasCodec;
import fc.xml.xmlr.xas.XasRefTree;

public class IdStore extends XasStore {
  
  public IdStore(StoreConfiguration sc ) throws IOException {
    this(sc.getStoreFile(), sc.getModel() == null ? XMLR1_ITEM_TREE :
      sc.getModel());
  }
  
  public IdStore(File f, TreeModel model) {
    //assert model.getIdentificationModel() == IdentificationModel.ID_ATTRIBUTE 
    //  : "No implementation for that tree model";  
    this.f = f;
    this.tm = model;
    writable = !f.exists() || f.canWrite();
  }
  /** @inheritDoc
   */

  @Override
  public ChangeBuffer getChangeBuffer() {
    if( !writable )
      return null;
    return new ChangeBuffer(new MutableRefTreeImpl(t.getRoot().getId()),t);
  }

  @Override
  protected void init(File f) {
    if( Measurements.STORE_TIMINGS )
      Time.stamp( Measurements.H_STORE_INIT );
    try {
      ls = new IdSource(f); 
    } catch (IOException e) {
      trap(e);
    }
    t = new XasRefTree(ls,(UniformXasCodec) tm.getCodec());
    Log.debug("Tree root is "+t.getRoot());
    if( Measurements.STORE_TIMINGS )
      Log.debug("Init took",Time.sinceFmt(Measurements.H_STORE_INIT));
  }

  @Override
  protected ItemTransform getOutTransform() {
    return new LazyTreeConcat();
  }
  
  protected static class LazyTreeConcat implements ItemTransform {

    RingBuffer<Item> q = new RingBuffer<Item>(2);
    Entry start = null;
    SeekableSource source = null;
    int len = -1, ltc=0;
    
    public boolean hasItems() {
      return !q.isEmpty();
    }

    public Item next() throws IOException {
      assert hasItems();
      return q.poll();
    }

    public boolean concatenates(Entry start, SeekableSource source, 
        LazyTree tree) {
      boolean concat = start.getOffset() + len == tree.getEntry().getOffset() &&
        tree.getEntry().getContext() == start.getContext();
      /*Log.debug("Concat of "+
          new Entry(start.getOffset(),len,start.getContext())+" and "+
          tree.entry+" is "+concat);*/
      return concat;
    }
    
    public void append(Item item) throws IOException {
      // Emit any in-progress if needed
      LazyTree lt = item instanceof LazyTree ?
          (LazyTree) item : null;
      boolean concatenates = lt != null && start != null
        && concatenates(start,source,lt);
      if( start != null && (lt==null || !concatenates ) ) {
        // Is in-progress, emit if !lt or non-concat lt 
        LazyTree lte = new LazyTree(new Entry(start.getOffset(),len,
            start.getContext()),source);
        //Log.debug("Emitting LazyTree of length "+lte.entry.getLength()+
        //    " (combines "+ltc+" trees)");
        q.offer(lte);
        start = null;
      }
      if( lt != null ) {
        // Lazy tree -- concatenate or start
        if( start != null ) {
          // Concat
          assert concatenates : "Non-concat should already have been emitted";
          len += lt.getEntry().getLength();
          ltc++;
        } else {
          // Start
          start = lt.getEntry();
          len = start.getLength();          
          source = lt.getSource();
          ltc=1;
        }
      } else {
        assert start == null : "In progress should be impossible here";
        // Ordinary item
        q.offer(item);
      }
    }
    
  }
  
  protected static class LazyTree extends FragmentItem {
    
    private Index.Entry entry;
    private SeekableSource source;
    
    public static final int LAZY_TREE = 0x00194200;
    
    public LazyTree(Index.Entry e, SeekableSource s) {
      super(LAZY_TREE, 1);
      this.entry = e;
      this.source = s;
    }
    
    public void appendTo (ItemTarget target) throws IOException {
      if ( !(target instanceof SerializerTarget) || 
          !(source instanceof ParserSource)) {
        source.setPosition(entry.getOffset(), entry.getContext());
        int end = entry.getOffset()+entry.getLength();
        Log.debug("Item-streaming..");
        while(source.getCurrentPosition()<end) {
          Item i = source.next();
          //Log.debug("... "+i);
          target.append(i);
        }
      } else {
        ParserSource ps = (ParserSource) source;
        SerializerTarget st = (SerializerTarget) target;
        source.setPosition(entry.getOffset(), entry.getContext());
        if( !Util.equals(ps.getEncoding(), st.getEncoding()))
          throw new IOException("Incompatible stream character encodings: "
              +ps.getEncoding() +" and "+ st.getEncoding() );
        //st.flush(); // Obsolete since patch-397
        //Time.stamp();
        OutputStream out = st.getOutputStream();
        IOUtil.copyStream(ps.getInputStream(), out , entry.getLength());
        //Log.info("Stream copy of "+entry.getLength()+" took",Time.sinceFmt());
        //out.flush();
        /*{
          // DEBUG
          source.setPosition(entry.getOffset(), entry.getContext());
          Log.debug("Copying stream of size "+entry.getLength());
          IOUtil.copyStream(ps.getInputStream(), System.out, entry
              .getLength());
          System.out.flush();
        }*/
        source.setPosition(entry.getEnd(), entry.getContext());
      }
    }

    public Index.Entry getEntry() {
      return entry;
    }

    public SeekableSource getSource() {
      return source;
    }
  }
  
  
  protected class IdSource extends LazySource {

    private SeekableSource source;
    private Map<Key,Entry> index=new HashMap<Key,Entry>();
    private Map<Key,Key> parents = new HashMap<Key,Key>();
    private Map<Key,List<Key>> childLists = new HashMap<Key,List<Key>>();
    
    private Key root = null;
    
    public IdSource(File f) throws IOException {
      init(f);
    }

    @Override
    Item getLazyTree(Key k) throws NodeNotFoundException, IOException {
      Entry e = lookupEx(k);
      return new LazyTree(e,source);
    }

    public Key getRoot() {
      return root;
    }

    public Key getParent(Key k) throws NodeNotFoundException {
      if( !parents.containsKey(k) )
        throw new NodeNotFoundException(k);
      return parents.get(k);
    }

    public boolean contains(Key k) {
      return index.containsKey(k);
    }

    public Iterator<Key> getChildKeys(Key k) throws NodeNotFoundException {
      List<Key> cl = childLists.get(k);
      if( cl == null )
        throw new NodeNotFoundException(k);
      return cl.iterator();
    }

    public void seek(Key k) throws NodeNotFoundException {
      Entry e = lookupEx(k);
      try {
        source.setPosition(e.getOffset(), e.getContext());
      } catch( IOException ex) {
        throw new NodeNotFoundException("Cannot re-position parser to ",k);
      }
      
    }

    public KeyIdentificationModel getKeyIdentificationModel() {
      return tm;
    }

    public void close() throws IOException {
      if( source != null )
        source.close();
    }

    public Item next() throws IOException {
      return source.next();
    }

    protected Entry lookup(Key k)  {
      return index.get(k);
    }

    protected Entry lookupEx(Key k) throws NodeNotFoundException {
      Entry e = index.get(k);
      if( k == null )
        throw new NodeNotFoundException(k);
      return e;
    }
    
    protected void init(File f) throws IOException {
      if( f.length() == 0 ) {
        root = null;
        return;
      }
      source = new SeekableKXmlSource(f.getPath());
      Stack<ArrayList<Key>> children = new Stack<ArrayList<Key>>() ;
      children.push(new ArrayList<Key>()); // Avoid many null checks
      Stack<Key> parentStack = new Stack<Key>();
      parentStack.push(null); 
      StartTag context = null;
      Stack<StartTag> sts = new Stack<StartTag>();
      sts.push(null);
      Stack<Integer> ps = new Stack<Integer>();
      boolean isText = false;
      int depth = Integer.MAX_VALUE;
      for (Item item;(item = source.next()) != null;) {
        Key k = tm.identify(item);
        //Log.debug("Item "+item+", key "+k+" of type "+(k != null ? k.getClass() : ""));
        root = root == null && k!=null ? k : root;
        if (isText && !Item.isContent(item)) {
          Integer pos = ps.pop();
          if( k != null ) {
            children.peek().add(k);
            parents.put(k, parentStack.peek());  
            addIndexEntry(k, pos, source.getPreviousPosition(), context );
          }
          isText = false;
        }
        if (Item.isStartTag(item)) {
          if( k != null )
            children.peek().add(k);
          children.push(new ArrayList<Key>());
          parentStack.push(k);
          context = (StartTag) item;
          sts.push(context);
          ps.push(source.getPreviousPosition());
        } else if (Item.isEndTag(item)) {
          if( parentStack.peek() != null ) {
            children.peek().trimToSize();
            childLists.put(parentStack.peek(), children.pop());
          } else
            children.pop();
          sts.pop();
          context = sts.peek();
          Integer pos = ps.pop();
          Key thisKey = parentStack.peek(); 
          parentStack.pop();
          if (depth >= sts.size() ) {
            addIndexEntry(thisKey, pos, source.getCurrentPosition(), context);
            parents.put( thisKey, parentStack.peek() );
          }
        } else if (Item.isContent(item)) {
          if (!isText) {
            ps.push(source.getPreviousPosition());
          }
          isText = true;
        } else if (Item.isDocumentDelimiter(item)) {
          isText = false;
          continue;
        } else if( k != null ) {
          children.peek().add(k);
          parents.put(k, parentStack.peek());            
          addIndexEntry(k, source.getPreviousPosition(), 
              source.getCurrentPosition(), context );
        }
      }
      Log.debug("Build index of size "+index.size());
      //Log.debug("Index entries are",index);
      //Log.debug("Parent index is",parents);
      //Log.debug("Child-list index is",childLists);
    }

    private void addIndexEntry(Key k, int start, int end, StartTag context) {
      index.put(k, new Entry(start,end-start,context));
    }
    
  }

}
// arch-tag: eccd6c61-006d-42c7-946c-e5a51aa4346b
//
