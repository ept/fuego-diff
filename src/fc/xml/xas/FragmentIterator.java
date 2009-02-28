/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class FragmentIterator implements Iterator<Item> {

    private FragmentItem fragment;
    private int index;
    private int length;


    FragmentIterator(FragmentItem fragment, int index) {
        this(fragment, index, Integer.MAX_VALUE);
    }


    FragmentIterator(FragmentItem fragment, int index, int length) {
        Verifier.checkInterval(0, index, fragment.length());
        Verifier.checkNotNegative(length);
        this.fragment = fragment;
        this.index = index;
        this.length = length;
    }


    public boolean hasNext() {
        return length > 0 && index < fragment.length();
    }


    public Item next() {
        if (hasNext()) {
            Item item = fragment.get(index);
            // FIXME: Need to determine skipping at next run
            // to consider potential convert
            int n = 1;
            if (FragmentItem.isFragment(item)) {
                n = ((FragmentItem) item).getSize();
            }
            index += n;
            length -= n;
            return item;
        } else {
            throw new NoSuchElementException("Fragment exhausted, index=" + index + ", length=" +
                                             length);
        }
    }


    public void remove() {
        throw new UnsupportedOperationException();
    }


    public Pointer pointer() {
        return new FragmentPointer(fragment, index);
    }

}

// arch-tag: ce29e1e3-d27b-4e89-a928-02ff9730bc2c
