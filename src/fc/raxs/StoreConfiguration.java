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

import java.io.File;
import java.io.IOException;

import fc.xml.xmlr.model.TreeModel;

/** Store configuration information.
 * 
 */
public interface StoreConfiguration {

  /** Store tree model. */
  public TreeModel getModel() throws IOException;
  
  /** Store XML file. */
  public File getStoreFile();
  
}
// arch-tag: 984266cd-e5a8-4e3b-9839-358472a98e5f
