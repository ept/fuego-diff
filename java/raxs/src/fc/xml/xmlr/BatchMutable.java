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


/** Interface for trees that can be restructured to a reftree. 
 */

public interface BatchMutable {
  
  /** Applies a reftree to this tree. If successful, the tree will have the
   * same structure and content as <i>t</i>.
   * 
   * @param t Tree to apply
   * @throws NodeNotFoundException if a node is missing.
   */
  
  public void apply(RefTree t) throws NodeNotFoundException;
  
}

// arch-tag: 5b48a083-79c9-4ba8-b71b-9992e94654af
