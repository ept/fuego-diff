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

import fuegocore.util.xas.Event;

/**
 * An interface for transforming an XML input stream.  The methods in
 * this interface are used to transform a stream of XML events into a
 * different stream.  The object is passed the events one at a time
 * and the transformed stream is pulled from the object as it is
 * built.
 */
public interface DoaMachine {

    /**
     * Check whether there are any events to read.  Since a single
     * input event may be transformed into several output events, the
     * output events need to be pulled in a loop for each input event.
     *
     * @return whether there are any stored events, i.e. whether
     * {@link #getEvent} would return a non-<code>null</code> value
     */
    boolean hasEvents ();

    /**
     * Get the next event in the transformed stream.  The returned
     * event is removed from the stored events.  This method will
     * periodically return <code>null</code> to indicate that new
     * events need to be fed in with {@link #nextEvent}.
     *
     * @return the next event from the transformed stream, or
     * <code>null</code> if there are no stored events
     */
    Event getEvent ();

    /**
     * Look at the next event in the transformed stream.  This method
     * will return but not remove the next event in the transformed
     * stream.  Like with {@link #getEvent}, <code>null</code> is
     * returned when no events are stored.
     *
     * @return the next event from the transformed stream, or
     * <code>null</code> if there are no stored events
     */
    Event peekEvent ();

    /**
     * Add an event to the stream to be transformed.  When the stored
     * events are exhausted, this method needs to be called to
     * possibly get more events.
     *
     * @param ev the next event in the stream to be transformed
     */
    void nextEvent (Event ev);

    /**
     * Promise that the next event will be of specified type.  In some
     * cases the DOA machine will insert events in front of an event
     * passed with {@link #nextEvent}.  Occasionally these inserted
     * events are needed by the parser before the actual event to pass
     * can be constructed.  This method is to be called in these
     * situations with the event type that would be passed by the next
     * call {@link #nextEvent}, so that the parser can get any
     * machine-inserted events before constructing the actual event.
     *
     * <p>If the type passed to this method is not the same as the
     * type of the event passed to the next {@link #nextEvent} call,
     * behaviour is undefined.  Also, this method may not be called
     * twice in succession without an interspersed call to {@link
     * #nextEvent}, not even if the passed type is the same on both
     * calls.
     *
     * <p>This method is in general assumed to be a no-op.  If it is
     * not so, an implementing class is expected to document the cases
     * where this method needs to be called.
     *
     * @param type the event type that will be passed next; one of the
     * event type constants in {@link Event}.
     */
    void promiseEvent (int type);

}
