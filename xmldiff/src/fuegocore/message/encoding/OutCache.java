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

package fuegocore.message.encoding;

import java.util.Stack;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * An output token cache used by the regular Xebu serializer.  As an
 * extension this class provides an advanced facility for two-phase
 * inserts.  In this case an object is tentatively inserted into the
 * cache.  This tentative insertion can then later be either cancelled
 * or committed as a regular insertion.  The cache behaves as if a
 * tentative insertion were a real insertion when seen through the
 * cache's public interface, but cancellation reveals the previous
 * situation.
 */
public class OutCache implements OutTokenCache {

    private Hashtable cache = new Hashtable();
    private Hashtable tentativeCache = new Hashtable();
    private Object[] reverseCache = new Object[XebuConstants.CACHE_SIZE];
    private Stack[] reverseTentativeCache =
	new Stack[XebuConstants.CACHE_SIZE];
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

    /**
     * Insert an object into the cache.  The returned value is the
     * integer this object is mapped into after the insertion.  It is
     * guaranteed that the returned values will be a consecutive
     * sequence of integers starting from <code>0</code> until the
     * cache is full, after which no guarantees are given.
     *
     * <p>As a restriction of the guarantee given above, tentative
     * insertions are also taken into account when determining when
     * the cache "rolls over".
     *
     * @param key {@inheritDoc}
     * @return {@inheritDoc}
     *
     * @see #tentativeInsert
     */
    public int insert (Object key) {
	Node node = internalInsert(key, cache);
	reverseEnsure(key, node.value);
	return node.value;
    }

    /**
     * Tentatively insert an object into the cache.  The returned
     * value is the integer this object will be mapped to if
     * committed.
     *
     * @param key the object to use
     * @return the integer the <code>key</code> maps to from now on
     * until (if ever) the insertion is cancelled
     *
     * @see #insert
     * @see #tentativeCancel
     * @see #tentativeCommit
     */
    public int tentativeInsert (Object key) {
	Node node = internalInsert(key, tentativeCache);
	synchronized (cache) {
	    Stack stack = reverseTentativeCache[node.value];
	    if (stack == null) {
		stack = new Stack();
		reverseTentativeCache[node.value] = stack;
	    }
	    stack.push(key);
	}
	return node.value;
    }

    /**
     * Fetch an object's corresponding integer from a cache.  The
     * returned value is also used for signaling errors; it will be -1
     * if <code>key</code> is not found in the cache.
     *
     * <p>If another object is currently tentatively mapped to the
     * same integer as this object actually is, the situation is
     * treated as if the object was not in the cache.
     *
     * @param key {@inheritDoc}
     * @return {@inheritDoc}
     */
    public int fetch (Object key) {
	Node node = null;
	synchronized (tentativeCache) {
	    node = (Node) tentativeCache.get(key);
	    if (node != null) {
		Stack stack =
		    reverseTentativeCache[node.value];
		if (stack != null && !stack.empty()) {
		    Object actualKey = stack.peek();
		    if (!key.equals(actualKey)) {
			return -1;
		    }
		}
	    }
	}
	if (node == null) {
	    synchronized (cache) {
		node = (Node) cache.get(key);
		if (node != null) {
		    synchronized (tentativeCache) {
			Stack stack =
			    reverseTentativeCache[node.value];
			if (stack != null && !stack.empty()) {
			    node = null;
			}
		    }
		}
	    }
	}
	if (node == null) {
	    return -1;
	}
	access(node);
	//	assert key.equals(fetchReverse(node.value)) :
	//	    key + " " + node.value + " " + reverseTentativeCache[node.value];
	return node.value;
    }

    public Object fetchReverse (int value) {
	Object result = null;
	synchronized (tentativeCache) {
	    Stack stack = reverseTentativeCache[value];
	    if (stack != null && !stack.empty()) {
		result = stack.peek();
	    }
	}
	if (result == null) {
	    synchronized (cache) {
		result = reverseCache[value];
	    }
	}
	return result;
    }

    /**
     * Cancel a previously-made tentative insertion.  If a tentative
     * mapping from the specified key to the specified value currently
     * exists, it is removed, and any previous mapping to the value
     * takes effect.  If the key is not mapped to the specified value,
     * nothing happens.
     *
     * @param key the object for which to cancel the mapping
     * @param value the value that the key has
     *
     * @see #tentativeInsert
     * @see #tentativeCommit
     */
    public void tentativeCancel (Object key, int value) {
	synchronized (tentativeCache) {
	    Node mappedValue = (Node) tentativeCache.remove(key);
	    if (mappedValue != null && mappedValue.value == value) {
		Stack stack = reverseTentativeCache[value];
		stack.removeElement(key);
	    }
	}
    }

    /**
     * Commit all currently active tentative insertions.  Any
     * tentative insertions made and not cancelled will be inserted
     * into the cache as if {@link #insert} had been called and
     * returned the values given in the tentative insertions.
     *
     * @see #insert
     * @see #tentativeInsert
     * @see #tentativeCancel
     */
    public void tentativeCommit () {
	synchronized (tentativeCache) {
	    for (Enumeration values = tentativeCache.elements();
		 values.hasMoreElements(); ) {
		Node node = (Node) values.nextElement();
		Stack stack = reverseTentativeCache[node.value];
		if (!stack.empty()) {
		    synchronized (cache) {
			Object key = stack.peek();
			//			assert node.value == fetch(key) : key.toString()
			//			    + " " + node.value + " " + fetch(key);
			cache.put(key, node);
			reverseEnsure(key, node.value);
		    }
		    stack.removeAllElements();
		}
	    }
	    tentativeCache.clear();
	}
    }

}
