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

import java.util.Stack;
import java.util.EmptyStackException;

/**
 * A queue (FIFO) of objects.  In MIDP there is no linked list class
 * that could be used as an efficient queue.  This is a from-scratch
 * typical implementation of such a data structure.  In the normal
 * Java world you are better off using a {@link java.util.LinkedList}
 * directly.
 *
 * <p>The operations have been modelled after those in {@link Stack}.
 * @author Jaakko Kangasharju
 */
public class Queue {

    private Node head = null;
    private Node tail = null;

    private Stack nodes = new Stack();

    private Node getNode (Object item) {
	if (nodes.empty()) {
	    return new Node(item);
	} else {
	    Node node = (Node) nodes.pop();
	    node.reset(item);
	    return node;
	}
    }

    /**
     * Insert an item at the rear of the queue.
     *
     * @param item the item to insert
     * @return the inserted item
     */
    public Object enqueue (Object item) {
	Node node = getNode(item);
	if (empty()) {
	    head = node;
	} else {
	    tail.next = node;
	}
	tail = node;
	return item;
    }

    /**
     * Remove the item at the head of the queue.
     *
     * @return the removed item
     *
     * @throws EmptyStackException if the queue is empty
     */
    public Object dequeue () throws EmptyStackException {
	if (empty()) {
	    throw new EmptyStackException();
	}
	Object item = head.getItem();
	nodes.push(head);
	head = head.next;
	return item;
    }

    /**
     * Return the item at the head of the queue.  Unlike {@link
     * #dequeue}, this method does not remove the item.
     *
     * @return the item at the head of the queue
     *
     * @throws EmptyStackException if the queue is empty
     */
    public Object peek () throws EmptyStackException {
	if (empty()) {
	    throw new EmptyStackException();
	} else {
	    return head.getItem();
	}
    }

    /**
     * Test whether the queue is empty.
     *
     * @return <code>true</code> if this queue contains no items,
     * <code>false</code> otherwise
     */
    public boolean empty () {
	return head == null;
    }

    public String toString () {
	Node node = head;
	StringBuffer result = new StringBuffer("[");
	boolean first = true;
	while (node != null) {
	    if (!first) {
		result.append(",");
	    }
	    result.append(node.getItem().toString());
	    node = node.next;
	    first = false;
	}
	result.append("]");
	return result.toString();
    }

    private static class Node {

	private Object item;
	public Node next;

	public Node (Object item) {
	    next = null;
	    this.item = item;
	}

	public Object getItem () {
	    return item;
	}

	public void reset (Object item) {
	    next = null;
	    this.item = item;
	}

    }

}

// arch-tag: 7bf2798b-d7ce-4b02-aab2-3d8ee95c4b18
