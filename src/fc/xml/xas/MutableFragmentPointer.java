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

import fc.util.log.Log;

public class MutableFragmentPointer extends FragmentPointer implements MutablePointer {

    private XasFragment xasFragment;


    protected MutableFragmentPointer(XasFragment fragment, int index) {
        super(fragment, index);
        xasFragment = fragment;
    }


    public MutableFragmentPointer(FragmentPointer pointer) {
        this((XasFragment) pointer.fragment, pointer.index);
    }


    public void set(Item item) {
        xasFragment.set(index, item);
    }


    public void insert(Item item) {
        xasFragment.convert(index);
        xasFragment.insert(index + 1, item);
    }


    public void insertFirstChild(Item item) {
        if (!xasFragment.convert(index)) { throw new IllegalStateException(
                                                                           "Pointer not at StartTag"); }
        XasFragment frag = (XasFragment) get();
        if (Log.isEnabled(Log.TRACE)) {
            Log.log("Fragment", Log.TRACE, frag);
        }
        frag.insert(1, item);
    }


    public Item delete() {
        Item result = get();
        if (Item.isStartTag(result)) {
            if (!xasFragment.convert(index)) { throw new IllegalStateException(
                                                                               "Conversion of item " +
                                                                                       result +
                                                                                       " failed"); }
            result = get();
        } else if (Item.isEndTag(result)) {
            if (Log.isEnabled(Log.TRACE)) {
                Log.log("Fragment", Log.TRACE, this);
                Log.log("Subfragment", Log.TRACE, xasFragment.subFragment(index - 2, 3));
            }
            throw new IllegalStateException("Attempt to destroy structure at item " + result);
        }
        return xasFragment.delete(index);
    }


    public void move(MutablePointer target) {
        if (!(target instanceof FragmentPointer)) { throw new IllegalArgumentException("Pointer " +
                                                                                       target +
                                                                                       " not pointing at a fragment"); }
        FragmentPointer fp = (FragmentPointer) target;
        if (fp.fragment != this.fragment) { throw new IllegalArgumentException(
                                                                               "Pointer pointing to wrong fragment"); }
        xasFragment.move(index, fp.index);
    }


    public void moveFirstChild(MutablePointer target) {
        if (!(target instanceof FragmentPointer)) { throw new IllegalArgumentException("Pointer " +
                                                                                       target +
                                                                                       " not pointing at a fragment"); }
        // System.out.println("moveFirstChild(" + this + "," + target + ")");
        MutableFragmentPointer fp = (MutableFragmentPointer) target;
        if (fp.fragment != this.fragment) { throw new IllegalArgumentException(
                                                                               "Pointer pointing to wrong fragment"); }
        // System.out.println(index);
        // System.out.println(fragment.subFragment(index - 1, 2));
        Item i = delete();
        // System.out.println(i);
        fp.insertFirstChild(i);
    }


    public void setPosition(int pos) {
        Verifier.checkSmallerEqual(0, pos);
        Verifier.checkSmallerEqual(pos, fragment.length());
        index = pos;
    }


    public XasFragment subFragment(int length) {
        return xasFragment.subFragment(index, length);
    }


    public StartTag getContext() {
        Item item = get();
        while (FragmentItem.isFragment(item)) {
            item = ((FragmentItem) item).get(0);
        }
        if (Item.isStartTag(item)) {
            return ((StartTag) item).getContext();
        } else {
            throw new IllegalStateException("Pointer not at start tag");
        }
    }


    public boolean inside(FragmentPointer pointer) {
        if (fragment == pointer.fragment && index <= pointer.index) {
            return pointer.index < index + size();
        } else {
            return false;
        }
    }


    int size() {
        Item item = get();
        if (Item.isStartTag(item)) {
            if (!xasFragment.convert(index)) { throw new IllegalStateException(
                                                                               "Conversion of item " +
                                                                                       item +
                                                                                       " failed"); }
            item = get();
        }
        if (FragmentItem.isFragment(item)) {
            return ((FragmentItem) item).getSize();
        } else {
            return 1;
        }
    }


    public Iterator<Item> childIterator() {
        if (xasFragment.convert(index)) {
            XasFragment frag = (XasFragment) get();
            return new FragmentIterator(frag, 1, frag.getSize() - 2);
        } else if (Item.isStartDocument(get())) {
            return new FragmentIterator(fragment, index + 1);
        } else {
            return new FragmentIterator(fragment, index, 0);
        }
    }


    public Pointer query(int[] path) {
        return xasFragment.query(new MutableFragmentPointer(xasFragment, index), path);
    }


    public void advanceLevel() {
        Verifier.checkSmaller(index, fragment.length());
        int step = 1;
        Item item = fragment.get(index);
        if (FragmentItem.isFragment(item)) {
            step = ((FragmentItem) item).getSize();
        } else if (Item.isStartTag(item)) {
            xasFragment.convert(index);
            step = ((FragmentItem) fragment.get(index)).getSize();
        }
        index += step;
    }

}

// arch-tag: c7484bb1-79fd-43bb-beee-e5cf1853dfef
