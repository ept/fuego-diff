/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import java.util.Enumeration;
import java.util.NoSuchElementException;

import fuegocore.util.Util;

/**
 * A class for viewing untyped sequences as typed ones. This class wraps an existing
 * {@link EventSequence}, transforming its content to {@link Event#TYPED_CONTENT} events on the way.
 * This is mostly useful for reading because in that case the parser is often supplied and cannot be
 * modified to understand typed content.
 * <p>
 * In detail, what this class does is wrap the {@link Enumeration} returned by
 * {@link EventSequence#events} into a new {@link Enumeration} that reads typing information from
 * element attributes and passes this information to its {@link ContentDecoder}, which presumably
 * knows how to decode the content.
 * <p>
 * Any methods, apart from {@link #events} just delegate to the underlying sequence, so their
 * semantics are the same as those of the underlying sequence.
 */
public class TypedEventStream implements EventSequence {

    private XmlReader reader;
    private EventSequence es;
    private ContentDecoder decoder;
    private EventList el = new EventList();

    private int depth = 0;
    private boolean insideStart = false;
    private String nextNamespace = null;
    private String nextName = null;
    private EventList currentAttributes = new EventList();
    private Event cachedEvent = null;
    private Event typeAttribute = null;
    private String[] namespaceStack = new String[8];
    private int[] namespaceCounts = new int[4];


    private void dumpStack() {
        System.out.println("Depth:  " + depth);
        System.out.print("Counts: [");
        for (int i = 0; i < namespaceCounts.length; i++) {
            if (i > 0) {
                System.out.print(", ");
            }
            System.out.print(namespaceCounts[i]);
        }
        System.out.println("]");
        System.out.print("Stack: [");
        for (int i = 0; i < namespaceStack.length; i++) {
            if (i > 0) {
                System.out.print(",");
            }
            System.out.print(namespaceStack[i]);
        }
        System.out.println("]");
    }


    private String getNamespace(String prefix) {
        // System.out.println("Namespace for prefix " + prefix);
        // dumpStack();
        for (int i = 2 * (namespaceCounts[depth] - 1); i >= 0; i -= 2) {
            if (Util.equals(prefix, namespaceStack[i + 1])) { return namespaceStack[i]; }
        }
        return decoder.mapNamespace(prefix);
    }


    private void namespacePrefix(String namespace, String prefix) {
        // System.out.println("Namespace " + namespace + ", prefix " + prefix);
        int current = namespaceCounts[depth + 1];
        namespaceStack = Util.ensureCapacity(namespaceStack, 2 * (current + 1));
        namespaceStack[2 * current] = namespace;
        namespaceStack[2 * current + 1] = prefix;
        namespaceCounts[depth + 1] += 1;
        decoder.insertPrefixMapping(namespace, prefix);
    }


    private Event scrollEvent() {
        Event ev = reader.advance();
        // System.out.println("Scrolling " + ev);
        if (ev != null) {
            if (ev.getType() == Event.START_ELEMENT) {
                depth += 1;
            } else if (ev.getType() == Event.END_ELEMENT) {
                depth -= 1;
            }
        }
        return ev;
    }


    private Event postDecodeProcess(Event ev) {
        if (!currentAttributes.isEmpty()) {
            // System.out.println("Giving attribute");
            cachedEvent = ev;
            ev = currentAttributes.remove(0);
        } else if (ev.getType() == Event.START_ELEMENT) {
            insideStart = true;
            namespaceCounts = Util.ensureCapacity(namespaceCounts, depth + 2);
            namespaceCounts[depth + 1] = namespaceCounts[depth];
        } else {
            insideStart = false;
        }
        return ev;
    }


    private Event nextEvent() {
        // System.out.println("nextEvent called");
        Event ev = null;
        if (cachedEvent != null) {
            if (!currentAttributes.isEmpty()) {
                ev = currentAttributes.remove(0);
            } else {
                Event result = cachedEvent;
                cachedEvent = null;
                ev = postDecodeProcess(result);
            }
        } else {
            ev = scrollEvent();
            // System.out.println(ev);
            if (ev != null) {
                if (ev.getType() == Event.NAMESPACE_PREFIX) {
                    namespacePrefix(ev.getNamespace(), (String) ev.getValue());
                }
                if (ev.getType() == Event.ATTRIBUTE) {
                    if (Util.equals(ev.getNamespace(), XasUtil.XSI_NAMESPACE) &&
                        Util.equals(ev.getName(), "type")) {
                        String type = (String) ev.getValue();
                        int i = type.indexOf(':');
                        String typePrefix = i >= 0 ? type.substring(0, i) : "";
                        nextName = type.substring(i + 1);
                        nextNamespace = getNamespace(typePrefix);
                        typeAttribute = ev;
                    } else {
                        currentAttributes.add(ev);
                    }
                    ev = nextEvent();
                } else if (insideStart && nextName != null) {
                    // System.out.println("Decoding");
                    reader.backup();
                    Object value = null;
                    if (ev.getType() == Event.TYPED_CONTENT) {
                        nextNamespace = ev.getNamespace();
                        nextName = ev.getName();
                        value = ev.getValue();
                        reader.advance();
                    } else {
                        value = decoder.decode(nextNamespace, nextName, reader, currentAttributes);
                    }
                    if (value != null) {
                        typeAttribute = null;
                        Event tmp = Event.createTypedContent(nextNamespace, nextName, value);
                        if (currentAttributes.isEmpty()) {
                            ev = tmp;
                        } else {
                            cachedEvent = tmp;
                            ev = currentAttributes.remove(0);
                        }
                    } else {
                        currentAttributes.add(typeAttribute);
                        ev = postDecodeProcess(ev);
                        reader.advance();
                    }
                    nextNamespace = null;
                    nextName = null;
                    insideStart = false;
                } else if (ev.getType() != Event.NAMESPACE_PREFIX) {
                    ev = postDecodeProcess(ev);
                }
            }
        }
        return ev;
    }


    private boolean fillUntil(int index) {
        Event ev;
        boolean read = false;
        while ((index < 0 || el.getLargestActiveIndex() < index) && (ev = nextEvent()) != null) {
            read = true;
            el.add(ev);
        }
        if (read) {
            while (reader.getCurrentPosition() <= es.getLargestActiveIndex() &&
                   (ev = nextEvent()) != null) {
                el.add(ev);
            }
            if (reader.getCurrentPosition() > es.getLargestActiveIndex()) {
                es.forget();
            }
        }
        return el.getLargestActiveIndex() >= index;
    }


    /**
     * Create a new typed event stream.
     * @param es
     *            the underlying event sequence from which to collect events
     * @param decoder
     *            the decoder to use for converting fragments of the event sequence into
     *            {@link Event#TYPED_CONTENT} events.
     */
    public TypedEventStream(EventSequence es, ContentDecoder decoder) {
        this.es = es;
        reader = new XmlReader(es);
        this.decoder = decoder;
    }


    public void reset(EventSequence es, ContentDecoder decoder) {
        this.es = es;
        reader.reset(es);
        this.decoder = decoder;
        el.reset();
        currentAttributes.clear();
    }


    public Event get(int index) {
        fillUntil(index);
        return el.get(index);
    }


    public EventSequence subSequence(int from, int to) {
        fillUntil(to - 1);
        return el.subSequence(from, to);
    }


    public Enumeration events() {
        return new EventEnumerator();
    }


    public void forgetUntil(int index) {
        el.forgetUntil(index);
    }


    public void forget() {
        el.forget();
    }


    public int getSmallestActiveIndex() {
        return el.getSmallestActiveIndex();
    }


    public int getLargestActiveIndex() {
        return el.getLargestActiveIndex();
    }


    /**
     * Returns whether this object is equal to some other object. The argument object need not be of
     * class <code>TypedEventStream</code>; it is sufficent that it implement the
     * {@link EventSequence} interface, in which case this method returns the same value as calling
     * {@link XasUtil#sequenceEquals} with arguments <code>this</code> and <code>o</code> would.
     * @param o
     *            the object to compare for equality
     * @return whether <code>o</code> is equal to this object
     */
    public boolean equals(Object o) {
        boolean result = false;
        if (o instanceof EventSequence) {
            result = XasUtil.sequenceEquals(this, (EventSequence) o);
        }
        return result;
    }


    public int hashCode() {
        return XasUtil.sequenceHashCode(this);
    }


    public String toString() {
        fillUntil(-1);
        return el.toString();
    }

    private class EventEnumerator implements Enumeration {

        private int index = 0;


        public boolean hasMoreElements() {
            return el.getLargestActiveIndex() >= index || fillUntil(index);
        }


        public Object nextElement() {
            if (fillUntil(index)) {
                return el.get(index++);
            } else {
                throw new NoSuchElementException("EventStream exhausted");
            }
        }

    }

}
