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

package fc.xml.xmlr.xas;

import java.io.IOException;

/** Exception indicating malformed Xml. */
public class MalformedXml extends IOException {

  public MalformedXml() {
    super();
  }

  public MalformedXml(String s) {
    super(s);
  }
  
}

// arch-tag: 0da848b9-e327-43ac-a960-a6901639f442
