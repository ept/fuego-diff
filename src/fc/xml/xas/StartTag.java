/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class StartTag extends Item {

    private Qname name;
    private AttributeNode atts;
    private PrefixNode prefixes;
    private PrefixNode initPrefixes;
    private StartTag parent;


    private void insertAttribute(AttributeNode curr, AttributeNode prev, AttributeNode next) {
        Verifier.checkNotEquals(curr, prev);
        if (next == null) {
            prev.setNext(curr);
        } else if (curr.compareTo(next) < 0) {
            curr.setNext(next);
            prev.setNext(curr);
        } else {
            insertAttribute(curr, next, next.getNext());
        }
    }


    private void removeAttribute(AttributeNode curr, AttributeNode prev, Qname name) {
        if (curr != null) {
            if (curr.getName().equals(name)) {
                prev.setNext(curr.getNext());
            } else {
                removeAttribute(curr.getNext(), curr, name);
            }
        }
    }


    private void insertPrefix(PrefixNode curr, PrefixNode prev, PrefixNode next) {
        Verifier.checkNotEquals(curr, prev);
        if (next == null) {
            prev.setNext(curr);
        } else if (next == initPrefixes || curr.compareTo(next) < 0) {
            curr.setNext(next);
            prev.setNext(curr);
        } else {
            insertPrefix(curr, next, next.getNext());
        }
    }


    private void insertPrefix(PrefixNode curr) {
        if (prefixes == null) {
            prefixes = curr;
        } else if (prefixes == initPrefixes || curr.compareTo(prefixes) < 0) {
            curr.setNext(prefixes);
            prefixes = curr;
        } else {
            insertPrefix(curr, prefixes, prefixes.getNext());
        }
    }


    private void insertAttribute(AttributeNode curr) {
        if (atts == null) {
            atts = curr;
        } else if (curr.compareTo(atts) < 0) {
            curr.setNext(atts);
            atts = curr;
        } else {
            insertAttribute(curr, atts, atts.getNext());
        }
    }


    public StartTag(Qname name) {
        this(name, null);
    }


    public StartTag(Qname name, StartTag parent) {
        super(START_TAG);
        Verifier.checkNotNull(name);
        this.name = name;
        this.atts = null;
        this.parent = parent;
        this.initPrefixes = parent != null ? parent.prefixes : null;
        this.prefixes = this.initPrefixes;
    }


    public Qname getName() {
        return name;
    }


    public StartTag getContext() {
        return parent;
    }


    // @NonNull
    public String getPrefix() {
        return getPrefix(name.getNamespace());
    }


    public void addAttribute(Qname name, Object value) {
        insertAttribute(new AttributeNode(name, value));
    }


    public void removeAttribute(Qname name) {
        if (atts != null) {
            if (atts.getName().equals(name)) {
                atts = atts.getNext();
            } else {
                removeAttribute(atts.getNext(), atts, name);
            }
        }
    }


    public void addPrefix(String namespace, String prefix) {
        insertPrefix(new PrefixNode(namespace, prefix));
    }


    // @NonNull
    public String getNamespace(String prefix) {
        Verifier.checkNotNull(prefix);
        String namespace = null;
        if (prefixes != null) {
            namespace = prefixes.getNamespace(prefix);
        } else if (prefix.length() == 0) {
            namespace = "";
        } else {
            throw new IllegalStateException("No namespace found for prefix " + prefix +
                                            " in mapping " + prefixes);
        }
        return namespace;
    }


    // @NonNull
    public String getPrefix(String namespace) {
        Verifier.checkNotNull(namespace);
        String prefix = null;
        if (prefixes != null) {
            prefix = prefixes.getPrefix(namespace);
        } else if (namespace.length() == 0) {
            prefix = "";
        } else {
            throw new IllegalStateException("No prefix found for namespace " + namespace +
                                            " in mapping " + prefixes);
        }
        return prefix;
    }


    public String ensurePrefix(String namespace, String prefix) {
        PrefixNode pn = prefixes;
        while (pn != null) {
            if (pn.getNamespace().equals(namespace)) { return pn.getPrefix(); }
            pn = pn.getNext();
        }
        addPrefix(namespace, prefix);
        return prefix;
    }


    public AttributeNode getAttribute(Qname name) {
        AttributeNode result = atts;
        while (result != null) {
            if (result.getName().equals(name)) {
                break;
            }
            result = result.getNext();
        }
        return result;
    }


    public Object getAttributeValue(Qname name) {
        AttributeNode node = getAttribute(name);
        if (node != null) {
            return node.getValue();
        } else {
            return null;
        }
    }


    public void setAttribute(Qname name, Object value) {
        AttributeNode an = getAttribute(name);
        if (an != null) {
            an.setValue(value);
        } else {
            addAttribute(name, value);
        }
    }


    public Object ensureAttribute(Qname name, Object value) {
        AttributeNode an = atts;
        while (an != null) {
            if (an.getName().equals(name)) { return an.getValue(); }
            an = an.getNext();
        }
        addAttribute(name, value);
        return value;
    }


    public Iterator<PrefixNode> sortedDetachedPrefixes() {
        if (parent != null) {
            if (prefixes != initPrefixes) {
                if (initPrefixes != null) {
                    return new DetachedPrefixIterator(
                                                      new MergePrefixIterator(
                                                                              localPrefixes(),
                                                                              parent.sortedDetachedPrefixes()));
                } else {
                    return allPrefixes();
                }
            } else {
                return parent.sortedDetachedPrefixes();
            }
        } else {
            return allPrefixes();
        }
    }


    public Iterator<PrefixNode> detachedPrefixes() {
        return new DetachedPrefixIterator(allPrefixes());
    }


    public Iterator<PrefixNode> localPrefixes() {
        return new PrefixIterator(prefixes, initPrefixes);
    }


    public Iterator<PrefixNode> allPrefixes() {
        return new PrefixIterator(prefixes, null);
    }


    public Iterator<AttributeNode> attributes() {
        return new AttributeIterator(atts);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof StartTag)) {
            return false;
        } else {
            StartTag st = (StartTag) o;
            if (!name.equals(st.name)) {
                return false;
            } else {
                // XXX - take into account namespaces and attributes
                AttributeNode a1 = null, a2 = null;
                for (a1 = atts, a2 = st.atts; a1 != null && a2 != null; a1 = a1.getNext(), a2 = a2.getNext()) {
                    if (!a1.equals(a2)) { return false; }
                }
                return a1 == null && a2 == null;
            }
        }
    }


    /**
     * Create a copy that has another context. The copy has the same Qname and attributes as this
     * object, but another context. That is, the prefix mappings of this object are not carried over
     * to the copy.
     * <p>
     * Note: The attribute list is not shared between the objects.
     * @param ctx
     *            new context of the tag
     */
    public StartTag withContext(StartTag ctx) {
        StartTag c = new StartTag(getName(), ctx);
        // Copy attributes
        for (Iterator<AttributeNode> i = attributes(); i.hasNext();) {
            AttributeNode a = i.next();
            c.addAttribute(a.getName(), a.getValue());
        }
        return c;
    }


    @Override
    public int hashCode() {
        int result = START_TAG;
        for (AttributeNode a = atts; a != null; a = a.getNext()) {
            result = 37 * result + a.hashCode();
        }
        return result;
    }


    @Override
    public String toString() {
        return "ST(" + name.toString() + " <" + String.valueOf(prefixes) + "> <" +
               String.valueOf(atts) + ">)";
    }

    private static class MergePrefixIterator implements Iterator<PrefixNode> {

        private Iterator<PrefixNode> left;
        private Iterator<PrefixNode> right;
        private PrefixNode leftCurrent;
        private PrefixNode rightCurrent;


        private void updateLeft() {
            if (left.hasNext()) {
                leftCurrent = left.next();
            } else {
                leftCurrent = null;
            }
        }


        private void updateRight() {
            if (right.hasNext()) {
                rightCurrent = right.next();
            } else {
                rightCurrent = null;
            }
        }


        public MergePrefixIterator(Iterator<PrefixNode> left, Iterator<PrefixNode> right) {
            this.left = left;
            this.right = right;
            updateLeft();
            updateRight();
        }


        public boolean hasNext() {
            return leftCurrent != null || rightCurrent != null;
        }


        public PrefixNode next() {
            if (hasNext()) {
                if (leftCurrent != null) {
                    if (rightCurrent != null) {
                        PrefixNode result = null;
                        int c = leftCurrent.compareTo(rightCurrent);
                        if (c < 0) {
                            result = leftCurrent;
                            updateLeft();
                        } else if (c > 0) {
                            result = rightCurrent;
                            updateRight();
                        } else {
                            updateRight();
                            result = next();
                        }
                        return result;
                    } else {
                        PrefixNode result = leftCurrent;
                        updateLeft();
                        return result;
                    }
                } else {
                    PrefixNode result = rightCurrent;
                    updateRight();
                    return result;
                }
            } else {
                throw new NoSuchElementException("No more prefixes");
            }
        }


        public void remove() {
            throw new UnsupportedOperationException("Remove not supported for prefixes");
        }

    }

    private static class DetachedPrefixIterator implements Iterator<PrefixNode> {

        private PrefixNode current;
        private Set<String> seenPrefixes = new HashSet<String>();
        private Iterator<PrefixNode> iterator;


        private void update() {
            while (iterator.hasNext()) {
                current = iterator.next();
                if (!seenPrefixes.contains(current.getPrefix())) {
                    break;
                }
            }
            if (!iterator.hasNext()) {
                current = null;
            }
        }


        public DetachedPrefixIterator(Iterator<PrefixNode> iterator) {
            this.iterator = iterator;
            update();
        }


        public boolean hasNext() {
            return current != null;
        }


        public PrefixNode next() {
            if (hasNext()) {
                PrefixNode result = current;
                seenPrefixes.add(result.getPrefix());
                update();
                return result;
            } else {
                throw new NoSuchElementException("No more prefixes");
            }
        }


        public void remove() {
            throw new UnsupportedOperationException("Remove not supported for prefixes");
        }

    }

    private static class PrefixIterator implements Iterator<PrefixNode> {

        private PrefixNode current;
        private PrefixNode terminator;


        public PrefixIterator(PrefixNode initial, PrefixNode terminator) {
            this.current = initial;
            this.terminator = terminator;
        }


        public boolean hasNext() {
            return current != null && current != terminator;
        }


        public PrefixNode next() {
            if (hasNext()) {
                PrefixNode result = current;
                current = current.getNext();
                return result;
            } else {
                throw new NoSuchElementException("No more prefixes, current=" + current +
                                                 ",terminator=" + terminator);
            }
        }


        public void remove() {
            throw new UnsupportedOperationException("Remove not supported for prefixes");
        }

    }

    private static class AttributeIterator implements Iterator<AttributeNode> {

        private AttributeNode current;


        public AttributeIterator(AttributeNode initial) {
            this.current = initial;
        }


        public boolean hasNext() {
            return current != null;
        }


        public AttributeNode next() {
            if (hasNext()) {
                AttributeNode result = current;
                current = current.getNext();
                return result;
            } else {
                throw new NoSuchElementException("No more attributes");
            }
        }


        public void remove() {
            throw new UnsupportedOperationException("Remove not supported for attributes");
        }

    }

}

// arch-tag: 8b6884cc-8b52-4652-847c-c0644a3ace9f
