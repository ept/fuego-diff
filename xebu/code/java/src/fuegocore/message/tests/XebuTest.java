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

package fuegocore.message.tests;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;

import junit.framework.TestCase;

import fuegocore.message.encoding.*;
import fuegocore.util.Util;
import fuegocore.util.xas.*;

/**
 * Test Xebu encoding.  This class is a collection of JUnit tests for
 * various aspects of the Xebu encoding implemented in the {@link
 * fuegocore.message.encoding} package.  Generic XML serialization and
 * parsing tests can be found in the {@link
 * fuegocore.util.tests.XasTest} class; the tests here exercise some
 * Xebu-specific functionality.
 */
public class XebuTest extends TestCase {

    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String FOO_NS = "http://www.hiit.fi/fuego/fc/test";
    private static final String LIST_TYPE = "list";

    private EventSequence intListDocument;
    private EventSequence simpleDocument;
    private EventSequence contentDocument;

    protected void setUp () {
	try {
	    EventSerializer s = new EventSerializer();
	    XmlWriter simple = new XmlWriter(s);
	    simple.addEvent(Event.createStartDocument());
	    simple.addEvent(Event.createNamespacePrefix(FOO_NS, "foo"));
	    simple.addEvent(Event.createStartElement(FOO_NS, "simple"));
	    simple.simpleElement(FOO_NS, "bar", "Non-random text",
				 Event.createAttribute("", "baz", "quux"));
	    simple.simpleElement(FOO_NS, "bar", "Non-random text",
				 Event.createAttribute("", "baz", "quux"));
	    simple.addEvent(Event.createEndElement(FOO_NS, "simple"));
	    simple.addEvent(Event.createEndDocument());
	    simpleDocument = s.getCurrentSequence();
	    System.out.println(simpleDocument);
	    EventSerializer il = new EventSerializer();
	    XmlWriter intList = new XmlWriter(il);
	    intList.addEvent(Event.createStartDocument());
	    intList.addEvent(Event.createNamespacePrefix(FOO_NS, "xyzzy"));
	    intList.addEvent(Event.createNamespacePrefix(XSI_NS, "xsi"));
	    intList.addEvent(Event.createNamespacePrefix(XSD_NS, "xsd"));
	    intList.addEvent(Event.createStartElement(FOO_NS, "lists"));
	    List l1 = new ArrayList();
	    l1.add(new Integer(100));
	    List l2 = new ArrayList();
	    for (int i = 0; i < 10; i++) {
		l2.add(new Integer(1000 * i * i));
	    }
	    intList.typedElement(FOO_NS, "first", FOO_NS, LIST_TYPE, l1);
	    intList.typedElement(FOO_NS, "second", FOO_NS, LIST_TYPE, l2);
	    intList.addEvent(Event.createEndElement(FOO_NS, "lists"));
	    intList.addEvent(Event.createEndDocument());
	    intListDocument = il.getCurrentSequence();
	    System.out.println(intListDocument);
	    EventSerializer cc = new EventSerializer();
	    XmlWriter content = new XmlWriter(cc);
	    content.addEvent(Event.createStartDocument());
	    content.addEvent(Event.createStartElement(FOO_NS, "name"));
	    EventList ln = new EventList();
	    ln.add(Event.createContent("Kangas"));
	    ln.add(Event.createContent("harju"));
	    content.complexElement(FOO_NS, "last", ln);
	    content.simpleElement(FOO_NS, "first", "Jaakko");
	    content.addEvent(Event.createEndElement(FOO_NS, "name"));
	    content.addEvent(Event.createEndDocument());
	    contentDocument = cc.getCurrentSequence();
	    System.out.println(contentDocument);
	} catch (IOException ex) {
	    /*
	     * Since we are using EventSerializers, this exception
	     * cannot happen.
	     */
	}
    }

    public XebuTest (String name) {
	super(name);
    }

    /**
     * Test a simple document with repeated content.  This class tests
     * a serialize-parse cycle for a simple document consisting of two
     * identical elements in a row.  This exercises the tokenization
     * and caching in Xebu.
     */
    public void testSimpleDocument () throws Exception {
	Set types = CodecIndustry.availableTypes();
	assertTrue("No codec types registered", types.size() > 0);
	for (Iterator i = types.iterator(); i.hasNext(); ) {
	    String type = (String) i.next();
	    if (!type.startsWith("application/x-ebu+")) {
		continue;
	    }
	    System.out.println(type);
	    XebuCodecFactory factory =
		(XebuCodecFactory) CodecIndustry.getFactory(type);
	    assertNotNull("Returned factory null for type " + type, factory);
	    Object token = new Object();
	    XebuSerializer ser = (XebuSerializer) factory.getNewEncoder(token);
	    assertNotNull("Returned serializer null for type " + type, ser);
	    ser.setProperty(XebuConstants.PROPERTY_COA_MACHINE,
			    new SimpleEoaMachine());
	    StringWriter target = new StringWriter();
	    ser.setOutput(target);
	    XasUtil.outputSequence(simpleDocument, ser);
	    String inter = target.toString();
	    System.out.println(Util.toPrintable(inter));
	    XebuParser par = (XebuParser) factory.getNewDecoder(token);
	    assertNotNull("Returned parser null for type " + type, par);
	    par.setProperty(XebuConstants.PROPERTY_COA_MACHINE,
			    new SimpleDoaMachine());
	    par.setInput(new StringReader(inter));
	    EventSequence res = new EventStream(par);
	    assertEquals("Enc-dec sequence failed for type " + type + "\n"
			 + simpleDocument + "\n\n" + res + "\n\n",
			 simpleDocument, res);
	}
    }

    /**
     * Test a document containing integer lists.  This class tests a
     * serialize-parse cycle for a document containing typed content
     * with a content codec and a COA machine registered for that
     * content.  The test ensures that typed content is correctly
     * encoded and COA machines work as specified.
     */
    public void testIntListDocument () throws Exception {
	Set types = CodecIndustry.availableTypes();
	assertTrue("No codec types registered", types.size() > 0);
	for (Iterator i = types.iterator(); i.hasNext(); ) {
	    String type = (String) i.next();
	    if (!type.startsWith("application/x-ebu+")) {
		continue;
	    }
	    System.out.println(type);
	    XebuCodecFactory factory =
		(XebuCodecFactory) CodecIndustry.getFactory(type);
	    assertNotNull("Returned factory null for type " + type, factory);
	    Object token = new Object();
	    XebuSerializer ser = (XebuSerializer) factory.getNewEncoder(token);
	    assertNotNull("Returned serializer null for type " + type, ser);
	    StringWriter target = new StringWriter();
	    ser.setOutput(target);
	    ContentEncoder tEnc = (ContentEncoder)
		ser.getProperty(XasUtil.PROPERTY_CONTENT_CODEC);
	    ser.setProperty(XasUtil.PROPERTY_CONTENT_CODEC,
			    new IntListEncoder(tEnc));
	    ser.setProperty(XebuConstants.PROPERTY_COA_MACHINE,
			    new IntListEoaMachine());
	    XasUtil.outputSequence(intListDocument, ser);
	    String inter = target.toString();
	    System.out.println(Util.toPrintable(inter));
	    XebuParser par = (XebuParser) factory.getNewDecoder(token);
	    assertNotNull("Returned parser null for type " + type, par);
	    par.setInput(new StringReader(inter));
	    par.setProperty(XebuConstants.PROPERTY_COA_MACHINE,
			    new IntListDoaMachine());
	    ContentDecoder tDec = (ContentDecoder)
		par.getProperty(XasUtil.PROPERTY_CONTENT_CODEC);
	    EventSequence res = new TypedEventStream
		(new EventStream(par), new IntListDecoder(tDec));
	    assertEquals("Enc-dec sequence failed for type " + type + "\n"
			 + intListDocument + "\n\n" + res + "\n\n",
			 intListDocument, res);
	}
    }

    /**
     * Test the separation of consecutive content events.  This tests
     * a serialize-parse cycle for a document where some content is
     * provided as a sequence of consecutive {@link Event#CONTENT}
     * events.  Since Xebu is intended to be streamable, this
     * separation needs to persist across the cycle.
     */
    public void testCoalescedContent () throws Exception {
	Set types = CodecIndustry.availableTypes();
	assertTrue("No codec types registered", types.size() > 0);
	for (Iterator i = types.iterator(); i.hasNext(); ) {
	    String type = (String) i.next();
	    if (!type.startsWith("application/x-ebu+")) {
		continue;
	    }
	    System.out.println(type);
	    XebuCodecFactory factory =
		(XebuCodecFactory) CodecIndustry.getFactory(type);
	    assertNotNull("Returned factory null for type " + type, factory);
	    Object token = new Object();
	    XebuSerializer ser = (XebuSerializer) factory.getNewEncoder(token);
	    assertNotNull("Returned serializer null for type " + type, ser);
	    StringWriter target = new StringWriter();
	    ser.setOutput(target);
	    ser.setProperty(XebuConstants.PROPERTY_COA_MACHINE,
			    new ContentEoaMachine());
	    XasUtil.outputSequence(contentDocument, ser);
	    String inter = target.toString();
	    System.out.println(Util.toPrintable(inter));
	    XebuParser par = (XebuParser) factory.getNewDecoder(token);
	    assertNotNull("Returned parser null for type " + type, par);
	    par.setInput(new StringReader(inter));
	    par.setProperty(XebuConstants.PROPERTY_COA_MACHINE,
			    new ContentDoaMachine());
	    EventSequence res = new EventStream(par);
	    assertEquals("Enc-dec sequence failed for type " + type + "\n"
			 + contentDocument + "\n\n" + res + "\n\n",
			 contentDocument, res);
	}
    }

    private static class IntListEncoder implements ContentEncoder {

	private ContentEncoder chain;

	public IntListEncoder (ContentEncoder chain) {
	    this.chain = chain;
	}

	public boolean encode (Object o, String namespace, String name,
			       TypedXmlSerializer ser)
	    throws IOException {
	    //System.out.println("ILE.encode called with");
	    //System.out.println("o=" + String.valueOf(o));
	    //System.out.println("namespace=" + namespace);
	    //System.out.println("name=" + name);
	    boolean result = false;
	    if (FOO_NS.equals(namespace) && LIST_TYPE.equals(name)
		&& o instanceof List) {
		List l = (List) o;
		XmlWriter writer = new XmlWriter(ser);
		for (int i = 0; i < l.size(); i++) {
		    writer.typedElement(FOO_NS, "item", XSD_NS, "int",
					l.get(i));
		}
		result = true;
	    } else {
		result = chain.encode(o, namespace, name, ser);
	    }
	    return result;
	}

    }

    private static class IntListDecoder extends ChainedContentDecoder {

	public IntListDecoder (ContentDecoder chain) {
	    super(null);
	    if (chain == null) {
		throw new IllegalArgumentException("Chained decoder must be"
						   + " non-null");
	    }
	    this.chain = chain;
	}

	public Object decode (String typeNs, String typeName,
			      XmlReader reader, EventList attributes) {
	    //System.out.println("ILD.decode called with");
	    //System.out.println("typeNs=" + typeNs);
	    //System.out.println("typeName=" + typeName);
	    //System.out.println("reader=" + reader);
	    Object result = null;
	    if (FOO_NS.equals(typeNs) && LIST_TYPE.equals(typeName)) {
		boolean reverse = false;
		for (int i = 0; i < attributes.size(); i++) {
		    Event ev = attributes.get(i);
		    if (FOO_NS.equals(ev.getNamespace())
			&& "reverse".equals(ev.getName())) {
			reverse = "true".equals(ev.getValue());
			attributes.remove(i);
		    }
		}
		ArrayList l = new ArrayList();
		Object item = expect(FOO_NS, "item", reader);
		while (item != null) {
		    if (reverse) {
			l.add(0, item);
		    } else {
			l.add(item);
		    }
		    item = expect(FOO_NS, "item", reader);
		}
		result = l;
	    } else if (chain != null) {
		result = chain.decode(typeNs, typeName, reader, attributes);
	    }
	    return result;
	}

    }

    private static class SimpleEoaMachine implements EoaMachine {

	private int state = 0;

	public Event nextEvent (Event ev) {
	    //System.out.println("State = " + state + ", Event = " + ev);
	    switch (state) {
	    case 0:
		if (ev.getType() == Event.START_ELEMENT
		    && Util.equals(ev.getNamespace(), FOO_NS)
		    && Util.equals(ev.getName(), "simple")) {
		    state = 1;
		}
		break;
	    case 1:
		if (ev.getType() == Event.START_ELEMENT
		    && Util.equals(ev.getNamespace(), FOO_NS)
		    && Util.equals(ev.getName(), "bar")) {
		    ev = null;
		    state = 2;
		} else {
		    state = 3;
		}
		break;
	    case 2:
		if (ev.getType() == Event.END_ELEMENT
		    && Util.equals(ev.getNamespace(), FOO_NS)
		    && Util.equals(ev.getName(), "bar")) {
		    ev = null;
		    state = 3;
		}
		break;
	    case 3:
		if (ev.getType() == Event.END_DOCUMENT) {
		    state = 0;
		}
		break;
	    }
	    //System.out.println("Output = " + ev);
	    return ev;
	}

	public boolean isInitialState () {
	    return state == 0 || state == 3;
	}

    }

    private static class SimpleDoaMachine extends IdentityDoaMachine {

	private int state = 0;

	public void nextEvent (Event ev) {
	    switch (state) {
	    case 0:
		if (ev.getType() == Event.START_ELEMENT
		    && Util.equals(ev.getNamespace(), FOO_NS)
		    && Util.equals(ev.getName(), "simple")) {
		    state = 1;
		}
		break;
	    case 1:
		queue.enqueue(Event.createStartElement(FOO_NS, "bar"));
		state = 2;
		break;
	    case 2:
		if (ev.getType() == Event.CONTENT) {
		    queue.enqueue(ev);
		    queue.enqueue(Event.createEndElement(FOO_NS, "bar"));
		    state = 3;
		    return;
		}
		break;
	    case 3:
		if (ev.getType() == Event.END_ELEMENT
		    && Util.equals(ev.getNamespace(), FOO_NS)
		    && Util.equals(ev.getName(), "simple")) {
		    state = 0;
		}
		break;
	    }
	    queue.enqueue(ev);
	}

    }

    private static class IntListEoaMachine implements EoaMachine {

	private int state = 0;
	private Map prefixMapping = new HashMap();

	public Event nextEvent (Event ev) {
	    //System.out.println("State = " + state + ", Event = " + ev);
	    if (ev.getType() == Event.NAMESPACE_PREFIX) {
		prefixMapping.put(ev.getNamespace(), ev.getValue());
	    }
	    switch (state) {
	    case 0:
		if (ev.getType() == Event.START_ELEMENT
		    && Util.equals(ev.getNamespace(), FOO_NS)
		    && Util.equals(ev.getName(), "lists")) {
		    ev = null;
		    state = 1;
		}
		break;
	    case 1:
		if (ev.getType() == Event.START_ELEMENT) {
		    state = 2;
		} else if (ev.getType() == Event.END_ELEMENT) {
		    ev = null;
		    state = 0;
		}
		break;
	    case 2:
		if (ev.getType() == Event.ATTRIBUTE
		    && Util.equals(ev.getNamespace(), XSI_NS)
		    && Util.equals(ev.getName(), "type")
		    && Util.equals(ev.getValue(),
				   ((String) prefixMapping.get(FOO_NS))
				   + ":list")) {
		    ev = null;
		} else if (ev.getType() == Event.START_ELEMENT
			   && Util.equals(ev.getNamespace(), FOO_NS)
			   && Util.equals(ev.getName(), "item")) {
		    ev = null;
		    state = 3;
		} else if (ev.getType() == Event.END_ELEMENT) {
		    state = 1;
		}
		break;
	    case 3:
		if (ev.getType() == Event.ATTRIBUTE
		    && Util.equals(ev.getNamespace(), XSI_NS)
		    && Util.equals(ev.getName(), "type")
		    && Util.equals(ev.getValue(),
				   ((String) prefixMapping.get(XSD_NS))
				   + ":int")) {
		    ev = null;
		} else if (ev.getType() == Event.END_ELEMENT
			   && Util.equals(ev.getNamespace(), FOO_NS)
			   && Util.equals(ev.getName(), "item")) {
		    ev = null;
		    state = 2;
		}
		break;
	    }
	    //System.out.println("Output = " + ev);
	    return ev;
	}

	public boolean isInitialState () {
	    return state == 0;
	}

    }

    private static class IntListDoaMachine extends IdentityDoaMachine {

	private Map prefixMapping = new HashMap();
	private int state = 0;

	public void nextEvent (Event ev) {
	    //System.out.println("State = " + state + ", Event = " + ev);
	    if (ev.getType() == Event.NAMESPACE_PREFIX) {
		prefixMapping.put(ev.getNamespace(), ev.getValue());
	    }
	    switch (state) {
	    case 0:
		if (ev.getType() == Event.START_ELEMENT) {
		    queue.enqueue(Event.createStartElement(FOO_NS, "lists"));
		    queue.enqueue(ev);
		    queue.enqueue
			(Event.createAttribute(XSI_NS, "type",
					       ((String)
						prefixMapping.get(FOO_NS))
					       + ":list"));
		    state = 2;
		} else if (ev.getType() == Event.END_DOCUMENT) {
		    queue.enqueue(Event.createStartElement(FOO_NS, "lists"));
		    queue.enqueue(Event.createEndElement(FOO_NS, "lists"));
		    queue.enqueue(ev);
		} else {
		    queue.enqueue(ev);
		}
		break;
	    case 1:
		if (ev.getType() == Event.END_DOCUMENT) {
		    queue.enqueue(Event.createEndElement(FOO_NS, "lists"));
		    queue.enqueue(ev);
		    state = 0;
		} else if (ev.getType() == Event.START_ELEMENT) {
		    queue.enqueue(ev);
		    queue.enqueue
			(Event.createAttribute(XSI_NS, "type",
					       ((String)
						prefixMapping.get(FOO_NS))
					       + ":list"));
		    state = 2;
		} else {
		    queue.enqueue(ev);
		}
		break;
	    case 2:
		queue.enqueue(ev);
		if (ev.getType() == Event.END_ELEMENT) {
		    state = 1;
		}
		break;
	    case 3:
		queue.enqueue(ev);
		if (ev.getType() == Event.TYPED_CONTENT) {
		    queue.enqueue(Event.createEndElement(FOO_NS, "item"));
		    state = 2;
		}
		break;
	    }
	}

	public void promiseEvent (int type) {
	    //System.out.println("State = " + state + ", Type = " + type);
	    if (state == 2 && type == Event.TYPED_CONTENT) {
		queue.enqueue(Event.createStartElement(FOO_NS, "item"));
		queue.enqueue
		    (Event.createAttribute(XSI_NS, "type",
					   ((String) prefixMapping.get(XSD_NS))
					   + ":int"));
		state = 3;
	    }
	}

    }

    private static class ContentEoaMachine implements EoaMachine {

	private int state = 0;

	public Event nextEvent (Event ev) {
	    switch (state) {
	    case 0:
		if (ev.getType() == Event.START_ELEMENT
		    && Util.equals(ev.getNamespace(), FOO_NS)) {
		    if (Util.equals(ev.getName(), "last")) {
			ev = null;
			state = 1;
		    } else if (Util.equals(ev.getName(), "first")) {
			ev = null;
			state = 2;
		    }
		}
		break;
	    case 1:
		if (ev.getType() == Event.END_ELEMENT
		    && Util.equals(ev.getNamespace(), FOO_NS)
		    && Util.equals(ev.getName(), "last")) {
		    ev = null;
		    state = 0;
		}
		break;
	    case 2:
		if (ev.getType() == Event.END_ELEMENT
		    && Util.equals(ev.getNamespace(), FOO_NS)
		    && Util.equals(ev.getName(), "first")) {
		    ev = null;
		    state = 0;
		}
		break;
	    }
	    return ev;
	}

	public boolean isInitialState () {
	    return state == 0;
	}

    }

    private static class ContentDoaMachine extends IdentityDoaMachine {

	private int state = 0;

	public void nextEvent (Event ev) {
	    switch (state) {
	    case 0:
		queue.enqueue(ev);
		if (ev.getType() == Event.START_ELEMENT
		    && Util.equals(ev.getNamespace(), FOO_NS)
		    && Util.equals(ev.getName(), "name")) {
		    queue.enqueue(Event.createStartElement(FOO_NS, "last"));
		    state = 1;
		}
		break;
	    case 1:
		queue.enqueue(ev);
		if (ev.getType() == Event.CONTENT) {
		    queue.enqueue(Event.createEndElement(FOO_NS, "last"));
		    queue.enqueue(Event.createStartElement(FOO_NS, "first"));
		    state = 2;
		}
		break;
	    case 2:
		queue.enqueue(ev);
		if (ev.getType() == Event.CONTENT) {
		    queue.enqueue(Event.createEndElement(FOO_NS, "first"));
		    state = 0;
		}
		break;
	    }
	}

    }

}
