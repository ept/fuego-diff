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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

import fc.util.Util;
import fc.util.log.Log;

/** A mutable reftree that buffers changes as a reftree. The class implements
 * a mutable reftree that upon construction is identical to some underlying
 * id-addressable reftree. The tree may then be modified using the
 * {@link MutableRefTree} interface. The underlying tree is known as the
 * <i>back</i> tree.
 * <p>A reftree expressing the tree using references to the back tree
 * is available through the {@link #getChangeTree getChangeTree} method at
 * each point during the change cycle.
 * <p>For instance, consider a change tree with the back tree
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
 */

// TODO: deletia tests recurse upwards towards root by key. This is very easy
// with Dewey keys-- maybe we should have some sort of descendant() test
// in the reftree with a dewey-key model
// NOTE: Depends on the following extended semantics for MutableRefTrees
// - root insert and delete possible
// - reference insert and delete possible
//
// NOTE: Returned nodes may not reflect the current tree state; i.e. if you
// do n=getNode(id1); remove a child of id1; it may be that child-iterating 
// n gives the old childlist (Potentially, we could fix this, but the
// expense (maybe) and correctness testing seem to be quite large tasks) 
public class ChangeBuffer extends DerefRefTree implements MutableRefTree,
  BatchMutable {

  protected Set<Key> delRoots = new HashSet<Key>(); // These contain back keys!
  protected Set<Key> delNodes = new HashSet<Key>(); // These contain back keys!

  // NOTE: It is vital we start with no-changes, otherwise delRoots won't tell
    // all deleted roots, and nodes from backingTree may show up via getNode()!
  protected MutableRefTree changeTree;
  protected Key changeTreeRoot = MutableRefTree.AUTO_KEY;
  protected Stack<Key> expandStack = new Stack<Key>();

  // Some random switches which we won't make public now
  protected boolean updateLoadsContent = false;

  // If true, node content is loaded to a node whenever the node is updated
  // or the content accessed; otherwise these are kept in the backingTree
  // as long as possible 
  
  public ChangeBuffer( IdAddressableRefTree backingTree) {
    this(backingTree,KeyMap.IDENTITY_MAP);
  }

  // FIXME: Its a bit stupid that the default backTree sorts by string
  // typecast, as this is quite slow. But it allows drop-in replacement for 
  // ChangeTree...
  public ChangeBuffer( IdAddressableRefTree backingTree, KeyMap kmap ) {
    this( new MutableRefTreeImpl(
        kmap.getFrontKey(backingTree.getRoot().getId()),
        MutableRefTreeImpl.ID_AS_STRINGS_BY_LENGTH_ALPHA),backingTree, kmap);
  }

  // NOTE: Will use changeTree as keymap, if it is a keymap
  public ChangeBuffer(MutableRefTree changeTree,
      IdAddressableRefTree backingTree ) {
    this(changeTree,backingTree, changeTree instanceof KeyMap ?
        (KeyMap) changeTree : KeyMap.IDENTITY_MAP);
  }
  
  public ChangeBuffer(MutableRefTree changeTree,
      IdAddressableRefTree backingTree, KeyMap map ) {
    this(changeTree,backingTree,map,MutableRefTree.AUTO_KEY);
  }

  /** Create change buffer.
   * 
   * @param changeTree tree to buffer changes in
   * @param backingTree backing tree
   * @param map keymap between change and backing trees
   * @param changeTreeRoot key to use for root in changeTree. If set to
   * {@link MutableRefTree#AUTO_KEY} (the default), the mapped root key of the
   * backing tree will be used.
   */
  public ChangeBuffer(MutableRefTree changeTree,
      IdAddressableRefTree backingTree, KeyMap map, Key changeTreeRoot ) {
    super(changeTree, backingTree, map );
    this.changeTree = changeTree;
    this.changeTreeRoot = changeTreeRoot;
    reset();
  }
  
  // ===== Specific methods

  /** Get the tree as a reftree. The reftree references nodes in the
   * underlying tree.
   *
   * @return the change tree as a reftree
   */
  
  public IdAddressableRefTree getChangeTree() {
    return changeTree;
  }
  
  /** Reset tree. Resets all changes made to the tree, causing the
   * tree to be identical to the back tree.
   */
  
  public void reset() {
    assert modCount++>0 || true;
    try {
      Key bid = backingTree.getRoot().getId();
      Key fid = km.getFrontKey( backingTree.getRoot().getId() );
      if( fid != null && changeTree.contains(fid) )
        changeTree.delete(fid);
      fid = km.getFrontKey( backingTree.getRoot().getId() );
      changeTree.insert(null, changeTreeRoot != MutableRefTree.AUTO_KEY ?
          changeTreeRoot : fid , TreeReference.create(bid) );
    } catch (NodeNotFoundException e) {
      throw new Error("Can't insert root treeref",e);
    }
    delRoots.clear();
    delNodes.clear();
    assert expandStack.isEmpty();
  }
  
  // ===== Mutability 
  
  /** @inheritDoc */
  public void delete(Key id) throws NodeNotFoundException {
    expand(id,false);
    addToDeletia(changeTree.getNode(id));
    changeTree.delete(id);
    assert modCount++>0 || true;
  }
    
  /** @inheritDoc */
  public final Key insert(Key parentId, Key newId, Object content)
      throws NodeNotFoundException {
    return insert(parentId, DEFAULT_POSITION , newId, content);
  }

  /** @inheritDoc */
  public Key insert(Key parentId, long pos, Key newId, Object c)
      throws NodeNotFoundException {
    if( contains(newId) )
      throw new IllegalArgumentException("Node already in tree "+newId);
    expand(parentId,true);
    //Log.debug("changeTree after child-expand of",parentId);
    //TreeUtil.dumpTree(changeTree, System.out);

    Key iKey = changeTree.insert(parentId, pos, newId, c);
    if( c instanceof Reference)
      removeFromDeletia((Reference) c);
    //Log.debug("changeTree tree is");
    //TreeUtil.dumpTree(changeTree, System.out);
    assert modCount++>0 || true;
    return iKey;
  }
  
  /** @inheritDoc */
  public Key move(Key nodeId, Key parentId) throws NodeNotFoundException {
    return move(nodeId,parentId,DEFAULT_POSITION);
  }

  /** @inheritDoc */
  public Key move(Key nodeId, Key parentId, long pos)
      throws NodeNotFoundException {
    expand(nodeId,false);
    expand(parentId,true);
    Key k = changeTree.move(nodeId, parentId, pos);
    assert modCount++>0 || true;
    return k;
  }

  /** @inheritDoc */
  public boolean update(Key nodeId, Object c)
      throws NodeNotFoundException {
    expand(nodeId,true); // We need child expand since we update the content
    assert !changeTree.getNode(nodeId).isTreeRef();
    RefTreeNode cn = changeTree.getNode(nodeId);
    assert cn != null : "Node should be in tree by expand:"+nodeId;
    boolean same = false;
    if( changeTree.getNode(nodeId).isNodeRef() ) {
      Object cb=getExistingBackNode(cn).getContent();
      same = Util.equals(c,cb); 
      if( updateLoadsContent || !same )
        changeTree.update(nodeId, same ? cb : c);
    } else 
      same = changeTree.update(nodeId, c);      
    if( c instanceof Reference)
      removeFromDeletia((Reference) c);
    assert same || modCount++>0 || true;
    return same;
  }

  // ======= Batch mutability 
  
  // TODO: the expandAll algorithm is a straightforward way to do it, 
  // but it may cause excessive expansion in changeTree (e.g. apply is a
  // single node deep in the backingTree->we expand the whole path to it
  // to no use for the end result). Another way
  // would be to make a new changeTree, and then copy all subtrees ref'd
  // from the old changeTree into the new one.

  /** @inheritDoc */
  public void apply(RefTree t) throws NodeNotFoundException {
    // BUGFIX-20061009-2: Test changetree, not backingTree
    if( !(changeTree instanceof BatchMutable) ) { 
      RefTrees.apply(t, this);
      return;
    }
    
    // Calculate new deletia sets
    // NOTE: We must do this BEFORE we change changeTree, in case this
    // changeTree is somehow related to t (e.g. the backing tree of a Diff)
    // (Fortunately, we *can* do this before :) )
    
    RefTree backRef = RefTrees.getRefTree(backingTree);
    Set[] expandedContentKeys = new Set[2];
    Set[] usedRefs = 
      RefTrees.normalize(backingTree, new RefTree[] {t,backRef}, 
        expandedContentKeys, KeyMap.UNMAPPABLE );
    Set usedNewTreeRefs = usedRefs[0];
    Set allowedTreeRefs =usedRefs[1];
    Set deletedNodeRefs = expandedContentKeys[0]; 
         // they got expanded since they are not ref'd -> deleted
    Set newDelNodes = new HashSet();
    {
      newDelNodes.clear();
      // Put all nodes in backingTree as deleted Nodes, stopping at 
      // the allowed references in backingTree (since these are 
      // either delete trees, or present in the new tree
      Set stopSet = allowedTreeRefs;
      LinkedList<RefTreeNode> queue = new LinkedList<RefTreeNode>();
      for(queue.add(backingTree.getRoot());!queue.isEmpty();) {
        RefTreeNode n = queue.removeLast();
        if( !stopSet.contains( n.getId() ) ) {
          newDelNodes.add(n.getId());
          for( Iterator i = n.getChildIterator(); i.hasNext();)
            queue.add((RefTreeNode) i.next());
        }
      }
      //Log.debug("Intermediate new node deletia ",delNodes);
      // Now, remove all nodeRefs present in the new tree, and we're left
      // with those deleted!
      assert queue.isEmpty();
      for(queue.add( t.getRoot() );!queue.isEmpty();) {
        RefTreeNode n = queue.removeLast();
        if( n.isNodeRef() )
          newDelNodes.remove(n.getReference().getTarget());
        for( Iterator i = n.getChildIterator(); i.hasNext();)
          queue.add((RefTreeNode) i.next());        
      }
    }

    // expand all nodes in t to the changeTree, so that we can then 
    expandAll(t.getRoot(),KeyMap.UNMAPPABLE);
    // run .apply on the changeTree
    ((BatchMutable) changeTree).apply(t);

    delNodes = newDelNodes;
    delRoots = allowedTreeRefs;
    delRoots.removeAll(usedNewTreeRefs);
    //Log.debug("New node deletia ",delNodes.size());
    //Log.debug("New tree deletia ",delRoots.size());
  }

  // ======= Helper code
  
  protected void expandAll(RefTreeNode n, KeyMap km) throws NodeNotFoundException {
    // Do visit, then expand, as this is a more efficient use of expand 
    // km is used to match non-reference nodes to those in the backing tree
    for(Iterator<RefTreeNode> ci = n.getChildIterator();ci.hasNext();)
      expandAll(ci.next(),km);
    Key toExpand =
      n.isReference() ? (n.getReference().getTarget()) : km.getBackKey(n.getId());
    if( toExpand != null ) {
      //Log.debug("Expanding "+toExpand);
      expand(toExpand,false);
    }
  }
  
  protected void addToDeletia(RefTreeNode n) {
    if( n.isReference() ) {
      Key target = ((RefTreeNode) n).getReference().getTarget();
      if( n.isNodeRef() )
        delNodes.add(target);
      else
        delRoots.add(target);
    } else {
      // Updated/inserted node in subtree
      // TODO: Having an insert flag for nodes could eliminate checking with
      // backTree for update/ins status of node 
      Key bk = km.getBackKey(n.getId());
      if( bk != null && backingTree.contains(bk)) 
        delNodes.add(bk);
    }
    for( Iterator i = n.getChildIterator();i.hasNext();)
      addToDeletia((RefTreeNode) i.next());
  }

  protected void removeFromDeletia(Reference ref)
      throws NodeNotFoundException {
    Key target = ref.getTarget();
    if( !backingTree.contains(target) )
      throw new NodeNotFoundException(target);
    if( !ref.isTreeReference() )  {
      if( delNodes.remove(target) ) {
        // Easy case: undelete node-ref and it was in deleted nodes
        assert !delRoots.contains(target);
      } else if( delRoots.contains(target) ){
        // More complex case: undelete node-ref and it is an undelete-tree -->
        // add all the children of the ref to delRoots
        // Note: by definition of delRoots, these cannot be ref'd from 
        // the changeTree
        assert !delNodes.contains(target);
        for( Iterator i = backingTree.childIterator(target);i.hasNext();) {
          Key bck = (Key) i.next();
          assert !changeTree.contains(km.getFrontKey(bck));
          delRoots.add(bck);
        }
      } else
        ; // Node was never deleted, so no resurrect needed
    } else {
      assert ref.isTreeReference();
      if( delRoots.remove(target) ) {
        // Easy case: undelete treeref-ref and it was in deleted refs
        assert !delNodes.contains(target);
      } else if (delNodes.remove(target)) {
        // Undeleted tree-ref, and the root was marked deleted -> remove any
        // delete-marked descendants of the ref from the sets. We need to check
        // if any deleted node is a descendant of this, in which case it is
        // no longer in the delete set
        // NOTE: here, we can remove nodes from nodeSet w/o adding its children
        // to delRoots, as those nodes will be included by the the treeRef n
        assert !delRoots.contains(target);
        Set<Key> resurrects = new HashSet<Key>();
        for( Key k : delRoots ) {
          for(;k!=null;k=backingTree.getParent(k)) {
            if( k.equals(target) ) {
              resurrects.add(k);
              break; // Found match, so no more looping
            }
          }
        }
        for( Key k : delNodes ) {
          for(;k!=null;k=backingTree.getParent(k)) {
            if( k.equals(target) ) {
              resurrects.add(k);
              break; // Found match, so no more looping
            }
          }
        }
        delRoots.removeAll(resurrects);
        delNodes.removeAll(resurrects);
      } else 
        ; // Node was never deleted, so no resurrect needed      
    }
  }
  
  @Override
  protected boolean isDeleted(Key bk) {
    assert backkey(bk);
    if( delNodes.contains(bk) )
      return true;
    try {
      // Check that nothing on the path to the root is deleted
      for(;bk == null || backingTree.contains(bk);
        bk=backingTree.getParent(bk)) {
        if( bk == null )
          return false;
        if( delRoots.contains(bk) )
          return true;
      }
      return true; // contains returned false      
    } catch (NodeNotFoundException e) {
      trap(e);
    }
    assert false: "Unreachable";
    return false;
  }
  
  // Makes sure id is in changeTree (as treeref, i.e. parent get 
  // expanded childlists)
  protected Key expand(Key eid, boolean expandChildren)
      throws NodeNotFoundException {
    //Log.debug("Changetree before expand of "+eid);
    //XmlrDebug.dumpTree(getChangeTree());
    if( eid == null )
      throw new NodeNotFoundException(eid);
    if( changeTree.contains(eid) &&
        !changeTree.getNode(eid).isTreeRef() )
      return eid; // Trivial case when eId is already expanded
    Key id = eid;
    assert frontkey(id);
    for(Key backId = km.getBackKey( id );id!=null; ) {
      // BUGFIX-20061009-0: KeyMaps may return keys not in backing tree, so
      // we need to do a contains check
      //Log.debug("Back key of "+id+" is "+backId+", eid="+eid);
      if( id==eid && !backingTree.contains(backId) )  
        throw new NodeNotFoundException(id);
      assert id!=eid || backId != null  // Assert for the above if
        : "Trying to expand key "+id+
        ", which is not in tree, and has no backKey";

      if( changeTree.contains(id) ) {
        assert id == null || changeTree.getNode(id).isTreeRef();
        Key pId = changeTree.getParent(id); 
        assert pId == null || changeTree.getNode(pId) != null :
          "ChangeTree returned non-existing parent "+pId+" of "+id;
        assert pId == null || !changeTree.getNode(pId).isTreeRef()
          : "treeref as parent of treeref in expandTree";
        //Node expandRoot = null;
        //Log.debug("Expansion phase, expand stack is",expandStack);
        /* BUGFIX-20070215-1: Do not expand children when node already
         * expanded and expandChildren = false.
         * If eid was already in changeTree, we'd get here immediately.
         * Then the for loop would expand the children of eid=id, regardless
         * of expandChildren status; "&& expandChildren" fixes this. In
         * other cases, it's still safe since expandChildren is set to
         * true on the first step upwards.
         */
        for(;id!=null && expandChildren;
            id=expandStack.isEmpty() ? null : expandStack.pop()) {
            //Log.debug("Expanding subtree "+id+" whose node now is "+
            //    changeTree.getNode(id));
            assert changeTree.getNode(id).isTreeRef(); // Only expand treerefs
            changeTree.update(id, NodeReference.create( km.getBackKey( id ) ) );
            /*changeTree.delete(id);
            changeTree.insert( pId, id, 
                new RefTreeNodeImpl.NodeReference( km.getBackKey( id ) ) );*/
            assert changeTree.getNode(id).isNodeRef() :
              "Non-std semantic for node-ref was busted";
            insertRefSubtree(changeTree, backingTree, id);
            //expandRoot = expandRoot == null ? n : expandRoot;
            pId = id;
        }
        //Log.debug("Changetree after expand of "+eid);
        //XmlrDebug.dumpTree(getChangeTree());
        assert expandStack.isEmpty();
        assert frontkey(pId);
        assert changeTree.contains(eid);
        return pId;
      } else if( expandChildren )
        // Put on stack those in [] (pushes grow to the right)
        // [p0-p1-p2-...-p(n-1)]-pn-m
        expandStack.push(id);
      else
        expandChildren = true; // This leaves out id given at call from stack if
                               // expandChildren was false
      backId = backingTree.getParent(backId);
      assert backId != null : "Found null backId->this front-key is now deleted?!: "+eid+
       "(now at front-key "+id+"). This may happen if the contains() method of your backing "+
       "tree is inconsistent with what is found on traversal.";
      id = km.getFrontKey(backId);
    }
    assert false : "The key "+id+" addressed a deleted part of the tree";
    expandStack.clear();
    //throw new NodeNotFoundException(eid);
    return null;
  }
  
  private void insertRefSubtree(MutableRefTree ft, IdAddressableRefTree bt,
      Key froot)
      throws NodeNotFoundException {
    assert backkey(froot);
    //Log.debug("Child lap starts...");
    for( Iterator i = bt.childIterator( km.getBackKey( froot ));i.hasNext(); ) {
      Key bckey = (Key) i.next();
      assert backkey(bckey);
      //Log.debug("Inserting node "+km.getFrontKey(bckey)+" (bkey="+bckey+") to ");
      //XmlrDebug.dumpTree(ft);
      ft.insert(froot, km.getFrontKey(bckey), TreeReference.create(bckey));
      assert ft.getNode(km.getFrontKey(bckey)).isTreeRef() :
        "TreeRef insert broken";
    }
  }
    
  protected boolean frontkey(Key id) {
    return true;
  }

  protected boolean backkey(Key id) {
    return true;
  }
  
}

// arch-tag: 3e5a069c-13a0-4f64-bd7e-8c4aeaf72c12
