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

import java.util.List;


class ListPosTransformer implements PosTransformer {
  List<Integer> l = null;

  public ListPosTransformer(List<Integer> l) {
    this.l = l;
  }

  public String transform(int pos) {
    return pos == -1 ? "-" :
            ""+(l.get(pos)>>16)+","+(l.get(pos)&0xffff);
  }

}
// arch-tag: dcbbd5cc-2b44-44b4-a5cf-a52d0d30aae2
//
