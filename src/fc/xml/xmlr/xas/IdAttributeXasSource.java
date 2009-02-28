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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fc.util.IOExceptionTrap;
import fc.util.Util;
import fc.xml.xas.Item;
import fc.xml.xas.Pointer;
import fc.xml.xas.XasUtil;
import fc.xml.xas.index.Document;
import fc.xml.xas.index.GlobalPointer;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.model.KeyIdentificationModel;

/**
 * RandomAccessSource that is navigated by keys read from the "id" attribute.
 */
public abstract class IdAttributeXasSource implements RandomAccessSource<Key> {

    protected Document xd;
    protected GlobalPointer pos;
    protected Iterator<Item> ii = null;
    protected GlobalPointer root = null;
    protected KeyIdentificationModel kim;
    protected IOExceptionTrap trap;


    /**
     * Create new source. The corresponding key identification model is
     * {@link KeyIdentificationModel#ID_AS_STRINGKEY}.
     * @param xd
     *            Xas Document
     * @param root
     *            root id
     */
    public IdAttributeXasSource(Document xd, Key root) {
        this(xd, root, KeyIdentificationModel.ID_AS_STRINGKEY);
    }


    /**
     * Create new source.
     * @param xd
     *            Xas Document
     * @param root
     *            root id
     * @param kim
     *            key identification model
     */
    public IdAttributeXasSource(Document xd, Key root, KeyIdentificationModel kim) {
        this.xd = xd;
        initLookup(xd);
        this.root = lookup(root);
        this.kim = kim;
        pos = this.root;
    }


    public IdAttributeXasSource(Document xd, GlobalPointer root) {
        this(xd, root, KeyIdentificationModel.ID_AS_STRINGKEY);
    }


    public IdAttributeXasSource(Document xd, GlobalPointer root, KeyIdentificationModel kim) {
        this.xd = xd;
        this.root = root;
        pos = this.root;
        this.kim = kim;
        initLookup(xd);
    }


    // Sets root to start tag automagically
    public static GlobalPointer getRootPointer(Document xd) throws MalformedXml {
        GlobalPointer p1 = xd.getRoot();
        assert p1 != null : "Document is missing root";
        GlobalPointer root = null;
        for (Pointer p : Util.iterable(p1.childPointers())) {
            Item i = XasUtil.skipFragment(p.get());
            // Log.debug("Scanning for root tag:",i);
            if (i != null && i.getType() == Item.START_TAG) {
                root = (GlobalPointer) p;
                break;
            }
        }
        if (root == null) throw new MalformedXml("Not root tag found.");
        return root;
    }


    public Key getRoot() {
        try {
            return identify(root);
        } catch (IOException e) {
            trap(e);
        }
        assert false;
        return null;
    }


    public Key getParent(Key k) throws NodeNotFoundException {
        GlobalPointer p = lookup(k);
        if (p == null) throw new NodeNotFoundException();
        try {
            return identify(p.getParent());
        } catch (IOException e) {
            trap(e);
        }
        assert false;
        return null;
    }


    public boolean contains(Key k) {
        return lookup(k) != null;
    }


    public Iterator<Key> getChildKeys(Key k) throws NodeNotFoundException {
        GlobalPointer p = lookup(k);
        if (p == null) throw new NodeNotFoundException();
        final Iterator<GlobalPointer> cpi = p.childPointers();
        return new Iterator<Key>() {

            public boolean hasNext() {
                return cpi.hasNext();
            }


            public Key next() {
                try {
                    return identify(cpi.next());
                } catch (IOException e) {
                    trap(e);
                }
                assert false;
                return null;
            }


            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }


    public void seek(Key k) throws NodeNotFoundException {
        GlobalPointer p = lookup(k);
        if (p == null) throw new NodeNotFoundException();
        pos = p;
        ii = null;
    }


    public Item next() throws IOException {
        return ensureItemIterator().hasNext() ? ii.next() : null;
    }


    protected Iterator<Item> ensureItemIterator() {
        return ii == null ? ii = pos.iterator() : ii;
    }


    public KeyIdentificationModel getKeyIdentificationModel() {
        return kim;
    }


    protected Key identify(GlobalPointer p) throws IOException {
        Item i = XasUtil.skipFragment(p.get());
        return kim.identify(i);
    }


    protected void trap(IOException e) {
        if (trap == null) throw new IOExceptionTrap.RuntimeIOException(e);
        else trap.trap(e);
    }


    protected abstract GlobalPointer lookup(Key id);


    protected abstract void initLookup(Document xd);

    /**
     * Convenience implementation that builds a hashmap index of all ids in memory.
     */
    public static class MemoryIdIndexSource extends IdAttributeXasSource {

        protected static Map<Key, GlobalPointer> index;


        public MemoryIdIndexSource(Document xd, Key root) {
            super(xd, root);
        }


        public MemoryIdIndexSource(Document xd, GlobalPointer root) {
            super(xd, root);
        }


        public MemoryIdIndexSource(Document xd, GlobalPointer root, KeyIdentificationModel kim) {
            super(xd, root, kim);
        }


        public MemoryIdIndexSource(Document xd, Key root, KeyIdentificationModel kim) {
            super(xd, root, kim);
        }


        @Override
        protected GlobalPointer lookup(Key id) {
            return index.get(id);
        }


        @Override
        protected void initLookup(Document xd) {
            index = new HashMap<Key, GlobalPointer>();
            try {
                doInit(xd.getRoot());
            } catch (IOException e) {
                trap(e);
            }
            // Log.debug("Index is ",index);
        }


        protected void doInit(GlobalPointer p) throws IOException {
            index.put(identify(p), p);
            for (Iterator<GlobalPointer> i = p.childPointers(); i.hasNext();)
                doInit(i.next());
        }


        public void close() throws IOException {
            // NOP
        }
    }
}

// arch-tag: 0f5a3f11-f3cd-4376-9aa8-993b5b3d6fc7

