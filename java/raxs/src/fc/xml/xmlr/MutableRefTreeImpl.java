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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fc.util.CompareUtil;
import fc.util.Util;

/** Default implementation of mutable reftree. 
 * 
 */

public class MutableRefTreeImpl extends AbstractMutableRefTree {

  private Map nodeByKey = new HashMap();
  private Comparator childPosCmp = null; //ID_AS_STRINGS_BY_LENGTH_ALPHA;
  
  /** Comparator ordering strings by ascending length, then by
   * the default Java String order.
   */ 
  public static final Comparator ID_AS_STRINGS_BY_LENGTH_ALPHA =
    new Comparator() {
    public int compare(Object o1, Object o2) {
      Key id1  = ((RefTreeNode) o1).getId();
      Key id2  = ((RefTreeNode) o2).getId();
      return id1 == null ? (id1==id2 ? 0 : -1) :
        CompareUtil.STRINGS_BY_LENGTH_ALPHA.compare(id1.toString(), 
            id2.toString());
    }
  };

  /** Comparator that will always put a new node last in the child list. */
  public static final Comparator ALWAYS_LAST = new Comparator() {
    public int compare(Object o1, Object o2) {
      return 1;
    }
  };

  /** Comparator that will always put a new node first in the child list. */
  public static final Comparator ALWAYS_FIRST = new Comparator() {
    public int compare(Object o1, Object o2) {
      return -1;
    }
  };
  
  private Key rootId;

  /** Create tree with a given root key. Child ordering is 
   * {@link #ALWAYS_LAST}. 
   */
  public MutableRefTreeImpl(Key rootId) {
    this.rootId = rootId;
    this.childPosCmp = ALWAYS_LAST;
  }
  
  /** Create tree by root key and given child order.
   * 
   * @param rootId root key
   * @param c child ordering
   */
  public MutableRefTreeImpl(Key rootId, Comparator c) {
    this.rootId = rootId;
    this.childPosCmp = c;
  }

  // NOTE: Allows removal of root
  /** @inheritDoc */
  @Override
  public void delete(Key id) throws NodeNotFoundException {
    Node n = (Node) nodeByKey.get(id);
    if( n == null )
      throw new NodeNotFoundException(id);
    cleanIndex(n);
    Node p = (Node) n.getParent();
    if( p == null ) {
      nodeByKey.clear();
      return;
    }
    boolean removed = p.removeChild(n);
    assert removed;
  }
  
  /** @inheritDoc */
  @Override
  public Key insert(Key parentId, long pos, Key newId, Object content)
      throws NodeNotFoundException {
    Node p = (Node) nodeByKey.get(parentId);
    if( p == null && parentId != null )
      throw new NodeNotFoundException(parentId);
    if( nodeByKey.containsKey(newId) )
      throw new IllegalArgumentException("Node already in tree "+newId);
    Node n = new Node(p,newId,content);
    nodeByKey.put(newId, n);
    if( p != null ) {
      if(pos==DEFAULT_POSITION) {
        pos = getPosFor(p,n);
      }
      p.addChild((int) pos, n); // Possible precision loss...
    } else
      rootId = n.getId();
    return newId;
  }

  /** @inheritDoc */
  @Override
  public Key move(Key nodeId, Key parentId, long pos)
      throws NodeNotFoundException {
    // FIXME: Check for cyclic moves
    Node n = (Node) nodeByKey.get(nodeId);
    Node p = (Node) nodeByKey.get(parentId);
    if( p == null || n == null )
      throw new NodeNotFoundException(n!=null ? parentId : nodeId);
    ((Node) n.getParent()).removeChild(n);
    if( pos == DEFAULT_POSITION )
      pos = getPosFor(p,n);
    p.addChild((int) pos, n); // Possible precision loss...
    return n.getId();
  }

  /** @inheritDoc */
  @Override
  public boolean update(Key id, Object content) throws NodeNotFoundException {
    Node n = (Node) nodeByKey.get(id);
    if( n == null )
      throw new NodeNotFoundException(id);
    if( !Util.equals(content, n.getContent()) ) {
      n.setContent(content);
      return true;
    }
    return false;
  }

  /** @inheritDoc */
  @Override
  public RefTreeNode getNode(Key id) {
    return (RefTreeNode) nodeByKey.get(id);
  }

  /** @inheritDoc */
  public RefTreeNode getRoot() {
    return getNode(rootId);
  }
  
  protected void cleanIndex(RefTreeNode n) {
    nodeByKey.remove(n.getId());
    for( Iterator i = n.getChildIterator(); i.hasNext();)
      cleanIndex((RefTreeNode) i.next());
  }
  
  protected int getPosFor(Node parent, Node n)  {
    int pos = 0;
    if( childPosCmp == ALWAYS_LAST )
      return parent.childCount();
    for( Iterator i = parent.getChildIterator();i.hasNext();) {
      if( childPosCmp.compare(n, i.next()) > 0 )
        pos ++;
      else
        return pos;
    }
    return pos;
  }
  
  private static class Node extends RefTreeNodeImpl {

    public Node(RefTreeNode parent, Key id, Object content) {
      super(parent, id, content);
    }

    /*
    @Override
    protected List makeList() {
      return new LinkedList();
    }*/
    
  }
}

// arch-tag: 91380d49-9880-44b8-b983-80eb96bd2c30

