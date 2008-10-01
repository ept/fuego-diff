/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util;

import java.util.Hashtable;

public final class ByteArray {

    private static final int MAX_SIZE = 256;

    private static Hashtable entries = new Hashtable();
    private static ByteArray[] keys = new ByteArray[MAX_SIZE];
    private static short[] nexts = new short[MAX_SIZE];
    private static short[] prevs = new short[MAX_SIZE];
    private static short head = -1;
    private static short tail = -1;

    private byte[] data;
    private int hash = 0;

    private static void insert (ByteArray array) {
	short size = (short) entries.size();
	if (size < MAX_SIZE) {
	    if (tail >= 0) {
		nexts[tail] = size;
	    } else {
		head = 0;
	    }
	    prevs[size] = tail;
	    nexts[size] = -1;
	    tail = size;
	} else {
	    // XXX KLUDGE-20070305-1: This was observed to be null in SeisGen
	    if (keys[tail] != null) {
		entries.remove(keys[tail]);
	    }
	}
	entries.put(array, new Entry(array, tail));
	access(tail);
	keys[tail] = array;
    }

    private static void access (short index) {
	if (head != index) {
	    short next = nexts[index];
	    short prev = prevs[index];
	    nexts[prev] = next;
	    if (next >= 0) {
		prevs[next] = prev;
	    } else {
		tail = prev;
	    }
	    nexts[index] = head;
	    prevs[index] = -1;
	    prevs[head] = index;
	    head = index;
	}
    }

    private ByteArray (byte[] data) {
	this.data = data;
    }

    public static ByteArray construct (byte[] data) {
	return new ByteArray(data).intern();
    }

    public ByteArray intern () {
	Entry entry = (Entry) entries.get(this);
	if (entry != null) {
	    access(entry.index);
	    return entry.array;
	} else {
	    insert(this);
	    return this;
	}
    }

    public byte[] getData () {
	return data;
    }

    public int hashCode () {
	if (hash == 0) {
	    for (int i = 0; i < data.length; i++) {
		hash = 37 * hash + data[i];
	    }
	}
	return hash;
    }

    public boolean equals (Object o) {
	if (this == o) {
	    return true;
	} else if (!(o instanceof ByteArray)) {
	    return false;
	} else {
	    ByteArray array = (ByteArray) o;
	    if (data.length != array.data.length) {
		return false;
	    } else {
		for (int i = 0; i < data.length; i++) {
		    if (data[i] != array.data[i]) {
			return false;
		    }
		}
		return true;
	    }
	}
    }

    private static final class Entry {

	public ByteArray array;
	public short index;

	public Entry (ByteArray array, short index) {
	    this.array = array;
	    this.index = index;
	}

    }

}

// arch-tag: fe945c7e-d47d-421d-87a9-001701aae208
