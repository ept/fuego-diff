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

package fc.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/** Immutable list on top of an array. Really fast access and subList.
 * Never copies the underlying array. 
 * @author Tancred Lindholm
 */

public class ImmutableArrayList<E> implements List<E> {

  private E[] array;
  int base;
  int len;

  public ImmutableArrayList(E[] array) {
    this(array,0,array.length);
  }

  public ImmutableArrayList(E[] array, int base, int end) {
    this.array=array;
    this.base = base;
    this.len = end-base;
  }

  public int size() {
    return len;
  }

  public boolean isEmpty() {
    return len==0;
  }

  public boolean contains(Object o) {
    throw new UnsupportedOperationException();
  }

  public Iterator<E> iterator() {
    return new IALIterator(base,base+len);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("[");
    for( int i=0;i<len;i++) {
      if( i > 0 )
        sb.append(',');
      sb.append(array[i+base].toString());
    }
    return sb.append(']').toString();
  }

  public E[] toArray() {
    return array;
  }

  public <E> E[] toArray(E[] a) {
    throw new UnsupportedOperationException();
  }

  public boolean add(E o) {
    throw new UnsupportedOperationException();
  }

  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  public boolean addAll(Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  public boolean equals(Object o) {
    throw new UnsupportedOperationException();
  }

  /* The java.lang.Object version should be OK
  public int hashCode() {
    throw new UnsupportedOperationException();
  }*/

  public boolean addAll(int index, Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  public E get(int index) {
    assert( index < len && index > -1);
    return array[base+index];
  }

  public E set(int index, E element) {
    throw new UnsupportedOperationException();
  }

  public void add(int index, E element) {
    throw new UnsupportedOperationException();
  }

  public E remove(int index) {
    throw new UnsupportedOperationException();
  }

  public int indexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  public int lastIndexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  public ListIterator<E> listIterator() {
    return new IALIterator(base,base+len);
  }

  public ListIterator<E> listIterator(int index) {
    assert(index<len);
    return new IALIterator(base+index,base+len);
  }

  public List<E> subList(int fromIndex, int toIndex) {
    assert(toIndex <= len && fromIndex <= toIndex && toIndex > -1);
    return new ImmutableArrayList<E>(array,base+fromIndex,base+toIndex);
  }

  private class IALIterator implements ListIterator<E> {

    int min,max,i;

    public IALIterator(int min, int max) {
      this.min = min;
      this.max = max;
      i=min;
    }

    public boolean hasNext() {
      return i<max;
    }

    public E next() {
      return array[i++];
    }

    public boolean hasPrevious() {
      return i>min;
    }

    public E previous() {
      return array[--i];
    }

    public int nextIndex() {
      return i;
    }

    public int previousIndex() {
      return i-1;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    public void set(E o) {
      throw new UnsupportedOperationException();
    }

    public void add(E o) {
      throw new UnsupportedOperationException();
    }
  }
}
// arch-tag: df125495-1c5d-4bf3-baff-25a1b900316d
//
