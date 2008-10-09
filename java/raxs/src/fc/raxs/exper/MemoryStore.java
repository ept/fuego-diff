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

package fc.raxs.exper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fc.raxs.Measurements;
import fc.raxs.Store;
import fc.util.Debug.Time;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.model.TreeModel;

public class MemoryStore extends Store {

  protected static Map<File,MutableRefTree> trees 
    = new HashMap<File,MutableRefTree>();
  
  protected MutableRefTree t;
  protected TreeModel tm;
  
  public static void forgetTrees() {
    trees.clear();
  }
  
  @Override
  public void close() throws IOException {
  }

  @Override
  public IdAddressableRefTree getTree() {
    return t;
  }

  @Override
  public TreeModel getTreeModel() {
    return tm;
  }

  @Override
  public boolean isWritable() {
    return true;
  }
  
  @Override
  public void open() throws IOException {
    init();
  }

  
  
  protected void init() {
    if( Measurements.STORE_TIMINGS ) 
      Time.stamp( Measurements.H_STORE_INIT );
  }
  
}
// arch-tag: 1ba36480-9fb4-4f77-b03b-f56bb1a9907b
//
