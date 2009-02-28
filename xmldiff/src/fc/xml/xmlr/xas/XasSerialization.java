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

package fc.xml.xmlr.xas;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import fc.xml.xas.EndDocument;
import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.ItemList;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.StartDocument;
import fc.xml.xas.StartTag;
import fc.xml.xas.XasUtil;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.NodeReference;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeImpl;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.Reference;
import fc.xml.xmlr.TreeReference;
import fc.xml.xmlr.model.KeyIdentificationModel;
import fc.xml.xmlr.model.TransientKey;
import fc.xml.xmlr.model.TreeModel;
import fc.xml.xmlr.model.TreeModels;
import fc.xml.xmlr.model.XasCodec;

// TODO: make readtree, writetree use wrapped DefaultReferenceCodec

/** Reftree XAS serialization and deserialization methods.
 * 
 */
public class XasSerialization  {
  
  //BUGFIX-20061016-2:Use ItemList i.s.t.o.f. ItemSource, as it is re-consumable
  /** Default UTF-8 document header. The value is
   * <code>[StartDocument.instance()]</code>
   * 
   */
  public static final ItemList DOC_HEADER_UTF8 =
    (new ItemList(StartDocument.instance()));

  /** Default UTF-8 document trailer.
   * <code>[EndDocument.instance()]</code>
   */
  public static final ItemList DOC_TRAILER_UTF8 =
    (new ItemList(EndDocument.instance()));

  /** Read reftree using XMLR-1 model. The tree model used is
   * <code>TreeModels.xmlr1Model().swapCodec(xc)</code>.
   * 
   * @param s item source to read from
   * @param xc XAS codec to use
   * @return read tree
   * @throws IOException if an I/O error occurs.
   */
  public static RefTree readTree(ItemSource s, XasCodec xc) 
    throws IOException {
    return readTree(s,TreeModels.xmlr1Model().swapCodec(xc));
  }

  /** Read reftree.
   * 
   * @param s item source to read from
   * @param tm tree model for building tree
   * @return read tree
   * @throws IOException if an I/O error occurs.
   */
  public static RefTree readTree(ItemSource s, TreeModel tm) 
    throws IOException {
    List<Item> header=new LinkedList<Item>();
    List<Item> trailer=new LinkedList<Item>();
    PeekableItemSource ps = s instanceof PeekableItemSource ?
        (PeekableItemSource) s : new PeekableItemSource(s);
    // Read header
    // BUGFIX-20070625-2: Properly read trees with pre-decoded RefItems
    for(Item i=ps.peek();i != null && 
      (i.getType() != Item.START_TAG && !RefItem.isRefItem(i) );
      i=ps.peek())
      header.add(ps.next());
    RefTree tree = 
      new RefTreeImpl(readTree(null,ps,tm,0,
          tm.getCodec() instanceof XasCodec.ReferenceCodec ));
    for(Item i=ps.peek();i != null;i=ps.peek())
      trailer.add(ps.next());
    //Log.debug("Header is ",header);
    //Log.debug("Trailer is ",trailer);
    return tree;
  }

  /** Write reftree. Uses the tree model
   * <code>TreeModels.xmlr1Model().swapCodec(xc)</code>, and the default header
   * and trailer.
   * @param tree tree to write
   * @param out item output
   * @param xc codec to use
   * @throws IOException
   */
  public static void writeTree(RefTree tree, ItemTarget out, XasCodec xc) 
    throws IOException {
    writeTree(tree,out,TreeModels.xmlr1Model().swapCodec(xc),
        XasUtil.itemSource( DOC_HEADER_UTF8 ),
        XasUtil.itemSource( DOC_TRAILER_UTF8 ) );
  }

  /** Write reftree. Uses the default header
   * and trailer.
   * 
   * @param tree tree to write
   * @param out item output
   * @param tm output tree model
   * @throws IOException
   */
  public static void writeTree(RefTree tree, ItemTarget out, TreeModel tm) 
    throws IOException {
    writeTree(tree,out,tm.getCodec(),tm,
        XasUtil.itemSource( DOC_HEADER_UTF8 ),
        XasUtil.itemSource( DOC_TRAILER_UTF8 ) );
  }

  /** Write reftree.
   * 
   * @param tree tree to write
   * @param out item output
   * @param c codec to use
   * @param kim key identification model to use
   * @throws IOException
   */  
  
  public static void writeTree(RefTree tree, ItemTarget out, XasCodec c,
      KeyIdentificationModel kim) 
    throws IOException {
    writeTree(tree,out,c,kim,
        XasUtil.itemSource( DOC_HEADER_UTF8 ),
        XasUtil.itemSource( DOC_TRAILER_UTF8 ) );
  }

  /** Write reftree.
   * 
   * @param tree tree to write
   * @param out item output
   * @param tm output tree model
   * @param header document header
   * @param trailer document trailer
   * @throws IOException
   */  
  public static void writeTree(RefTree tree, ItemTarget out, TreeModel tm,
      ItemSource header, ItemSource trailer) throws IOException {
    writeTree(tree,out,tm.getCodec(),tm,header,trailer);    
  }
  
  /** Write reftree.
   * 
   * @param tree tree to write
   * @param out item output
   * @param c codec to use
   * @param kim key identification model to use
   * @param header document header
   * @param trailer document trailer
   * @throws IOException
   */  
  public static void writeTree(RefTree tree, ItemTarget out, 
      XasCodec c, KeyIdentificationModel kim, 
      ItemSource header, ItemSource trailer) throws IOException {
    AutoET aout = new AutoET(out,kim);
    XasUtil.copy(header,aout);
    writeTree((RefTreeNode) tree.getRoot(), aout, c, 
        c instanceof XasCodec.ReferenceCodec);
    XasUtil.copy(trailer,aout);
  }
  
  protected static RefTreeNode readTree(RefTreeNode parent, 
      PeekableItemSource s, TreeModel tm,  
      int childPos, boolean customRefCodec ) throws IOException {
    Item rawfirst = s.peek(), first=rawfirst;
    //Log.debug("readTree @ "+rawfirst);
    if( !customRefCodec )
      first = RefItem.decode(first);
    Reference ref = null;
    if(first == null )
      throw new IOException("Unexpected end of item stream");
    Object content = null;
    Key k = tm.identify(first);
    if( !customRefCodec && RefItem.isRefItem(first) ) {
      RefItem ri = (RefItem) first;
      s.next();
      content = ri.isTreeRef() ?  
        new TreeReference( tm.makeKey( ri.getTarget() ) ) :
        new NodeReference( tm.makeKey( ri.getTarget() ) );
      // BUGFIX-20061107-20: we did not consume treeref close tag, as described
      // below
      // Check if we decoded a treeref item (as opposed to treeref items
      // in the underlying itemSource), in which case we need to eat the ET()
      if( ri.isTreeRef() && first != rawfirst )  {
        if( RefItem.whatItem( s.peek() ) != RefItem.ITEM_TREEREF_CLOSE ) {
          throw new IOException("Expected "+new EndTag(RefItem.REF_TAG_TREE)+
            "got "+s.next());
        }
        s.next();
      }
    } else {
      assert !(tm.getCodec() instanceof UniformXasCodec) || 
        s.newquota(((UniformXasCodec) tm.getCodec()).size());
      content = tm.decode(s, tm);
      assert !(tm.getCodec() instanceof UniformXasCodec) || s.getLeft()==0 : 
        "Tree decoder violated size agreement of "+
        ((UniformXasCodec) tm.getCodec()).size()+" items.";
    }      

    RefTreeNode n = tm.build(parent, k, content, childPos);
    
    //Log.debug("Starting tree at item "+first);
    if( first.getType() == Item.START_TAG || n.isNodeRef() ) {
      // Has children until end tag or end node ref
      Item last = s.peek();
      //Log.debug("Starting child list with "+last);
      int pos=0;
      // BUGFIX-20070625-2  
      for(;last != null && last.getType()!=Item.END_TAG
               && !( last instanceof RefNodeItem && ((RefNodeItem) last).isEndTag());
        last=s.peek()) {
        //Log.debug("Reading subtree at "+s.peek());
        readTree(n,s,tm,pos++,customRefCodec);
      }
      //Log.debug("Closing with "+s.peek());
      if ( n.isNodeRef() ) {
        Item nodeRefEnd = RefItem.decode( s.next() );
        if( nodeRefEnd.getType() != RefNodeItem.NODE_REFERENCE || 
            !((RefNodeItem) nodeRefEnd).isEndTag() )
          throw new IOException("Expected end of node reference "+ref+", got "+
              nodeRefEnd);
      } else if(last != null && last.getType()==Item.END_TAG )
        s.next(); // Eat ET
    }
    return n;
  }

  protected static void writeTree(RefTreeNode node, AutoET out, 
      XasCodec codec, boolean customRefTags ) throws IOException {
    assert !( codec instanceof UniformXasCodec) || 
      out.newquota(((UniformXasCodec) codec).size());
    out.mark(node.getId());
    boolean emitRef = node.isReference() && !customRefTags;
    if( emitRef ) {
      RefItem ri = RefItem.makeStartItem(node.getReference(),out.context());
      out.append(ri);
    } else
      codec.encode(out,node, out.context());      
    assert !(codec instanceof UniformXasCodec) || out.getLeft() == 0 :
      "Tree encoder violated size agreement of "+
      ((UniformXasCodec) codec).size()+" items.";
    for( Iterator i = node.getChildIterator();i.hasNext();)
      writeTree((RefTreeNode) i.next(), out, codec, customRefTags);
    if( emitRef && node.isNodeRef() ) {
      RefItem ri = RefItem.makeEndItem(node.getReference());
      out.append(ri);
    }
    out.autoET();
  }
    
  private static class AutoET implements ItemTarget {
    
    private int left=0;
    private ItemTarget dst;
    private Stack<Item> lastST = new Stack<Item>();
    private StartTag MARKER = null;
    private StartTag context = null;
    private KeyIdentificationModel kim;
    private static final Key KEY_USED = new TransientKey();
    private Key k=KEY_USED;
    
    public AutoET(ItemTarget dst, KeyIdentificationModel kim) {
      this.dst = dst;
      this.kim = kim;
    }

    public void mark(Key k) {
      this.k = k;
      lastST.push(MARKER);
    }
    
    public void append(Item item) throws IOException {
      if( item.getType() == Item.START_TAG ) {
        context = (StartTag) item;
        lastST.push(context);
      } else if ( RefItem.isRefItem( item  ) ) {
        context = ((RefItem) item).getContext();
        lastST.push(item);        
      }
      left--;
      if( k != KEY_USED ) {
        item = kim.tag( item, k, kim );
        k = KEY_USED;
      }
      dst.append(item);
    }
    
    public void autoET() throws IOException {
      assert !lastST.isEmpty() : "Unbalanced calls to mark and autoET";
      for(Item i=lastST.pop();i!=MARKER;i=lastST.pop()) {        
        if( Item.isStartTag(i))
          dst.append(new EndTag(((StartTag) i).getName()));
      }
      Item ci  = lastST.size() > 0 ? lastST.peek() : null;
      // Scan for previous context by going towards the stack top
      // A bit ugly, as it relies on the list-ish Java stack implementation
      //Log.debug("Stack is",lastST);  
      for(int i =-2;lastST.size()+i > 0 && ci == MARKER; i--)
        ci=lastST.elementAt(lastST.size()+i);      
      // BUGFIX-20061214-10: the test should be lastST.size()*+i* == 0
      //                     but it is not available, so we disable the assert
      //assert lastST.size() == 0 || ci != MARKER;
      context = ci == null || Item.isStartTag(ci) ? 
          (StartTag) ci : ((RefItem) ci).getContext();
    }
    
    public boolean newquota(int q) {
      left = q;
      return true;
    }

    public int getLeft() {
      return left;
    }
    
    public StartTag context() {
      return context;
    }
  }
}

// arch-tag: 7baca156-7abc-4916-a8c7-8cc91f3a24b5

