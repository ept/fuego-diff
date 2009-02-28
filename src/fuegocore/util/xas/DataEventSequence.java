/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

// $Id: DataEventSequence.java,v 1.2 2006/03/14 12:21:06 jkangash Exp $
// Moved here on 2004-10-19, before that:
// Id: fuegocore/syxaw/util/EssentialEvents.java,v 1.2 2004/08/18 14:44:43 ctl Exp
package fuegocore.util.xas;

/**
 * Event sequence suitable for reading data-oriented XML. The class strips out events that in
 * frequently are of no significance to data-oriented XML.
 * <p>
 * These stripped events are {@link Event#COMMENT}, {@link Event#PROCESSING_INSTRUCTION}, and
 * {@link Event#NAMESPACE_PREFIX}. {@link Event#CONTENT} consisting of only whitespace are also
 * stripped by default. The {@link Event#START_DOCUMENT} and {@link Event#END_DOCUMENT} events may
 * optionally be stripped.
 */

public class DataEventSequence extends TransformedEventStream {

    protected boolean normalizeWhitespace = true;
    protected boolean stripDocumentStartEnd = false;


    public DataEventSequence(EventSequence in) {
        super(in);
    }


    public DataEventSequence(EventSequence in, boolean normalizeWhitespace,
                             boolean stripDocumentStartEnd) {
        super(in);
        this.normalizeWhitespace = normalizeWhitespace;
        this.stripDocumentStartEnd = stripDocumentStartEnd;
    }


    @Override
    protected void transform(Event ev, EventList el, XmlReader xr) {
        // Ignore comments
        if (ev.getType() == Event.COMMENT) return;
        // Ignore whitespace
        else if (ev.getType() == Event.CONTENT && normalizeWhitespace &&
                 isWhiteSpace((String) ev.getValue())) return;
        // Ignore PIs
        else if (ev.getType() == Event.PROCESSING_INSTRUCTION) return;
        // Ignore namespace mapping events
        else if (ev.getType() == Event.NAMESPACE_PREFIX) return;
        else if ((ev.getType() == Event.START_DOCUMENT || ev.getType() == Event.END_DOCUMENT) &&
                 stripDocumentStartEnd) return;
        // Passed filter
        el.add(ev);
    }


    protected final boolean isWhiteSpace(String s) {
        int len = s.length();
        for (int pos = 0; pos < len; pos++)
            if (s.charAt(pos) > ' ' && !Character.isWhitespace(s.charAt(pos))) return false;
        return true;
    }
}
