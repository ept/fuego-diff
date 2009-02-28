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

/**
 * An event sequence with constant space use.  This class wraps an
 * existing {@link EventSequence} and keeps track of how many events
 * are in the wrapped sequence.  When the size of the wrapped sequence
 * grows beyond a specified threshold, the sequence's {@link
 * EventSequence#forgetUntil} method is automatically called to bring
 * its size back to a low value.
 *
 * <p>At construction time, both the minimum and maximum values for
 * the number of events are given.  Whenever the number of events
 * exceeds the specified maximum value, it is brought back to the
 * minimum, with the preserved events being the latest ones.
 */
public class ForgetfulEventStream implements EventSequence {

    private EventSequence es;
    private int low;
    private int high;

    /**
     * The default minimum value for the number of events.
     */
    public static final int DEFAULT_LOW = 16;

    /**
     * The default maximum value for the number of events.
     */
    public static final int DEFAULT_HIGH = 64;

    private void checkForForget () {
	if (es.getLargestActiveIndex() - es.getSmallestActiveIndex() >= high) {
	    es.forgetUntil(es.getLargestActiveIndex() - low + 1);
	}
    }

    /**
     * The default constructor.  This constructor sets the minimum and
     * maximum sizes to their default values.
     *
     * @param es the event sequence to wrap
     */
    public ForgetfulEventStream (EventSequence es) {
	this(es, DEFAULT_LOW, DEFAULT_HIGH);
    }

    /**
     * The threshold constructor.  This constructor sets the minimum
     * and maximum sizes to the specified values.
     *
     * @param es the event sequence to wrap
     * @param low the minimum number of events permitted
     * @param high the maximum number of events permitted
     */
    public ForgetfulEventStream (EventSequence es, int low, int high) {
	if (low > high) {
	    throw new IllegalArgumentException("Must have low <= high, got "
					       + "low=" + low + ",high="
					       + high);
	}
	this.es = es;
	this.low = low;
	this.high = high;
    }

    public Event get (int index) {
	Event result = es.get(index);
	checkForForget();
	return result;
    }

    public EventSequence subSequence (int from, int to) {
	EventSequence result = es.subSequence(from, to);
	checkForForget();
	return result;
    }

    public Enumeration events () {
	return es.events();
    }

    public void forgetUntil (int index) {
	es.forgetUntil(index);
    }

    public void forget () {
	es.forget();
    }

    public int getSmallestActiveIndex () {
	return es.getSmallestActiveIndex();
    }

    public int getLargestActiveIndex () {
	return es.getLargestActiveIndex();
    }

}
