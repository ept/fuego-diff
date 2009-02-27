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

// $Id: NodeNotFoundException.java,v 1.4 2004/12/17 13:48:18 ctl Exp $
package fc.xml.xmlr;

import java.io.IOException;

/** Exception indicating that a node was not found. Only raised
 * by operations that requires the node in question to exist.
 */

public class NodeNotFoundException extends Exception {

  private Key id;

  /** Create a new exception. */
  public NodeNotFoundException() { //FIXME-W: Consider deprecating
    super();
  }

  /** Create a new exception.
   * @param id id of missing node
   */
  public NodeNotFoundException(Key id) {
    super("Node: " + (id == null ? "null" : id +" (type "+id.getClass()+")") );
    this.id = id;
  }

  /** Create a new exception.
   * @param msg additional explanation
   * @param id id of missing node
   */
  public NodeNotFoundException(String msg,Key id) {
    super(msg+" " + id);
    this.id = id;
  }

  /** Get id of missing node.
   * @return id of the missing node which caused the exception
   */

  public Key getId() {
    return id;
  }
  
  public IOException makeIOException() {
    IOException ex = new IOException(getMessage());
    ex.initCause(this);
    return ex;
  }
}
// arch-tag: b871dc488468810e3fbd101d6fee9376 *-
