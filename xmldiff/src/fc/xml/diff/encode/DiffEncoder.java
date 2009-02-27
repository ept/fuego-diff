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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import fc.xml.diff.Segment;
import fc.xml.xas.Item;

public interface DiffEncoder {

  public void encodeDiff(List<Item> base, List<Item> doc,
      List<Segment<Item>> matches, List<Item> preamble, OutputStream out)
      throws IOException;

}
// arch-tag: 3eaf5e57-ee9d-46a5-aff7-14214b9e5776
//
