/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.transform;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import fc.xml.xas.Item;
import fc.xml.xas.ItemList;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.Text;

/**
 * Event sequence suitable for reading data-oriented XML. The class strips out events that in
 * frequently are of no significance to data-oriented XML.
 * <p>
 * These stripped events are {@link Item#COMMENT}, {@link Item#PI}. {@link Item#TEXT} consisting of
 * only whitespace are also stripped by default. The {@link Item#START_DOCUMENT} and
 * {@link Item#END_DOCUMENT} events may optionally be stripped.
 */

public class DataItems implements ItemTransform {

    protected boolean normalizeWhitespace = true;
    protected boolean stripDocumentStartEnd = false;

    protected Queue<Item> queue = new LinkedList<Item>();

    // Preamble = all stuff except SD until first SE
    protected ItemList preamble = new ItemList();
    protected boolean sawFirstSE = false;


    public DataItems() {
    }


    public DataItems(boolean normalizeWhitespace, boolean stripDocumentStartEnd) {
        this.normalizeWhitespace = normalizeWhitespace;
        this.stripDocumentStartEnd = stripDocumentStartEnd;
    }


    public ItemList getPreamble() {
        return preamble;
    }


    public boolean hasItems() {
        return !queue.isEmpty();
    }


    public Item next() throws IOException {
        return queue.poll();
    }


    public void append(Item i) throws IOException {
        if (!sawFirstSE) {
            sawFirstSE = Item.isStartTag(i);
            if (!sawFirstSE && !Item.isStartDocument(i)) preamble.append(i);
        }
        // Ignore comments
        if (i.getType() == Item.COMMENT) return;
        // Ignore whitespace
        else if (Item.isText(i) && normalizeWhitespace && isWhiteSpace(((Text) i).getData())) return;
        // Ignore PIs
        else if (i.getType() == Item.PI) {
            return;
        } else if ((i.getType() == Item.START_DOCUMENT || i.getType() == Item.END_DOCUMENT) &&
                   stripDocumentStartEnd) return;
        // Passed filter
        queue.offer(i);
    }


    protected final boolean isWhiteSpace(String s) {
        int pos = 0;
        int len = s.length();
        if (len == 0) return true;
        for (; pos < len; pos++)
            if (s.charAt(pos) > ' ' && !Character.isWhitespace(s.charAt(pos))) return false;
        return true;
    }

}

// arch-tag: c80e5351-ef27-4093-99d3-040a630dbd5d
