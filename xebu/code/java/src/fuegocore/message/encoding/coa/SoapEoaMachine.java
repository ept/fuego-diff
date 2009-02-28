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

import fuegocore.message.encoding.EoaMachine;
import fuegocore.message.encoding.IdentityEoaMachine;
import fuegocore.util.xas.Event;
import fuegocore.util.xas.XasUtil;
import fuegocore.util.Util;

/**
 * An extensible EOA machine for SOAP messages.  This class provides a
 * {@link EoaMachine} specific to SOAP messages.  Extension points are
 * defined at the initial sequence of namespace mappings, at the SOAP
 * header, and at the content of the SOAP body.  {@link EoaMachine}
 * implementations may be provided by applications for any of these.
 *
 * <p>The {@link EoaMachine} for the namespace mapping sequence
 * receives all namespace mappings not recognized by this class.  It
 * may remove any that it recognizes itself.
 *
 * <p>The {@link EoaMachine} for the SOAP header receives all events
 * between the {@link Event#START_ELEMENT} and {@link
 * Event#END_ELEMENT} events for the SOAP header, including these
 * delimiting events.  It must not remove the {@link
 * Event#END_ELEMENT} event, since otherwise the {@link
 * SoapDoaMachine} at the receiving end would not be able to
 * distinguish the separation between the SOAP header and body.
 *
 * <p>The {@link EoaMachine} for the SOAP body receives all events
 * between the {@link Event#START_ELEMENT} and {@link
 * Event#END_ELEMENT} events for the SOAP body, but not the actual
 * delimiters.
 */
public class SoapEoaMachine extends IdentityEoaMachine {

    private EoaMachine nsEoa;
    private EoaMachine headerEoa;
    private EoaMachine bodyEoa;

    private boolean isDroppableNs (Event ev) {
	return (Util.equals(ev.getNamespace(), XasUtil.SOAP_NAMESPACE)
		&& Util.equals(ev.getValue(), "soapenv"))
	    || (Util.equals(ev.getNamespace(), XasUtil.XSD_NAMESPACE)
		&& Util.equals(ev.getValue(), "xsd"))
	    || (Util.equals(ev.getNamespace(), XasUtil.XSI_NAMESPACE)
		&& Util.equals(ev.getValue(), "xsi"));
    }

    /**
     * Construct a machine with no extensions.  The constructed
     * machine will only remove SOAP-specific events.
     */
    public SoapEoaMachine () {
	this(null);
    }

    /**
     * Construct a machine with special body encoding.  The
     * constructed machine will pass the content of the SOAP body to
     * the provided machine.
     *
     * @param bodyEoa the {@link EoaMachine} to use for the body of
     * the SOAP message
     */
    public SoapEoaMachine (EoaMachine bodyEoa) {
	this(null, bodyEoa);
    }

    /**
     * Construct a machine with special header and body encoding.  The
     * constructed machine will pass the contents of the SOAP header
     * and the SOAP body to the corresponding machines.
     *
     * @param headerEoa the {@link EoaMachine} to use for the header
     * of the SOAP message
     * @param bodyEoa the {@link EoaMachine} to use for the body of
     * the SOAP message
     */
    public SoapEoaMachine (EoaMachine headerEoa, EoaMachine bodyEoa) {
	this(null, headerEoa, bodyEoa);
    }

    /**
     * Construct a machine with special encoding for all extensible
     * parts.  The constructed machine will pass the namespace
     * mappings, the SOAP header, and the SOAP body to the
     * corresponding machines.
     *
     * @param nsEoa the {@link EoaMachine} to use for any additional
     * namespace mappings at the beginning of the SOAP message
     * @param headerEoa the {@link EoaMachine} to use for the header
     * of the SOAP message
     * @param bodyEoa the {@link EoaMachine} to use for the body of
     * the SOAP message
     */
    public SoapEoaMachine (EoaMachine nsEoa, EoaMachine headerEoa,
			   EoaMachine bodyEoa) {
	this.nsEoa = nsEoa;
	this.headerEoa = headerEoa;
	this.bodyEoa = bodyEoa;
    }

    public Event nextEvent (Event ev) {
	if (ev != null) {
	    switch (state) {
	    case 0:
		if (ev.getType() == Event.NAMESPACE_PREFIX) {
		    if (isDroppableNs(ev)) {
			ev = null;
		    } else if (nsEoa != null) {
			ev = nsEoa.nextEvent(ev);
		    }
		} else if (ev.getType() == Event.START_ELEMENT
			   && Util.equals(ev.getNamespace(),
					  XasUtil.SOAP_NAMESPACE)
			   && Util.equals(ev.getName(), "Envelope")) {
		    ev = null;
		    state = 1;
		}
		break;
	    case 1:
		if (ev.getType() == Event.START_ELEMENT
		    && Util.equals(ev.getNamespace(),
				   XasUtil.SOAP_NAMESPACE)) {
		    if (Util.equals(ev.getName(), "Header")
			&& headerEoa != null) {
			ev = headerEoa.nextEvent(ev);
			state = 2;
		    } else if (Util.equals(ev.getName(), "Body")) {
			ev = null;
			state = 3;
		    }
		} else if (ev.getType() == Event.END_ELEMENT
			   && Util.equals(ev.getNamespace(),
					  XasUtil.SOAP_NAMESPACE)
			   && Util.equals(ev.getName(), "Envelope")) {
		    ev = null;
		    state = 0;
		}
		break;
	    case 2: {
		boolean stop = false;
		if (ev.getType() == Event.END_ELEMENT
		    && Util.equals(ev.getNamespace(),
				   XasUtil.SOAP_NAMESPACE)
		    && Util.equals(ev.getName(), "Header")) {
		    stop = true;
		}
		ev = headerEoa.nextEvent(ev);
		if (stop) {
		    state = 1;
		}
		break;
	    }
	    case 3:
		if (ev.getType() == Event.END_ELEMENT
		    && Util.equals(ev.getNamespace(),
				   XasUtil.SOAP_NAMESPACE)
		    && Util.equals(ev.getName(), "Body")) {
		    ev = null;
		    state = 1;
		} else if (bodyEoa != null) {
		    ev = bodyEoa.nextEvent(ev);
		}
		break;
	    default:
		throw new IllegalStateException("Machine in invalid state "
						+ state);
	    }
	}
	return ev;
    }

}
