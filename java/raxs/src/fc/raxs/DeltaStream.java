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

// $Id: DeltaStream.java,v 1.3 2004/11/26 16:28:56 ctl Exp $
package fc.raxs;


/** Stream of delta data. DeltaStreams are streams with an associated
 * <i>base version</i>. If the base version is set, the stream either
 * produces or accepts content expressed as a delta to the base version.
 */

public interface DeltaStream {

  /** Set the base version for the delta.
   *
   * @param version delta base version, or 
   * {@link RandomAccessXmlStore#NO_VERSION} if no delta encoding is used. 
   * @throws NoSuchVersionException if the base version is unknown to the
   * object
   */
  public void setBaseVersion(int version) throws NoSuchVersionException;

  /** Get size of the delta.
   *
   * @return size of the delta, or <code>-1L</code> if it is unknown.
   */
  public long getDeltaSize();

  /** Read base version.
   */
  public int getBaseversion();
  
}
// arch-tag: bd8a4f7af7dbf1958f1dab6a546417a6 *-
