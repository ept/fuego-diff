/*
 * Copyright 2006 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import java.io.Writer;
import java.io.OutputStream;

import org.xmlpull.v1.XmlSerializer;

import fuegocore.util.Util;

/**
 * A class for creating an {@link EventSequence} from serialization.
 * This class builds an event sequence by accepting calls on the
 * {@link TypedXmlSerializer} interface, adding events
 * for calls that would add events to the output stream.  The main
 * purpose of this is to permit building an abstract representation of
 * the XML document from a legacy application that expects to be
 * outputting directly to a stream.
 */
public class EventSerializer implements TypedXmlSerializer {

    private EventList current = new EventList();
    private int depth = 0;
    private String[] elementStack = new String[12];

    /**
     * Return the collected sequence.  This method returns the
     * sequence of events that have so far been collected.  It is
     * returned as read-only, since it is not intended for
     * modification by applications.
     */
    public EventSequence getCurrentSequence () {
	return current;
    }

    // XmlSerializer implementation begins

    public void setFeature (String name, boolean state) {
	throw new IllegalStateException("Feature " + name + " not supported");
    }

    public boolean getFeature (String name) {
	return false;
    }

    public void setProperty (String name, Object value) {
	throw new IllegalStateException("Property " + name + " not supported");
    }

    public Object getProperty (String name) {
	return null;
    }

    /**
     * A do-nothing method.  Since the events are inserted into the
     * event sequence, setting an output stream is meaningless.
     * However, this method must not throw an exception, since an
     * application will expect a successful call to this before
     * outputting events.
     */
    public void setOutput (OutputStream os, String encoding) {
    }

    /**
     * A do-nothing method.  Since the events are inserted into the
     * event sequence, setting an output stream is meaningless.
     * However, this method must not throw an exception, since an
     * application will expect a successful call to this before
     * outputting events.
     */
    public void setOutput (Writer writer) {
    }

    public void startDocument (String encoding, Boolean standalone) {
	current.add(Event.createStartDocument());
    }

    public void endDocument () {
	current.add(Event.createEndDocument());
    }

    public void setPrefix (String prefix, String namespace) {
	current.add(Event.createNamespacePrefix(namespace, prefix));
    }

    public String getPrefix (String namespace, boolean generatePrefix) {
	return null;
    }

    public int getDepth () {
	return depth;
    }

    public String getNamespace () {
	if (depth == 0) {
	    return null;
	} else {
	    return elementStack[2 * (depth - 1)];
	}
    }

    public String getName () {
	if (depth == 0) {
	    return null;
	} else {
	    return elementStack[2 * depth - 1];
	}
    }

    public XmlSerializer startTag (String namespace, String name) {
	elementStack = Util.ensureCapacity(elementStack, 2 * (depth + 1));
	elementStack[2 * depth] = namespace;
	elementStack[2 * depth + 1] = name;
	depth += 1;
	current.add(Event.createStartElement(namespace, name));
	return this;
    }

    public XmlSerializer attribute (String namespace, String name,
				    String value) {
	current.add(Event.createAttribute(namespace, name, value));
	return this;
    }

    public XmlSerializer endTag (String namespace, String name) {
	if (depth <= 0) {
	    throw new IllegalStateException("End tag {" + namespace + "}"
					    + name + " without start tag");
	}
	depth -= 1;
	current.add(Event.createEndElement(namespace, name));
	return this;
    }

    public XmlSerializer text (String text) {
	current.add(Event.createContent(text));
	return this;
    }

    public XmlSerializer text (char[] ch, int start, int length) {
	return text(new String(ch, start, length));
    }

    public TypedXmlSerializer typedContent (Object content, String namespace,
					    String name) {
	current.add(Event.createTypedContent(namespace, name, content));
	return this;
    }

    public void cdsect (String data) {
	text(data);
    }

    public void entityRef (String text) {
	current.add(Event.createEntityReference(text));
    }

    public void processingInstruction (String text) {
	current.add(Event.createProcessingInstruction(text));
    }

    public void comment (String text) {
	current.add(Event.createComment(text));
    }

    public void docdecl (String text) {
    }

    public void ignorableWhitespace (String text) {
	this.text(text);
    }

    public void flush () {
    }

    public String toString () {
	return current.toString();
    }

}
