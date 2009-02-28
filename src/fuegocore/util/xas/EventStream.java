/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import java.util.Vector;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import org.xmlpull.v1.XmlPullParserException;

import fuegocore.util.Util;

/**
 * An XML sequence that is incrementally compiled from a stream. This class implements the
 * construction of XML event sequences by using a pull parser to read XML events as they are
 * requested by callers of its methods. This makes it possible to process XML documents in a
 * streaming fashion, i.e. process parts before the whole is even available.
 * <p>
 * The only way to get events read from the stream is to request them. If there is a need to get
 * every event out from the stream, the {@link #events} method should be used to get an
 * {@link Enumeration} and iterate through the events.
 */
public class EventStream implements EventSequence {

    private Vector current;
    private TypedXmlParser source;
    private boolean inProgress = true;
    private int smallestIndex = 0;


    private void dumpState() {
        System.out.println("First index: " + smallestIndex);
        System.out.println("Event list: " + current);
    }


    private int toPrivate(int index) {
        return index - smallestIndex;
    }


    private int fromPrivate(int index) {
        return index + smallestIndex;
    }


    private Event entityToEvent(String name) {
        String text = null;
        if (name.charAt(0) == '#') {
            text = source.getText();
        } else if (Util.equals(name, "lt")) {
            text = "<";
        } else if (Util.equals(name, "gt")) {
            text = ">";
        } else if (Util.equals(name, "amp")) {
            text = "&";
        } else if (Util.equals(name, "apos")) {
            text = "'";
        } else if (Util.equals(name, "quot")) {
            text = "\"";
        }
        if (text != null) {
            return Event.createContent(text);
        } else {
            return Event.createEntityReference(name);
        }
    }


    private boolean getNextEvent() {
        boolean result = false;
        if (inProgress) {
            try {
                int eventType = source.getEventType();
                switch (eventType) {
                    case TypedXmlParser.START_DOCUMENT:
                        current.addElement(Event.createStartDocument());
                        break;
                    case TypedXmlParser.END_DOCUMENT:
                        current.addElement(Event.createEndDocument());
                        inProgress = false;
                        break;
                    case TypedXmlParser.START_TAG: {
                        int nsStart = source.getNamespaceCount(source.getDepth() - 1);
                        int nsEnd = source.getNamespaceCount(source.getDepth());
                        for (int i = nsStart; i < nsEnd; i++) {
                            String prefix = source.getNamespacePrefix(i);
                            String uri = source.getNamespaceUri(i);
                            current.addElement(Event.createNamespacePrefix(uri, prefix));
                        }
                        current.addElement(Event.createStartElement(source.getNamespace(),
                                                                    source.getName()));
                        int atts = source.getAttributeCount();
                        for (int i = 0; i < atts; i++) {
                            current.addElement(Event.createAttribute(
                                                                     source.getAttributeNamespace(i),
                                                                     source.getAttributeName(i),
                                                                     source.getAttributeValue(i)));
                        }
                        break;
                    }
                    case TypedXmlParser.END_TAG:
                        current.addElement(Event.createEndElement(source.getNamespace(),
                                                                  source.getName()));
                        break;
                    case TypedXmlParser.TEXT: {
                        current.addElement(Event.createContent(source.getText()));
                        break;
                    }
                    case TypedXmlParser.OBJECT:
                        current.addElement(Event.createTypedContent(source.getNamespace(),
                                                                    source.getName(),
                                                                    source.getObject()));
                        break;
                    case TypedXmlParser.COMMENT:
                        current.addElement(Event.createComment(source.getText()));
                        break;
                    case TypedXmlParser.PROCESSING_INSTRUCTION:
                        current.addElement(Event.createProcessingInstruction(source.getText()));
                        break;
                    case TypedXmlParser.ENTITY_REF:
                        String name = source.getName();
                        current.addElement(entityToEvent(name));
                        break;
                }
                source.nextToken();
                result = true;
            } catch (Exception ex) {
                ex.printStackTrace();
                /*
                 * Just set inProgress to be false, since the document parsed so far may be of
                 * interest anyway.
                 */
                inProgress = false;
            }
        }
        return result;
    }


    private boolean fillUntil(int index) {
        while (inProgress && (index < 0 || current.size() <= toPrivate(index))) {
            getNextEvent();
        }
        return current.size() > toPrivate(index);
    }


    public EventStream(TypedXmlParser source) {
        this.source = source;
        try {
            source.setFeature(TypedXmlParser.FEATURE_PROCESS_NAMESPACES, true);
        } catch (XmlPullParserException ex) {
            // Don't bother; namespaces must be supported
            ex.printStackTrace();
        }
        current = new Vector();
    }


    public void reset(TypedXmlParser source) {
        this.source = source;
        try {
            source.setFeature(TypedXmlParser.FEATURE_PROCESS_NAMESPACES, true);
        } catch (XmlPullParserException ex) {
            // Don't bother; namespaces must be supported
            ex.printStackTrace();
        }
        current.removeAllElements();
        inProgress = true;
        smallestIndex = 0;
    }


    /**
     * Return the event at the specified point in the sequence. If the sequence does not yet contain
     * enough events and the stream has not been exhausted, this method will read additional events
     * from the stream until either the requested event is available or the stream has been
     * exhausted.
     * @param index
     *            the index of the event to return
     */
    public Event get(int index) {
        // System.out.println("ES.get(" + index + ")");
        // dumpState();
        if (fillUntil(index)) {
            return (Event) current.elementAt(toPrivate(index));
        } else {
            return null;
        }
    }


    /**
     * Return a sequence of events between specified indices. This method returns a subsequence of
     * this sequence starting at the specified starting index and ending just before the specified
     * ending index. If there are not enough events already read and the stream has not been
     * exhausted, this method will read additional events from the stream until either enough events
     * are available or the stream has been exhausted.
     * @param from
     *            the starting index
     * @param to
     *            the ending index
     * @return the list of events in this sequence between <code>from</code> (inclusive) and
     *         <code>to</code> (exclusive).
     */
    public EventSequence subSequence(int from, int to) {
        // System.out.println("ES.subSequence(" + from + "," + to + ")");
        fillUntil(to - 1);
        EventList result = new EventList();
        for (int i = from; i < to; i++) {
            result.add((Event) current.elementAt(toPrivate(i)));
        }
        return result;
    }


    /**
     * Return an iterator over the events in this sequence. All objects returned by the iterator
     * will be of type {@link Event}. The returned iterator will cause additional events to be read
     * from the stream into the sequence automatically as they are requested.
     */
    public Enumeration events() {
        return new EventEnumerator();
    }


    public void forgetUntil(int index) {
        // System.out.println("ES.forgetUntil(" + index + ")");
        int large = toPrivate(index);
        if (large == current.size()) {
            forget();
        } else if (index >= smallestIndex && large < current.size()) {
            int size = current.size() - large;
            Vector result = new Vector(size);
            for (int i = 0; i < size; i++) {
                result.addElement(current.elementAt(i + index));
            }
            current = result;
            smallestIndex = index;
        } else {
            throw new IllegalArgumentException("Index " + index + " out of range");
        }
    }


    public void forget() {
        // System.out.println("ES.forget()");
        // dumpState();
        smallestIndex += current.size();
        current.removeAllElements();
        // dumpState();
    }


    public int getSmallestActiveIndex() {
        return fromPrivate(0);
    }


    public int getLargestActiveIndex() {
        return fromPrivate(current.size() - 1);
    }


    /**
     * Returns whether this object is equal to some other object. The argument object need not be of
     * class <code>EventStream</code>; it is sufficent that it implement the {@link EventSequence}
     * interface, in which case this method returns the same value as calling
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


    /**
     * Return a string representation of this object. The string returned by this method will
     * contain all events that are logically part of the stream, so this method will read events
     * from the stream until the stream is exhausted. This method should therefore be used only in
     * debugging code.
     * @return a string representation of this object
     */
    public String toString() {
        fillUntil(-1);
        return current.toString();
    }

    private class EventEnumerator implements Enumeration {

        private int index = 0;


        public boolean hasMoreElements() {
            return current.size() > toPrivate(index) || fillUntil(index);
        }


        public Object nextElement() {
            if (fillUntil(index)) {
                return current.elementAt(toPrivate(index++));
            } else {
                throw new NoSuchElementException("EventStream exhausted");
            }
        }

    }

}
