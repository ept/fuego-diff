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

package fc.xml.xmlr.model;

import fc.xml.xmlr.Key;

/** Key based on object identity. The toString() output may be identical for
 * some keys, in the very unlikely case that the VM produces non-unique
 * <code>System.identityHashCode()</code>s.
 */
public class TransientKey implements Key {

  public String toString() {
    assert false : "Tried to serialize transient key";
    return null;
  }  
  public String debugString() {
    return "t-"+Integer.toHexString(System.identityHashCode(this));
  }
  
  public static TransientKey createKey()  {
    return new TransientKey();
  }
}

// arch-tag: 904f7f1e-ba65-4601-a27e-c20e12e0bc06

