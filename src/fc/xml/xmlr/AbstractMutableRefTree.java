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

// $Id: AbstractMutableRefTree.java,v 1.4 2005/02/10 10:22:07 ctl Exp $
package fc.xml.xmlr;

/** Convenience base class for mutable reftree implementations. Provides
 * implementations of <code>move</code> and <code>insert</code> without
 * position arguments in terms of those with positions, in order to protect
 * against inconsistencies among these.
 */

public abstract class AbstractMutableRefTree extends IdAddressableRefTreeImpl
    implements MutableRefTree {

  public abstract void delete(Key id) throws NodeNotFoundException;


  /** Insert a new non-reference node. Equivalent to
   * <code>insert(parentId,{@link #DEFAULT_POSITION},newId,content)</code>
   *
   * @param parentId id of parent to the new node
   * @param newId id of the new node, must not already exist in the tree
   * @param content content object of new node
   * @throws NodeNotFoundException if the <code>parentId</code> node is
   * not in the tree.
   */
  // Final to 1) allow inlining 2) discourage foul-ups in derived classes
  public final Key insert(Key parentId, Key newId, Object content)
      throws NodeNotFoundException{
    return insert(parentId, -1L, newId, content);
  }

  public abstract Key insert(Key parentId, long pos, Key newId,
                              Object content) throws NodeNotFoundException;


  // Final to 1) allow inlining 2) discourage foul-ups in derived classes
  /** Move a node in the tree.  Equivalent to
   * <code>move(nodeId,parentId,{@link #DEFAULT_POSITION})</code>
   *
   * @param nodeId node to move
   * @param parentId new parent of the node
   * @throws NodeNotFoundException if <code>nodeId</code> or
   * <code>parentId</code> is missing from the tree
   */

  public final Key move(Key nodeId, Key parentId) throws
      NodeNotFoundException {
    return move(nodeId, parentId, -1L);
  }

  public abstract Key move(Key nodeId, Key parentId, long pos) throws
      NodeNotFoundException;

  public abstract boolean update(Key nodeId, Object content) throws
      NodeNotFoundException;

}
// arch-tag: 25b6d034ef650715fb8ca0a0d6c45047 *-
