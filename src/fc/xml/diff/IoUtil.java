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

// $Id: EventUtil.java,v 1.5.2.1 2006/06/30 12:48:04 ctl Exp $

package fc.xml.diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.List;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import fc.util.ImmutableArrayList;
import fc.xml.xas.AttributeNode;
import fc.xml.xas.Comment;
import fc.xml.xas.EndTag;
import fc.xml.xas.EntityRef;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.NullItem;
import fc.xml.xas.ParserSource;
import fc.xml.xas.Pi;
import fc.xml.xas.StartTag;
import fc.xml.xas.Text;
import fc.xml.xas.TransformSource;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.XmlPullSource;
import fc.xml.xas.transform.DataItems;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.model.XasCodec;
import fc.xml.xmlr.xas.XasSerialization;

public class IoUtil {

  public static final boolean PRESERVE_ENTITY_REFS = false;
  
  private IoUtil() {
  }

  public static <F extends ItemTransform> ItemSource getEventSequence(
      ItemSource pa, Class<F> model ) throws
      IOException, IllegalArgumentException {
    ItemTransform t = null;
    if( model != null ) {
      try {
        t = model.getConstructor(new Class[] {}).
            newInstance( new Object[] {});
      } catch (SecurityException ex) {
        throw new IllegalArgumentException(ex);
      } catch (NoSuchMethodException ex) {
        throw new IllegalArgumentException(ex);
      } catch (InvocationTargetException ex) {
        throw new IllegalArgumentException(ex);
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException(ex);
      } catch (IllegalAccessException ex) {
        throw new IllegalArgumentException(ex);
      } catch (InstantiationException ex) {
        throw new IllegalArgumentException(ex);
      }
    }
    if( t != null )
      return new TransformSource(pa,t);
    else
      return pa;
  }

  public static List<Item> makeEventList( ItemSource es,
                                          List<Item>  preamble,
                                          List<Integer> posList,
                                          XmlPullParser pa ) throws
            IOException, IllegalArgumentException {

    if( es instanceof Preamble && preamble != null )
      preamble.addAll(((Preamble) es).getPreamble());
    // Traversing hoops to avoid 1) copying es yet another time
    // b) one-at-a-time insert to COWlist, because COW.add() is O(n)!

    // Get the whole es into memory buffer
    /*int last=0,contentsInRow=0; // contentsInRow calculates how many
     // C events have a preceding C event; this is exactly the number of
     // C events that will be suppressed by diff canonicalization
    for( Item i = null; (posList == null ||
          posList.add((pa.getLineNumber()<<16)+pa.getColumnNumber()) ) &&
          (i=es.next())!=null ; ) {
      if( Item.isText(i) &&
        (es.peek() != null && Item.isText(es.peek() ) ) )
       contentsInRow++;
      last++;
    }
    final int esSize=last; */
    Item[] array = canonicalizeForDiff(es);
    //assert( array[array.length-1]!=null ); // Target array was too large...
    return new ImmutableArrayList<Item>(array);
  }

  /** Do some canonicalization needed for proper diffs. These are
   * 1) combine multiple Content events into one
   * 2) Normalize order of attributes
   * 3) Normalize order of namespace prefixes
   * @param es EventSequence es to canonicalize
   * @param esSize int size of es
   * @param array Event[] target array
   */
  private static Item[] canonicalizeForDiff( ItemSource es ) throws
      IllegalStateException, IOException {
    // FIXME-20061113-4: Concatenation on T() by filter

    // Algorithm states:
    // 0 = default (scanning)
    // 1 = last was text
    // 2 = emit text + scan
    
    
    Item[] array = new Item[1024];
    int alen = array.length;
    StringBuilder sb = new StringBuilder();
    int state = 0, dest = 0, i=0;
    for (Item e=null;(e = es.next())!=null;i++) {
      Item toAdd = null;
      if( state == 0 || state == 2) {
        if ( state == 2) {
          // Need to emit prev string
          // Array as follows
          // [dest-2] = string placeholder
          // [dest-1] = item terminating string scan
          // [dest] = TDB on this lap
          assert array[dest-2] == NullItem.instance(); 
          array[dest-2] = new Text( sb.toString() );              
          sb.setLength(0);
          state = 0;
        }
        if( isContent(e) ) {
          sb.append(textContent(e));
          toAdd = NullItem.instance(); // Placeholder for string
          state = 1;
        } else 
          toAdd= e;
      } else if( state == 1) {
        if( isContent(e) ) 
          sb.append(textContent(e));
        else {
          state = 2;
          toAdd = e;
        }
      } else
        throw new IllegalStateException("Unknown algorithm state");
      if( toAdd != null ) {
        if( dest >= alen ) { 
          // Array management
          // policy: double up to 4M entries, them grow by 4M entries a time
          // FIXME: Requires 2x final memory consumption due to array copy :(
          // Seems like the ways around this are very limited in Java :(
          int grow = Math.min(array.length, 4*1024*1024);
          Item[] na = new Item[alen+grow];
          System.arraycopy(array, 0 , na, 0, alen);
          array = na;
          alen +=grow;
        }
        //Log.debug("Putting "+toAdd+" at "+dest+", next state="+state);
        //if( toAdd instanceof StartTag )
        //  Log.debug("Context of "+toAdd,((StartTag) toAdd).getContext());
        array[dest++] = toAdd;
      }
    }
    // Finally, shrink array to desired size
    Item[] na = new Item[dest];
    System.arraycopy(array, 0 , na, 0, dest);
    return na;
  }

  private static CharSequence textContent(Item i) {
    return Item.isText(i) ? ((Text) i).getData() : 
      "&"+((EntityRef) i).getName()+";";
  }

  private static boolean isContent(Item i) {
    return PRESERVE_ENTITY_REFS ? Item.isText(i) : Item.isContent(i);
  }

  public static List<Item> getEventSequence(String f, List<Item> preamble) throws
    IOException {
    FileInputStream in = new FileInputStream(f);
    ParserSource pa = IoUtil.getXmlParser(in);
    return makeEventList(
        getEventSequence(pa,DataItems.class),preamble,null,
        null /*FIXME-20061113-3: pa */);
  }

  public static HashAlgorithm<Item> getEventHashAlgorithm() {
    return EVENT_HA;
  }

  private static HashAlgorithm<Item> EVENT_HA = new EventHasher();

  static class EventHasher implements HashAlgorithm<Item> {

    public short quickHash(Item o) {
      int hc = o.hashCode();
      return (short) ((hc&0xffff)^(hc>>16));
    }

    public void secureDigest(List<Item> el, MessageDigest md) {
      for (Item e : el) {
        // Note: since we feed the type code to m d, we needn't worry
        // about e.g. comment and text having the same string when digesting
        // the actual content
        md.update(new byte[] {(byte) e.getType(),
                  (byte) (e.getType() >> 8), (byte) (e.getType() >> 16),
                  (byte) (e.getType() >> 24)});
        if ( Item.isStartTag(e) ) {
          StartTag st = (StartTag) e;
          md.update(st.getName().getName().getBytes());
          md.update(st.getName().getNamespace().getBytes());
          for( Iterator<AttributeNode> i = st.attributes(); i.hasNext();) {
            AttributeNode an = i.next();
            md.update(an.getName().getNamespace().getBytes());
            md.update(an.getName().getName().getBytes());
            md.update(an.getValue().toString().getBytes());
          }
        } else if( Item.isEndTag(e)) {
          EndTag et = (EndTag) e;
          md.update(et.getName().getNamespace().getBytes());
          md.update(et.getName().getName().getBytes());
        } else if( Item.isText(e)) {
          Text t = (Text) e;
          md.update(t.getData().getBytes());
        } else if( e.getType() == Item.COMMENT ) {
          Comment c = (Comment) e;
          md.update(c.getText().getBytes());          
        } else if ( e.getType() == Item.ENTITY_REF ) {
          EntityRef er = (EntityRef) e;
          md.update(er.getName().getBytes());                    
        } else if ( e.getType() == Item.PI ) {
          Pi pi = (Pi) e;
          md.update(pi.getTarget().getBytes());                    
        } else if ( e.getType() == Item.START_DOCUMENT ) {
          // Type code is enough                    
        } else if ( e.getType() == Item.END_DOCUMENT ) {
          // Type code is enough                    
        }
        //      FIXME-20061113-1:  add any other item types
      }
    }
  }

  public static final ParserSource 
    getXmlParser(InputStream is) throws IOException {
    return new XmlPullSource(new KXmlParser(),is);
  }

  public static void writeRefTree(MutableRefTree baset, File basef, XasCodec model) 
    throws IOException {
    OutputStream os = null;
    try {
      os = new FileOutputStream(basef);
      ItemTarget out = new XmlOutput(os,"UTF-8");
      XasSerialization.writeTree(baset, out, model);
    } finally {
      if( os != null )
        os.close();
    }
  }

}
