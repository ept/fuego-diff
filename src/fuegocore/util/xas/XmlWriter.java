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

import java.io.IOException;

/**
 * A higher-level class for creating XML event sequences.  This class
 * provides methods for building XML documents at the level of
 * complete elements.  This eases the construction of XML, since the
 * application does not need to keep track of e.g. ending elements.
 * Every element addition method is defined to return
 * <code>this</code> to permit chaining of invocations.
 */
public class XmlWriter {

    private TypedXmlSerializer current;

    /**
     * Construct using an existing serializer.
     *
     * @param ser the serializer to output the sequence to
     */
    public XmlWriter (TypedXmlSerializer ser) {
	current = ser;
    }

    /**
     * Add a raw event.  In cases where structured addition of XML
     * content is not possible (such as when adding comments or
     * namespace prefixes) it is necessary to be able to add
     * individual events.  This method is not usually intended for
     * adding element-related events, but may be used for such,
     * e.g. to avoid too deep a hierarchy of elements at various
     * levels in code.
     *
     * @param event the event to add
     */
    public XmlWriter addEvent (Event event) throws IOException {
	XasUtil.outputEvent(event, current);
	return this;
    }

    /**
     * Add a raw events.  In cases where structured addition of XML
     * content is not possible (such as when adding comments or
     * namespace prefixes) it is necessary to be able to add
     * individual events.  This method is not usually intended for
     * adding element-related events, but may be used for such,
     * e.g. to avoid too deep a hierarchy of elements at various
     * levels in code.
     *
     * @param es the event sequence to add
     */

    public XmlWriter addEvents (EventSequence es) throws IOException {
        XasUtil.outputSequence(es, current);
        return this;
    }

    /**
     * Insert an empty element with no attributes.  This method just
     * adds a start-end event pair to the event sequence.
     *
     * @param namespace the namespace URI of the element to add
     * @param name the local name of the element to add
     */
    public XmlWriter emptyElement (String namespace, String name)
	throws IOException {
	return emptyElement(namespace, name, (EventSequence) null);
    }

    /**
     * Insert an empty element with a specified attribute.  This
     * method adds a start-end event pair to the event sequence with
     * the start event containing the given attribute.
     *
     * @param namespace the namespace URI of the element to add
     * @param name the local name of the element to add
     * @param attribute the attribute to insert for the start event;
     * must be of type {@link Event#ATTRIBUTE}
     */
    public XmlWriter emptyElement (String namespace, String name,
				   Event attribute)
	throws IOException {
	EventList attributes = null;
	if (attribute != null) {
	    attributes = new EventList();
	    attributes.add(attribute);
	}
	return emptyElement(namespace, name, attributes);
    }

    /**
     * Insert an empty element with specified attributes.  This method
     * adds a start-end event pair to the event sequence with the
     * start event containing the given attributes.
     *
     * @param namespace the namespace URI of the element to add
     * @param name the local name of the element to add
     * @param attributes the attributes to insert for the start event;
     * the given sequence may contain only events of type {@link
     * Event#ATTRIBUTE}
     */
    public XmlWriter emptyElement (String namespace, String name,
				   EventSequence attributes)
	throws IOException {
	return simpleElement(namespace, name, null, attributes);
    }

    /**
     * Insert an element with simple content and no attributes.  This
     * method adds a start-end event pair to the event sequence with
     * the content between them and the start event containing no
     * attributes.
     *
     * @param namespace the namespace URI of the element to add
     * @param name the local name of the element to add
     * @param content the content to give to the element
     */
    public XmlWriter simpleElement (String namespace, String name,
				    String content)
	throws IOException {
	return simpleElement(namespace, name, content, (EventSequence) null);
    }

    /**
     * Insert an element with simple content and a specified
     * attribute.  This method adds a start-end event pair to the
     * event sequence with the content between them and the start
     * event containing the given attribute.
     *
     * @param namespace the namespace URI of the element to add
     * @param name the local name of the element to add
     * @param content the content to give to the element
     * @param attribute the attribute to insert for the start event;
     * must be of type {@link Event#ATTRIBUTE}
     */
    public XmlWriter simpleElement (String namespace, String name,
				    String content, Event attribute)
	throws IOException {
	EventList attributes = null;
	if (attribute != null) {
	    attributes = new EventList();
	    attributes.add(attribute);
	}
	return simpleElement(namespace, name, content, attributes);
    }

    /**
     * Insert an element with simple content and specified attributes.
     * This method adds a start-end event pair to the event sequence
     * with the content between them and the start event containing
     * the given attributes.
     *
     * @param namespace the namespace URI of the element to add
     * @param name the local name of the element to add
     * @param content the content to give to the element
     * @param attributes the attributes to insert for the start event;
     * the given sequence may contain only events of type {@link
     * Event#ATTRIBUTE}
     */
    public XmlWriter simpleElement (String namespace, String name,
				    String content, EventSequence attributes)
	throws IOException {
	current.startTag(namespace, name);
	if (attributes != null) {
	    XasUtil.outputSequence(attributes, current);
	}
	if (content != null) {
	    current.text(content);
	}
	current.endTag(namespace, name);
	return this;
    }

    /**
     * Insert an element with typed content and no attributes.  This
     * method adds a start-end event pair to the event sequence with
     * the content of the specified type between them and the start
     * event containing no attributes.
     *
     * @param namespace the namespace URI of the element to add
     * @param name the local name of the element to add
     * @param typeNamespace the namespace URI of the content's type name
     * @param typeName the local name of the content's type name
     * @param content the content to give to the element
     */
    public XmlWriter typedElement (String namespace, String name,
				   String typeNamespace, String typeName,
				   Object content)
	throws IOException {
	return typedElement(namespace, name, typeNamespace, typeName, content,
			    (EventSequence) null);
    }

    /**
     * Insert an element with typed content and a specified attribute.
     * This method adds a start-end event pair to the event sequence
     * with the content of the specified type between them and the
     * start event containing the given attribute.
     *
     * @param namespace the namespace URI of the element to add
     * @param name the local name of the element to add
     * @param typeNamespace the namespace URI of the content's type name
     * @param typeName the local name of the content's type name
     * @param content the content to give to the element
     * @param attribute the attribute to insert for the start event;
     * must be of type {@link Event#ATTRIBUTE}
     */
    public XmlWriter typedElement (String namespace, String name,
				   String typeNamespace, String typeName,
				   Object content, Event attribute)
	throws IOException {
	EventList attributes = null;
	if (attribute != null) {
	    attributes = new EventList();
	    attributes.add(attribute);
	}
	return typedElement(namespace, name, typeNamespace, typeName, content,
			    attributes);
    }

    /**
     * Insert an element with typed content and specified attributes.
     * This method adds a start-end event pair to the event sequence
     * with the content of the specified type between them and the
     * start event containing the given attributes.
     *
     * @param namespace the namespace URI of the element to add
     * @param name the local name of the element to add
     * @param typeNamespace the namespace URI of the content's type name
     * @param typeName the local name of the content's type name
     * @param content the content to give to the element
     * @param attributes the attributes to insert for the start event;
     * the given sequence may contain only events of type {@link
     * Event#ATTRIBUTE}
     */
    public XmlWriter typedElement (String namespace, String name,
				   String typeNamespace, String typeName,
				   Object content, EventSequence attributes)
	throws IOException {
	current.startTag(namespace, name);
	if (attributes != null) {
	    XasUtil.outputSequence(attributes, current);
	}
	if (content != null) {
	    current.typedContent(content, typeNamespace, typeName);
	}
	current.endTag(namespace, name);
	return this;
    }

    /**
     * Insert an element with arbitrary content and no attributes.
     * This method adds a start-end event pair to the event sequence
     * with the content between them and the start event containing no
     * attributes.
     *
     * @param namespace the namespace URI of the element to add
     * @param name the local name of the element to add
     * @param content the content to give to the element
     */
    public XmlWriter complexElement (String namespace, String name,
				     EventSequence content)
	throws IOException {
	return complexElement(namespace, name, content, (EventSequence) null);
    }

    /**
     * Insert an element with arbitrary content and a specified
     * attribute.  This method adds a start-end event pair to the
     * event sequence with the content between them and the start
     * event containing the given attribute.
     *
     * @param namespace the namespace URI of the element to add
     * @param name the local name of the element to add
     * @param content the content to give to the element
     * @param attribute the attribute to insert for the start event;
     * must be of type {@link Event#ATTRIBUTE}
     */
    public XmlWriter complexElement (String namespace, String name,
				     EventSequence content,
				     Event attribute)
	throws IOException {
	EventList attributes = null;
	if (attribute != null) {
	    attributes = new EventList();
	    attributes.add(attribute);
	}
	return complexElement(namespace, name, content, attributes);
    }

    /**
     * Insert an element with arbitrary content and specified
     * attributes.  This method adds a start-end event pair to the
     * event sequence with the content between them and the start
     * event containing the given attributes.
     *
     * @param namespace the namespace URI of the element to add
     * @param name the local name of the element to add
     * @param content the content to give to the element
     * @param attributes the attributes to insert for the start event;
     * the given sequence may contain only events of type {@link
     * Event#ATTRIBUTE}
     */
    public XmlWriter complexElement (String namespace, String name,
				     EventSequence content,
				     EventSequence attributes)
	throws IOException {
	current.startTag(namespace, name);
	if (attributes != null) {
	    XasUtil.outputSequence(attributes, current);
	}
	if (content != null) {
	    XasUtil.outputSequence(content, current);
	}
	current.endTag(namespace, name);
	return this;
    }

    /**
     * Flush the underlying serializer.
     */
    public void flush() throws IOException {
	current.flush();
    }

}
