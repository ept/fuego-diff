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

/** A reftree reference. */

public interface Reference {
  /** Get target.*/
  public Key getTarget();
  
  /** Returns true if the reference is a tree reference.
   * 
   * @return <code>true</code> if the reference is a tree reference
   */
  public boolean isTreeReference();
}

// arch-tag: c50c5c13-4b73-4564-b108-2be287c29d04
