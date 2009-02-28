/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.message.encoding;

import fuegocore.util.xas.Event;

/**
 * A default EOA machine implementation. This class is used as the default {@link EoaMachine} by
 * {@link XebuSerializer} if no {@link EoaMachine} has been installed by the application. Like its
 * counterpart {@link IdentityDoaMachine}, this class is intended for use as a common superclass for
 * {@link EoaMachine} implementations. The useful contributions are the introduction of the
 * {@link #state} field and a default implementation of the {@link #isInitialState} method.
 */
public class IdentityEoaMachine implements EoaMachine {

    /**
     * The current state of the machine. In this implementation this field is never updated. A
     * subclass will want to update this in its {@link #nextEvent} method. The value <code>0</code>
     * is the initial state.
     */
    protected int state = 0;


    /**
     * Return the argument. Since <code>IdentityEoaMachine</code> does no transformation to the
     * input stream, the transformed event is always the input event.
     */
    public Event nextEvent(Event ev) {
        return ev;
    }


    /**
     * Return whether the current state is the initial state. For <code>IdentityEoaMachine</code>,
     * this method always returns <code>true</code>, but it is written so that the same
     * implementation may return <code>false</code> for a subclass.
     * @return <code>true</code> if the value of {@link #state} is <code>0</code>,
     *         <code>false</code> otherwise
     */
    public boolean isInitialState() {
        return state == 0;
    }

}
