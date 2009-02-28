/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ItemList implements Iterable<Item>, ItemTarget {

    private List<Item> items = new ArrayList<Item>();


    public ItemList() {

    }


    public ItemList(Item i) {
        this();
        items.add(i);
    }


    public ItemList(Item... initItems) {
        for (Item i : initItems) {
            items.add(i);
        }
    }


    public Item get(int index) {
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        } else {
            throw new IndexOutOfBoundsException("Index " + index + " not inside [0," +
                                                items.size() + ")");
        }
    }


    public void append(Item item) {
        items.add(item);
    }


    public boolean isEmpty() {
        return items.isEmpty();
    }


    public Iterator<Item> iterator() {
        return new ItemIterator();
    }


    public ItemSource source() {
        return new ItemIterator();
    }


    public XasFragment fragment() {
        return new XasFragment(items, items.get(0));
    }


    @Override
    public int hashCode() {
        return items.hashCode();
    }


    public int size() {
        return items.size();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ItemList)) {
            return false;
        } else {
            ItemList l = (ItemList) o;
            return items.equals(l.items);
        }
    }


    @Override
    public String toString() {
        return items.toString();
    }

    private class ItemIterator implements Iterator<Item>, ItemSource {

        private int index = 0;


        public boolean hasNext() {
            return index < items.size();
        }


        public Item next() {
            if (hasNext()) {
                return items.get(index++);
            } else {
                return null;
            }
        }


        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}

// arch-tag: 048bf430-4585-477f-a92f-39e29459b608
