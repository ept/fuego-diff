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

package fc.util.holder;

/**
 * 
 * @author Tancred Lindholm
 *
 * @param <T>
 */
public class Holder<T>{
  private T held;
  
  /**
   *  Default constructor. Leaves the held value uninitialized (initialized to the default Java value)
   * 
   */
  public Holder() {
 }
  
  /**
   *  Recommended constructor. Sets the value of the contained element to <pre>thing</pre>. 
   * @param thing The element for this holder to contain.
   */
  public Holder(T thing) {
     this.held = thing;
  }
  
  /**
   *  Sets the value of this holder's contained element to <pre>thing</pre> 
   * @param thing The element to replace the current one in this holder.
   */
  public void set(T thing) {
    this.held = thing;
  }
  
  /**
   *  Returns the value of this holder's contained element.
   * @return A value contained in this holder, of the parametrized type.
   */
  public T get() {
    return this.held;
  }
  
  /**
   *  @return A String representation of the contained element.
   */
  public String toString() {
    return held.toString();
  }
  
  /**
   * @return true if the contained object matches the one compared with. 
   */
  public boolean equals(Object o) {
      return held.equals(o);
  }
  
  /**
   *  @return a new Holder with the same object that is contained within this one.
   */
  public Object clone() {
    return new Holder<T>(held);
  }
  
  /**
   *  @return The hashcode of the contained object.
   */
  public int hashCode() {
      return held.hashCode();
  }
}
