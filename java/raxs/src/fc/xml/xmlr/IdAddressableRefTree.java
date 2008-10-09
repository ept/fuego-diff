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

// $Id: IdAddressableRefTree.java,v 1.4 2004/11/22 12:45:03 ctl Exp $
package fc.xml.xmlr;

import java.util.Iterator;

/** A reftree that can be accessed by node ids in a random-access fashion. The
 * identifiers of the nodes in this class of trees are required to be unique.
 * An IdAddressableRefTree is typically used to hold the common base tree to
 * which the references in a set of reftrees point.
 */

public interface IdAddressableRefTree extends RefTree {

  /** Get node by id.
   * @param id id of node to access
   * @return node in the tree, or <code>null</code> if the tree has no such node
   */

  public RefTreeNode getNode(Key id);

  /** Returns <code>true</code> if this tree contains the given node.
   * @param id id of node to search for
   * @return boolean <code>true</code> if the node exists in the tree
   */
  public boolean contains(Key id);

  /** Get parent id of node id.
   * @param nid id of node, whose parent id is desired
   * @return id of parent of <code>nid</code>, or <code>null</code>
   * if <code>nid</code> is root
   * @throws NodeNotFoundException if <code>nid</code> is not in the tree.
   */
  public Key getParent(Key nid) throws NodeNotFoundException;

  /** Get child ids of node id.
   * @param id id of node, whose child ids are desired
   * @return Iterator over the {@link java.lang.String} child ids
   * @throws NodeNotFoundException if <code>id</code> is not in the tree.
   */
  // Note: iterator is over child ids, not nodes!
  public Iterator<Key> childIterator(Key id) throws NodeNotFoundException;
}
// arch-tag: 9c7eadd5d076df7b4e272354cf9f013e *-
