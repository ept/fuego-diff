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
 * An interface for transforming an XML output stream.  The methods in
 * this interface are used to transform a stream of XML events into a
 * different stream.  The object is passed the events one at a time
 * and it will return the events to output as it is passed the input
 * events.
 */
public interface EoaMachine {

    /**
     * Get the next event to output.  The argument event of this
     * method is the next event in the stream to be transformed and
     * the returned event is the next one to output.  If the returned
     * event is <code>null</code>, no output is to be performed yet.
     *
     * @param ev the next event in the input stream
     * @return the next event in the output stream, or
     * <code>null</code> if that cannot yet be determined
     */
    Event nextEvent (Event ev);

    /**
     * Return whether the current state is the initial state.  When
     * events are fed into an EOA machine, the machine's state
     * presumably changes to accommodate the omission of events.  In
     * some applications it is important to know whether the machine
     * is in the middle of some processing or whether it is behaving
     * normally.  The caller can then adjust its own behavior based on
     * the machine's current state.
     *
     * @return <code>true</code> if the machine is in its initial
     * state, <code>false</code> otherwise
     */
    boolean isInitialState ();

}
