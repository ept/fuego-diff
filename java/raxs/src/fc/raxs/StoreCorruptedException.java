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

/** Exception indicated corrupted store.
 */
public class StoreCorruptedException extends IOException {

  private static final long serialVersionUID = -3509433214695107907L;

  public StoreCorruptedException() {
    super("The store has been corrupted and needs manual repair.");
  }

  public StoreCorruptedException(String message) {
    super(message);
  }

  public StoreCorruptedException(String message, Throwable cause) {
    super(message);
    this.initCause(cause);
  }
  
}

// arch-tag: a6baf584-9e2c-4e8d-a1b5-7e732d73da2c

