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

import fc.xml.xas.Item;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.StartTag;
import fc.xml.xas.typing.TypedItem;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.model.KeyIdentificationModel;
import fc.xml.xmlr.model.XasCodec;

/** XAS Codec that translate nodes into a fixed number of XAS items.
 */
public interface UniformXasCodec extends XasCodec {

  /** Size of content in items, if known. The method should return the
   * uniform size in items of content (if known). Return <code>-1</code>
   * if the size is unknown, or if it varies. Some code using a TreeModel
   * requires fixed size content (see the Javadoc of the class in question).
   * 
   * @return size in items of content,  <code>-1</code> if unknown 
   */
  public int size();

  public static final UniformXasCodec ITEM_CODEC = new ItemCodec();
  
  /** Items as content. Typed items are decoded as their value.
   */
  public static class ItemCodec implements UniformXasCodec {
  
    public Object decode(PeekableItemSource is, KeyIdentificationModel kim)
        throws IOException {
      Item i = is.next();
      if( i.getType() == TypedItem.TYPED )
        return ((TypedItem) i).getValue();
      return i;
    }
  
    public void encode(ItemTarget t, RefTreeNode n, StartTag context) throws IOException {
      assert n.getContent() instanceof Item : "Invalid content type "+
        n.getContent().getClass();
      Item i = (Item) n.getContent();
      t.append( i );
    }
  
    public int size() {
      return 1;
    }
  }    
}

// arch-tag: 10a93354-b45f-4223-925e-d9e114a70064
