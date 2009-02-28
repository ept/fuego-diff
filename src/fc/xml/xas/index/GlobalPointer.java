/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.index;

import java.util.Iterator;

import fc.util.Util;
import fc.xml.xas.MutableFragmentPointer;
import fc.xml.xas.Item;
import fc.xml.xas.MutablePointer;
import fc.xml.xas.Pointer;

/**
 * A pointer to a {@link Document}. This class is a {@link MutablePointer} pointing to a
 * {@link Document}. Compared to {@link MutableFragmentPointer} this class has the benefit that the
 * {@link #move(MutablePointer)} operation only requires that its target point to the same document,
 * and not the same fragment.
 */
public class GlobalPointer implements MutablePointer {

    protected Document document;
    protected DeweyKey key;
    protected MutableFragmentPointer pointer;


    protected GlobalPointer(Document document, DeweyKey key) {
        this(document, key, document.getFragment().pointer());
    }


    GlobalPointer(Document document, DeweyKey key, MutableFragmentPointer pointer) {
        this.document = document;
        this.key = key;
        this.pointer = pointer;
    }


    public Item get() {
        return pointer.get();
    }


    public void set(Item item) {
        pointer.set(item);
    }


    public void insert(Item item) {
        pointer.insert(item);
    }


    public void insertFirstChild(Item item) {
        pointer.insertFirstChild(item);
    }


    public Item delete() {
        return pointer.delete();
    }


    public void move(MutablePointer target) {
        if (!(target instanceof GlobalPointer)) { throw new IllegalArgumentException("Pointer " +
                                                                                     target +
                                                                                     " not pointing at a document"); }
        GlobalPointer gt = (GlobalPointer) target;
        if (gt.document != this.document) { throw new IllegalArgumentException(
                                                                               "GlobalPointer pointing to"
                                                                                       + " wrong document"); }
        if (pointer.isSibling(gt.pointer)) {
            pointer.move(gt.pointer);
        } else {
            gt.insert(delete());
        }
    }


    public void moveFirstChild(MutablePointer target) {
        if (!(target instanceof GlobalPointer)) { throw new IllegalArgumentException("Pointer " +
                                                                                     target +
                                                                                     " not pointing at a document"); }
        GlobalPointer gt = (GlobalPointer) target;
        if (gt.document != this.document) { throw new IllegalArgumentException(
                                                                               "GlobalPointer pointing to"
                                                                                       + " wrong document"); }
        if (pointer.isSibling(gt.pointer)) {
            pointer.moveFirstChild(gt.pointer);
        } else {
            gt.insertFirstChild(delete());
        }
    }


    public Pointer query(int[] path) {
        // FIXME: ctl put some code to ensure a globalpointer is returned
        // JK should check it's ok
        DeweyKey qkey = key;
        // == null && path.length > 0
        // ? DeweyKey.initial() : key;
        for (int i : path) {
            qkey = qkey != null ? qkey.child(i) : DeweyKey.topLevel(i);
        }
        if (qkey != key) {
            MutableFragmentPointer p = (MutableFragmentPointer) pointer.query(path);
            return p == null ? null : new GlobalPointer(document, qkey, p);
        } else {
            return this;
        }
    }


    public void canonicalize() {
        pointer.canonicalize();
    }


    public Iterator<Item> iterator() {
        return pointer.iterator();
    }


    public Iterator<Item> childIterator() {
        return pointer.childIterator();
    }


    // TODO 4JK A fast child pointer iterator
    // ctl thinks this should move to xas.Pointer interface
    public Iterator<GlobalPointer> childPointers() {
        return new Iterator<GlobalPointer>() {

            int childpath[] = { 0 };


            public boolean hasNext() {
                return query(childpath) != null;
            }


            public GlobalPointer next() {
                Pointer p = query(childpath);
                childpath[0]++;
                return (GlobalPointer) p;
            }


            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }


    // TODO ctl code, jkangash should review that this is ok; perhaps even
    // make it faster
    public GlobalPointer getParent() {
        DeweyKey parent = key.up();
        // Log.info("Looking for "+parent+" deconstr=",parent.deconstruct());
        return parent != null ? (GlobalPointer) document.query(parent.deconstruct())
                : document.getRoot();
    }


    // CTL: It seems reasonable that a global pointer can tell its dewey key
    // (since it should always know where in a document it sits...; and
    // indeed does so internally)
    public DeweyKey getKey() {
        return key;
    }


    @Override
    public int hashCode() {
        return (key == null ? 0 : key.hashCode()) ^ System.identityHashCode(document) ^
               pointer.hashCode();
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof GlobalPointer && Util.equals(((GlobalPointer) o).key, key) &&
               ((GlobalPointer) o).document == document &&
               ((GlobalPointer) o).pointer.equals(pointer);
    }


    @Override
    public String toString() {
        return "GP(" + key + "," + System.identityHashCode(document) + "," + pointer + ")";
    }
}

// arch-tag: 9e98b76c-c7d0-4f37-9827-eb80e1893c5b
