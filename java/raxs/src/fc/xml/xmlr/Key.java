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

/** Interface for objects that are reftree node keys.
 */

public interface Key {
  // adds the semantic that this gives the serialized form of the string
  /** Return string representation of key. This representation is used for
   * serializing the key.
   */
  
  public String toString();
}

// arch-tag: e20bd4df-e38c-4919-b29b-4180b65f19c1
