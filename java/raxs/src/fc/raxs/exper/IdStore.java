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
import fc.xml.xmlr.MutableRefTreeImpl;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTrees;

public class IdStore extends MemoryStore {
  
  public IdStore(StoreConfiguration sc) throws IOException {
    t = trees.get(sc.getStoreFile());
    if( t == null ) {
      t = new MutableRefTreeImpl(null, MutableRefTreeImpl.ALWAYS_LAST);
      trees.put(sc.getStoreFile(), t);
    }
    tm = sc.getModel();
  }

  @Override
  public ChangeBuffer getChangeBuffer() {
    return new ChangeBuffer(new MutableRefTreeImpl(null,
        MutableRefTreeImpl.ALWAYS_LAST), t);
  }

  @Override
  public void apply(RefTree nt) throws NodeNotFoundException {
    if( Measurements.STORE_TIMINGS ) {
      Time.stamp( Measurements.H_STORE_APPLY );
      Time.stamp( Measurements.H_STORE_APPLY_WRITETREE );
    }
    if( t.getRoot() == null )
      t.insert(null, nt.getRoot().getId(), nt.getRoot().getContent());
    RefTrees.apply(nt, t);
    if( Measurements.STORE_TIMINGS ) 
      Time.stamp( Measurements.H_STORE_APPLY_WRITETREE_END );
    init();
  }
}

// arch-tag: 09cc1fb6-dfe9-4c3b-8743-0ea5e1c2f726
//
