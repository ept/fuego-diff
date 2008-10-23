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

package fc.exper;

import java.io.FileOutputStream;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import fc.util.Measurer;
import fc.util.Util;

public class DomToc {

    public static void main (String[] args) {
	try {
	    if (args.length != 1) {
		System.err.println("Usage: DomToc <file>");
		System.exit(1);
	    }
	    Measurer.init(Measurer.TIMING);
	    Measurer timer = Measurer.get(Measurer.TIMING);
	    String fileName = args[0];
	    String outName = fileName.concat(".dtoc");
	    int end = 10;
	    for (int i = 0; i < end; i++ ) {
		Util.runGc();
		long beginMemory = Util.usedMemory();
		DocumentBuilderFactory factory = DocumentBuilderFactory
			.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Object token = timer.start();
		Document document = builder.parse(fileName);
		Element tocNode = null;
		Element chapter = null;
		Element section = null;
		Stack<Node> stack = new Stack<Node>();
		stack.push(document);
		while (!stack.isEmpty()) {
		    Node node = stack.pop();
		    switch (node.getNodeType()) {
		    case Node.DOCUMENT_NODE: {
			NodeList nodes = node.getChildNodes();
			if (nodes != null) {
			    int n = nodes.getLength();
			    for (int j = n - 1; j >= 0; j-- ) {
				stack.push(nodes.item(j));
			    }
			}
			break;
		    }
		    case Node.ELEMENT_NODE: {
			String name = node.getNodeName();
			Element element = (Element) node;
			if (name.equals("toc")) {
			    tocNode = element;
			} else if (name.equals("chapter")) {
			    String id = element.getAttribute("id");
			    if (id != null) {
				chapter = document.createElement("tocchap");
				chapter.setAttribute("ref", "#" + id);
				tocNode.appendChild(chapter);
			    }
			} else if (name.equals("sect1")) {
			    String id = element.getAttribute("id");
			    if (id != null) {
				section = document.createElement("tocsect1");
				section.setAttribute("ref", "#" + id);
				chapter.appendChild(section);
			    }
			} else if (name.equals("sect2")) {
			    String id = element.getAttribute("id");
			    if (id != null) {
				Element subsection = document
					.createElement("tocsect2");
				subsection.setAttribute("ref", "#" + id);
				section.appendChild(subsection);
			    }
			    break;
			}
			NodeList nodes = node.getChildNodes();
			if (nodes != null) {
			    int n = nodes.getLength();
			    for (int j = n - 1; j >= 0; j-- ) {
				stack.push(nodes.item(j));
			    }
			}
			break;
		    }
		    }
		}
		if (i >= end - 2) {
		    timer.finish(token, "TOC construction");
		}
		long endMemory = Util.usedMemory();
		System.out.println("TOC memory: " + (endMemory - beginMemory));
		Util.runGc();
		endMemory = Util.usedMemory();
		System.out.println("Document size: " + (endMemory - beginMemory));
		Util.runGc();
		beginMemory = Util.usedMemory();
		token = timer.start();
		OutputFormat format = new OutputFormat();
		XMLSerializer ser = new XMLSerializer(new FileOutputStream(
			outName), format);
		ser.serialize(document);
		if (i >= end - 2) {
		    timer.finish(token, "Output");
		}
		endMemory = Util.usedMemory();
		System.out.println("Output memory: " + (endMemory - beginMemory));
		if (i >= end - 2) {
		    timer.output(System.out);
		}
	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
    }

}

// arch-tag: 98a1fa5e-d570-42cd-9a85-06af49054626
