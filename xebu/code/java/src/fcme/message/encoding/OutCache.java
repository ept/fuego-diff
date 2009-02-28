/*
 * Copyright 2006 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fcme.message.encoding;

import java.util.Stack;
import java.util.Hashtable;
import java.util.Enumeration;

import fuegocore.message.encoding.OutTokenCache;
import fuegocore.message.encoding.XebuConstants;

/**
 * An output token cache used by the FCME Xebu serializer.  This is a
 * pure implementation of the interface, with no additional
 * functionality.
 */
public class OutCache implements OutTokenCache {

    private Hashtable cache = new Hashtable();
    private Object[] reverseCache = new Object[XebuConstants.CACHE_SIZE];
    private int size = INITIAL_SLOT;
    private Node first = null;
    private Node last = null;

    private static final int INITIAL_SLOT = 0x00;

    private Node nextSlot () {
	Node node;
	if (size < XebuConstants.CACHE_SIZE) {
	    int value = size++;
	    if (first == null) {
		node = new Node(value, null, null);
		first = last = node;
	    } else {
		node = new Node(value, first, null);
		first.prev = node;
		first = node;
	    }
	} else {
	    node = last;
	}
	return node;
    }

    private void access (Node node) {
	if (node != first) {
	    Node next = node.next;
	    Node prev = node.prev;
	    if (next != null) {
		next.prev = prev;
	    } else {
		last = prev;
	    }
	    prev.next = next;
	    node.next = first;
	    node.prev = null;
	    first.prev = node;
	    first = node;
	}
    }

    private Node internalInsert (Object key, Hashtable insertCache) {
	Node node;
	synchronized (insertCache) {
	    node = nextSlot();
	    access(node);
	    insertCache.put(key, node);
	}
	return node;
    }

    private void reverseEnsure (Object key, int value) {
	synchronized (cache) {
	    if (reverseCache[value] != null) {
		cache.remove(reverseCache[value]);
	    }
	    reverseCache[value] = key;
	}
    }

    private static class Node {

	public Node prev;
	public Node next;
	public final int value;

	public Node (int value, Node next, Node prev) {
	    this.value = value;
	    this.next = next;
	    this.prev = prev;
	}

    }

    public int insert (Object key) {
        Node node = internalInsert(key, cache);
	reverseEnsure(key, node.value);
	return node.value;
    }

    public int fetch (Object key) {
	Node node = null;
	synchronized (cache) {
	    node = (Node) cache.get(key);
	}
	if (node == null) {
	    return -1;
	}
	access(node);
	return node.value;
    }

    public Object fetchReverse (int value) {
	Object result = null;
	synchronized (cache) {
	    result = reverseCache[value];
	}
	return result;
    }

}
