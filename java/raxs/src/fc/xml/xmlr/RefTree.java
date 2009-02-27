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

// $Id: RefTree.java,v 1.2 2004/11/22 12:45:07 ctl Exp $
package fc.xml.xmlr;

/** Base interface for a reftree, i.e. an XMLR document parse tree. RefTrees
 * consist of nodes of class {@link RefTreeNode RefTreeNode}. */

public interface RefTree {

  /** Get root node of the reftree.
   *
   * @return RefTreeNode
   */
  public RefTreeNode getRoot();
  
  /** Interface for trees that are selectively held in memory.*/ 
  public interface Unforceable {
    
    /** Inform the tree that a subtree is no longer actively used.
     * @param k root of the subtree.
     */
    public void unforce(Key k);
  }

}
// arch-tag: 67f683b77de6e2955e095c62f767d462 *-
