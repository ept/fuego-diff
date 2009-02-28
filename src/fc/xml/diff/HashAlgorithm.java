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

// $Id: HashAlgorithm.java,v 1.3 2005/10/12 16:46:38 ctl Exp $
package fc.xml.diff;

import java.security.MessageDigest;
import java.util.List;

public interface HashAlgorithm<E> {
  public short quickHash(E o);
  public void secureDigest(List<E> o, MessageDigest md);
}
