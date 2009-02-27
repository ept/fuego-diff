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

/** Map for mapping between keys of two trees. The two trees are conceptually
 * the back (bottom/underlying/old) tree and the front (top/overlying/new) tree.
 * The methods have names to reflect which which tree the returned key belongs.
 * <p>
 * NOTE: When using this interface, be careful to consistently denote one tree as
 * front and the other as back, and your code shall be sublime. Mix these up,
 * and you won't make head or tails of anything.
 */
 // FIXME: In practice, a KeyMap may not always return an existing node in
 // the back tree w/o knowing the backTree fully (i.e. a treeref in front tree
 // won't tell the extent of the ref'd tree (i.e. is treeref=/1/2, but is 
 // /1/2/3/99 in the tree?
 // We need to fix the semantics -- does the returned node always exist, and
 // code appropriately; when fixed we may need to update BUGFIX-20061009-0
 // appropriately

public interface KeyMap {
  /** Get back key corresponding to the front key.
   * 
   * @param frontKey
   * @return mapped key, or <code>null</code> if not translatable
   */
  public Key getBackKey( Key frontKey );
  
  /** get front key for corresponding back key.
   * 
   * @param backKey
   * @return mapped key, or <code>null</code> if not translatable
   */
  public Key getFrontKey( Key backKey );
  
  /** Identity map.
   * 
   */
  public static final KeyMap IDENTITY_MAP = new KeyMap()  {

    public Key getBackKey(Key frontKey) {
      return frontKey;
    }

    public Key getFrontKey(Key backKey) {
      return backKey;
    }
    
  };

  
  /** No matches map.
   * 
   */
  public static final KeyMap UNMAPPABLE = new KeyMap()  {

    public Key getBackKey(Key frontKey) {
      return null;
    }

    public Key getFrontKey(Key backKey) {
      return null;
    }
    
  };
  
  /** Reverse map. That is, front keys of the underlying map appear as back 
   * keys, and vice versa. */
  public static class ReverseMap implements KeyMap {

    KeyMap m;
    
    /** Create reverse of given map. 
     * 
     * @param m map to reverse
     */
    public ReverseMap(KeyMap m) {
      this.m = m;
    }

    /** @inheritDoc */
    public Key getBackKey(Key k) {
      return m.getFrontKey(k);
    }

    /** @inheritDoc */
    public Key getFrontKey(Key k) {
      return m.getBackKey(k);
    }
    
  }
  
}

// arch-tag: 98498cfa-65f4-4838-844b-cefb9279e706
