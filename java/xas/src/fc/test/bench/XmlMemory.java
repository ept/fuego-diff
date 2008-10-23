/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.test.bench;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.kxml2.io.KXmlParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

import fc.util.Util;
import fc.xml.xas.Item;
import fc.xml.xas.ItemList;
import fc.xml.xas.XmlPullSource;

public class XmlMemory {

    private static Object result = null;

    private static void saxParse (String fileName) throws Exception {
	SAXParserFactory factory = SAXParserFactory.newInstance();
	factory.setNamespaceAware(true);
	SAXParser parser = factory.newSAXParser();
	XMLReader reader = parser.getXMLReader();
	DefaultHandler2 handler = new DefaultHandler2();
	reader.setContentHandler(handler);
	reader.setProperty("http://xml.org/sax/properties/lexical-handler",
		handler);
	reader.parse(fileName);
    }

    private static void domParse (String fileName) throws Exception {
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	factory.setNamespaceAware(true);
	DocumentBuilder builder = factory.newDocumentBuilder();
	Document document = builder.parse(fileName);
	Stack<Node> stack = new Stack<Node>();
	stack.push(document);
	while (!stack.isEmpty()) {
	    Node node = stack.pop();
	    switch (node.getNodeType()) {
	    case Node.DOCUMENT_NODE: {
		NodeList nodes = node.getChildNodes();
		if (nodes != null) {
		    int n = nodes.getLength();
		    for (int i = n - 1; i >= 0; i-- ) {
			stack.push(nodes.item(i));
		    }
		}
	    }
	    case Node.ELEMENT_NODE: {
		NamedNodeMap atts = node.getAttributes();
		if (atts != null) {
		    int n = atts.getLength();
		    for (int i = 0; i < n; i++ ) {
			Attr att = (Attr) atts.item(i);
			result = att.getNodeName();
		    }
		}
		NodeList nodes = node.getChildNodes();
		if (nodes != null) {
		    int l = nodes.getLength();
		    for (int i = l - 1; i >= 0; i-- ) {
			stack.push(nodes.item(i));
		    }
		}
	    }
	    }
	}
	result = document;
    }

    private static void xasParse (String fileName) throws Exception {
	InputStream in = new FileInputStream(fileName);
	KXmlParser parser = new KXmlParser();
	XmlPullSource source = new XmlPullSource(parser, in);
	ItemList list = new ItemList();
	Item item;
	while ((item = source.next()) != null) {
	    list.append(item);
	}
	result = list;
    }

    public static void main (String[] args) {
	try {
	    if (args.length != 2) {
		System.err.println("Usage: XmlMemory (sax|dom|xas) <file>");
		System.exit(1);
	    }
	    long beginMemory = 0;
	    int end = 10;
	    for (int i = 0; i < end; i++ ) {
		result = null;
		Util.runGc();
		beginMemory = Util.usedMemory();
		if (args[0].equals("sax")) {
		    saxParse(args[1]);
		} else if (args[0].equals("dom")) {
		    domParse(args[1]);
		} else if (args[0].equals("xas")) {
		    xasParse(args[1]);
		} else {
		    System.err.println("Usage: XmlMemory (sax|dom|xas) <file>");
		    System.exit(1);
		}
		long spentMemory = Util.usedMemory();
		Util.runGc();
		long endMemory = Util.usedMemory();
		System.out.println("Total memory spent: "
			+ (spentMemory - beginMemory));
		System.out.println("Object size: " + (endMemory - beginMemory));
		System.out
			.println("Result: " + System.identityHashCode(result));
	    }
	    // System.in.read();
	} catch (Exception ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
    }

}

// arch-tag: 31d1ae36-e537-45ba-8a98-4150fd2fe0ab
