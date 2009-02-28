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
import java.util.Iterator;

import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.Item;
import fc.xml.xas.Pointer;
import fc.xml.xas.XasUtil;
import fc.xml.xas.index.Document;
import fc.xml.xas.index.GlobalPointer;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.XmlrDebug;
import fc.xml.xmlr.model.IdentificationModel;
import fc.xml.xmlr.model.KeyIdentificationModel;
import fc.xml.xmlr.model.KeyModel;

/**
 * RandomAccessSource that is navigated by Dewey keys.
 */
public class DeweyXasSource implements RandomAccessSource<DeweyKey> {

    protected Document xd;
    protected GlobalPointer pos;
    protected Iterator<Item> ii = null;
    protected DeweyKey root = DeweyKey.ROOT_KEY;

    // FIXME: This is band-aid, need to make this properly.
    protected KeyIdentificationModel kim = new KeyIdentificationModel(new KeyModel() {

        public Key makeKey(Object s) throws IOException {
            return DeweyKey.createKey(s.toString());
        }
    }, new IdentificationModel() {

        final Key AUTO_KEY = null;


        public Key identify(Item i, KeyModel km) throws IOException {
            return AUTO_KEY;
        }


        public Item tag(Item i, Key k, KeyModel km) {
            return i;
        }

    });


    public DeweyXasSource(Document xd, DeweyKey root) {
        this.xd = xd;
        this.root = root;
        pos = getPointer(root);
    }


    public DeweyXasSource(Document xd) {
        this.xd = xd;
        pos = xd.getRoot();
        root = new DeweyKey(pos.getKey());
    }


    // Sets root to start tag automagically
    public static DeweyXasSource createForRootTag(Document xd) throws MalformedXml {
        GlobalPointer p1 = xd.getRoot();
        assert p1 != null : "Document is missing root";
        DeweyKey root = null;
        for (Pointer p : Util.iterable(p1.childPointers())) {
            Item i = XasUtil.skipFragment(p.get());
            // Log.debug("Scanning for root tag:",i);
            if (i != null && i.getType() == Item.START_TAG) {
                root = new DeweyKey(((GlobalPointer) p).getKey());
                break;
            }
        }
        if (root == null) throw new MalformedXml("Not root tag found.");
        // Log.debug("Automagic root is ",root);
        return new DeweyXasSource(xd, root);
    }


    public DeweyKey getRoot() {
        return root;
    }


    public DeweyKey getParent(Key k) {
        return k == null || !(k instanceof DeweyKey) || root.equals(k) ? null : ((DeweyKey) k).up();
    }


    public Iterator<DeweyKey> getChildKeys(Key k) throws NodeNotFoundException {
        if (!(k instanceof DeweyKey)) throw new NodeNotFoundException(k);
        GlobalPointer p = getPointer((DeweyKey) k);
        if (p == null) throw new NodeNotFoundException(k);
        final Iterator<GlobalPointer> ip = p.childPointers();
        return new Iterator<DeweyKey>() {

            public boolean hasNext() {
                return ip.hasNext();
            }


            public DeweyKey next() {
                return new DeweyKey(ip.next().getKey());
            }


            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

    }


    public void seek(DeweyKey k) throws NodeNotFoundException {
        GlobalPointer p = getPointer(k);
        if (p == null) throw new NodeNotFoundException(k);
        pos = p;
        ii = null;
    }


    public Item next() throws IOException {
        return ensureItemIterator().next();
    }


    public boolean contains(Key k) {
        return k instanceof DeweyKey && (xd.query(((DeweyKey) k).deconstruct()) != null);
    }


    protected Iterator<Item> ensureItemIterator() {
        return ii == null ? ii = pos.iterator() : ii;
    }


    protected GlobalPointer getPointer(DeweyKey k) {
        if (!k.isDescendantSelf(root)) return null;
        return (GlobalPointer) xd.query(k.deconstruct());
    }


    public KeyIdentificationModel getKeyIdentificationModel() {
        return kim;
    }


    public void close() throws IOException {
        // NOP
    }

}

// arch-tag: 9e4fe36d-c38b-4048-a881-25e6cf3954c4

