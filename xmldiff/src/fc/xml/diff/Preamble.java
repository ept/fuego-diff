/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-xmldiff-users@hoslab.cs.helsinki.fi.
 */

// $Id: Preamble.java,v 1.1.2.1 2006/06/30 12:48:04 ctl Exp $
package fc.xml.diff;

import java.util.List;

import fc.xml.xas.Item;

public interface Preamble {
  public List<Item> getPreamble();
}
// arch-tag: 46f2c329-3eb5-40e2-b060-eeea728a0dfd
