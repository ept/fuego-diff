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

public class FragmentPointer implements Pointer {

    protected FragmentItem fragment;
    protected int index;


    protected FragmentPointer(FragmentItem fragment, int index) {
        this.fragment = fragment;
        this.index = index;
    }


    public FragmentPointer copy() {
        return new FragmentPointer(fragment, index);
    }


    public boolean behind(FragmentPointer pointer) {
        return fragment == pointer.fragment && index > pointer.index;
    }


    public boolean behindEqual(FragmentPointer pointer) {
        return fragment == pointer.fragment && index >= pointer.index;
    }


    public boolean inside(FragmentPointer pointer) {
        if (fragment == pointer.fragment && index <= pointer.index) {
            return pointer.index < index + size();
        } else {
            return false;
        }
    }


    private int size() {
        Item item = get();
        if (Item.isStartTag(item)) {
            int i = index + 1;
            int depth = 1;
            while (depth > 0) {
                item = fragment.get(i);
                if (Item.isStartTag(item)) {
                    depth += 1;
                } else if (Item.isEndTag(item)) {
                    depth -= 1;
                }
                i += 1;
            }
            return i - index;
        } else if (FragmentItem.isFragment(item)) {
            return ((FragmentItem) item).getSize();
        } else {
            return 1;
        }
    }


    public FragmentPointer translate(FragmentPointer source, FragmentPointer target) {
        Verifier.checkNotNull(source);
        Verifier.checkNotNull(target);
        if (fragment == source.fragment) {
            int offset = index - source.index + 1;
            if (target.fragment == source.fragment && target.index > source.index) {
                offset -= 1;
            }
            target.fragment.checkIndex(target.index + offset);
            return new FragmentPointer(target.fragment, target.index + offset);
        } else {
            return this;
        }
    }


    public FragmentPointer translate(int offset) {
        Verifier.checkBetween(0, index + offset, fragment.length());
        if (offset != 0) {
            /*
             * Item item = fragment.get(index + offset); while (isNull(item)) { offset += 1; item =
             * fragment.get(index + offset); }
             */
            return new FragmentPointer(fragment, index + offset);
        } else {
            return this;
        }
    }


    public boolean isSibling(FragmentPointer pointer) {
        return fragment == pointer.fragment;
    }


    public Item get() {
        return fragment.get(index);
    }


    public void canonicalize() {
        Item item = get();
        while (item instanceof XasFragment) {
            fragment = (XasFragment) item;
            index = 0;
            item = get();
        }
    }


    public Pointer query(int[] path) {
        return ((XasFragment) fragment).query(path);
    }


    public Iterator<Item> iterator() {
        return new FragmentIterator(fragment, index);
    }


    public Iterator<Item> iterator(int length) {
        return new FragmentIterator(fragment, index, length);
    }


    public void advance() {
        Verifier.checkSmaller(index, fragment.length());
        int step = 1;
        Item item = fragment.get(index);
        if (FragmentItem.isFragment(item)) {
            step = ((FragmentItem) item).getSize();
        }
        index += step;
    }


    public void advanceLevel() {
        Verifier.checkSmaller(index, fragment.length());
        index += size();
    }


    @Override
    public String toString() {
        return "P(" + index + "," + System.identityHashCode(fragment) + ")";
        // return "P(" + index + ", " + fragment + ")";
    }


    @Override
    public boolean equals(Object o) {
        return o != null &&
               o == this ||
               ((o instanceof FragmentPointer) && ((FragmentPointer) o).fragment == fragment && ((FragmentPointer) o).index == index);
    }


    @Override
    public int hashCode() {
        return index ^ System.identityHashCode(fragment);
    }

}

// arch-tag: 5b2d3de5-5d06-4919-8b68-d88e1c58396c
