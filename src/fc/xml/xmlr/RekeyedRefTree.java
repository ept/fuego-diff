/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr;

import java.util.Iterator;

import fc.util.Util;

/**
 * reftree with remapped keys. The object is a reftree with the same structure and content as an
 * existing reftree, but with the node keys remapped according to a given keymap.
 */
public class RekeyedRefTree extends AbstractMutableRefTree {

    private KeyMap map;
    private RefTree ft;
    private IdAddressableRefTree ift;
    private MutableRefTree mft;
    private Class LOCAL_KEY_CLASS = LocalKey.class;


    private RekeyedRefTree(RefTree frontTree, KeyMap map) {
        this.map = map;
        ft = frontTree;
        ift = frontTree instanceof IdAddressableRefTree ? (IdAddressableRefTree) ft : null;
        mft = frontTree instanceof MutableRefTree ? (MutableRefTree) ft : null;
    }


    /**
     * Create remapped tree. The remapped tree is considered the front tree, and the original tree
     * the back tree.
     * @param frontTree
     *            tree with keys to remap.
     * @param map
     *            Key map
     * @return re-keyed tree
     */
    public static RefTree create(RefTree frontTree, KeyMap map) {
        return new RekeyedRefTree(frontTree, map);
    }


    /**
     * Create remapped tree. The remapped tree is considered the front tree, and the original tree
     * the back tree.
     * @param frontTree
     *            tree with keys to remap.
     * @param map
     *            Key map
     * @return re-keyed tree
     */
    public static IdAddressableRefTree create(IdAddressableRefTree frontTree, KeyMap map) {
        return new RekeyedRefTree(frontTree, map);
    }


    /**
     * Create remapped tree. The remapped tree is considered the front tree, and the original tree
     * the back tree.
     * @param frontTree
     *            tree with keys to remap.
     * @param map
     *            Key map
     * @return re-keyed tree
     */
    public static MutableRefTree create(MutableRefTree frontTree, KeyMap map) {
        return new RekeyedRefTree(frontTree, map);
    }


    /** @inheritDoc */
    @Override
    public void delete(Key id) throws NodeNotFoundException {
        mft.delete(getExistingLocal(id));
    }


    /** @inheritDoc */
    @Override
    public Key insert(Key parentId, long pos, Key newId, Object content)
            throws NodeNotFoundException {
        assert !(content instanceof Reference) : "ref insert not coded"; // FIXME
        Key lp = getExistingLocal(parentId);
        return LocalKey.create(mft.insert(lp, newId, content), ft);
    }


    /** @inheritDoc */
    @Override
    public Key move(Key nodeId, Key parentId, long pos) throws NodeNotFoundException {
        Key lp = getExistingLocal(parentId);
        Key ln = getExistingLocal(nodeId);
        return mft.move(ln, lp, pos);
    }


    /** @inheritDoc */
    @Override
    public boolean update(Key nodeId, Object content) throws NodeNotFoundException {
        Key ln = getExistingLocal(nodeId);
        return mft.update(ln, content);
    }


    /** @inheritDoc */
    @Override
    public RefTreeNode getNode(Key id) {
        return ift.getNode(getRealKey(id));
    }


    /** @inheritDoc */
    public RefTreeNode getRoot() {
        return wrap(ft.getRoot());
    }


    private Key getExistingLocal(Key id) throws NodeNotFoundException {
        Key local = getRealKey(id);
        if (local == null || !ift.contains(id))
            throw new NodeNotFoundException("Not found (front key=" + local + ")", id);
        return local;
    }


    private Key getRealKey(Key id) {
        if (id == null) return null;
        if (LOCAL_KEY_CLASS == id.getClass()) {
            if (((LocalKey) id).inScope(ft)) return id;
        }
        // Must be a back key
        return map.getFrontKey(id);
    }


    private RefTreeNode wrap(RefTreeNode n) {
        return n == null ? null : new RekeyNode(n);
    }

    private class RekeyNode implements RefTreeNode {

        RefTreeNode n;
        Key cachedKey;


        public RekeyNode(RefTreeNode n) {
            this.n = n;
        }


        public Key getId() {
            if (cachedKey == null) {
                Key bk = n.isReference() ? n.getReference().getTarget() : map.getBackKey(n.getId());
                cachedKey = bk == null ? LocalKey.create(n.getId(), ft) : bk;
            }
            return cachedKey;
        }


        public RefTreeNode getParent() {
            return wrap(n.getParent());
        }


        public Object getContent() {
            return n.getContent();
        }


        public Iterator getChildIterator() {
            final Iterator i = n.getChildIterator();
            return new Iterator() {

                public boolean hasNext() {
                    return i.hasNext();
                }


                public Object next() {
                    return wrap((RefTreeNode) i.next());
                }


                public void remove() {
                    i.remove();
                }

            };
        }


        public Reference getReference() {
            return n.getReference();
        }


        public boolean isReference() {
            return n.isReference();
        }


        public boolean isTreeRef() {
            return n.isTreeRef();
        }


        public boolean isNodeRef() {
            return n.isNodeRef();
        }

    }

    private static class LocalKey implements Key {

        // public static final RefTree BACK_SCOPE = null;

        Key key;
        RefTree scope;


        public static LocalKey create(Key k, RefTree scope) {
            assert scope != null;
            if (k == null) return null;
            else return new LocalKey(k, scope);
        }


        public LocalKey(Key k, RefTree scope) {
            this.key = k;
            this.scope = scope;
        }


        @Override
        public boolean equals(Object o) {
            return o instanceof LocalKey && o != null && Util.equals(scope, ((LocalKey) o).scope) &&
                   Util.equals(key, ((LocalKey) o).key);
        }


        @Override
        public int hashCode() {
            return key.hashCode() ^ scope.hashCode();
        }


        @Override
        public String toString() {
            return "LK(" + key.toString() + "," + System.identityHashCode(scope) + ")";
        }


        public Key getKey() {
            return key;
        }


        public RefTree getScope() {
            return scope;
        }


        public boolean inScope(RefTree t) {
            return /* scope == BACK_SCOPE || */scope == t;
        }
    }

    /**
     * Tree that re-maps reference targets by a keymap. Use with algorithms that do not have a
     * keymap parameter.
     */

    public static class RemapRefsTree implements RefTree {

        private RefTree t;
        private KeyMap map;


        public RemapRefsTree(RefTree t, KeyMap map) {
            this.t = t;
            this.map = map;
        }


        public RefTreeNode getRoot() {
            return wrap(t.getRoot());
        }


        private RefTreeNode wrap(RefTreeNode n) {
            return n == null ? null : new ReRefNode(n);
        }

        private class ReRefNode implements RefTreeNode {

            private RefTreeNode n;


            public ReRefNode(RefTreeNode n) {
                this.n = n;
            }


            public Iterator getChildIterator() {
                final Iterator i = n.getChildIterator();
                return new Iterator() {

                    public boolean hasNext() {
                        return i.hasNext();
                    }


                    public Object next() {
                        return wrap((RefTreeNode) i.next());
                    }


                    public void remove() {
                        i.remove();
                    }

                };
            }


            public Object getContent() {
                return n.getContent();
            }


            public Key getId() {
                return n.getId();
            }


            public RefTreeNode getParent() {
                return wrap(n.getParent());
            }


            public Reference getReference() {
                Reference r = n.getReference();
                if (r == null) return null;
                if (r.isTreeReference()) {
                    return new TreeReference(map.getFrontKey(r.getTarget()));
                } else return new NodeReference(map.getFrontKey(r.getTarget()));
            }


            public boolean isNodeRef() {
                return n.isNodeRef();
            }


            public boolean isReference() {
                return n.isReference();
            }


            public boolean isTreeRef() {
                return n.isTreeRef();
            }

        }
    }
}

// arch-tag: c0aada09-f0aa-4d4f-9e54-c464773f6b36

