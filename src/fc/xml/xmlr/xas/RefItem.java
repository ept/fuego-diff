/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.xas;

import java.io.IOException;
import java.util.Iterator;

import fc.xml.xas.AttributeNode;
import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.Qname;
import fc.xml.xas.SerializableItem;
import fc.xml.xas.StartTag;
import fc.xml.xmlr.Reference;
import fc.xml.xmlr.model.KeyModel;

/**
 * XAS Reference item. This class provides a bridge between {@link fc.xml.xmlr.Reference References}
 * and XAS items.
 */
abstract public class RefItem extends Item implements SerializableItem {

    /** Node reference open. */
    public static final int ITEM_NODEREF_OPEN = 2;
    /** Tree reference open. */
    public static final int ITEM_TREEREF_OPEN = 1;
    /** Node reference close. */
    public static final int ITEM_NODEREF_CLOSE = 3;
    /** Tree reference close. */
    public static final int ITEM_TREEREF_CLOSE = 4;
    /** Not a reference. */
    public static final int ITEM_NONE = 0;
    /** Item is decoded reference. */
    public static final int ITEM_DECODED = -1;

    static final int CATEGORY = 0x3100;
    static final int CATEGORY_MASK = 0xffffff00;

    // Event default XML mapping

    /** Namespace for XMLR tags. */
    public static final String REF_NS = "http://www.hiit.fi/fc/xml/ref";
    /** Namespace for XMLR attributes. */
    public static final String REF_ATT_NS = "";
    /** Tag name for tree reference. */
    public static final Qname REF_TAG_TREE = new Qname(REF_NS, "tree");
    /** Tag name for node reference. */
    public static final Qname REF_TAG_NODE = new Qname(REF_NS, "node");
    /** Attribute name for id target. */
    public static final Qname REF_ATT_ID = new Qname("", "id");
    /** Attribute name for generic target. */
    public static final Qname REF_ATT_TARGET = new Qname("", "ref");


    // /** Attribute name for XPath target. */
    // public static final String REF_ATT_PATH = "path";
    // /** Attribute name for generic target. */
    // public static final String REF_ATT_TARGET = "target";

    protected RefItem(int type) {
        super(type);
    }


    /**
     * Identify XAS item as reference item.
     * @param i
     *            item to identify
     * @return Type code, see the <code>ITEM_</code> static fields of this class
     */
    public static final int whatItem(Item i) {
        if (i == null) return ITEM_NONE;
        if (i.getType() == Item.START_TAG) {
            if (REF_TAG_TREE.equals(((StartTag) i).getName())) return ITEM_TREEREF_OPEN;
            else if (REF_TAG_NODE.equals(((StartTag) i).getName())) return ITEM_NODEREF_OPEN;
            else return ITEM_NONE;
        }
        if (i.getType() == Item.END_TAG) {
            if (REF_TAG_NODE.equals(((EndTag) i).getName())) return ITEM_NODEREF_CLOSE;
            else if (REF_TAG_TREE.equals(((EndTag) i).getName())) return ITEM_TREEREF_CLOSE;
            else return ITEM_NONE;
        }
        if ((i.getType() & CATEGORY_MASK) == CATEGORY) return ITEM_DECODED; // Already decoded
        return ITEM_NONE;
    }


    /**
     * Decode XAS item as reference item.
     * @param i
     *            item to decode.
     * @return decode RefItem, or <i>i</i> if <i>i</i> is already a RefItem, or <code>null</code> if
     *         <i>i</i> decodes to no item (it is the end tag for a tree reference)
     * @throws IOException
     *             if an I/O error occurs
     */
    public static final Item decode(Item i) throws IOException {
        switch (whatItem(i)) {
            case ITEM_DECODED:
                return i;
            case ITEM_TREEREF_CLOSE:
                return null; // BUGFIX-20070625-1
            case ITEM_NONE:
                return i;
            case ITEM_TREEREF_OPEN:
                return new RefTreeItem((StartTag) i, ((StartTag) i).getContext());
            case ITEM_NODEREF_OPEN:
                return new RefNodeItem((StartTag) i, ((StartTag) i).getContext());
            case ITEM_NODEREF_CLOSE:
                return new RefNodeItem((EndTag) i);
            default:
                assert false;
        }
        assert false;
        return null;
    }


    /**
     * Check if item decodes as ref item. An instance of <code>RefItem</code> does <i>not</i> decode
     * as a reference item.
     * @param i
     *            item to test
     * @return <code>true</code> if item decodes as ref item.
     */
    public static final boolean decodesAsRef(Item i) {
        return whatItem(i) != 0;
    }


    /**
     * Check if an item is an instance of RefItem.
     * @param i
     *            item to test
     * @return <code>true</code> if item is a RefItem.
     */
    public static final boolean isRefItem(Item i) {
        return (i.getType() & CATEGORY_MASK) == CATEGORY;
    }


    /**
     * Make XAS start item for a reference.
     * @param r
     *            reference to encode
     * @param ctx
     *            XAS context
     * @return start item
     */
    public static RefItem makeStartItem(Reference r, StartTag ctx) {
        if (r.getTarget() == null) throw new IllegalArgumentException("null reference");
        if (r.isTreeReference()) return new RefTreeItem(r.getTarget(), ctx);
        else return new RefNodeItem(r.getTarget(), ctx);
    }


    /**
     * Make XAS end item for reference.
     * @param r
     *            reference to encode
     * @return end item
     */
    public static RefItem makeEndItem(Reference r) {
        if (r.getTarget() == null) throw new IllegalArgumentException("null reference");
        if (r.isTreeReference()) return null;
        else return RefNodeItem.END_REF;
    }


    /**
     * Create reference to this item.
     * @param km
     *            key model to use
     * @return reference
     * @throws IOException
     *             if an I/O error occurs
     */
    public abstract Reference createReference(KeyModel km) throws IOException;


    /** The value of the target attribute of the ref tag. */
    // NOTE: not Key, because those belong to trees
    // NOTE2: not String, because we want to be able to use typed attributes
    public abstract Object getTarget();


    /** Get any extra attributes found in the decoded item. */
    public abstract Iterator<AttributeNode> getExtraAttributes();


    /** Get named attribute. */
    public abstract AttributeNode getAttribute(Qname n);


    /** Add extra attribute for XAS encoding. */
    public abstract void addExtraAttribute(AttributeNode an);


    /** Is the RefItem a tree reference. */
    public abstract boolean isTreeRef();


    /** Get reference item context. */
    public abstract StartTag getContext();

}

// arch-tag: c702d2fc-75f2-420d-9253-f57f5e9a9e59
