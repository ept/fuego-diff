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

package fc.xml.xmlr.model;

import java.io.IOException;

import fc.xml.xas.Item;
import fc.xml.xmlr.Key;

/** Combined key and identification model.
 */
public class KeyIdentificationModel implements KeyModel, IdentificationModel {

  protected KeyModel km;
  protected IdentificationModel im;

  /** Create new model.
   * 
   * @param km key model
   * @param im identification model
   */
  public KeyIdentificationModel(KeyModel km, IdentificationModel im) {
    this.km = km;
    this.im = im;
  }
  
  /** Identify item using this key model. */
  public Key identify(Item i) throws IOException {
    return identify(i,this);
  }

  /** Tag item using key.
   * 
   * @param i Item
   * @param k key
   * @return tagged item
   */
  public Item tag(Item i,Key k) {
    return tag(i,k,this);
  }

  /** @inheritDoc */
  public Key makeKey(Object s) throws IOException {
    return km.makeKey(s);
  }

  /** Identify item using a foreign key model.
   * @param i item
   * @param nkm key model to use
   */
  public Key identify(Item i, KeyModel nkm) throws IOException {
    return im.identify(i, nkm);
  }

  /** Tag item using a foreign key model and key.
   * @param i item
   * @param k key
   * @param km key model to use
   */

  public Item tag(Item i, Key k, KeyModel km) {
    return im.tag(i, k, km);
  }
  
  /** String keys encoded in "id" attributes.
   */
  
  public static final KeyIdentificationModel
    ID_AS_STRINGKEY = new KeyIdentificationModel(KeyModel.STRINGKEY,
        IdentificationModel.ID_ATTRIBUTE);
      
}

// arch-tag: 67456416-46ea-45d5-b748-7de682c35699

