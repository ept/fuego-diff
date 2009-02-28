/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.model;

import java.io.IOException;
import java.util.Iterator;

import fc.xml.xas.AttributeNode;
import fc.xml.xas.Item;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.xas.RefItem;
import fc.xml.xmlr.xas.RefNodeItem;
import fc.xml.xmlr.xas.RefTreeItem;

/** Model for identifying XAS items. */
public interface IdentificationModel {

    /**
     * Identify item.
     * @param i
     *            item to identify
     * @param km
     *            Key model use to construct key
     * @return item key
     * @throws IOException
     *             if an I/O error occurs
     */
    public Key identify(Item i, KeyModel km) throws IOException;


    /**
     * Tag item with serialized key. Typically, this method would add an <code>id</code> attribute
     * to {@link fc.xml.xas.StartTag StartTag} items.
     * @param i
     *            item to tag
     * @param k
     *            key to tag the item with
     * @param km
     *            Key model use to serialize key
     * @return tagged item
     */
    public Item tag(Item i, Key k, KeyModel km);

    /**
     * Model that identifies items by the "id" attribute.
     */
    public static final IdentificationModel ID_ATTRIBUTE = new IdentificationModel() {

        public final Qname ID_ATTR = new Qname("", "id");


        public Key identify(Item i, KeyModel km) throws IOException {
            if (i.getType() == Item.START_TAG) {
                StartTag st = (StartTag) i;
                AttributeNode an = st.getAttribute(ID_ATTR);
                Object id = an == null ? null : an.getValue();
                return km.makeKey(id);
            }
            if (i.getType() == RefTreeItem.TREE_REFERENCE ||
                i.getType() == RefNodeItem.NODE_REFERENCE) {
                RefItem ri = (RefItem) i;
                // Check for ID attr, if not found forge key using target
                for (Iterator<AttributeNode> ia = ri.getExtraAttributes(); ia.hasNext();) {
                    AttributeNode an = ia.next();
                    if (ID_ATTR.equals(an.getName())) return km.makeKey(an.getValue());
                }
                return km.makeKey(ri.getTarget());
            } else return km.makeKey(null);
        }


        public Item tag(Item i, Key k, KeyModel km) {
            if (k == null || k instanceof TransientKey) // FIXME: slow check?
                return i;
            if (Item.isStartTag(i)) {
                ((StartTag) i).ensureAttribute(ID_ATTR, k.toString());
                // ((StartTag) i).getAttribute(ID_ATTR) == null ) {
                // ((StartTag) i).addAttribute(ID_ATTR, k.toString());
            } else if (RefItem.isRefItem(i)) {
                RefItem ri = (RefItem) i;
                // if( XasUtil.findAttribute(ID_ATTR, ri.getExtraAttributes())
                // == null )
                if (ri.getAttribute(ID_ATTR) == null)
                    ri.addExtraAttribute(new AttributeNode(ID_ATTR, k.toString()));
            }
            return i;
        }

    };
}

// arch-tag: e3a05884-3059-41e8-a438-c8f51a6fed00

