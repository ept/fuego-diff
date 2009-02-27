/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

/**
 * A piece of XML. The core item types each correspond to pieces of actual XML
 * syntax. Extension items may provide other interpretations, since they are
 * allowed to define their own processing. Item types are represented as
 * subclasses, so there is little common functionality in this class.
 */
public abstract class Item {

    public static final int START_DOCUMENT = 1;
    public static final int END_DOCUMENT = 2;
    public static final int START_TAG = 3;
    public static final int END_TAG = 4;
    public static final int TEXT = 5;
    public static final int PI = 6;
    public static final int COMMENT = 7;
    public static final int ENTITY_REF = 8;
    public static final int DOCTYPE = 9;

    protected static final int CLASS_MASK = 0xFF0000;

    private int type;

    protected Item (int type) {
	this.type = type;
    }

    public int getType () {
	return type;
    }

    protected static boolean hasType (Item i, int t) {
	return i != null && (i.type & ~CLASS_MASK) == t;
    }

    protected static boolean hasClass (Item i, int c) {
	return i != null && (i.type & CLASS_MASK) == c;
    }

    public static boolean isStartDocument (Item i) {
	return hasType(i, START_DOCUMENT);
    }

    public static boolean isEndDocument (Item i) {
	return hasType(i, END_DOCUMENT);
    }

    public static boolean isDocumentDelimiter (Item i) {
	return isStartDocument(i) || isEndDocument(i);
    }

    public static boolean isStartTag (Item i) {
	return hasType(i, START_TAG);
    }

    public static boolean isStartItem (Item i) {
	return isStartTag(i) || isStartDocument(i);
    }

    public static boolean isEndTag (Item i) {
	return hasType(i, END_TAG);
    }

    public static boolean isEndItem (Item i) {
	return isEndTag(i) || isEndDocument(i);
    }

    public static boolean isText (Item i) {
	return hasType(i, TEXT);
    }

    public static boolean isEntityRef (Item i) {
	return hasType(i, ENTITY_REF);
    }

    public static boolean isContent (Item i) {
	return isText(i) || isEntityRef(i);
    }

}

// arch-tag: 9a59afd5-87ac-4fd0-a007-7fdbcb96a545
