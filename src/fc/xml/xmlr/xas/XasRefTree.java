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

import fc.util.IOExceptionTrap;
import fc.util.log.Log;
import fc.xml.xas.Item;
import fc.xml.xas.XasUtil;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.Reference;

/**
 * RandomAcessSource to Reftree bridge.
 */
public class XasRefTree implements IdAddressableRefTree {

    private IOExceptionTrap trap = IOExceptionTrap.DEFAULT_TRAP;
    private UniformXasCodec tm;
    // NOTE: Do not enable the cache without fixing it -- currently its useless
    // because keys are re-created all the time
    // private SmallLRUCache contentCache = new SmallLRUCache(16);
    private boolean decoderefs = true;
    private PeekableItemSource.OneItemSource ois = new PeekableItemSource.OneItemSource(null);

    protected RandomAccessSource<Key> source;


    /**
     * Create reftree from RandomAccessSource.
     * @param source
     *            source
     * @param tm
     *            codec to use for node content
     */
    public XasRefTree(RandomAccessSource<? extends Key> source, UniformXasCodec tm) {
        this.tm = tm;
        this.source = (RandomAccessSource<Key>) source; // FIXME:
        // semi-genericized
    }


    public RefTreeNode getNode(Key id) {
        return source.contains(id) ? wrap(id) : null;
    }


    public boolean contains(Key id) {
        return source.contains(id);
    }


    public Key getParent(Key nid) throws NodeNotFoundException {
        return source.getParent(nid);
    }


    public Iterator<Key> childIterator(Key id) throws NodeNotFoundException {
        return source.getChildKeys(id);
    }


    public RefTreeNode getRoot() {
        return getNode(source.getRoot());
    }


    public XasNode wrap(Key k) {
        return k == null ? null : new XasNode(k);
    }

    protected class XasNode implements RefTreeNode {

        private Key k;


        public XasNode(Key k) {
            this.k = k;
        }


        public Key getId() {
            return k;
        }


        public RefTreeNode getParent() {
            try {
                return wrap(source.getParent(k));
            } catch (NodeNotFoundException e) {
                trap(e);
            }
            assert false;
            return null;
        }


        public Object getContent() {
            Object content = null; // CcontentCache.get(k); // FIXME:
            // performance suffers if
            // there are several Objects for the same p
            if (content != null) return content;
            // We want to hide any fragment constructs here (like stripping ( (
            // )
            try {
                Item i = getItem(k);
                if (decoderefs) i = RefItem.decode(i);
                if (RefItem.isRefItem(i)) return null;
                if (tm.size() == 1) {
                    ois.refill(i);
                    content = tm.decode(ois, source.getKeyIdentificationModel());
                    // CcontentCache.put(k, content);
                } else {
                    Log.fatal("TreeModels with size!=1 not yet implemented");
                    assert false;
                }
                return content;
            } catch (IOException ex) {
                trap(ex);
            }
            return null;
        }


        public Iterator getChildIterator() {
            try {
                final Iterator<Key> cki = source.getChildKeys(k);
                return new Iterator() {

                    public boolean hasNext() {
                        return cki.hasNext();
                    }


                    public Object next() {
                        return wrap(cki.next());
                    }


                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            } catch (NodeNotFoundException e) {
                trap(e);
            }
            assert false;
            return null;
        }


        public boolean isReference() {
            return RefItem.isRefItem(getItem(k));
        }


        public boolean isTreeRef() {
            return getItem(k).getType() == RefTreeItem.TREE_REFERENCE;
        }


        public boolean isNodeRef() {
            return getItem(k).getType() == RefNodeItem.NODE_REFERENCE;
        }


        public Reference getReference() {
            try {
                return isReference() ? ((RefItem) getItem(k)).createReference(source.getKeyIdentificationModel())
                        : null;
            } catch (IOException e) {
                trap(e);
            }
            assert false;
            return null;
        }

    }


    public void setTrap(IOExceptionTrap trap) {
        this.trap = trap;
    }


    private Item getItem(Key k) {
        try {
            source.seek(k);
            return XasUtil.skipFragment(source.next());
        } catch (IOException e) {
            trap(e);
        } catch (NodeNotFoundException e) {
            trap(e);
        }
        assert false;
        return null;
    }


    protected void trap(Exception e) {
        Log.error("XasRefTree trapped exception", e);
        if (!(e instanceof IOException)) {
            IOException ee = new IOException("Trapped exception");
            ee.initCause(e);
            e = ee;
        }
        if (trap != null) trap.trap((IOException) e);
        else throw new IOExceptionTrap.RuntimeIOException((IOException) e);

    }

}
// arch-tag: d26ef761-9c44-4de8-9330-da943be685e5
