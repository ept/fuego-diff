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

import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.XasUtil;

/** Item source that allows peeking one item ahead. Also support quota
 * checking.
 */
public class PeekableItemSource implements ItemSource {

  int left = 0;
  Item putBack;
  int pbCount =0;
  ItemSource s;
  
  /** Create item source.
   * 
   * @param s underlying source
   */
  public PeekableItemSource(ItemSource s) {
    this.s = s;
  }

  /** @inheritDoc */
  public Item next() throws IOException {
    fill();
    Item i = putBack;
    if( i != null ) {
      left--;
      putBack = s.next();
    }
    return i;
  }

  private void fill() throws IOException {
    if( pbCount == 0 ) {
      left--;
      putBack = s.next();
      pbCount++;
    }
  }

  /** Peek next item.
   * 
   * @return next item, or <code>null</code> if none
   * @throws IOException if an I/O error occurs
   */
  public Item peek() throws IOException { 
    fill();
    return putBack;
  }

  /** Allocate new quota. The quota is basically a counter of consumed items
   * 
   * @param items number of items
   * @return <code>true</code>
   */
  public boolean newquota(int items) {
    left = items;
    return true;
  }
  
  /** Read quota. 
   * 
   * @return number of items not yet read since last {@link #newquota(int)}.
   */
  public int getLeft() {
    return left;
  }

  /** Item source holding exactly one item. The source is refillable.
   * 
   */

  public static class OneItemSource extends PeekableItemSource {

    //private boolean checkReads=false;
    
    public OneItemSource(Item i) {
      super(XasUtil.EMPTY_SOURCE);
      refill(i);
    }
    
    public void refill(Item i) {
        left = 1;
        putBack = i;
        pbCount = 1;
    }
  }  
}

// arch-tag: c8e6e0c7-b13f-4238-8fa1-90aa990cc0ad
