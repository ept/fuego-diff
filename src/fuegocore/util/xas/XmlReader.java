/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import fuegocore.util.Util;

/**
 * A higher-level class for extracting information from XML sequences. Typically when forming data
 * structures from received XML data the format of the document is well known and consists of
 * several repeating constructs (e.g. a record is an element with each item in the record getting a
 * sub-element of the record's element). This class is intended to provide easy operations for these
 * common cases.
 */
public class XmlReader {

    private EventSequence es;
    private int index = 0;


    private void skipNamespaces() {
        Event ev = es.get(index);
        while (ev != null && ev.getType() == Event.NAMESPACE_PREFIX) {
            index += 1;
            ev = es.get(index);
        }
    }


    /**
     * Construct a new object reading the specified sequence. It is permitted for the sequence to
     * receive additional events at the end at least for as long as {@link #isFinished} returns
     * <code>false</code>.
     * <p>
     * The methods of this class may assume that the supplied event sequence represents a
     * well-formed XML document. Any violation of this assumption may be detected but applications
     * should not rely on it.
     * @param es
     *            the event sequence to read
     */
    public XmlReader(EventSequence es) {
        this.es = es;
    }


    /**
     * Return whether the sequence has ended. After this method returns <code>true</code> calling
     * any reading method is undefined.
     * @return whether all of the sequence has been read
     */
    public boolean isFinished() {
        return es.get(index) == null;
    }


    /**
     * Return the event at the current point. This is a low-level method providing access to the
     * event at the current point.
     * @return the event at the current point, or <code>null</code> if the current event can not be
     *         determined (e.g. because of the sequence coming to an end)
     */
    public Event getCurrentEvent() {
        return es.get(index);
    }


    /**
     * Advance the current point by one. This low-level method can be used to skip over events that
     * are not structured, e.g. namespace prefix mappings. The advancing only happens if there are
     * more events in the sequence.
     * @return the next event in the sequence, or <code>null</code> if the sequence has ended
     */
    public Event advance() {
        if (!isFinished()) {
            return es.get(index++);
        } else {
            return null;
        }
    }


    /**
     * Decrease the current position by one. Looking ahead one event in the event sequence is often
     * useful in reading. This method allows the caller to back up one step, i.e. when it determines
     * that the last received event should be read again.
     */
    public void backup() {
        backup(1);
    }


    /**
     * Decrease the current position by the given amount. On occasion more than one event of
     * lookahead is needed, so this method provides a way to back up more than one position. The
     * given amount must be positive and no larger than the current index.
     * <p>
     * This method is often better replaced by using {@link #getCurrentPosition} and
     * {@link #setCurrentPosition}, which remove the need to keep track of the number of events read
     * in the application.
     * @param amount
     *            the amount by which to decrease the current position
     */
    public void backup(int amount) {
        if (amount >= 1 && index >= amount) {
            index -= amount;
        }
    }


    /**
     * Return the current position. The value returned by this method should only be used in a
     * subsequent call to {@link #setCurrentPosition}, since the application cannot know how this
     * class keeps track of its position in the sequence.
     * @return the current position of this reader in the underlying sequence
     */
    public int getCurrentPosition() {
        return index;
    }


    /**
     * Set the current position. The argument of this method should have been received from a
     * previous call to {@link #getCurrentPosition}. The argument <code>0</code> can reliably be
     * used to set the position to the beginning of the sequence.
     * @param pos
     *            the new current position
     */
    public void setCurrentPosition(int pos) {
        index = pos;
    }


    /**
     * Reset the current event sequence. This method sets the current event sequence to the
     * specified one and sets the current position at the beginning.
     * <p>
     * Rationale for this method: the case is often that {@link EventSequence} objects are extracted
     * from some larger {@link EventSequence} sequentially through the methods of this class. Then
     * it is nicer if a new <code>XmlReader</code> object does not need to be constructed for each
     * of these subsequences.
     * @param es
     *            the event sequence to make the new current sequence
     */
    public void reset(EventSequence es) {
        this.es = es;
        index = 0;
    }


    /**
     * Skip the element starting at the current point. This method will move the current point after
     * the next whole element if the event at the current point is an element start event. If the
     * current event is something else, nothing happens.
     * @return <code>true</code> if the current point was moved, <code>false</code> otherwise
     */
    public boolean skipElement() {
        boolean result = false;
        int i = index;
        Event current = es.get(index);
        if (current != null && current.getType() == Event.START_ELEMENT) {
            int depth = 1;
            Event ev;
            while (depth > 0 && (ev = es.get(++index)) != null) {
                if (ev.getType() == Event.START_ELEMENT) {
                    depth++;
                } else if (ev.getType() == Event.END_ELEMENT) {
                    depth--;
                }
            }
            if (depth == 0) {
                index += 1;
                result = true;
            }
        }
        return result;
    }


    /**
     * Skip the named element starting at the current point. This method will move the current point
     * after the next whole element if the event at the current point is an element start event with
     * the specified name. If the current event is something else, nothing happens.
     * @param namespace
     *            the desired namespace of the element
     * @param name
     *            the desired local name of the element
     * @return <code>true</code> if the current point was moved, <code>false</code> otherwise
     */
    public boolean skipElement(String namespace, String name) {
        boolean result = false;
        Event current = es.get(index);
        if (Util.equals(current.getNamespace(), namespace) && Util.equals(current.getName(), name)) {
            result = skipElement();
        }
        return result;
    }


    /**
     * Return the (presumed simple) content of the next element. This method will return the content
     * of the next element and move the current point after it, on the assumption that the current
     * point is at element start of the element named in the arguments and the element's content is
     * simple (i.e. a single string, possibly in several pieces). If the assumption does not hold,
     * nothing happens.
     * @param namespace
     *            the desired namespace of the element
     * @param name
     *            the desired local name of the element
     * @return the content of the element at current point, or <code>null</code> if the assumptions
     *         do not hold
     */
    public String simpleContent(String namespace, String name) {
        String result = null;
        int i = index;
        Event ev = es.get(i);
        while (ev.getType() == Event.NAMESPACE_PREFIX) {
            i += 1;
            ev = es.get(i);
        }
        if (ev.getType() == Event.START_ELEMENT && Util.equals(ev.getNamespace(), namespace) &&
            Util.equals(ev.getName(), name)) {
            do {
                i += 1;
                ev = es.get(i);
            } while (ev.getType() == Event.ATTRIBUTE);
            if (ev.getType() == Event.CONTENT) {
                StringBuffer buffer = new StringBuffer((String) ev.getValue());
                i += 1;
                ev = es.get(i);
                while (ev.getType() == Event.CONTENT) {
                    buffer.append((String) ev.getValue());
                    i += 1;
                    ev = es.get(i);
                }
                if (ev.getType() == Event.END_ELEMENT &&
                    Util.equals(ev.getNamespace(), namespace) && Util.equals(ev.getName(), name)) {
                    result = buffer.toString();
                    index = i + 1;
                }
            }
        }
        return result;
    }


    /**
     * Return the typed content of the next element. This method will return the content of the next
     * element and move the current point after it, on the assumption that the current point is at
     * element start of the element named in the arguments and the element's content is typed. If
     * the assumption does not hold, nothing happens.
     * @param namespace
     *            the desired namespace of the element
     * @param name
     *            the desired local name of the element
     * @param typeNs
     *            the namespace of the type
     * @param typeName
     *            the local name of the type
     * @return the content of the element at current point, or <code>null</code> if the assumptions
     *         do not hold
     */
    public Object typedContent(String namespace, String name, String typeNs, String typeName) {
        Object result = null;
        int i = index;
        Event ev = es.get(i);
        while (ev.getType() == Event.NAMESPACE_PREFIX) {
            i += 1;
            ev = es.get(i);
        }
        if (ev.getType() == Event.START_ELEMENT && Util.equals(ev.getNamespace(), namespace) &&
            Util.equals(ev.getName(), name)) {
            do {
                i += 1;
                ev = es.get(i);
            } while (ev.getType() == Event.ATTRIBUTE);
            if (ev.getType() == Event.TYPED_CONTENT && Util.equals(ev.getNamespace(), typeNs) &&
                Util.equals(ev.getName(), typeName)) {
                Object value = ev.getValue();
                i += 1;
                ev = es.get(i);
                if (ev.getType() == Event.END_ELEMENT &&
                    Util.equals(ev.getNamespace(), namespace) && Util.equals(ev.getName(), name)) {
                    result = value;
                    index = i + 1;
                }
            }
        }
        return result;
    }


    /**
     * Return the content of the next element. This method will return the content of the next
     * element and move the current point after it, on the assumption that the current point is at
     * element start of the element named in the arguments. If the assumption does not hold, nothing
     * happens.
     * @param namespace
     *            the desired namespace of the element
     * @param name
     *            the desired local name of the element
     * @return the content of the element at current point, or <code>null</code> if the assumptions
     *         do not hold
     */
    public EventSequence completeContent(String namespace, String name) {
        EventSequence result = null;
        int i = index;
        Event ev = es.get(i);
        while (ev.getType() == Event.NAMESPACE_PREFIX) {
            i += 1;
            ev = es.get(i);
        }
        if (ev.getType() == Event.START_ELEMENT && Util.equals(ev.getNamespace(), namespace) &&
            Util.equals(ev.getName(), name)) {
            do {
                i += 1;
                ev = es.get(i);
            } while (ev.getType() == Event.ATTRIBUTE);
            int j = i - 1;
            int depth = 0;
            while (depth >= 0 && (ev = es.get(++j)) != null) {
                if (ev.getType() == Event.START_ELEMENT) {
                    depth += 1;
                } else if (ev.getType() == Event.END_ELEMENT) {
                    depth -= 1;
                }
            }
            if (ev != null && ev.getType() == Event.END_ELEMENT) {
                result = es.subSequence(i, j);
                index = j + 1;
            }
        }
        return result;
    }


    /**
     * Return the element starting at the current point. If the current point is at an element start
     * event, this method will return it (starting from its element start event and ending at its
     * element end event) and move the current point after it. If the current point is not at an
     * element start event, nothing happens.
     * @return the element starting at the current point as an event sequence, or <code>null</code>
     *         if an element does not start at the current point
     */
    public EventSequence currentElement() {
        EventSequence result = null;
        int init = index;
        skipNamespaces();
        int i = index;
        if (skipElement()) {
            result = es.subSequence(i, index);
        }
        if (result == null) {
            index = init;
        }
        return result;
    }


    /**
     * Return the element delimiter starting at the current point. If the current point is at an
     * element start event, this method will return it as well as any attributes (starting from its
     * element start event and ending at its last attribute event) and move the current point after
     * it. If the current point is at an element end event, that event is returned, and the current
     * point is updated to the event after it. If the current point is not an start/end element
     * event, nothing happens.
     * @return the element delimiter starting at the current point as an event sequence, or
     *         <code>null</code> if the current point is not at an element start or end event
     */
    public EventSequence currentDelimiter() {
        return currentDelimiter(null, null, true);
    }


    /**
     * Return the element delimiter starting at the current point, if the element namespace:name
     * matches the arguments.
     */

    public EventSequence currentDelimiter(String namespace, String name) {
        return currentDelimiter(namespace, name, false);
    }


    protected EventSequence currentDelimiter(String namespace, String name, boolean ignoreName) {
        EventSequence result = null;
        int i = index;
        skipNamespaces();
        Event current = es.get(i);
        if (current == null) return null;
        boolean nameMatch = ignoreName ||
                            (Util.equals(current.getNamespace(), namespace) && Util.equals(
                                                                                           current.getName(),
                                                                                           name));

        if (current.getType() == Event.START_ELEMENT && nameMatch) {
            do {
                current = es.get(++i);
            } while (current != null && current.getType() == Event.ATTRIBUTE);
            result = es.subSequence(index, i);
            index = i;
        } else if (current.getType() == Event.END_ELEMENT && nameMatch) {
            result = es.subSequence(index, index + 1);
            index++;
        }
        return result;
    }


    /**
     * Return the named element starting at the current point. If the current point is at an element
     * start event with the specified name, this method will return it (starting from its element
     * start event and ending at its element end event) and move the current point after it. If the
     * current point is not at an element start event with the specified name, nothing happens.
     * @param namespace
     *            the desired namespace of the element
     * @param name
     *            the desired local name of the element
     * @return the element starting at the current point as an event sequence, or <code>null</code>
     *         if a desired element does not start at the current point
     */
    public EventSequence currentElement(String namespace, String name) {
        EventSequence result = null;
        int init = index;
        skipNamespaces();
        int i = index;
        if (skipElement(namespace, name)) {
            result = es.subSequence(i, index);
        }
        if (result == null) {
            index = init;
        }
        return result;
    }


    /**
     * Return the next named element at this level. If the current point is at an element start
     * event, this method will skip over elements and content at this depth until it discovers an
     * element with the specified name. Such an element is then returned and the current point moved
     * after it. If such an element is not found, nothing happens.
     * <p>
     * Note: It may be a common occurrence to use this method to get the child elements of an
     * element one by one when their order is not known in advance. In this case calls to this
     * method need to be wrapped between calls to {@link #getCurrentPosition} and
     * {@link #setCurrentPosition}.
     * @param namespace
     *            the desired namespace of the element
     * @param name
     *            the desired local name of the element
     * @return the next element named as specified at the same depth as the element starting at the
     *         current point, or <code>null</code> if such an element is not found for some reason
     */
    public EventSequence nextElement(String namespace, String name) {
        EventSequence result = null;
        int i = index;
        Event current = es.get(index);
        while (current != null && current.getType() != Event.END_ELEMENT) {
            if (current.getType() == Event.START_ELEMENT) {
                if (Util.equals(current.getNamespace(), namespace) &&
                    Util.equals(current.getName(), name)) {
                    break;
                }
                skipElement();
            } else {
                index += 1;
            }
            current = es.get(index);
        }
        if (current != null) {
            result = currentElement(namespace, name);
        }
        if (result == null) {
            index = i;
        }
        return result;
    }


    /**
     * Return the next named element at or below this level. This method starts looking from the
     * current position, descending inside elements. If it finds an element with the specified name
     * before an element end event at the current level, it returns this element and leaves the
     * current point after it. If such an element is not found, nothing happens.
     * @param namespace
     *            the desired namespace of the element
     * @param name
     *            the desired local name of the element
     * @return the next element named as specified at the same or greater depth as the element
     *         containing the current point, or <code>null</code> if such an element is not found
     *         for some reason
     */
    public EventSequence nextInsideElement(String namespace, String name) {
        EventSequence result = null;
        int i = index;
        int depth = 0;
        Event current = null;
        while (depth >= 0 && (current = es.get(index)) != null) {
            if (current.getType() == Event.START_ELEMENT) {
                if (Util.equals(current.getNamespace(), namespace) &&
                    Util.equals(current.getName(), name)) {
                    break;
                }
                depth += 1;
            } else if (current.getType() == Event.END_ELEMENT) {
                depth -= 1;
            }
            index += 1;
        }
        if (depth >= 0 && current != null) {
            result = currentElement(namespace, name);
        }
        if (result == null) {
            index = i;
        }
        return result;
    }


    public String toString() {
        return "XmlReader(" + index + ")" + es.toString();
    }

}
