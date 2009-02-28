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

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/** Default implementation of RAXS version history.
 * 
 */
public class VersionHistoryImpl implements VersionHistory {

  protected RandomAccessXmlStore raxs;

  /** Create history for store.
   */
  public VersionHistoryImpl(RandomAccessXmlStore raxs) {
    this.raxs = raxs;
  }

  /** @inheritDoc */
  public int getCurrentVersion() {
    return raxs.getCurrentVersion();
  }

  /** @inheritDoc */
  public InputStream getData(int version) {
    return new DeltaInputStream(version,raxs);
  }

  /** @inheritDoc */
  public int getPreviousData(int version) {
    int pv = version-1;
    // Scan back until inequality
    for(;pv>RandomAccessXmlStore.FIRST_VERSION-2 &&
      versionsEqual( pv, version );pv--)
      ;
    return pv < RandomAccessXmlStore.FIRST_VERSION ?
        RandomAccessXmlStore.FIRST_VERSION : pv;
  }

  /** @inheritDoc */
  public List<Integer> listVersions() {
    LinkedList<Integer> verlist = new LinkedList<Integer>();
    if( raxs.getCurrentVersion() != RandomAccessXmlStore.NO_VERSION ) {
      for( int v = raxs.getOldestVersion();v<=raxs.getCurrentVersion();v++)
        verlist.add(v);
    }
    return verlist;
  }

  /** @inheritDoc */
  public boolean versionsEqual(int v1, int v2 ) {
    return v1 != RandomAccessXmlStore.NO_VERSION && v1 == v2;
  }

}

// arch-tag: 6bd5aed0-2be6-4e7d-aa8c-571c5a659dd5
