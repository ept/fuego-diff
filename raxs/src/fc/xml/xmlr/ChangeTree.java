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

// $Id: ChangeTree.java,v 1.13 2004/12/27 17:37:52 ctl Exp $

package fc.xml.xmlr;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import fc.util.CompareUtil;
import fc.util.Util;
import fc.util.log.Log;


/** A mutable reftree that buffers changes as a reftree. The class implements
 * a mutable reftree that upon construction is identical to some underlying
 * id-addressable reftree. The tree may then be modified using the
 * {@link MutableRefTree} interface.
 * <p><b>NOTE:</b> Newer implementations should use 
 * {@link fc.xml.xmlr.ChangeBuffer}, which provides the same functionality,
 * but has a more solid and modular implementation. This class will soon be
 * deprecated.
 * <p>A reftree expressing the tree using references to the underlying tree
 * is available through the {@link #getChangeTree getChangeTree} method at
 * each point during the change cycle.
 * <p>For instance, consider a change tree with the underlying tree
 * <pre>
 * &lt;directory id="1" name="syxaw.hiit.fi"&gt;
 * &nbsp; &lt;file name="kernel.bin" version="314" object="0agh5678zxlkj6h7" id="2"  /&gt;
 * &nbsp; &lt;directory  name="subdir"  id="3"&gt;
 * &nbsp;   &lt;file name="file.txt" version="1" object="0agh5678zxwwj6h8" id="4" /&gt;
 * &nbsp; &lt;/directory&gt;
 * &lt;/directory&gt;
 * </pre>
 * We then insert a new node into the changeTree:
 * <code>insert("1","5","&lt;file name='newfile' &gt;")</code>. Calling
 * <code>getChangeTree</code> after the insert would yield the reftree
 * <pre>
 * &lt;ref:node id="1"&gt;
 * &nbsp; &lt;ref:tree id="2"  /&gt;
 * &nbsp; &lt;ref:tree id="3" /&gt;
 * &nbsp; &lt;file name="newfile" id="5" /&gt;
 * &lt;/ref:node&gt;
 * </pre>
 * <p>This class is in some senses the dual of 
 * {@link RefTrees#apply(RefTree, MutableRefTree, Set, RefTrees.IdMap)}: the
 * former applies a reftree as a series of operations on a mutable reftree, the
 * latter expresses the result of a series of operations on a mutable reftree
 * as a reftree.
 * <p>Note that the implementation does not guarantee that a minimal
 * (in terms of nodes) reftree is returned by <code>getChangeTree</code>;
 * changes that cancel out may yield a reftree with more than a single
 * tree reference to the root of the underlying tree.
 * <p><b>Note:</b> Be careful to always update node content via the
 * {@link #update update} method. Accessing the <code>RefTreeNode</code>s
 *  directly may not be intercepted by the class, and may change the underlying
 * tree.<p>
 * <b>Note, BUG:</b>getNode() may return nodes from inside a deleted tree.
 */

// NOTE: deletion is a bit tricky; we don't want to traverse the target tree
// unnecessarily = mark only roots as deleted
// BUT: this my cause inconsistencies in getNode(), unless we check all
// nodes on the path to the root... but that's a performance penalty
// Disable recursive check in isDeleted() to gain more SPEED
//
public class ChangeTree extends AbstractMutableRefTree {

  protected MutableMappingIndex ki; // = new SimpleIndex();
  private Set delRoots = new HashSet();
  private Node refRoot = null;
  private IdAddressableRefTree refTree = null;

  private long changeCount = 0;

  private IdAddressableRefTree backingTree;
  private MutableRefTree changeTarget;

  /** Create a new change tree. The tree is initially identical to the
   * underlying tree.
   * @param backingTree underlying tree
   */
  public ChangeTree(IdAddressableRefTree backingTree ) {
    this(backingTree,null);
  }

  /** Create a new change tree. The tree is initially identical to the
   * underlying tree. The changes to this tree are mirrored to
   * <code>changeTarget</code>.
   *
   * @param backingTree underlying tree
   * @param changeTarget tree to which any changes are mirrored. May be
   * same as <code>backingTree</code>.
   */
  public ChangeTree(IdAddressableRefTree backingTree, MutableRefTree changeTarget ) {
    this.backingTree=backingTree;
    this.changeTarget=changeTarget;
    reset();
  }

  /** Get the tree as a reftree. The reftree references nodes in the
   * underlying tree.
   *
   * @return the change tree as a reftree
   */
  public IdAddressableRefTree getChangeTree() {
    return refTree;
  }

  /** Reset tree. Resets all changes made to the tree, causing the
   * tree to be identical to its underlying tree.
   */

  public void reset() {
    initKeyIndex();
    Key rootId = ki.getFrontKey(backingTree.getRoot().getId());
    delRoots.clear();
    refRoot = new Node(rootId,null,null,true,false);
    refTree = new IdAddressableRefTreeImpl() {
      public RefTreeNode getRoot() {
        return new ProxyNode( refRoot, false );
      }

      public RefTreeNode getNode(Key id) {
        return ChangeTree.this.getNode(id,false);
      }
    };
    ki.move(rootId,refRoot);
    changeCount = 0;
  }

  protected void initKeyIndex() {
    ki = new SimpleIndex();
  }
  
  public RefTreeNode getRoot() {
    return new ProxyNode(refRoot,true); //  target.getRoot();
  }

  // Id navigation

  // FIXME,BUGREPORT: May return a node from inside a deleted subtree
  // Assume base tree = r
  //                   a b
  //                      c
  // Now, move c as child of a and delete a . The bug is that
  // findNode(c)!=null.
  // Conjecture: This happens because isDeleted recurses up to
  // the root using the *original* location, not the deleted location.
  // Test case: faxma.test.SyntheticDirTree rev 1.3 27.2.2006
  // On fix: remove bug description from class javadoc!

  public RefTreeNode getNode(Key id) {
    return getNode(id,true);
  }

  protected RefTreeNode getNode(Key id, boolean transparent ) {
    RefTreeNode n = findNode(id);
    if( n == null ) n= backingTree.getNode( ki.getBackKey(id));
    if( n!=null )
      n = new ProxyNode(n,transparent);
    return n != null && !isDeleted(n) ? n : null;
  }

//  int __deletdepth = 0;
  protected boolean isDeleted( RefTreeNode n ) {
    if( n == null )
      return false;
    assert frontkey(n.getId());
    if( delRoots.contains(n.getId()) )
     return true;
//    __deletdepth++;
//    if( __deletdepth > 100 )
//      Log.log("Infinite delete recursion :(",Log.ASSERTFAILED);
    if( isDeleted(n.getParent()) ) {
      //delRoots.add(n.getId()); // Cache whole path to node as deleted -> faster look-up
      // Actually, pretty useless: most nodes are not deleted -> must scan to
      // root :( anyway
//      __deletdepth--;
      return true;
    }
//    __deletdepth--;
    return false;
  }

  // Mutability

  public void delete(Key id) throws NodeNotFoundException {
    assert frontkey(id);
    RefTreeNode m = getNode(id);
    if( m == null ) // Already deleted
      throw new NodeNotFoundException(id);
    RefTreeNode p = m.getParent();
    if( p==null )
      throw new IllegalArgumentException("Cannot delete root");
    taint( p.getId(), null,true);
    Node n = findNode(id);
    // Remove from reftree
    if( ((Node) n.getParent()).children.remove(id) == null )
      Log.log("Parent/child inconsistency",Log.ASSERTFAILED);
    ki.remove(id);
    delRoots.add(id);
    changeCount ++;
    // Propagate change
    if( changeTarget != null )
      changeTarget.delete(id);
  }

  public Key insert(Key parentId, long pos, Key newId, Object content) throws
      NodeNotFoundException {
    assert frontkey(parentId);
    assert frontkey(newId);
    taint( parentId,null , true);
    Node n = findNode(parentId);
    Node newNode = new Node(ki.forge( newId ),n,content,false,false);
    newNode.children = childListFactory();
    n.children.put(newId,newNode);
    ki.move(newId,newNode);
    changeCount ++;
//    if( delRoots.contains(newId) ) 
//      Log.info("Resurrect of "+newId+". Old content="+
//          backingTree.getNode(newId).getContent()+", new="+
//          content);
    assert frontkey(newNode.getId());
    delRoots.remove(newNode.getId()); // BUGFIX 010906: Remove the node from the deletia 
                            // There should be no risk of getting the original,
                            // as newNode is in the changed tree and its index,
                            // which take precedence over the backing tree
    // Propagate change
    if( changeTarget != null )
      changeTarget.insert(parentId,pos,newId,content);
    return newId;
  }

  public Key move(Key nodeId, Key parentId, long pos)
      throws NodeNotFoundException {
    assert frontkey(parentId);
    assert frontkey(nodeId);
    verifyMove(nodeId,parentId);
    taint(parentId,null,true);
    Node n = findNode(nodeId);
    Node p = findNode(parentId);
    // Move from source code...
    if( n== null ) {
      n = new Node(nodeId,p,null,true,false);
      Key origParentId =  ki.getFrontKey( backingTree.getNode( 
          ki.getBackKey( nodeId ) ).getParent().getId() );
      taint( origParentId,null,true);
      Node origParent = findNode( origParentId );
      origParent.children.remove(nodeId);
    } else {
      ( (Node) n.getParent()).children.remove(nodeId);
      ((Node) n).parent = p;
    }
    // ...to dest code
    p.children.put(nodeId,n);
    ki.move(nodeId,n);
    changeCount ++;
    // Propagate change
    if( changeTarget != null ) 
      changeTarget.move(nodeId,parentId,pos);
    return nodeId;
  }

  public boolean update(Key nodeId, Object content) throws NodeNotFoundException{
    assert frontkey(nodeId);
    Object oldContent = getNode(nodeId).getContent();
    if( content.equals(oldContent) ) {
      //taint(nodeId,null,true); // This is actually somewhat questionable!!!!
                               // It means that if you touch a node with upd, you expand it
                               // OTOH, this makes RefTrees.apply() work due to its access pattern
      return false;
    }
    //Log.log("---------Old content is ",Log.INFO, fc.syxaw.proto.Util.toString( oldContent ));
    //Log.log("---------New content is ",Log.INFO,  fc.syxaw.proto.Util.toString( content ));
    taint( nodeId,content,true);
    changeCount ++;
    // Propagate change
    if( changeTarget != null )
      changeTarget.update(nodeId,content);
    return true;
  }

  // Check for the case that id is an ancestor of newParent
  protected void verifyMove( Key id, Key newParent ) {
    if( Util.equals(id,newParent) ) {
      //Log.log("Moving node to be child of its own subtree", Log.ASSERTFAILED);
      throw new IllegalArgumentException
          ("Moving node to be child of its own subtree");
    }
    if( newParent != null ) {
      try {
        verifyMove(id, getParent(newParent));
      } catch (NodeNotFoundException ex) {
        Log.log("Broken tree",Log.ASSERTFAILED);
      }
    }
  }

  protected Node findNode(Key id) {
    assert frontkey(id);
    return (Node) ki.get(id);
  }

  protected RefTreeNode taint(Key id, Object content, boolean expandChildren)
      throws NodeNotFoundException {
    assert frontkey(id);
    Node n = findNode(id);
    if( n== null ) {
      // No such node, taint parent
      taint( ki.getFrontKey(
          backingTree.getParent( ki.getBackKey(id) ) ),null,true);
      n = findNode(id);
      if( n == null )
        Log.log("The node "+id+" should exist now.",Log.ASSERTFAILED);
    }
    if( expandChildren && n.isTreeRef ) {
      n.expandChildren(backingTree, ki.getBackKey( id ));
    }
    if( content != null ) {
      n.expandContent(content);
    }
    /*java.io.PrintWriter pw = new java.io.PrintWriter(System.out);
    dump(pw,refRoot);
    pw.flush();*/
    return n;
  }


  private /*static*/ class Node implements RefTreeNode {

    private Key id;
    private RefTreeNode parent;
    private Object content;
    private Map children= null; //null means same chlist as in target
    private boolean isTreeRef;
    private boolean isNodeRef;

    public Node(Key id, RefTreeNode parent, Object content,
                boolean isTreeRef, boolean isNodeRef ) {
      assert frontkey( id );
      if( content == null && !(isTreeRef || isNodeRef))
        Log.log("null content when not ref not supported",Log.ASSERTFAILED);
      this.id = id;
      this.parent = parent;
      this.content = content;
      this.isTreeRef = isTreeRef;
      this.isNodeRef = isNodeRef;
    }

    public Key getId() {
      return id;
    }

    public RefTreeNode getParent() {
      return parent;
    }

    public Object getContent() {
      return content;
    }

    public int getChildCount() {
      if( children == null && isNodeRef )
        Log.log("A nodref should always have an expanded chlist",Log.ASSERTFAILED);
      return children == null ? 0 : children.size();
    }

    public boolean isReference() {
      return isTreeRef || isNodeRef;
    }

    public boolean isTreeRef() {
      return isTreeRef;
    }

    public boolean isNodeRef() {
      return isNodeRef;
    }

    public Iterator getChildIterator() {
      if( children == null && isNodeRef )
        Log.log("A nodref should always have an expanded chlist",Log.ASSERTFAILED);
      return children != null ?
          children.values().iterator() : Collections.EMPTY_LIST.iterator();
    }


    boolean contentExpanded() {
      return /*(expandflags&CONTENT_EXPANDED)!=0 &&*/
          content != null;
    }

    boolean childrenExpanded() {
      return /*(expandflags&CHILDREN_EXPANDED)!=0 &&*/ children != null;
    }

    private void expandContent( Object content ) {
      this.content = content;
      this.isNodeRef = false;
    }

    private void expandChildren( IdAddressableRefTree target,  Key id ) 
      throws NodeNotFoundException {
      assert backkey(id);
      children = childListFactory();

      for( Iterator<Key> i=target.childIterator(id);i.hasNext(); ) {
        Key cid = i.next();
        Key fcid = ki.getFrontKey(cid);
        RefTreeNode child  = 
          new Node( fcid, this, null, true, false );
        children.put(fcid,child);
        ki.move(fcid,child);
      }
      isNodeRef = isTreeRef;
      isTreeRef = false;
    }

    public Reference getReference() {
      // FIXME: This whole class is deprecated.
      throw new UnsupportedOperationException();
    }
  }

  public boolean hasChanges() {
    return changeCount > 0;
  }

  private static Map childListFactory() {
    // FIXME-W Unordered inserts really need "ordering help" -> 
    // MutableRefTrees should take a id comparator for construction!
    return new TreeMap(CompareUtil.STRINGS_BY_LENGTH_ALPHA);
  }


  // Node class for opaque traversal of this tree = get the reftree
  // It's a little kludgy that we have to encapsulate the Node objects
  // this way, but externally it's much nicer than having two
  // modes for the tree

  private class ProxyNode implements RefTreeNode {

    private RefTreeNode n;
    private boolean transparent;

    public ProxyNode(RefTreeNode n, boolean transparent) {
      assert !(n instanceof Node) || frontkey(n.getId());
      if( n == null )
        Log.log("Can't proxy null",Log.ASSERTFAILED);
      this.n = n;
      this.transparent = transparent;
    }

    public Key getId() {
      return n instanceof Node ?  n.getId() : ki.getFrontKey(n.getId());
    }

    public RefTreeNode getParent() {
      assert !(n instanceof Node) || frontkey(n.getId());
      // First try changed tree, then backing tree; ensures that
      // we "jump back" into the change tree if going up from
      // a node in the backing tree
      RefTreeNode p = findNode( n instanceof Node ? n.getId() : 
          ki.getFrontKey( n.getId() ) );
      if( p != null )
        p= p.getParent();
      else
        p = n.getParent();
      return p != null ?
          new ProxyNode( p, transparent ) : null;
    }

    public Object getContent() {
      assert !(n instanceof Node) || frontkey(n.getId());
      Object content = n.getContent();
/*OLD      if( n instanceof Node && content == null && transparent )
        return backingTree.getNode(n.getId()).getContent();*/
      if( n instanceof Node && !((Node) n).contentExpanded()
          && transparent ) {
        RefTreeNode n2 = backingTree.getNode( ki.getBackKey( n.getId()) );
        return n2!= null ? n2.getContent() : null;
      }
      return content;
    }


    public Iterator getChildIterator() {
      assert !(n instanceof Node) || frontkey(n.getId());
      //Log.debug("Looking for key ",n.getId());
      final Iterator niter =
          n instanceof Node && transparent && !((Node) n).childrenExpanded() ?
          backingTree.getNode( ki.getBackKey( n.getId()) ).getChildIterator() : 
                n.getChildIterator();
      return new Iterator() {
        public void remove() {
          niter.remove();
        }

        public boolean hasNext() {
          return niter.hasNext();
        }

        public Object next() {
          ProxyNode p = new ProxyNode( (RefTreeNode) niter.next(),transparent );
          //if( isDeleted(p) ) // This check is quite expensive
          //  Log.log("Returning deleted node",Log.ASSERTFAILED);
          return p;
        }
      };

    }

    public boolean isReference() {
      return isTreeRef() || isNodeRef();
    }

    public boolean isTreeRef() {
      if(/* n instanceof Node && !((Node) n).childrenExpanded() &&*/ transparent ) {
        RefTreeNode n2 = backingTree.getNode( n instanceof Node ?
            ki.getBackKey( n.getId()) : n.getId() );
        return n2 !=null ? n2.isTreeRef() : n.isTreeRef();
      }
      if( (!n.isTreeRef())^(((Node) n).childrenExpanded()) )
        Log.log("treeref/child discrepancy",Log.ASSERTFAILED);
      return n.isTreeRef();
    }

    public boolean isNodeRef() {
      if( /*n instanceof Node && !((Node) n).childrenExpanded() &&*/ transparent ) {
        RefTreeNode n2 = backingTree.getNode(
            n instanceof Node ? ki.getBackKey( n.getId() ) : n.getId() );
        return n2 !=null ? n2.isNodeRef() : n.isNodeRef();
      }
      if( (n.isNodeRef())^(n.getContent()==null) && !n.isTreeRef() )
        Log.log("ref/content discrepancy; nr="+n.isNodeRef()+" cnt",
                Log.ASSERTFAILED,n.getContent());
      return n.isNodeRef();
    }

    public Reference getReference() {
      // FIXME-20061016-1: Only works with XMLR1 model!!!! 
      return !isReference() ? null :
        (isTreeRef() ? new TreeReference(n.getId()) 
            : new NodeReference(n.getId()));
    }
  }

  protected boolean frontkey(Key id) {
    return true;
  }

  protected boolean backkey(Key id) {
    return true;
  }
  
  // This class is DEAD CODE
  private abstract static class MutableMappingIndex { // FIXME: Obviously needs to be refined...
    // key mapping stuff
    public abstract Key getBackKey( Key frontKey );
    public abstract Key getFrontKey( Key backKey );
    // key alloc
    public abstract Key forge(Key insKey);
    // index ops
    public abstract RefTreeNode get(Key fk);
    public abstract void move(Key fk, RefTreeNode n); // if fk not found=then, add
    public abstract RefTreeNode remove(Key fk);
  }
  
  // This class is DEAD CODE
  private static class SimpleIndex extends MutableMappingIndex {

    private Map nodeById = new HashMap();

    public Key getBackKey(Key fk) {
      return fk;
    }

    public Key getFrontKey(Key bk) {
      return bk;
    }

    public Key forge(Key insKey) {
      return insKey;
    }

    public RefTreeNode get(Key fk) {
      return (RefTreeNode) nodeById.get(fk);
    }

    public void move(Key fk, RefTreeNode n) {
      nodeById.put(fk, n);
    }

    public RefTreeNode remove(Key fk) {
      return (RefTreeNode) nodeById.remove(fk);
    }
    
  }
}
// arch-tag: ed1e1f7cc8d521cfa3cf9411fdfa021c *-
