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

import java.util.List;
import java.util.ListIterator;

import fc.xml.diff.Segment;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;

public class ListOfMatchedEvents implements ItemSource {

  List<Segment<Item>> ml;
  ListIterator<Segment<Item>> mi;
  Segment<Item> current = null;
  ListOfMatchedEvents tagger = null;

  private List<Item> el;
  private int pos=0;
  
  public ListOfMatchedEvents(List<Segment<Item>> ml, List<Item> doc ) {
    this.el = doc;
    this.ml = ml;
    this.tagger = this;
    mi= ml.listIterator();
    current = mi.hasNext() ? mi.next() : null;
  }

  protected Segment<Item> getTag(int pos) {
    if( current == null )
      return null;
    if( current.getPosition() <= pos &&
        (current.getPosition()+current.getLength()) > pos )
      return current;
    if( pos < current.getPosition() ) {
      // Need to scan backwards until something begins on pos
      while( mi.hasPrevious() ) {
        current = mi.previous();
        if( current.getPosition() <= pos ) {
          assert( current.getPosition() <= pos );
          assert( (current.getPosition()+current.getLength()) > pos );
          return current;
        }
      }
      //Log.log("Null, because "+current+" did not have previous. Query="+pos,Log.ERROR);
      return null; // none found
    } else {
      // We need to scan forwards until something ends on pos
      while( mi.hasNext() ) {
        current = mi.next();
        if( pos < (current.getPosition() +current.getLength()) ) {
          assert( current.getPosition() <= pos );
          assert( (current.getPosition()+current.getLength()) > pos );
          return current;
        }
      }
      //Log.log("Null, because "+current+" did not have next. Query="+pos,Log.ERROR);
      return null; // None found
    }
  }


  protected int getLength(Segment<Item> s) {
    return s.getOp() != COPY ? s.getInsert().size() : s.getLength();
  }

  public Item next() {
    return pos < el.size() ? el.get(pos++) : null;
  }
  
  public Item peek() {
    return pos < el.size() ? el.get(pos) : null;
  }

  public Item currentItem() {
    return (pos -1) < el.size() ? el.get(pos-1) : null;
  }

  public Segment<Item> getCurrentTag() {
    return (pos -1) < el.size() ? getTag(pos-1) : null;
  }    
  
  public int getCurrentPosition() {
    return pos-1;
  }    
  
  public boolean equals(Object obj) {
    throw new UnsupportedOperationException();
    /*
    return obj instanceof EventSequence &&
      XasUtil.sequenceEquals(this,(EventSequence) obj);*/
  }

  public String toString() {
    return el.toString();
  }

  public int hashCode() {
    return el.hashCode();
  }
  
}
// arch-tag: 7a166985-6b5b-4959-9273-6678b0f16086
//
