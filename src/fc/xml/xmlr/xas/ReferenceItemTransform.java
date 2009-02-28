/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.xas;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import fc.xml.xas.Item;
import fc.xml.xas.ItemTransform;

/**
 * Transform that decodes reference items.
 */
public class ReferenceItemTransform implements ItemTransform {

    protected Queue<Item> queue = new LinkedList<Item>();


    public boolean hasItems() {
        return !queue.isEmpty();
    }


    public Item next() throws IOException {
        return queue.poll();
    }


    public void append(Item item) throws IOException {
        Item i = RefItem.decode(item);
        if (i != null) // BUGFIX-20070625-1: Do not emit </ref:tree> to queue
            queue.offer(i);
    }

}

// arch-tag: 0d1fba1e-7093-42e7-b667-40d89d281b72
