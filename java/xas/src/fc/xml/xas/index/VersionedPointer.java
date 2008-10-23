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

package fc.xml.xas.index;

import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.Item;
import fc.xml.xas.MutableFragmentPointer;
import fc.xml.xas.MutablePointer;
import fc.xml.xas.WrapperItem;
import fc.xml.xas.XasFragment;

public class VersionedPointer extends GlobalPointer {

    private VersionNode current;

    VersionedPointer (VersionedDocument document, DeweyKey key) {
	this(document, key, document.getFragment().pointer());
    }

    VersionedPointer (VersionedDocument document, DeweyKey key,
	    MutableFragmentPointer pointer) {
	super(document, key, pointer);
	current = document.getVersion();
    }

    public DeweyKey getKey () {
	update();
	return key;
    }

    private void update () {
	if (!isValid()) {
	    throw new IllegalStateException("Pointer not valid");
	}
	directUpdate();
    }

    private void directUpdate () {
	if (Log.isEnabled(Log.TRACE)) {
	    Log.log("directUpdate()", Log.TRACE);
	}
	while (!current.isSentinel()) {
	    Log.log("Current", Log.TRACE, current);
	    if (pointer != null) {
		// System.out.println("Current: " + current);
		DeweyKey newKey = current.update(key);
		Log.log("Old key", Log.TRACE, key);
		Log.log("New key", Log.TRACE, newKey);
		if (key != newKey) {
		    if (Log.isEnabled(Log.TRACE)) {
			Log.log("Key update: " + key + " -> " + newKey,
			    Log.TRACE);
		    }
		    key = newKey;
		    if (key != null) {
			if (Log.isEnabled(Log.TRACE)) {
			    Log.log("Pointer update: " + pointer, Log.TRACE);
			}
			pointer = new MutableFragmentPointer(current.update(pointer));
			if (Log.isEnabled(Log.TRACE)) {
			    Log.log(" -> " + pointer, Log.TRACE);
			}
		    } else {
			pointer = null;
		    }
		}
	    }
	    current = current.getNext();
	}
    }

    public boolean isValid () {
	directUpdate();
	return pointer != null;
    }

    public Item get () {
	// System.out.println("get(),this=" + this);
	update();
	Item result = super.get();
	// System.out.println("get()=" + result);
	if (result instanceof WrapperItem) {
	    // System.out.println(pointer);
	}
	return result;
    }

    public void set (Item item) {
	update();
	super.set(item);
    }

    private void directInsert (Item item) {
	super.insert(item);
    }

    public void insert (Item item) {
	update();
	((VersionedDocument) document).insertAfter(key, pointer);
	directInsert(item);
    }

    private void directInsertFirstChild (Item item) {
	super.insertFirstChild(item);
    }

    public void insertFirstChild (Item item) {
	update();
	((VersionedDocument) document).insertAt(key.down(), pointer);
	directInsertFirstChild(item);
    }

    private Item directDelete () {
	return super.delete();
    }

    public Item delete () {
	update();
	((VersionedDocument) document).delete(key, pointer);
	return directDelete();
    }

    public void move (MutablePointer target) {
	if (!(target instanceof GlobalPointer)) {
	    throw new IllegalArgumentException("Pointer " + target
		    + " not pointing at a document");
	}
	GlobalPointer gt = (GlobalPointer) target;
	if (gt.document != this.document) {
	    throw new IllegalArgumentException("GlobalPointer pointing to"
		    + " wrong document");
	}
	if (Util.equals(key, gt.key)) {
	    return;
	}
	VersionedPointer vt = null;
	update();
	if (target instanceof VersionedPointer) {
	    vt = (VersionedPointer) target;
	    vt.update();
	}
	((VersionedDocument) document).moveAfter(key, pointer, gt.key,
	    gt.pointer);
	if (pointer.isSibling(gt.pointer)) {
	    pointer.move(gt.pointer);
	} else if (vt != null) {
	    vt.directInsert(this.directDelete());
	} else {
	    gt.insert(this.directDelete());
	}
    }

    public void moveFirstChild (MutablePointer target) {
	if (!(target instanceof GlobalPointer)) {
	    throw new IllegalArgumentException("Pointer " + target
		    + " not pointing at a document");
	}
	GlobalPointer gt = (GlobalPointer) target;
	if (gt.document != this.document) {
	    throw new IllegalArgumentException("GlobalPointer pointing to"
		    + " wrong document");
	}
	// System.out.println("moveFirstChild(" + target + "),this=" + this);
	VersionedPointer vt = null;
	update();
	if (target instanceof VersionedPointer) {
	    vt = (VersionedPointer) target;
	    vt.update();
	}
	((VersionedDocument) document).moveTo(key, pointer, gt.key.down(),
	    ((XasFragment) gt.pointer.get()).pointer());
	if (pointer.isSibling(gt.pointer)) {
	    // System.out.println("isSibling(" + pointer + "," + gt.pointer
	    // + ")");
	    pointer.moveFirstChild(gt.pointer);
	} else if (vt != null) {
	    vt.directInsertFirstChild(this.directDelete());
	} else {
	    gt.insertFirstChild(this.directDelete());
	}
    }

    public String toString () {
	return "VP(" + current + "," + key + "," + pointer + ")";
    }

}

// arch-tag: 388c9521-f0f2-46ca-8b8d-59ec1deb62f5
