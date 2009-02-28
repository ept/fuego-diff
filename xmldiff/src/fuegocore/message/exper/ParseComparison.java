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

package fuegocore.message.exper;

import java.io.Reader;
import java.io.FileReader;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.Writer;
import java.io.StringWriter;
import java.util.Enumeration;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.XMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.SAXException;

import org.apache.xml.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.DOMSerializer;
import org.apache.xerces.parsers.DOMParser;

import org.xmlpull.v1.XmlPullParser;

import fuegocore.util.TimeMeasurer;
import fuegocore.util.ExtUtil;
import fuegocore.util.xas.EventSequence;
import fuegocore.util.xas.EventStream;
import fuegocore.util.xas.TypedXmlParser;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.DefaultXmlParser;
import fuegocore.util.xas.DefaultXmlSerializer;
import fuegocore.util.xas.SaxBridge;
import fuegocore.util.xas.XasUtil;
import fuegocore.util.xas.XasExtUtil;

/**
 * Compare XML parsing and serialization speeds and memory use.  This
 * class compares DOM, SAX, and XAS parsing and serialization.
 * Measurements are taken both of the time spent and memory used, the
 * latter as a total consumption and retained memory.
 *
 * <p>The command-line arguments are the type of XML processor to use,
 * whether to serialize also, and the names of the XML files to read.
 * Available types are <code>sax</code>, <code>dom</code>,
 * <code>xas</code>, <code>xassax</code>, and <code>xasdom</code>, of
 * which the last two ones use the compatibility interfaces of the XAS
 * API.
 */
public class ParseComparison {

    private static final int BUSY_LOOP = 20;

    private ParseComparison () {
    }

    public static void main (String[] args) {
	try {
	    long start = System.currentTimeMillis();
	    if (args.length < 3) {
		System.err.println("Usage: ParseComparison <type> <target> "
				   + " <file...>");
		System.exit(1);
	    }
	    String type = args[0];
	    boolean doWrite = Boolean.valueOf(args[1]).booleanValue();
	    String[] contents = new String[args.length - 2];
	    for (int i = 2; i < args.length; i++) {
		Reader reader = new BufferedReader(new FileReader(args[i]));
		StringBuffer buffer = new StringBuffer();
		char[] tempBuffer = new char[4096];
		int charsRead;
		while ((charsRead =
			reader.read(tempBuffer, 0, tempBuffer.length))
		       > 0) {
		    buffer.append(tempBuffer, 0, charsRead);
		}
		contents[i - 2] = buffer.toString();
	    }
	    StringWriter[] targets = new StringWriter[contents.length];
	    for (int i = 0; i < targets.length; i++) {
		targets[i] = new StringWriter(contents[i].length());
	    }
	    Object[] results = new Object[contents.length];
	    Encoder encoder = null;
	    if (type.equals("sax")) {
		encoder = new SaxEncoder();
	    } else if (type.equals("dom")) {
		encoder = new DomEncoder();
	    } else if (type.equals("xas")) {
		encoder = new XasEncoder();
	    } else if (type.equals("xassax")) {
		encoder = new XasSaxEncoder();
	    } else if (type.equals("xasdom")) {
		encoder = new XasDomEncoder();
	    } else {
		System.err.println("Invalid type " + type);
		System.exit(2);
	    }
	    for (int i = 0; i < BUSY_LOOP; i++) {
		for (int j = 0; j < contents.length; j++) {
		    encoder.encode(doWrite, contents[j], targets[j]);
		    targets[j] = new StringWriter(contents[j].length());
		}
	    }
	    TimeMeasurer timer = new TimeMeasurer();
	    timer.init();
	    String wallString = "Wall clock time";
	    System.out.println("GC run at " + (System.currentTimeMillis()
					       - start));
	    ExtUtil.runGc();
	    long beginMemory = ExtUtil.usedMemory();
	    long before = beginMemory;
	    long collected = 0;
	    for (int i = 0; i < contents.length; i++) {
		Object wallToken = timer.beginWall();
		results[i] = encoder.encode(doWrite, contents[i], targets[i]);
		long after = ExtUtil.usedMemory();
		if (after < before) {
		    collected += before - after;
		}
		before = after;
		timer.end(wallToken, wallString);
		//System.out.println(targets[i].toString());
	    }
	    long memory = ExtUtil.usedMemory() - beginMemory;
	    System.out.println(wallString + ": " + timer.getTime(wallString)
			       + " (" + timer.getNumber(wallString) + ")");
	    System.out.println("Memory usage: " + memory);
	    System.out.println("Collected memory: " + collected);
	    ExtUtil.runGc();
	    memory = ExtUtil.usedMemory() - beginMemory;
	    System.out.println("Leftover memory: " + memory);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private static interface Encoder {

	Object encode (boolean doWrite, String source, Writer target);

    }

    private static class SaxEncoder implements Encoder {

	public Object encode (boolean doWrite, String source, Writer target) {
	    try {
		XMLReader reader = XMLReaderFactory.createXMLReader
		    ("org.apache.xerces.parsers.SAXParser");
		ContentHandler handler = null;
		if (doWrite) {
		    handler = new XMLSerializer(target, new OutputFormat());
		} else {
		    handler = new DefaultHandler();
		}
		reader.setContentHandler(handler);
		InputSource input = new InputSource(new StringReader(source));
		reader.parse(input);
		return null;
	    } catch (Exception ex) {
		ex.printStackTrace();
		return null;
	    }
	}

    }

    private static class DomEncoder implements Encoder {

	public Object encode (boolean doWrite, String source, Writer target) {
	    try {
		DOMParser parser = new DOMParser();
		parser.parse(new InputSource(new StringReader(source)));
		Document doc = parser.getDocument();
		if (doWrite) {
		    DOMSerializer ser = new XMLSerializer(target,
							  new OutputFormat());
		    ser.serialize(doc);
		}
		return doc;
	    } catch (Exception ex) {
		ex.printStackTrace();
		return null;
	    }
	}

    }

    private static class XasEncoder implements Encoder {

	public Object encode (boolean doWrite, String source, Writer target) {
	    try {
		TypedXmlParser parser = new DefaultXmlParser();
		parser.setInput(new StringReader(source));
		EventSequence seq = new EventStream(parser);
		if (doWrite) {
		    TypedXmlSerializer ser = new DefaultXmlSerializer();
		    ser.setOutput(target);
		    XasUtil.outputSequence(seq, ser);
		} else {
		    for (Enumeration e = seq.events(); e.hasMoreElements(); ) {
			e.nextElement();
		    }
		}
		return seq;
	    } catch (Exception ex) {
		ex.printStackTrace();
		return null;
	    }
	}

    }

    private static class XasSaxEncoder implements Encoder {

	public Object encode (boolean doWrite, String source, Writer target) {
	    try {
		TypedXmlParser parser = new DefaultXmlParser();
		parser.setInput(new StringReader(source));
		EventSequence seq = new EventStream(parser);
		ContentHandler handler = null;
		if (doWrite) {
		    handler = new XMLSerializer(target, new OutputFormat());
		} else {
		    handler = new DefaultHandler();
		}
		SaxBridge bridge = new SaxBridge(seq);
		bridge.output(handler);
		return null;
	    } catch (Exception ex) {
		ex.printStackTrace();
		if (ex instanceof SAXException) {
		    Exception e = ((SAXException) ex).getException();
		    if (e != null) {
			e.printStackTrace();
		    }
		}
		return null;
	    }
	}

    }

    private static class XasDomEncoder implements Encoder {

	public Object encode (boolean doWrite, String source, Writer target) {
	    try {
		TypedXmlParser parser = new DefaultXmlParser();
		parser.setInput(new StringReader(source));
		EventSequence seq = new EventStream(parser);
		Document doc = XasExtUtil.newDocument();
		XasExtUtil.sequenceToDom(doc, seq);
		if (doWrite) {
		    DOMSerializer ser = new XMLSerializer(target,
							  new OutputFormat());
		    ser.serialize(doc);
		}
		return doc;
	    } catch (Exception ex) {
		ex.printStackTrace();
		return null;
	    }
	}

    }

}
