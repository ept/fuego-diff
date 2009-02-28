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
import fuegocore.util.Queue;

/**
 * A default DOA machine implementation.  This class is used as the
 * default {@link DoaMachine} by {@link XebuParser} if no {@link
 * DoaMachine} has been installed by the application.  Since the
 * {@link DoaMachine} interface is quite complex, and commonly only
 * {@link #nextEvent} and possibly {@link #promiseEvent} require
 * special treatment, this class is also suitable as a base class for
 * other {@link DoaMachine} implementations.
 */
public class IdentityDoaMachine implements DoaMachine {

    /**
     * The queue from which the transformed event stream is extracted.
     * The event access methods of this class read the events from the
     * head of this queue.  A subclass should insert any events to
     * this queue in {@link #nextEvent}.
     */
    protected Queue queue = new Queue();

    public boolean hasEvents () {
	return !queue.empty();
    }

    public Event getEvent () {
	Event result = null;
	if (!queue.empty()) {
	    result = (Event) queue.dequeue();
	}
	return result;
    }

    public Event peekEvent () {
	Event result = null;
	if (!queue.empty()) {
	    result = (Event) queue.peek();
	}
	return result;
    }

    /**
     * Insert the event into the internal queue.  This method simply
     * inserts the provided event into the queue.
     *
     * @param ev the event to insert into the queue.
     */
    public void nextEvent (Event ev) {
	queue.enqueue(ev);
    }

    /**
     * A no-op method.  This method does nothing, since the identity
     * mapping does not require promises.
     */
    public void promiseEvent (int type) {
    }

}
