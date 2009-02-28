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

/**
 * An event sequence that coalesces all content events.  This {@link
 * EventSequence} implementation takes an underlying {@link
 * EventSequence} and provides a view of that where there are no
 * consecutive {@link Event#CONTENT} events.  This is useful when
 * e.g. using the XML serialization format and there are many escaped
 * characters that would each be returned as separate {@link
 * Event#CONTENT} events.
 */
public class CoalescedEventSequence extends TransformedEventStream {

    private StringBuffer content = new StringBuffer();

    protected void transform (Event ev, EventList el, XmlReader xr) {
	if (ev != null) {
	    if (ev.getType() == Event.CONTENT) {
		Event e = xr.getCurrentEvent();
		boolean looped = false;
		while (e != null && e.getType() == Event.CONTENT) {
		    if (!looped) {
			content.append((String) ev.getValue());
		    }
		    looped = true;
		    content.append((String) e.getValue());
		    xr.advance();
		    e = xr.getCurrentEvent();
		}
		if (looped) {
		    ev = Event.createContent(content.toString());
		    content.setLength(0);
		}
	    }
	    el.add(ev);
	}
    }

    /**
     * Standard constructor.
     *
     * @param es the {@link EventSequence} to wrap and provide content
     * coalescing for
     */
    public CoalescedEventSequence (EventSequence es) {
	super(es);
    }

}
