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

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A class for viewing an event sequence through a transformation.
 * This class wraps an existing {@link EventSequence}, transforming
 * each event read from the sequence using a specified method.  This
 * allows implementing e.g. a filtered sequence where events of a
 * specific type are deleted.
 *
 * <p>By default the view of the underlying sequence is the identity.
 * The intent is that this class is subclassed by some class
 * overriding the {@link #transform} method with its own desired
 * policy.
 */
public class TransformedEventStream implements EventSequence {

    private XmlReader xr;
    private EventSequence es;
    private EventList el = new EventList();

    /**
     * Flag telling whether to acquire more events from the underlying
     * sequence.  A subclass may set this to <code>false</code> in the
     * {@link #transform} method to indicate that no further events
     * should be read from the underlying sequence.
     */
    protected boolean inProgress = true;

    private boolean fillUntil (int index) {
	Event ev;
	boolean read = false;
	while (inProgress
	       && (index < 0 || el.getLargestActiveIndex() < index)
	       && (ev = xr.advance()) != null) {
	    read = true;
	    transform(ev, el, xr);
	}
	if (read) {
	    while (xr.getCurrentPosition() <= es.getLargestActiveIndex()
		   && (ev = xr.advance()) != null) {
		transform(ev, el, xr);
	    }
	    if (xr.getCurrentPosition() > es.getLargestActiveIndex()) {
		es.forget();
	    }
	}
	return el.getLargestActiveIndex() >= index;
    }

    /**
     * Insert an event with transformation to the result sequence.
     * This method just adds the given {@link Event} object at the end
     * of the given {@link EventList}.
     *
     * <p>A subclass overriding this method may transform the provided
     * {@link Event} object into the provided {@link EventList} in any
     * manner it wishes.  So one event can turn into zero, one, or
     * more events, possibly vanishing itself in the process while
     * still generating some events.
     *
     * <p>While it is possible to manipulate the provided {@link
     * EventList} with methods other than {@link
     * EventList#add(Event)}, this is not recommended as it may create
     * inconsistencies (e.g. if the {@link #events} method has been
     * called somewhere).
     *
     * @param ev the next event from the underlying sequence
     * @param el the list to which the transformed event is inserted
     * @param xr the reader from which <code>ev</code> came; the
     * cursor is immediately following the spot of <code>ev</code>
     */
    protected void transform (Event ev, EventList el, XmlReader xr) {
	el.add(ev);
    }

    /**
     * The default constructor.
     *
     * @param es the event sequence to wrap
     */
    public TransformedEventStream (EventSequence es) {
	this.es = es;
	xr = new XmlReader(es);
    }

    public void reset (EventSequence es) {
	this.es = es;
	xr.reset(es);
	el.reset();
    }

    public Event get (int index) {
	fillUntil(index);
	return el.get(index);
    }

    public EventSequence subSequence (int from, int to) {
	fillUntil(to - 1);
	return el.subSequence(from, to);
    }

    public Enumeration events () {
	return new EventEnumerator();
    }

    public void forgetUntil (int index) {
	el.forgetUntil(index);
    }

    public void forget () {
	el.forget();
    }

    public int getSmallestActiveIndex () {
	return el.getSmallestActiveIndex();
    }

    public int getLargestActiveIndex () {
	return el.getLargestActiveIndex();
    }

    /**
     * Returns whether this object is equal to some other object.  The
     * argument object need not be of class
     * <code>TransformedEventStream</code>; it is sufficent that it
     * implement the {@link EventSequence} interface, in which case
     * this method returns the same value as calling {@link
     * XasUtil#sequenceEquals} with arguments <code>this</code> and
     * <code>o</code> would.
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
	fillUntil(-1);
	return el.toString();
    }

    private class EventEnumerator implements Enumeration {

	private int index = 0;

	public boolean hasMoreElements () {
	    return el.getLargestActiveIndex() >= index || fillUntil(index);
	}

	public Object nextElement () {
	    if (fillUntil(index)) {
		return el.get(index++);
	    } else {
		throw new NoSuchElementException("EventStream exhausted");
	    }
	}

    }

}
