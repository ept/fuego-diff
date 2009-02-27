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

package fc.xml.diff.encode;

public class DefaultPosTranformer implements PosTransformer {
  public String transform(int pos) {
    return pos == -1 ? "-" : String.valueOf(pos);
  }

}
// arch-tag: 204ba094-e053-4125-a429-d97ed7f4ffa2
//
