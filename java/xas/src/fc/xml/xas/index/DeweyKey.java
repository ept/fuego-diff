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
import fc.xml.xas.Verifier;

/**
 * A representation of a Dewey key. A Dewey key is a sequence of non-negative
 * integers, each integer denoting the index of the child to pick next when
 * going deeper in an XML document. Dewey keys are used in XAS because they
 * permit efficient structural comparison.
 * 
 * Dewey keys have a one-to-one correspondence with <code>int</code> arrays
 * containing non-negative values. The root key corresponds to an empty array.
 * The class has methods for converting in either direction between these
 * representations.
 */
public final class DeweyKey {

    private static final DeweyKey ROOT = new DeweyKey(null, 0);

    private DeweyKey parent;
    private int value;

    private DeweyKey (DeweyKey parent, int value) {
	this.parent = parent;
	this.value = value;
    }

    /**
         * Get a Dewey key corresponding to the root of a tree.
         */
    public static DeweyKey root () {
	return ROOT;
    }

    /**
         * Get a Dewey key corresponding to the first child of the root of a
         * tree.
         */
    public static DeweyKey initial () {
	return topLevel(0);
    }

    /**
         * Get a Dewey key corresponding to a child of the root of a tree.
         * 
         * @param value the index of the child
         * 
         * @throws IllegalArgumentException if <code>value</code> is negative
         */
    public static DeweyKey topLevel (int value) {
	if (value < 0) {
	    throw new IllegalArgumentException("DeweyKey component " + value
		    + " negative");
	}
	return new DeweyKey(ROOT, value);
    }

    private static DeweyKey construct (int[] path, int last) {
	if (last == 0) {
	    return topLevel(path[0]);
	} else {
	    return construct(path, last - 1).child(path[last]);
	}
    }

    /**
         * Create a Dewey key by appending to an existing key.
         * 
         * @param path the sequence of indices to append
         */
    public DeweyKey append (int[] path) {
	if (path != null) {
	    return append(path, 0, path.length);
	} else {
	    return this;
	}
    }

    /**
         * Create a Dewey key by appending to an existing key.
         * 
         * @param path an array containing the sequence of indices to append
         * @param offset the offset of the first index in <code>path</code>
         * @param length the number of indices to append
         */
    public DeweyKey append (int[] path, int offset, int length) {
	DeweyKey k = this;
	for (int i = 0; i < length; i++) {
	    k = k.child(path[offset + i]);
	}
	return k;
    }

    /**
         * Create a Dewey key from an integer array.
         * 
         * @param path the sequence of indices of the created key
         */
    public static DeweyKey construct (int[] path) {
	if (path == null || path.length == 0) {
	    return ROOT;
	} else {
	    return construct(path, path.length - 1);
	}
    }

    /**
         * Create a Dewey key from an integer array.
         * 
         * @param path an array containing the sequence of indices of the
         *        created key
         * @param offset the offset of the first index in <code>path</code>
         * @param length the number of indices to use
         */
    public static DeweyKey construct (int[] path, int offset, int length) {
	if (path == null || length == 0) {
	    return ROOT;
	}
	if (offset < 0 || length < 0 || offset + length > path.length) {
	    throw new IllegalArgumentException("offset=" + offset + ",length="
		    + length);
	}
	if (length == 1) {
	    return topLevel(path[offset]);
	} else {
	    return construct(path, offset, length - 1);
	}
    }

    private int[] deconstruct (int depth) {
	int[] result;
	if (parent.isRoot()) {
	    result = new int[depth + 1];
	    result[0] = value;
	} else {
	    result = parent.deconstruct(depth + 1);
	    result[result.length - depth - 1] = value;
	}
	return result;
    }

    /**
         * Convert a Dewey key to an integer array.
         */
    public int[] deconstruct () {
	if (isRoot()) {
	    return new int[0];
	} else {
	    return deconstruct(0);
	}
    }

    public boolean isRoot () {
	return this == ROOT;
    }

    public DeweyKey up () {
	if (isRoot()) {
	    throw new IllegalStateException("Cannot go up from root DeweyKey");
	}
	return parent;
    }

    public DeweyKey next () {
	if (isRoot()) {
	    throw new IllegalStateException(
		"Root DeweyKey does not have a next key");
	}
	return new DeweyKey(parent, value + 1);
    }

    public DeweyKey prev () {
	if (isRoot()) {
	    throw new IllegalStateException(
		"Root DeweyKey does not have a previous key");
	} else if (value == 0) {
	    throw new IllegalStateException("Key " + this + " is first child");
	} else {
	    return new DeweyKey(parent, value - 1);
	}
    }

    public DeweyKey down () {
	return child(0);
    }

    public DeweyKey child (int value) {
	if (value < 0) {
	    throw new IllegalArgumentException("Negative value " + value
		    + " not allowed as DeweyKey component");
	}
	return new DeweyKey(this, value);
    }

    public DeweyKey next (DeweyKey ancestor) {
	Verifier.checkNotNull(ancestor);
	if (Util.equals(parent, ancestor)) {
	    return next();
	} else if (parent.isRoot()) {
	    throw new IllegalArgumentException("Key " + ancestor
		    + " not an ancestor of " + this);
	} else {
	    return parent.next(ancestor).child(value);
	}
    }

    public DeweyKey prev (DeweyKey ancestor) {
	Verifier.checkNotNull(ancestor);
	if (Util.equals(parent, ancestor)) {
	    return prev();
	} else if (parent.isRoot()) {
	    throw new IllegalArgumentException("Key " + ancestor
		    + " not an ancestor of " + this);
	} else {
	    return parent.prev(ancestor).child(value);
	}
    }

    public DeweyKey replaceAncestorSelf (DeweyKey old, DeweyKey repl) {
	Verifier.checkNotNull(old);
	Verifier.checkNotNull(repl);
	if (equals(old)) {
	    return repl;
	} else {
	    return replaceAncestor(old, repl);
	}
    }

    public DeweyKey replaceAncestor (DeweyKey old, DeweyKey repl) {
	Verifier.checkNotNull(old);
	Verifier.checkNotNull(repl);
	if (Util.equals(parent, old)) {
	    if (repl.isRoot()) {
		return DeweyKey.topLevel(value);
	    } else {
		return repl.child(value);
	    }
	} else if (parent == null || parent.isRoot()) { 
	    // BUGFIX-20061215-1: Null check for parent (catches doing 
	    // replaceAncestor on ROOT); w/o it we get an NPX
	    throw new IllegalArgumentException("Key " + old
		    + " not an ancestor of " + this);
	} else {
	    return parent.replaceAncestor(old, repl).child(value);
	}
    }

    public boolean isChild (DeweyKey key) {
	Verifier.checkNotNull(key);
	return Util.equals(parent, key);
    }

    public boolean isDescendant (DeweyKey key) {
	Verifier.checkNotNull(key);
	return key.isRoot() || isChild(key)
		|| (!parent.isRoot() && parent.isDescendant(key));
    }

    public boolean isDescendantSelf (DeweyKey key) {
	Verifier.checkNotNull(key);
	return this.equals(key) || this.isDescendant(key);
    }

    public boolean precedeSibling (DeweyKey key) {
	Verifier.checkNotNull(key);
	return !key.isRoot() && Util.equals(parent, key.parent)
		&& value < key.value;
    }

    public boolean followSibling (DeweyKey key) {
	Verifier.checkNotNull(key);
	return !key.isRoot() && key.precedeSibling(this);
    }

    public boolean descendantFollowSibling (DeweyKey key) {
	Verifier.checkNotNull(key);
	if (key.isRoot()) {
	    return false;
	}
	DeweyKey targetKey = key.parent;
	int targetValue = key.value;
	DeweyKey currentKey = parent;
	int currentValue = value;
	if (targetKey.isRoot()) {
	    while (!currentKey.isRoot()) {
		currentValue = currentKey.value;
		currentKey = currentKey.parent;
	    }
	    return currentValue > targetValue;
	} else {
	    while (!currentKey.isRoot() && !currentKey.equals(targetKey)) {
		currentValue = currentKey.value;
		currentKey = currentKey.parent;
	    }
	    if (currentKey.isRoot()) {
		return false;
	    } else {
		return currentValue > targetValue;
	    }
	}
    }

    public DeweyKey commonAncestor (DeweyKey key) {
	Verifier.checkNotNull(key);
	while (!this.isDescendant(key)) {
	    key = key.parent;
	}
	return key;
    }

    public int getLastStep () {
	return value;
    }

    public int size () {
	if (isRoot()) {
	    return 0;
	} else {
	    return parent.size() + 1;
	}
    }

    public int hashCode () {
	return 37 * Util.hashCode(parent) + value;
    }

    public boolean equals (Object o) {
	if (this == o) {
	    return true;
	} else if (!(o instanceof DeweyKey)) {
	    return false;
	} else {
	    DeweyKey d = (DeweyKey) o;
	    return value == d.value && Util.equals(parent, d.parent);
	}
    }

    public String toString () {
	if (isRoot()) {
	    return "/";
	} else if (parent.isRoot()) {
	    return "/" + value;
	} else {
	    return parent.toString() + "/" + value;
	}
    }

}

// arch-tag: 774a82a9-16e1-46c6-84d3-b28c0072726d
