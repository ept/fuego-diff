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

package fc.xml.xmlr;

import java.util.Iterator;

import fc.util.log.Log;

/** Miscellaneous XMLR utilities. 
 * 
 *
 */
public class XmlrUtil {

  public static int countNodes(RefTree t) {
    return countNodes(t.getRoot(),0);
  }

  private static int countNodes(RefTreeNode root, int i) {
    i++;
    for( Iterator<RefTreeNode> it = root.getChildIterator();it.hasNext();)
      i=countNodes(it.next(),i);
    return i;
  }

  /*
  public static class KeyIterator implements Iterator {
    
    Iterator i;
    
    KeyIterator(Iterator nodes) {
      
    }

    public boolean hasNext() {
      return i.hasNext();
    }

    public Object next() {
      return ((RefTreeNode) i.next()).getId();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
    
  }*/
  
  /** LRU cache for small quantities. Small = quantities for which a linear
   * array search is fast enough, e.g. 32 items. LRU swap-to-front happens both
   * on put and get. All operations are O(1), but 1 is larger for
   * large caches :).
   */
  // IMPL NOTE: if asserts are enabled, the cache keeps stats of hits/misses;
  // as well as logs these on finalization 
  /*
  public static final class SmallLRUCache  {
    private int hits=0; // NOTE: Needed for profiling only
    private int calls=0; // NOTE: Needed for profiling only
    private int capacity;
    private int mask;
    private int nextPos=0;
    private Object[] keyvals=null; // Array of keys and values. keys in range
                           // 0...capacity-1, and the corresponding value to 
                           // a key at index i is at capacity+i
    
    
    public SmallLRUCache(int bits) {
      capacity=1<<bits;
      mask=capacity-1;
      keyvals=new Object[2*capacity];
      Object senitel = new Object();
      for( int i=0;i<capacity;i++) 
        keyvals[i]=senitel; // Init to key array to unreachable key
    }
    
    public final void put(Object key, Object value) {
      //Log.debug("Put "+key+","+value);
      assert ++calls > 0;
      int where = locate(key);
      if( where != -1) {
        // It's already here
        int prevpos=(nextPos -1)&mask;
        if( where != prevpos ) {
          // Needs move to front
          keyvals[where]=keyvals[prevpos];
          keyvals[capacity+where]=keyvals[capacity+prevpos];
          keyvals[prevpos]=key;
          keyvals[capacity+prevpos]=value;
        }
        return;
      }
      keyvals[nextPos+capacity]=value;
      keyvals[nextPos++]=key;
      nextPos&=mask; // Wrap nextPos 
    }
    
    public final Object get(Object key) {
      //Log.debug("Get "+key);
      assert ++calls > 0;
      int where = locate(key);
      if( where != -1) {
        int prevpos=(nextPos -1)&mask;
        // It's here
        Object value = keyvals[capacity+where];
        if( where != prevpos ) {
          // Needs move to front
          keyvals[where]=keyvals[prevpos];
          keyvals[capacity+where]=keyvals[capacity+prevpos];
          keyvals[prevpos]=key;
          keyvals[capacity+prevpos]=value;
        }
        return value;
      }
      return null;
    }
    
    private final int locate(Object key)  {
      int low = nextPos;
      int high = nextPos+capacity;
      for(int i = low; i< high; i++) {
        if( keyvals[i&mask] == key ) {
          assert ++hits > 0;
          return i&mask;
        }
      }
      return -1;
    }

    protected void finalize() {
      assert stats()>0;
    }
    
    private int stats() {
      Log.info("Cache stats for cache "+hashCode()+" (hits/all): "+hits+"/"+calls);
      return 1;
    }
    
    /* 
    // Test code:
    public static void main(String[] args) {
      SmallLRUCache c = new SmallLRUCache(4);
      Object[] keys={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
      Object[] vals={100+0,100+1,100+2,100+3,100+4,100+5,100+6,100+7,100+8,
        100+9,100+10,100+11,100+12,100+13,100+14,100+15};
      for( int i = 0; i<keys.length;i++ )
        c.put(keys[i], vals[i]);
      for( int i = 0; i<keys.length;i++ )
        assert c.get(keys[i]) == vals[i];
      c = new SmallLRUCache(3);
      c.put(keys[8], vals[8]);
      for( int i = 0; i<keys.length;i++ ) {
        c.get(keys[8]); // Keep keys[8] warm
        c.put(keys[i], vals[i]);
      }
      assert c.get(keys[0]) == null; // Verify cold key was lost
      assert c.get(keys[8]) == vals[8]; // Verify warm key was kept
    }*//*
    
  
  }*/
}

// arch-tag: c1689962-aedf-4fd5-b820-8f40fa544aaa

