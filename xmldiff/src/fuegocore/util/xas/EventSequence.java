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
 * An interface for representing an XML document as a sequence of
 * events.  This interface provides a read-only interface to a
 * sequence of {@link Event} objects representing an XML document
 * (complete or a fragment).  Actual implementations of this interface
 * will provide their own ways of populating a sequence (this could
 * include, for instance, {@link java.util.List}-like additions or
 * parsing data from a {@link java.io.InputStream}).
 */
public interface EventSequence {

    /**
     * Return the event at the specified point in the sequence.
     *
     * @param index the index of the event to return
     * @return the event at the specified index, or <code>null</code>
     * if the index is out of range for this sequence
     */
    Event get (int index);

    /**
     * Return a sequence of events between specified indices.  This
     * method returns a subsequence of this sequence starting at the
     * specified starting index and ending just before the specified
     * ending index.  Implementing classes may indicate the actual
     * type of the returned sequence and what can be done with it.
     *
     * @param from the starting index
     * @param to the ending index
     * @return the list of events in this sequence between
     * <code>from</code> (inclusive) and <code>to</code> (exclusive).
     */
    EventSequence subSequence (int from, int to);

    /**
     * Return an enumeration of the events in this sequence.  All
     * objects returned by the enumeration will be of type {@link
     * Event}.
     */
    Enumeration events ();

    /**
     * Forget an initial sequence of this event sequence.  This method
     * will clear any events with a lower index than that specified.
     * This means, among other things, that {@link #get} will return
     * <code>null</code> for indices lower than the specified one.
     * After calling this method, an immediate call to {@link
     * #getSmallestActiveIndex} will return the specified index.
     *
     * @param index the index before which to forget any gathered
     * events.  This must be at most the value returned by {@link
     * #getLargestActiveIndex}.
     */
    void forgetUntil (int index);

    /**
     * Forget the current active sequence.  This method will clear any
     * events with an index lower than or equal to that returned by
     * {@link #getLargestActiveIndex}.  This is its own method instead
     * of a possibility for the argument of {@link #forgetUntil} to
     * permit more efficient implementation.  After this method is
     * called, no call to the {@link #get} method is guaranteed to
     * return a non-<code>null</code> value.
     */
    void forget ();

    /**
     * Return the smallest index having an associated event.  If this
     * method returns a non-negative value, it is guaranteed that a
     * {@link #get} on that value will return a non-<code>null</code>
     * {@link Event}, and a {@link #get} on smaller values will return
     * <code>null</code>.
     *
     * @return the smallest index having an event in this sequence, or
     * a negative value if no events are known to be available
     */
    int getSmallestActiveIndex ();

    /**
     * Return the largest index having an associated event.  If this
     * method returns a non-negative value, it is guaranteed that a
     * {@link #get} on that value will return a non-<code>null</code>
     * {@link Event}.  Calls to {@link #get} on larger values may
     * return non-<code>null</code> values, but this requires
     * acquiring these events from some outside source.
     *
     * @return the largest index having a known event in this
     * sequence, or a negative value if no events are known to be
     * available
     */
    int getLargestActiveIndex ();

}
