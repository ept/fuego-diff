/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-xmldiff-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.diff.encode;

import static fc.xml.diff.Segment.Operation.COPY;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import fc.util.StringUtil;
import fc.util.log.Log;
import fc.xml.diff.Segment;
import fc.xml.xas.EndDocument;
import fc.xml.xas.Item;
import fc.xml.xas.ItemList;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.StartDocument;
import fc.xml.xas.TransformSource;
import fc.xml.xas.XasUtil;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.transform.NsPrefixFixer;
import fc.xml.xmlr.NodeReference;
import fc.xml.xmlr.TreeReference;
import fc.xml.xmlr.model.StringKey;
import fc.xml.xmlr.xas.RefItem;

public class RefTreeEncoder implements DiffEncoder {
  
  public RefTreeEncoder() {
    
  }

  public void encodeDiff(List<Item> base,
                                    List<Item> doc,
                                    List<Segment<Item>> matches,
                                    List<Item> preamble,
                                    OutputStream out ) throws IOException {
    //time("match_end", times);
    ItemList elr = new ItemList();
    elr.append(StartDocument.instance());
    ListOfMatchedEvents rd = new ListOfMatchedEvents(matches, doc);
    do {
      rd.next();
    } while( !Item.isStartItem( rd.peek() ) );
    encodeDiff(rd, elr, true, new LinkedList<RefItem>(),
                    new MultiXPath(base),
                    new String[1],base);
    elr.append(EndDocument.instance());
    //Log.debug("Diff as reftee seq");
    //OutputStream rtout = new java.io.FileOutputStream("/tmp/rt");
    //XasUtil.copy(elr.source(), XasDebug.itemDump(System.err));
    //rtout.close();
    
    long _er=System.currentTimeMillis();
    ItemSource des = getOuputTransform(elr.source());
    //System.out.println("Reftree-ES is:\n"+res.getCurrentSequence());
    
    XmlOutput dser = new XmlOutput(out,"UTF-8");
    dser.FIXMEdisableContextCheck();
    //dser.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output",
    //                true);
    XasUtil.copy(des, dser);
    dser.flush(); // BUGFIX070619-2: You should always flush for clean results 
    out.close();
  }

  protected ItemSource getOuputTransform(ItemSource is) {
    return new TransformSource( is, new NsPrefixFixer() );
  }

  // Assume at ST
  // topPath[0] is filled in with XPath of the encoded subtree root
  // (it is null if the subtree is not a refnode)
  protected Boolean encodeDiff( ListOfMatchedEvents mr, ItemTarget out, 
                                   boolean parentChanged,
                                   LinkedList<RefItem> tagStack,
                                   MultiXPath xp, String[] topPath,
                                   List<Item> base ) throws IOException {
    //Log.debug("peek()="+mr.peek()+", topPath="+topPath[0]+", pos="+
    //    mr.getCurrentPosition());
    Item e = mr.peek();
    if( Item.isEndTag(e) ) {
      return null; // End of childlist
    }
    mr.next(); // BUGFIX070619-1: Don't do assert e == mr.next();, -da wont work
    Segment<Item> matchTag = mr.getCurrentTag();
    int matchPos = mr.getCurrentPosition();
    boolean changed = matchTag.getOp() != COPY;
    boolean isText = isTextLike(e);
    boolean isTag = Item.isStartTag(e); 
    String thisPath = changed ? null : // Inserts have no path!
                      getBaseRefTarget( matchPos, matchTag, xp );
    //Log.log("Path is "+thisPath+" for item "+e,Log.INFO);
    if( changed || parentChanged ) {
      // Dump enqueued open tags
      for( Item ev : tagStack )
        out.append(ev);
      tagStack.clear();
    }
    if( !changed ) {
      // FIXME-20061212-2: Proper handling of contexts
      // Suggestion: Itemtarget that tracks context automagically.
      tagStack.addLast( RefItem.makeStartItem( isText ? 
          new TreeReference(StringKey.createKey(thisPath)) :
          new NodeReference(StringKey.createKey(thisPath)), null ) );
    } else {
      out.append(e);
    }
    // Child list
    boolean childrenChanged=false;
    int delayedChildTrees = 0;
    int __TSMARK=tagStack.size();
    boolean orderChanged=false;
    // BUGFIX060313-2: Don't loop if text node
    for (Boolean childchange = null;
      !isText && (childchange = encodeDiff(mr, out,
                                           changed | childrenChanged, tagStack,
                                           xp, topPath, base)) != null; ) {
      Item nexte = mr.currentItem();
      if (nexte != null && mr.getCurrentTag() != matchTag)
        // NOTE,FIXME-070620-1: Detects back-to-back CPY ops as a discontinuity
        orderChanged = true;
      childrenChanged |= childchange;
      if (!childrenChanged && !changed) {
        assert (topPath[0] != null);
        //Log.log("Creating treeref to "+topPath[0]+" current ev "+mr.getCurrentPosition()+
        //        " "+  mr.getCurrentEvent(),Log.INFO);
        // // FIXME-20061212-2
        tagStack.addLast( RefItem.makeStartItem(
                new TreeReference(StringKey.createKey(topPath[0])), null ) );
        delayedChildTrees++;
      }
    }
    // Consume end tag
    if( isTag ) {
      e = mr.next();
      assert e == null || Item.isEndItem(e);
      // BUGFIX070620-3: Detect the case where the new doc doesn't
      // include the final list items of the base doc, i.e.
      // base=<a><b/><c/><d/></a>  new=<a><b/><c/></a>
      // We require that the end tag belongs to the same match area for this.
      // A stricter test would be to test that the last matched child item was 
      // at the end of the match area. However, I do not think this makes much 
      // of a difference 
      if( !orderChanged && e!=null && mr.getCurrentTag()!=matchTag)
        orderChanged = true;
    }
    //Log.debug("After child list of "+thisPath+", the mtags (top,now) "+matchTag+","+mr.getCurrentTag()+", orderchanged="+orderChanged);
    //Log.debug("The contentChange="+changed+",childChange="+childrenChanged+", tag stack is",tagStack);
    // remove any delayed child trees, three cases,
    // u/U=changed yes/no, c/C=childChange o/O=order
    // 1: emit all delayed: ucO
    // 2: remove all delayed uco
    // 3: do't touch stack: all others
    if( !childrenChanged && !changed && !orderChanged ) {
      for (;delayedChildTrees > 0;delayedChildTrees--)
        tagStack.removeLast(); // Ref will never be needed
      assert ( tagStack.size() == __TSMARK );
    } else if( !childrenChanged && !changed && orderChanged ) {
      // Only order changed
      for( Item ev : tagStack )
        out.append(ev);
      tagStack.clear();
      childrenChanged = true;
      // assert( tagStack.size() == 0); // Obvious from .clear() above
    } else {
      // The size()=1 case below is when a tentative open-ref-node is on the
      // stack (which will be converted to a ref-tree node and emitted)
      assert (tagStack.size() ==
              (!changed && !childrenChanged && !orderChanged ? 1 : 0));
    }
    if( !changed ) {
      if( !childrenChanged ) {
        tagStack.removeLast(); // Ref will never be needed
        if( parentChanged ) {
           // Emit a treeref, as parent has changes // FIXME-20061212-2
          RefItem ri = RefItem.makeStartItem(
              new TreeReference(StringKey.createKey(thisPath)),null);
          out.append(ri);
        }
      }
      else if (!isText ) {
        // Close ref emitted due to child changes
        RefItem ri = RefItem.makeEndItem(
            new NodeReference(StringKey.createKey(thisPath)));
        out.append(ri);
      }
    } else if( isTag )
      out.append(e); // EE (only with elements)
    assert( !changed || tagStack.size() == 0);
    topPath[0] = thisPath; // NOTE: Keep close to return to ensure correct topPath
                           // on exit (child loop changes topPath!!!)
    return changed | childrenChanged;
  }

  protected String getBaseRefTarget(int branchPos, Segment<Item> match, MultiXPath xp) {
    // Enable the _xp2 code here if you suspect MultiXPath is buggy
    // The _xp2 code will just make a new NumXPath, and seek to pos from start
    //NumericXPath __xp2 = new NumericXPath(    xp.paths.get(xp.paths.firstKey()).es );
    int offset = branchPos - match.getPosition();
    assert( offset < match.getLength() );
    //Log.log("Match for "+branchPos+" is "+(match.getOffset()+offset)+
    //        " mseg="+match,Log.INFO);
    //Log.log("Seeking xp to "+(match.getOffset()+offset+1),Log.INFO);
    //__xp2.moveto(0);
    //__xp2.moveto(match.getOffset()+offset+1);
    //return __xp2.toString();
    return xp.getPath(match.getOffset()+offset+1,match.getOffset(), match.getInsertLen());
  }


  public static class MultiXPath {
  
    private SortedMap<Integer,NumericXPath> paths = new
                                            TreeMap<Integer,NumericXPath>();
  
    public MultiXPath(List<Item> es) {
      paths.put(0,new NumericXPath(es));
    }
  
    public String getPath(int ix, int cacheIx, int len) {
      //Log.log("Patching "+ix+", in cache "+cacheIx,Log.INFO);
      assert( ix >= cacheIx );
      NumericXPath exact = paths.get(cacheIx);
      if( exact == null ) {
        // Find last below
        SortedMap<Integer,NumericXPath> lessPaths = paths.headMap(cacheIx);
        assert( lessPaths != null );
        NumericXPath refpath= paths.get(lessPaths.lastKey());
        //Log.log("Reference XPath starts at chunk "+lessPaths.lastKey()+
        //        ", has moved to "+refpath.pos,Log.INFO);
        exact = new NumericXPath( refpath );
        //Log.log("New XPath starting at "+cacheIx,Log.INFO);
        paths.put(cacheIx,exact);
        exact.moveto(ix); // DEBUG ONLY!
      }
      exact.moveto(ix);
      return exact.toString();
    }
  }

  private static boolean isTextLike(Item i ) {
    if( i == null)
      return false;
    int type = i.getType();
    return type == Item.TEXT || type == Item.COMMENT ||
    type == Item.ENTITY_REF ||
    type == Item.PI;
  }

  public static class NumericXPath {
  
    private static final int MAXDEPTH=128;
    private int len=0;
    private int[] counters= new int[MAXDEPTH];
    private int pos=0;
    private boolean lastWasText=false;
    private List<Item> es;
  
    public NumericXPath( List<Item> es ) {
      this.es = es;
    }
  
    public NumericXPath( NumericXPath base ) {
      len = base.len;
      System.arraycopy(base.counters,0,counters,0,MAXDEPTH);
      pos = base.pos;
      es = base.es;
      lastWasText = base.lastWasText;
    }
  
    public void moveto(int target) {
      int _op =pos;
      if( target < pos ) {
        Log.log("Backing: target="+target+", pos="+pos,Log.FATALERROR, new Throwable());
      }
      assert( target >= pos ); // Never go back.
      for(;pos<target;pos++) {
        Item e = es.get(pos);
        if( e == null && es.get(pos-1)==null )
          Log.log("Moved >1 beyond ED(). This should not happen...",Log.WARNING);
        int type = e==null ? -1 : e.getType(); // Allow moving beyond ED
        lastWasText = isTextLike(e);
        switch (type) {
          case -1:
          case Item.END_DOCUMENT:
          case Item.START_DOCUMENT:
          //FIXME-20061113-1:Item types not yet in XAS2. Fix commented out below
          //case Item.DOCTYPE_DECL:
            break;
          case Item.COMMENT:
          case Item.TEXT:
          case Item.ENTITY_REF:
          case Item.PI:
            down();
            up();
            forwardNextDown();
            break;
          case Item.START_TAG:
          //FIXME-20061113-1:            
          //case Item.TYPED_CONTENT:
            down();
            break;
          case Item.END_TAG:
            up();
            forwardNextDown();
            break;
        }
        //Log.log("Scanning "+pos+": "+es.get(pos)+", after="+toString(),Log.INFO);
      }
      //Log.log("Moved from "+_op+" to "+target+", new len="+len,Log.INFO);
    }
  
    public void down() {
      len++;
    };
  
    public void up() {
      counters[len]=0;
      len--;
    };
  
    public void forwardNextDown() {
      counters[len]++;
    };
  
    public String toString() {
      StringBuffer sb = new StringBuffer();
      for( int i=0;i<len;i++) {
        sb.append('/');
        sb.append(counters[i]);
      }
      if( lastWasText )
        sb.append('/').append(counters[len]-1);
      return sb.toString();
    }
  
    public static void test(List<Item> es) {
      NumericXPath xp = new NumericXPath(es);
      for(int i=0;es.get(i)!=null;i++) {
        xp.moveto(i+1);
        System.out.println(
                StringUtil.format(String.valueOf(i),-5)+
                StringUtil.format(xp.toString(),-20)+
                " "+ fc.util.Util.toPrintable( es.get(i).toString() ));
      }
  
    }
  }
}
// arch-tag: 6763904d-6168-434d-bf62-77d9f9913eb4
//
