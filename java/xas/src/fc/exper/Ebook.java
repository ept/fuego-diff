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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import fc.util.Util;
import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.index.DeweyKey;
import fc.xml.xas.index.Index;
import fc.xml.xas.index.KeyIterator;
import fc.xml.xas.index.LazyFragment;
import fc.xml.xas.index.SeekableKXmlSource;

public class Ebook {

    private static final Qname BOOK_NAME = new Qname("", "book");
    private static final Qname BOOKINFO_NAME = new Qname("", "bookinfo");
    private static final Qname PREFACE_NAME = new Qname("", "preface");
    private static final Qname PRESECTION_NAME = new Qname("", "section");
    private static final Qname CHAPTER_NAME = new Qname("", "chapter");
    private static final Qname APPENDIX_NAME = new Qname("", "appendix");
    private static final Qname GLOSSARY_NAME = new Qname("", "glossary");
    private static final Qname SECTION_NAME = new Qname("", "sect1");
    private static final Qname SUBSECTION_NAME = new Qname("", "sect2");
    private static final Qname IMAGE_NAME = new Qname("", "imagedata");
    private static final Qname ID_ATT_NAME = new Qname("", "id");

    private static final long DEFAULT_SEED = 29041978L;
    private static Random random;
    private static final String PREFACE_OPS = "rrrrrrrsss";
    private static final String PRESECTION_OPS = "rrrrrsssss";
    private static final String CHAPTER_OPS = "rrrrrrrrrs";
    private static final String APPENDIX_OPS = "rrrsssssss";
    private static final String SECTION_OPS = "rrrrrrrsss";
    private static final String SUBSECTION_OPS = "rrrrrssspp";
    private static final String IMAGE_OPS = "rrrrrrrrrs";

    private static SeekableKXmlSource source = null;
    private static StartTag bookTag = null;
    private static LazyFragment chapFrag = null;
    private static LazyFragment sectFrag = null;
    private static ArrayList<LazyFragment> sectFrags = new ArrayList<LazyFragment>();
    private static ArrayList<LazyFragment> subsectFrags = new ArrayList<LazyFragment>();
    private static long initialMemory;

    private static long[] skipInfo = new long[8];

    private static Index buildIndex (String fileName) throws Exception {
	source = new SeekableKXmlSource(fileName);
	Index index = new Index(source);
	StartTag context = null;
	Stack<StartTag> sts = new Stack<StartTag>();
	sts.push(null);
	Stack<Integer> ps = new Stack<Integer>();
	Item item;
	KeyIterator ki = new KeyIterator();
	while ((item = source.next()) != null) {
	    if (Item.isStartTag(item)) {
		context = (StartTag) item;
		sts.push(context);
		ps.push(source.getPreviousPosition());
		if (((StartTag) item).getName().equals(BOOK_NAME)) {
		    bookTag = (StartTag) item;
		}
	    } else if (Item.isEndTag(item)) {
		sts.pop();
		context = sts.peek();
		Integer pos = ps.pop();
		Qname name = ((EndTag) item).getName();
		if (name.equals(BOOK_NAME) || name.equals(BOOKINFO_NAME)
			|| name.equals(PREFACE_NAME)
			|| name.equals(PRESECTION_NAME)
			|| name.equals(CHAPTER_NAME)
			|| name.equals(APPENDIX_NAME)
			|| name.equals(GLOSSARY_NAME)
			|| name.equals(SECTION_NAME)
			|| name.equals(SUBSECTION_NAME)
			|| name.equals(IMAGE_NAME)) {
		    index.insert(ki.current().up(), pos, source
			    .getCurrentPosition(), context);
		}
	    }
	    ki.update(item);
	}
	return index;
    }

    public static void main (String[] args) {
	try {
	    if (args.length != 1) {
		System.err.println("Usage: Ebook <file>");
		System.exit(1);
	    }
	    String fileName = args[0];
	    Index index = null;
	    int end = 1;
	    for (int i = 0; i < end; i++ ) {
		index = null;
		Util.runGc();
		long beginMemory = Util.usedMemory();
		index = buildIndex(fileName);
		Util.runGc();
		long endMemory = Util.usedMemory();
		System.out.println("Index size: " + (endMemory - beginMemory));
	    }
	    source = new SeekableKXmlSource(fileName);
	    index.setSource(source);
	    random = new Random(DEFAULT_SEED);
	    Util.runGc();
	    initialMemory = Util.usedMemory();
	    LazyFragment document = new LazyFragment(index, DeweyKey.initial(),
		    bookTag);
	    document.force(1);
	    Util.runGc();
	    long endMemory = Util.usedMemory();
	    System.out.println("Unforced document size: "
		    + (endMemory - initialMemory));
	    int n = document.length();
	    for (int i = 0; i < n; i++ ) {
		Item item = document.get(i);
		if (item instanceof LazyFragment) {
		    LazyFragment fragment = (LazyFragment) item;
		    StartTag st = (StartTag) fragment.get(0);
		    Qname name = st.getName();
		    if (name.equals(PREFACE_NAME)) {
			processChapter((String) st
				.getAttributeValue(ID_ATT_NAME), fragment,
				PREFACE_OPS);
		    } else if (name.equals(CHAPTER_NAME)) {
			processChapter((String) st
				.getAttributeValue(ID_ATT_NAME), fragment,
				CHAPTER_OPS);
		    } else if (name.equals(APPENDIX_NAME)) {
			processChapter((String) st
				.getAttributeValue(ID_ATT_NAME), fragment,
				APPENDIX_OPS);
		    }
		}
	    }
	    System.out.println("Chapters: " + skipInfo[1] + "/" + skipInfo[0]);
	    System.out.println("Sections: " + skipInfo[3] + "/" + skipInfo[2]);
	    System.out.println("Subsections: " + skipInfo[5] + "/"
		    + skipInfo[4]);
	    System.out.println("Images: " + skipInfo[7] + "/" + skipInfo[6]);
	} catch (Exception ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
    }

    private static void processChapter (String id, LazyFragment fragment,
	    String ops) throws IOException {
	skipInfo[0] += 1;
	if (chapFrag != null) {
	    chapFrag.unforce();
	    sectFrags.clear();
	    subsectFrags.clear();
	}
	Util.runGc();
	System.out.println("Chapter " + id + " start: "
		+ (Util.usedMemory() - initialMemory));
	char d = ops.charAt(random.nextInt(ops.length()));
	if (d == 'r') {
	    skipInfo[1] += 1;
	    System.out.println("Forcing fragment");
	    fragment.force(1);
	    chapFrag = fragment;
	    processCurrentChapter();
	}
	Util.runGc();
	System.out.println("Chapter " + id + " end: "
		+ (Util.usedMemory() - initialMemory));
    }

    private static void processCurrentChapter () throws IOException {
	int n = chapFrag.length();
	for (int i = 0; i < n; i++ ) {
	    Item item = chapFrag.get(i);
	    if (item instanceof LazyFragment) {
		LazyFragment fragment = (LazyFragment) item;
		StartTag st = (StartTag) fragment.get(0);
		Qname name = st.getName();
		if (name.equals(SECTION_NAME)) {
		    processSection((String) st.getAttributeValue(ID_ATT_NAME),
			    fragment, SECTION_OPS);
		} else if (name.equals(PRESECTION_NAME)) {
		    processSection((String) st.getAttributeValue(ID_ATT_NAME),
			    fragment, PRESECTION_OPS);
		}
	    }
	}
    }

    private static void processSection (String id, LazyFragment fragment,
	    String ops) throws IOException {
	skipInfo[2] += 1;
	if (sectFrag != null) {
	    sectFrag.unforce();
	    sectFrags.add(sectFrag);
	    subsectFrags.clear();
	}
	Util.runGc();
	System.out.println("Section " + id + " start: "
		+ (Util.usedMemory() - initialMemory));
	char d = ops.charAt(random.nextInt(ops.length()));
	if (d == 'r') {
	    skipInfo[3] += 1;
	    System.out.println("Forcing fragment");
	    fragment.force(1);
	    sectFrag = fragment;
	    processCurrentSection();
	}
	Util.runGc();
	System.out.println("Section " + id + " end: "
		+ (Util.usedMemory() - initialMemory));
    }

    private static void processCurrentSection () throws IOException {
	int n = sectFrag.length();
	for (int i = 0; i < n; i++ ) {
	    Item item = sectFrag.get(i);
	    if (item instanceof LazyFragment) {
		LazyFragment fragment = (LazyFragment) item;
		StartTag st = (StartTag) fragment.get(0);
		String id = (String) st.getAttributeValue(ID_ATT_NAME);
		Qname name = st.getName();
		if (name.equals(IMAGE_NAME)) {
		    skipInfo[6] += 1;
		    Util.runGc();
		    System.out.println("Image start: "
			    + (Util.usedMemory() - initialMemory));
		    char d = IMAGE_OPS.charAt(random
			    .nextInt(IMAGE_OPS.length()));
		    if (d == 'r') {
			skipInfo[7] += 1;
			Index.Entry entry = fragment.getIndex().find(
				fragment.getKey());
			if (entry != null) {
			    System.out.println("Reading bytes");
			    source.setPosition(entry.getOffset(), entry
				    .getContext());
			    InputStream in = source.getInputStream();
			    byte[] buffer = new byte[2048];
			    int len = entry.getLength();
			    int left = len > buffer.length ? buffer.length
				    : len;
			    int s;
			    while ((s = in.read(buffer, 0, left)) > 0) {
				left -= s;
			    }
			} else {
			    System.out.println("Forcing fragment");
			    fragment.force(1);
			}
		    }
		    Util.runGc();
		    System.out.println("Image end: "
			    + (Util.usedMemory() - initialMemory));

		} else if (name.equals(SUBSECTION_NAME)) {
		    skipInfo[4] += 1;
		    subsectFrags.add(fragment);
		    Util.runGc();
		    System.out.println("Subsection " + id + " start: "
			    + (Util.usedMemory() - initialMemory));
		    char d = SUBSECTION_OPS.charAt(random
			    .nextInt(SUBSECTION_OPS.length()));
		    if (d == 'r') {
			skipInfo[5] += 1;
			System.out.println("Forcing fragment");
			fragment.force(1);
			processSubsection(fragment);
			Util.runGc();
			System.out.println("Subsection " + id + " end: "
				+ (Util.usedMemory() - initialMemory));
		    }
		}
	    }
	}
    }

    private static void processSubsection (LazyFragment fragment)
	    throws IOException {
	int n = fragment.length();
	for (int i = 0; i < n; i++ ) {
	    Item item = fragment.get(i);
	    if (item instanceof LazyFragment) {
		LazyFragment lf = (LazyFragment) item;
		StartTag st = (StartTag) lf.get(0);
		Qname name = st.getName();
		if (name.equals(IMAGE_NAME)) {
		    skipInfo[6] += 1;
		    Util.runGc();
		    System.out.println("Image start: "
			    + (Util.usedMemory() - initialMemory));
		    char d = IMAGE_OPS.charAt(random
			    .nextInt(IMAGE_OPS.length()));
		    if (d == 'r') {
			skipInfo[7] += 1;
			Index.Entry entry = lf.getIndex().find(lf.getKey());
			if (entry != null) {
			    System.out.println("Reading bytes");
			    source.setPosition(entry.getOffset(), entry
				    .getContext());
			    InputStream in = source.getInputStream();
			    byte[] buffer = new byte[2048];
			    int len = entry.getLength();
			    int left = len > buffer.length ? buffer.length
				    : len;
			    int s;
			    while ((s = in.read(buffer, 0, left)) > 0) {
				left -= s;
			    }
			} else {
			    System.out.println("Forcing fragment");
			    lf.force(1);
			}
		    }
		    Util.runGc();
		    System.out.println("Image end: "
			    + (Util.usedMemory() - initialMemory));

		}
	    }
	}
    }
}

// arch-tag: b30ed7bb-0cf7-46c9-9559-a9064fe2b1e5
