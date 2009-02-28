/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.message.encoding;

import java.util.Stack;
import java.util.Vector;
import java.util.Hashtable;

import fuegocore.util.xas.Event;
import fuegocore.util.xas.EventSequence;
import fuegocore.util.xas.EventList;

/**
 * A cache mapping event sequences to integers with the sequences given piecemeal. This cache is
 * intended to be used when caching sequences that are read an event at a time. The design is
 * stack-based: the last sequence to be started will be the first sequence to end. All events
 * encountered are added to every sequence currently being processed.
 */
public class SequenceCache {

    private static final int initialIndex = 0x00;
    private EventList buffer = new EventList();
    private int index = initialIndex;
    private Stack indices = new Stack();
    private Hashtable cache = new Hashtable();
    private Vector values = new Vector();
    private Node first = null;
    private Node last = null;


    private Node nextNode() {
        Node node;
        if (index < XebuConstants.SEQUENCE_CACHE_SIZE) {
            int value = index++;
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


    private void access(Node node) {
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


    /**
     * Start encoding a new sequence.
     */
    public void start() {
        indices.push(new Integer(buffer.size()));
    }


    /**
     * Check whether any sequences are currently being processed. The return value of this method
     * indicates whether {@link #start} has been more called more times than {@link #end}.
     */
    public boolean isActive() {
        return !indices.empty();
    }


    /**
     * Add the given event to each sequence. This method advances each sequence currently being
     * processed by the given event.
     */
    public void next(Event ev) {
        if (!indices.empty()) {
            buffer.add(ev);
        }
    }


    /**
     * Return the cached value of the current topmost sequence. If the current sequence has no
     * value, <code>-1</code> is returned. This method is intended to be used mainly for checking
     * whether the current topmost sequence has already been cached.
     */
    public int value() {
        int result = -1;
        if (!indices.empty()) {
            Integer begin = (Integer) indices.peek();
            EventSequence es = buffer.subSequence(begin.intValue());
            Node node = (Node) cache.get(es);
            if (node != null) {
                result = node.value;
            }
        }
        return result;
    }


    /**
     * End the current topmost sequence and return its value. If this sequence was already cached,
     * the previous value is returned and nothing is changed. If there already was a sequence mapped
     * to the resulting cache index, that sequence is purged.
     */
    public int end() {
        int result = -1;
        if (!indices.empty()) {
            Integer begin = (Integer) indices.pop();
            EventSequence es = buffer.subSequence(begin.intValue());
            Node node = (Node) cache.get(es);
            if (node == null) {
                node = nextNode();
                result = node.value;
                cache.put(es, node);
                if (result < values.size()) {
                    EventSequence old = (EventSequence) values.elementAt(result);
                    if (old != null) {
                        cache.remove(old);
                    }
                } else {
                    values.setSize(result + 1);
                }
                values.setElementAt(es, result);
            } else {
                result = node.value;
            }
            access(node);
            if (indices.empty()) {
                buffer = new EventList();
            }
        }
        return result;
    }


    /**
     * Forget the current topmost sequence. If for some reason the caller does not wish to cache the
     * current sequence, it can call this method to forget the current topmost sequence. If there is
     * no current sequence, this method does nothing.
     */
    public void forget() {
        if (!indices.empty()) {
            indices.pop();
        }
    }


    /**
     * Return the length of the current topmost sequence. If there is no current sequence,
     * <code>-1</code> is returned.
     */
    public int topLength() {
        if (indices.empty()) {
            return -1;
        } else {
            Integer begin = (Integer) indices.peek();
            return buffer.size() - begin.intValue();
        }
    }


    /**
     * Fetch a sequence corresponding to a given value. If there is no sequence corresponding to the
     * value, <code>null</code> is returned.
     */
    public EventSequence fetch(int value) {
        if (value < 0 || value >= values.size()) {
            return null;
        } else {
            return (EventSequence) values.elementAt(value);
        }
    }


    /**
     * Convert this cache to a string. The string representation is intended for debugging, and
     * reflects the implementation strongly. It is unintelligible to people not familiar with the
     * internals.
     */
    @Override
    public String toString() {
        return buffer.toString() + "\n" + indices.toString();
    }

    private static class Node {

        public Node prev;
        public Node next;
        public final int value;


        public Node(int value, Node next, Node prev) {
            this.value = value;
            this.next = next;
            this.prev = prev;
        }

    }

}
