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

import fc.util.RingBuffer;
import fc.xml.xas.EntityRef;
import fc.xml.xas.Item;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.Text;

/**
 * Filter that coalesces consecutive content items into one. Optionally, Entity refs may be kept as
 * separate items. <b>Note:</b> Content items after ED will be removed. This is a bug/feature that
 * cannot be bypassed, since we do not know when the underlying item source ends, and thus cannot
 * know when the final combined content item should be output.
 */
// BUGFIX-20070913-1: Fixed broken logic that would cause early end-of-stream
// in some cases
public class CoalesceContent implements ItemTransform {

    protected RingBuffer queue = new RingBuffer();
    protected Item nextDecoded = null;
    protected boolean coalesceEntities = false;


    /** Coalesce all content events. */
    public CoalesceContent() {
        this(true);
    }


    /**
     * Coalesce content events.
     * @param coalesceEntities
     *            <code>true</code> if entity refs should be coalesced
     */
    public CoalesceContent(boolean coalesceEntities) {
        this.coalesceEntities = coalesceEntities;
    }


    public boolean hasItems() {
        return fill() != null;
    }


    public Item fill() {
        if (nextDecoded != null) return nextDecoded;
        if (queue.isEmpty()) return null;
        int pos = 1;
        Item i = (Item) queue.get(0);
        int len = 0;
        if ((coalesceEntities && Item.isContent(i)) || Item.isText(i)) {
            for (Item i2 = null; pos < queue.size() && Item.isContent(i2 = (Item) queue.get(pos)); pos++) {
                // Log.debug("Surveying "+i2+" from" +i);
                if (Item.isText(i2)) len += ((Text) i2).getData().length();
                else if (coalesceEntities && Item.isEntityRef(i2)) len += ((EntityRef) i2).getName().length() + 2;
                else break; // Stop combining
            }
            if (pos == queue.size()) return null; // Cannot decode, since queue ends in content (we
            // need more)
        }
        // Log.debug("Pos, queue= "+pos,queue);
        // Now, position 0..pos contain items to combine
        if (pos == 1) {
            nextDecoded = (Item) queue.poll();
            return nextDecoded;
        }
        StringBuilder sb = new StringBuilder(len);
        for (int j = 0; j < pos; j++) {
            Item i2 = (Item) queue.poll();
            // Log.debug("Appending "+i2);
            if (Item.isText(i2)) sb.append(((Text) i2).getData());
            else {
                sb.append('&');
                sb.append(((EntityRef) i2).getName());
                sb.append(';');
            }
        }
        nextDecoded = new Text(sb.toString());
        return nextDecoded;
    }


    public void append(Item item) throws IOException {
        queue.offer(item);
    }


    public Item next() throws IOException {
        if (nextDecoded == null) fill();
        Item i = nextDecoded;
        nextDecoded = null;
        return i;
    }
}
// arch-tag: 6298af08-d4cb-4e0d-ab63-d75a2fff95e9
//
