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
import java.util.Collections;
import java.util.List;

/** Version history of a RAXS store.
 */
public interface VersionHistory {

  /** The empty version history. */
  public static final VersionHistory EMPTY_HISTORY = new VersionHistory() {

    public int getCurrentVersion() {
      return RandomAccessXmlStore.NO_VERSION;
    }

    public InputStream getData(int version) {
      return null;
    }

    public int getPreviousData(int version) {
      return RandomAccessXmlStore.NO_VERSION;
    }

    public List<Integer> listVersions() {
      return Collections.<Integer>emptyList();
    }

    public boolean versionsEqual(int v1, int v2) {
      return false;
    }
    
  };

  /** Get current version of the store.
   */
  public int getCurrentVersion();

  /** Get data stream for a version.
   * 
   * @param version version to retrieve
   * @return input stream to the data, or <code>null</code> if no such data. 
   */
  // TODO: retrieving reftrees rather than streams makes more sense at the
  // RAXS level.
  public InputStream getData(int version);
  
  /** Get previous version with changed content. Returns the maximum version
   * <i>u</i>, 
   * for which the content of the store differs from version <i>version</i>.
   * 
   * @param version
   * @return version whose content differs from <i>version</i>
   */
  public int getPreviousData(int version);
  
  /** List versions in the history.
   * 
   * @return list of versions in the store, sorted in ascending order.
   */
  public List<Integer> listVersions();
  
  /** Compare content of versions for equality. May return false negatives.
   * 
   * @param v1
   * @param v2
   * @return <code>true</code> if the store content at <i>v1</i> and 
   * <i>v2</i> is the same.
   */
  public boolean versionsEqual(int v1, int v2 );
}

// arch-tag: 32e971ff-101b-440a-b9fb-f28e2ddc6117

