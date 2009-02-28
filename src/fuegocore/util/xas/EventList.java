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

package fuegocore.util.xas;

import java.util.Vector;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A {@link Vector}-based representation of XML event sequences.  This
 * class implements the construction of XML event sequences by the use
 * of methods similar in signature to those in the {@link Vector}
 * interface.  The main difference is that the methods in this class
 * only take {@link Event} objects as arguments instead of arbitrary
 * objects.
 *
 * <p>The invariants that the modification methods in this class are
 * intended to guarantee are <ul> <li>All events of type {@link
 * Event#ATTRIBUTE} are immediately preceded by either an {@link
 * Event#ATTRIBUTE} event or an {@link Event#START_ELEMENT} event</li>
 * <li>Events of type {@link Event#CONTENT} are neither preceded nor
 * succeeded by events of the same type</li></ul>
 */
public class EventList implements EventSequence {

    private Vector current;
    private int smallestIndex = 0;

    private int toPrivate (int index) {
	return index - smallestIndex;
    }

    private int fromPrivate (int index) {
	return index + smallestIndex;
    }

    private boolean coalesceNext (int index) {
	boolean result = false;
	if (index < current.size() - 1) {
	    Event curr = (Event) current.elementAt(index);
	    Event next = (Event) current.elementAt(index + 1);
	    if (curr.getType() == Event.CONTENT
		&& next.getType() == Event.CONTENT) {
		Event ev = Event.createContent((String) curr.getValue()
					       + (String) next.getValue());
		current.removeElementAt(index + 1);
		current.setElementAt(ev, index);
		result = true;
	    }
	}
	return result;
    }

    /**
     * Construct an empty event sequence.
     */
    public EventList () {
	current = new Vector();
    }

    public int size () {
	return current.size();
    }

    public boolean isEmpty () {
	return current.isEmpty();
    }

    /**
     * Remove all events from the sequence.
     */
    public void clear () {
	current.removeAllElements();
    }

    public void reset () {
	current.removeAllElements();
	smallestIndex = 0;
    }

    /**
     * Add an event to the sequence.  This method adds the given event
     * to the end of the sequence, verifying that it is allowed and
     * coalescing adjacent {@link Event#CONTENT} events.
     *
     * @param ev the event to add
     *
     * @throws IllegalArgumentException if the given event is not
     * allowed at this point
     */
    public void add (Event ev) {
	add(fromPrivate(current.size()), ev);
    }

    /**
     * Add an event to the sequence at the specified place.  This
     * method adds the given event to have the specified index in the
     * sequence, shifting subsequent events, if any, to the right.
     * The method verifies that addition is allowed and coalesces
     * adjacent {@link Event#CONTENT} events.
     *
     * @param ev the event to add
     *
     * @throws IllegalArgumentException if the given event is not
     * allowed at this point
     * @throws IndexOutOfBoundsException if the index is out of range
     * (smaller than <code>0</code> or larger than the current size of
     * the sequence)
     */
    public void add (int index, Event ev) {
	int i = toPrivate(index);
	if (i < 0 || i > current.size()) {
	    throw new IndexOutOfBoundsException("Index " + index
						+ " out of range");
	}
	if (ev.getType() == Event.ATTRIBUTE) {
	    if (i > 0) {
		Event prev = (Event) current.elementAt(i - 1);
		int prevType = prev.getType();
		if (prevType != Event.START_ELEMENT
		    && prevType != Event.ATTRIBUTE) {
		    throw new IllegalArgumentException("ATTR not allowed "
						       + "after type "
						       + prevType);
		}
	    }
	}
	current.insertElementAt(ev, i);
    }

    /**
     * Add all events from another event sequence to the end of this
     * sequence.  The events are added as if the sequence was iterated
     * through and each individual event was added with {@link
     * #add(Event)}.
     *
     * @param es the event sequence to append
     */
    public void addAll (EventSequence es) {
	for (Enumeration e = es.events(); e.hasMoreElements(); ) {
	    Event ev = (Event) e.nextElement();
	    add(ev);
	}
    }

    /**
     * Remove an event at the specified index.  This method removes
     * the event at the given index from the event sequence.
     *
     * @param index the index of the event to remove
     * @return the removed event
     */
    public Event remove (int index) {
	index = toPrivate(index);
	Event result = (Event) current.elementAt(index);
	current.removeElementAt(index);
	return result;
    }

    public Event get (int index) {
	index = toPrivate(index);
	if (index >= 0 && index < current.size()) {
	    return (Event) current.elementAt(index);
	} else {
	    return null;
	}
    }

    public EventSequence subSequence (int from) {
	return subSequence(from, current.size());
    }

    public EventSequence subSequence (int from, int to) {
	int f = toPrivate(from);
	int t = toPrivate(to);
	if (f < 0 || t > current.size()) {
	    throw new IllegalArgumentException("Out of range: from=" + from +
					       ", to=" + to);
	}
	return new SubSequence(from, to);
    }

    public Enumeration events () {
	return new ListEnumerator(this, getSmallestActiveIndex(),
				  getLargestActiveIndex() + 1);
    }

    public void forgetUntil (int index) {
	int large = toPrivate(index);
	if (index == smallestIndex) {
	    return;
	} else if (large == current.size()) {
	    forget();
	} else if (index > smallestIndex && large < current.size()) {
	    int size = current.size() - large;
	    Vector result = new Vector(size);
	    for (int i = 0; i < size; i++) {
		result.addElement(current.elementAt(i + index));
	    }
	    current = result;
	    smallestIndex = index;
	} else {
	    throw new IllegalArgumentException("Index " + index
					       + " out of range");
	}
    }

    public void forget () {
	smallestIndex += current.size();
	current.removeAllElements();
    }

    public int getSmallestActiveIndex () {
	return fromPrivate(0);
    }

    public int getLargestActiveIndex () {
	return fromPrivate(current.size() - 1);
    }

    /**
     * Returns whether this object is equal to some other object.  The
     * argument object need not be of class <code>EventList</code>; it
     * is sufficent that it implement the {@link EventSequence}
     * interface, in which case this method returns the same value as
     * calling {@link XasUtil#sequenceEquals} with arguments
     * <code>this</code> and <code>o</code> would.
     *
     * @param o the object to compare for equality
     * @return whether <code>o</code> is equal to this object
     */
    public boolean equals (Object o) {
	boolean result = false;
	if (o instanceof EventSequence) {
	    result = XasUtil.sequenceEquals(this, (EventSequence) o);
	}
	return result;
    }

    public int hashCode () {
	return XasUtil.sequenceHashCode(this);
    }

    public String toString () {
	return current.toString();
    }

    private class SubSequence implements EventSequence {

	private EventList parent;
	private int originalLow;
	private int high;
	private int low;

	private boolean check (int index) {
	    int value = translate(index);
	    return value >= low && value < high;
	}

	private int translate (int index) {
	    return index + originalLow;
	}

	public SubSequence (int low, int high) {
	    this.parent = EventList.this;
	    this.originalLow = this.low = low;
	    this.high = high;
	}

	public Event get (int index) {
	    if (!check(index)) {
		return null;
	    }
	    return parent.get(translate(index));
	}

	public EventSequence subSequence (int from, int to) {
	    if (!check(from) || !check(to - 1)) {
		throw new IllegalArgumentException("Out of range: from=" + from
						   + ",to=" + to);
	    }
	    return parent.subSequence(translate(from), translate(to));
	}

	public Enumeration events () {
	    return new ListEnumerator(parent, low, high);
	}

	public void forgetUntil (int index) {
	    low = index;
	}

	public void forget () {
	    low = high;
	}

	public int getSmallestActiveIndex () {
	    return low;
	}

	public int getLargestActiveIndex () {
	    return high - 1;
	}

	public boolean equals (Object o) {
	    boolean result = false;
	    if (o instanceof EventSequence) {
		result = XasUtil.sequenceEquals(this, (EventSequence) o);
	    }
	    return result;
	}

	public int hashCode () {
	    return XasUtil.sequenceHashCode(this);
	}

	public String toString () {
	    Vector result = new Vector(high - low);
	    for (int i = 0; i < high - low; i++) {
		result.addElement(get(i));
	    }
	    return result.toString();
	}

    }

    private static class ListEnumerator implements Enumeration {

	private EventSequence seq;
	private int index;
	private int high;

	public ListEnumerator (EventSequence seq, int low, int high) {
	    this.seq = seq;
	    this.index = low;
	    this.high = high;
	}

	public boolean hasMoreElements () {
	    return index < high;
	}

	public Object nextElement () {
	    if (index < high) {
		return seq.get(index++);
	    } else {
		throw new NoSuchElementException("List exhausted");
	    }
	}

    }

}
