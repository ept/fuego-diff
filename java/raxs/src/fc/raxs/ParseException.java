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

import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.index.SeekableSource;

/** Input/output error due to failed parsing. Provides special constructors
 * for associating a XAS parsing context with the errors.
 * 
 */

public class ParseException extends IOException {

  private static final long serialVersionUID = 3969969085591891623L;

  public ParseException() {
    super();
  }
  
  /** Create a new instance.
   * 
   * @param msg Error message
   */
  public ParseException(String msg) {
    super(msg);
  }

  /** Create a new instance.
   * 
   * @param msg Error message
   * @param i offending item
   */
  public ParseException(String msg, Item i) {
    super(msg+" XML item: "+i);
  }

  /** Create a new instance.
   * 
   * @param msg Error message
   * @param i offending item
   * @param is item source of offending item, positioned a close to 
   * <code>i</code> as possible
   */


  public ParseException(String msg, Item i, ItemSource is) {
    super(msg+" at XML item: "+i+(
        is instanceof SeekableSource ? 
            " at stream offset "+
            ((SeekableSource) is).getCurrentPosition(): ""));
  }
  
}
// arch-tag: 0b95bd13-c360-40a1-92ca-2fb83b14b265

