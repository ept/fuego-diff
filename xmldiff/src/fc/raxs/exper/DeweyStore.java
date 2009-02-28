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

import java.io.IOException;

import fc.raxs.Measurements;
import fc.raxs.StoreConfiguration;
import fc.util.Debug.Time;
import fc.xml.xmlr.ChangeBuffer;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.xas.MutableDeweyRefTree;

public class DeweyStore extends MemoryStore {
  
  public DeweyStore(StoreConfiguration sc) throws IOException {
    t = MemoryStore.trees.get(sc.getStoreFile());
    if( t == null ) {
      t = new MutableDeweyRefTree();
      MemoryStore.trees.put(sc.getStoreFile(), t);
    }
    tm = sc.getModel();
  }

  @Override
  public ChangeBuffer getChangeBuffer() {
    MutableDeweyRefTree mt = new MutableDeweyRefTree();
    mt.setForceAutoKey(true);
    return new ChangeBuffer(mt, t, mt);
  }

  @Override
  public void apply(RefTree nt) throws NodeNotFoundException {
    if( Measurements.STORE_TIMINGS ) {
      Time.stamp( Measurements.H_STORE_APPLY );
      Time.stamp( Measurements.H_STORE_APPLY_WRITETREE );
    }
    ((MutableDeweyRefTree) t).apply(nt);
    if( Measurements.STORE_TIMINGS ) 
      Time.stamp( Measurements.H_STORE_APPLY_WRITETREE_END );
    init();
  }
}
// arch-tag: 1925562d-2836-4282-9859-b36dc3f3788a
//
