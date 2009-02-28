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

package fuegocore.message.encoding.coa;

import fuegocore.message.encoding.DoaMachine;
import fuegocore.message.encoding.IdentityDoaMachine;
import fuegocore.util.xas.Event;
import fuegocore.util.xas.XasUtil;
import fuegocore.util.Util;

/**
 * An extensible DOA machine for SOAP messages.  This class provides a
 * {@link DoaMachine} specific to SOAP messages.  Extension points are
 * defined at the initial sequence of namespace mappings, at the SOAP
 * header, and at the content of the SOAP body.  {@link DoaMachine}
 * implementations may be provided by applications for any of these.
 *
 * <p>The {@link DoaMachine} for the namespace mapping sequence is
 * provided the {@link Event#START_DOCUMENT} event starting the
 * message.  It is expected to react to this by providing all of its
 * namespace mappings.  It must not include any other events.
 *
 * <p>The {@link DoaMachine} for the SOAP header, if provided,
 * receives events until the {@link Event#END_ELEMENT} event for the
 * SOAP header (this must be included; see {@link SoapEoaMachine}
 * class documentation for reasons).  It needs to begin by emitting a
 * {@link Event#START_ELEMENT} event for the SOAP header, and must
 * eventually emit the {@link Event#END_ELEMENT} event, but other than
 * that the only restriction is that its output sequence describe a
 * well-formed SOAP header.
 *
 * <p>The {@link DoaMachine} for the SOAP body receives all events
 * forming the content of the SOAP body.  This class handles the
 * {@link Event#START_ELEMENT} and {@link Event#END_ELEMENT} events
 * for the body; the provided machine only needs to supply the
 * content.  This content must form a well-formed SOAP body.
 *
 * <p>Any provided machines need to prepare to be called for multiple
 * complete messages in sequence.  In particular this means that any
 * state that they keep must be reset to its initial value after their
 * work is done for a particular message.
 */
public class SoapDoaMachine extends IdentityDoaMachine {

    private DoaMachine nsDoa;
    private DoaMachine headerDoa;
    private DoaMachine bodyDoa;

    private int state = 0;

    private static final Event START_ENVELOPE =
	Event.createStartElement(XasUtil.SOAP_NAMESPACE, "Envelope");
    private static final Event START_HEADER =
	Event.createStartElement(XasUtil.SOAP_NAMESPACE, "Header");
    private static final Event START_BODY =
	Event.createStartElement(XasUtil.SOAP_NAMESPACE, "Body");
    private static final Event END_ENVELOPE =
	Event.createEndElement(XasUtil.SOAP_NAMESPACE, "Envelope");
    private static final Event END_HEADER =
	Event.createEndElement(XasUtil.SOAP_NAMESPACE, "Header");
    private static final Event END_BODY =
	Event.createEndElement(XasUtil.SOAP_NAMESPACE, "Body");

    private void enqueueNs () {
	queue.enqueue(Event.createNamespacePrefix(XasUtil.SOAP_NAMESPACE,
						  "soapenv"));
	queue.enqueue(Event.createNamespacePrefix(XasUtil.XSD_NAMESPACE,
						  "xsd"));
	queue.enqueue(Event.createNamespacePrefix(XasUtil.XSI_NAMESPACE,
						  "xsi"));
    }

    private void getEvents (DoaMachine doa, Event ev, boolean enqueue) {
	if (doa != null) {
	    if (ev != null) {
		doa.nextEvent(ev);
	    }
	    while (doa.hasEvents()) {
		queue.enqueue(doa.getEvent());
	    }
	} else if (enqueue && ev != null) {
	    queue.enqueue(ev);
	}
    }

    private void getEvents (DoaMachine doa, Event ev) {
	getEvents(doa, ev, true);
    }

    /**
     * Construct a machine with no extensions.  The constructed
     * machine will only add SOAP-specific events.
     */
    public SoapDoaMachine () {
	this(null);
    }

    /**
     * Construct a machine with special body decoding.  The
     * constructed machine will pass the content of the SOAP body to
     * the provided machine.
     *
     * @param bodyDoa the {@link DoaMachine} to use for the body of
     * the SOAP message
     */
    public SoapDoaMachine (DoaMachine bodyDoa) {
	this(null, bodyDoa);
    }

    /**
     * Construct a machine with special header and body decoding.  The
     * constructed machine will pass the contents of the SOAP header
     * and the SOAP body to the corresponding machines.
     *
     * @param headerDoa the {@link DoaMachine} to use for the header
     * of the SOAP message
     * @param bodyDoa the {@link DoaMachine} to use for the body of
     * the SOAP message
     */
    public SoapDoaMachine (DoaMachine headerDoa, DoaMachine bodyDoa) {
	this(null, headerDoa, bodyDoa);
    }

    /**
     * Construct a machine with special decoding for all extensible
     * parts.  The constructed machine will pass the namespace
     * mappings, the SOAP header, and the SOAP body to the
     * corresponding machines.
     *
     * @param nsDoa the {@link DoaMachine} to use for any additional
     * namespace mappings at the beginning of the SOAP message
     * @param headerDoa the {@link DoaMachine} to use for the header
     * of the SOAP message
     * @param bodyDoa the {@link DoaMachine} to use for the body of
     * the SOAP message
     */
    public SoapDoaMachine (DoaMachine nsDoa, DoaMachine headerDoa,
			   DoaMachine bodyDoa) {
	this.nsDoa = nsDoa;
	this.headerDoa = headerDoa;
	this.bodyDoa = bodyDoa;
    }

    public Event getEvent () {
	Event result = super.getEvent();
	if (state == 2 && Util.equals(result, END_HEADER)) {
	    queue.enqueue(START_BODY);
	    state = 3;
	}
	//System.out.println("SDM: VALUE=" + result + " IN STATE " + state);
	return result;
    }

    public void nextEvent (Event ev) {
	if (ev != null) {
	    //System.out.println("SDM: event=" + ev + ",state=" + state);
	    switch (state) {
	    case 0:
		queue.enqueue(ev);
		if (ev.getType() == Event.START_DOCUMENT) {
		    enqueueNs();
		    getEvents(nsDoa, ev, false);
		    state = 1;
		}
		break;
	    case 1:
		if (ev.getType() == Event.NAMESPACE_PREFIX) {
		    queue.enqueue(ev);
		} else {
		    queue.enqueue(START_ENVELOPE);
		    if (headerDoa != null || Util.equals(ev, START_HEADER)) {
			getEvents(headerDoa, ev);
			state = 2;
		    } else {
			queue.enqueue(START_BODY);
			getEvents(bodyDoa, ev);
			state = 3;
		    }
		}
		break;
	    case 2:
		getEvents(headerDoa, ev);
		break;
	    case 3:
		if (ev.getType() == Event.END_DOCUMENT) {
		    getEvents(bodyDoa, ev, false);
		    queue.enqueue(END_BODY);
		    queue.enqueue(END_ENVELOPE);
		    queue.enqueue(ev);
		    state = 0;
		} else {
		    getEvents(bodyDoa, ev);
		}
		break;
	    default:
		throw new IllegalStateException("Machine in invalid state "
						+ state);
	    }
	    //System.out.println("SDM: queue=" + queue);
	}
    }

    public void promiseEvent (int type) {
	//System.out.println("SDM: promise type=" + type + ",state=" + state);
	if ((state == 1 || state == 2) && headerDoa != null) {
	    headerDoa.promiseEvent(type);
	    if (state == 1) {
		queue.enqueue(START_ENVELOPE);
		state = 2;
	    }
	    getEvents(headerDoa, null);
	} else if (state == 3 && bodyDoa != null) {
	    bodyDoa.promiseEvent(type);
	    getEvents(bodyDoa, null);
	}
    }

}
