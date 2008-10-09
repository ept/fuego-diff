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
import java.io.OutputStream;

import fc.util.io.DelayedInputStream;
import fc.util.log.Log;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.XmlOutput;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.xas.XasSerialization;

/** Input stream potentially containing delta-encoded content.
 */

public class DeltaInputStream extends DelayedInputStream implements DeltaStream {

  protected int baseVersion = RandomAccessXmlStore.NO_VERSION;
  protected int version = RandomAccessXmlStore.NO_VERSION;
  protected RandomAccessXmlStore raxs;

  /** Construct a new stream.
   * 
   * @param version version of the store that the stream encodes
   * @param raxs store that the stream encodes
   */
  
  public DeltaInputStream(int version, RandomAccessXmlStore raxs) {
    super();
    this.version = version;
    this.raxs = raxs;
  }

  /** @inheritDoc
   */
  
  public void setBaseVersion(int base) throws NoSuchVersionException {
    baseVersion = base;
    in = null;  
  }

  /** @inheritDoc
   */
  
  public long getDeltaSize() {
    return -1l;
  }

  @Override
  protected void stream(OutputStream out) throws IOException {
    raxs.open();
    try {
      ItemTarget xo = new XmlOutput(out,"utf-8");
      RefTree t = raxs.getRefTree(version, baseVersion);
      XasSerialization.writeTree(t, xo, raxs.getConfiguration().getModel());
      out.flush();
    } finally {
      try {
        out.close();
      } catch( IOException e) {
        Log.error("Cannot close stream",e);
      }
      raxs.close();
    }
  }

  /** @inheritDoc
   */

  public int getBaseversion() {
    return baseVersion;
  } 
}

// arch-tag: f69025c4-0416-463e-8030-efff399941e1
