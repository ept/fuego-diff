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

package fc.raxs;

import java.io.IOException;

import fc.xml.xmlr.BatchMutable;
import fc.xml.xmlr.ChangeBuffer;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.model.TreeModel;

/** Base class for XAS Item stores. 
 */

public abstract class Store implements BatchMutable {
  
  /** Apply a new tree to the store. Store the given tree in the store. 
   * References in <i>t</i> are resolved against the current tree in the store.
   * The default implementation calls <code>RefTrees.apply(t, getTree())</code>,
   * and thus requires <code>getTree()</code> to return a 
   * {@link MutableRefTree}. 
   * @param t new tree
   */
  public void apply(RefTree t) throws NodeNotFoundException {
    if( !isWritable() || !(getTree() instanceof MutableRefTree) )
      throw new IllegalStateException("Read-only store");
    MutableRefTree ct = (MutableRefTree) getTree();
    RefTrees.apply(t, ct);
  }
  
  /** Close store.
   * 
   * @throws IOException
   */
  public abstract void close() throws IOException;
  
  /** Get change buffer for store. The default implementation returns
   *  <code>new ChangeBuffer(getTree())</code>.
   * @return store change buffer 
   */
  public ChangeBuffer getChangeBuffer() {
    return new ChangeBuffer(getTree());
  }
  
  /** Get store contents as a reftree.
   * 
   * @return store contents
   */
  public abstract IdAddressableRefTree getTree(); 
  
  /** Get store tree model.
   * 
   * @return store tree model
   */
  public abstract TreeModel getTreeModel();

  /** Check if store is writable. The default implementation returns 
   * <code>true</code>.
   * 
   */
  public boolean isWritable() {
    return true;
  }

  /** Open store.
   * 
   * @throws IOException
   */
  public abstract void open() throws IOException;
  
}

// arch-tag: 09bf723d-b8d5-473c-80d6-c46242186240
