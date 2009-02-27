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

//$Id: NoSuchVersionException.java,v 1.3 2004/11/26 16:28:57 ctl Exp $

package fc.raxs;

import java.io.IOException;

/** Exception indicating that a version was not found. Only raised
 * by operations that expect the version in question to exist.
 */

public class NoSuchVersionException extends IOException {

  private static final long serialVersionUID = -356238555861780712L;

  private int ver;

  /** Create a new exception.
   *
   * @param ver missing version
   */
  public NoSuchVersionException(int ver) {
    super("Version: " + ver);
    this.ver = ver;
  }

  /** Get missing version.
   *
   * @return missing version
   */
  public int getVersion() {
    return ver;
  }
}
// arch-tag: ef3ce4ca85d4cac7d1bf6b8b8ff1066f *-
//
