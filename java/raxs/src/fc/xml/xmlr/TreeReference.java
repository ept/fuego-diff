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

/** A tree reference.
 */
public class TreeReference implements Reference {
  
  private Key target;

  /** Create new instance.
   * 
   * @param k reference target
   * @return instance
   */
  public static TreeReference create(Key k) {
    return new TreeReference(k);
  }

  /** Create new instance.
   * 
   * @param target reference target
   */  
  public TreeReference(Key target) {
    this.target = target;
  }
  
  /** @inheritDoc */
  public Key getTarget() {
    return target;
  }

  /** Returns true. */
  public final boolean isTreeReference() {
    return true;
  }

}

// arch-tag: 067d92d6-ca55-4283-be17-f610ae45ce78
