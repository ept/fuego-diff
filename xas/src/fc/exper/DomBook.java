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

import java.util.Random;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fc.util.Util;

public class DomBook {

    private static final long DEFAULT_SEED = 29041978L;
    private static Random random;
    private static final String PREFACE_OPS = "rrrrrrrsss";
    private static final String PRESECTION_OPS = "rrrrrsssss";
    private static final String CHAPTER_OPS = "rrrrrrrrrs";
    private static final String APPENDIX_OPS = "rrrsssssss";
    private static final String SECTION_OPS = "rrrrrrrsss";
    private static final String SUBSECTION_OPS = "rrrrrssspp";
    private static final String IMAGE_OPS = "rrrrrrrrrs";

    private static long initialMemory;
    private static long[] skipInfo = new long[8];

    public static void main (String[] args) {
	try {
	    if (args.length != 1) {
		System.err.println("Usage: DomBook <file>");
		System.exit(1);
	    }
	    String fileName = args[0];
	    int end = 1;
	    for (int i = 0; i < end; i++ ) {
		Util.runGc();
		initialMemory = Util.usedMemory();
		random = new Random(DEFAULT_SEED);
		DocumentBuilderFactory factory = DocumentBuilderFactory
			.newInstance();
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
			    for (int j = n - 1; j >= 0; j-- ) {
				stack.push(nodes.item(j));
			    }
			}
			break;
		    }
		    case Node.ELEMENT_NODE: {
			String name = node.getNodeName();
			Element element = (Element) node;
			String ops = null;
			int index = -1;
			if (name.equals("preface")) {
			    ops = PREFACE_OPS;
			    index = 0;
			} else if (name.equals("chapter")) {
			    ops = CHAPTER_OPS;
			    index = 0;
			} else if (name.equals("appendix")) {
			    ops = APPENDIX_OPS;
			    index = 0;
			} else if (name.equals("sect1")) {
			    ops = SECTION_OPS;
			    index = 2;
			} else if (name.equals("section")) {
			    ops = PRESECTION_OPS;
			    index = 2;
			} else if (name.equals("sect2")) {
			    ops = SUBSECTION_OPS;
			    index = 4;
			} else if (name.equals("imagedata")) {
			    ops = IMAGE_OPS;
			    index = 6;
			} else if (name.equals("bookinfo")) {
			    break;
			} else if (name.equals("glossary")) {
			    break;
			}
			if (ops != null) {
			    skipInfo[index] += 1;
			    Util.runGc();
			    System.out.println("Start "
				    + element.getAttribute("id") + ": "
				    + (Util.usedMemory() - initialMemory));
			    char d = ops.charAt(random.nextInt(ops.length()));
			    if (d != 'r') {
				break;
			    }
			    System.out.println("Processing node");
			    skipInfo[index + 1] += 1;
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
		System.out.println("Chapters: " + skipInfo[1] + "/"
			+ skipInfo[0]);
		System.out.println("Sections: " + skipInfo[3] + "/"
			+ skipInfo[2]);
		System.out.println("Subsections: " + skipInfo[5] + "/"
			+ skipInfo[4]);
		System.out
			.println("Images: " + skipInfo[7] + "/" + skipInfo[6]);
	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
    }
}

// arch-tag: 6c8a365f-cd19-4254-afd9-4301a32a9c8a
