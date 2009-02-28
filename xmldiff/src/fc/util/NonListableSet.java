/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fc-util-users@hoslab.cs.helsinki.fi.
 */

// $Id: NonListableSet.java,v 1.1 2004/08/04 09:40:32 ctl Exp $
package fc.util;

import java.util.Set;
import java.util.Collection;
import java.util.Iterator;

/** A set that is immutable, and whose members cannot be enumerated.
 * In practice, this is a Set which only supports querying for item membership.
 * Useful for infinite or very large sets that are only sparsely accessed.
 * @author Tancred Lindholm
 */

public abstract class NonListableSet implements Set {

  public NonListableSet() {
  }

  public int size() {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  public boolean isEmpty() {
    throw new UnsupportedOperationException();
  }

  public Object[] toArray() {
    throw new UnsupportedOperationException();
  }

  public boolean add(Object o) {
    throw new UnsupportedOperationException();
  }

  public abstract boolean contains(Object o);

  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  public boolean addAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  public boolean containsAll(Collection c) {
    for( Iterator i = c.iterator();i.hasNext();) {
      if( !contains(i.next()) )
        return false;
    }
    return true;
  }

  public boolean removeAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  public boolean retainAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  public Iterator iterator() {
    throw new UnsupportedOperationException();
  }

  public Object[] toArray(Object[] a) {
    throw new UnsupportedOperationException();
  }
}
// arch-tag: 2085726933be75f768554ccf30f1dd75 *-
