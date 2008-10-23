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
import java.util.ArrayList;
import java.util.Stack;

import fc.util.Measurer;
import fc.util.Util;
import fc.xml.xas.EndDocument;
import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.ItemList;
import fc.xml.xas.MutablePointer;
import fc.xml.xas.Qname;
import fc.xml.xas.StartDocument;
import fc.xml.xas.StartTag;
import fc.xml.xas.XasFragment;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.index.DeweyKey;
import fc.xml.xas.index.Index;
import fc.xml.xas.index.LazyFragment;
import fc.xml.xas.index.SeekableKXmlSource;

public class Toc {

    private static final Qname TOC_NAME = new Qname("", "toc");
    private static final Qname CHAPTER_NAME = new Qname("", "chapter");
    private static final Qname SECTION_NAME = new Qname("", "sect1");
    private static final Qname SUBSECTION_NAME = new Qname("", "sect2");
    private static final Qname TOCCHAP_NAME = new Qname("", "tocchap");
    private static final Qname TOCSECT_NAME = new Qname("", "tocsect1");
    private static final Qname TOCSUBSECT_NAME = new Qname("", "tocsect2");
    private static final Qname ID_ATT_NAME = new Qname("", "id");
    private static final Qname REF_ATT_NAME = new Qname("", "ref");

    private static SeekableKXmlSource source = null;

    private static Index buildIndex (String fileName) throws Exception {
	source = new SeekableKXmlSource(fileName);
	Index index = new Index(source);
	DeweyKey k = DeweyKey.initial();
	StartTag context = null;
	Stack<StartTag> sts = new Stack<StartTag>();
	sts.push(null);
	Stack<Integer> ps = new Stack<Integer>();
	boolean isText = false;
	Item item;
	while ((item = source.next()) != null) {
	    if (isText && !Item.isContent(item)) {
		k = k.next();
		isText = false;
	    }
	    if (Item.isStartTag(item)) {
		context = (StartTag) item;
		sts.push(context);
		ps.push(source.getPreviousPosition());
		k = k.down();
	    } else if (Item.isEndTag(item)) {
		sts.pop();
		context = sts.peek();
		k = k.up();
		Integer pos = ps.pop();
		Qname name = ((EndTag) item).getName();
		if (name.equals(CHAPTER_NAME) || name.equals(SECTION_NAME)
			|| name.equals(SUBSECTION_NAME)) {
		    index.insert(k, pos, source.getCurrentPosition(), context);
		}
		k = k.next();
	    } else if (Item.isContent(item)) {
		isText = true;
	    } else if (Item.isDocumentDelimiter(item)) {
		isText = false;
		continue;
	    } else {
		k = k.next();
	    }
	}
	return index;
    }

    public static void main (String[] args) {
	try {
	    if (args.length != 1 && args.length != 2) {
		System.err.println("Usage: Toc <file> [<with index>]");
		System.exit(1);
	    }
	    Measurer.init(Measurer.TIMING);
	    Measurer timer = Measurer.get(Measurer.TIMING);
	    String fileName = args[0];
	    String outName = fileName.concat(".toc");
	    int end = 10;
	    Index index = null;
	    for (int i = 0; i < end; i++) {
		index = null;
		Util.runGc();
		long beginMemory = Util.usedMemory();
		Object token = timer.start();
		index = args.length == 2 ? buildIndex(fileName) : null;
		if (i >= end - 2) {
		    timer.finish(token, "Index building");
		}
		Util.runGc();
		long endMemory = Util.usedMemory();
		System.out.println("Index size: " + (endMemory - beginMemory));
		source = new SeekableKXmlSource(fileName);
		ItemList list = new ItemList();
		list.append(StartDocument.instance());
		Item item;
		DeweyKey k = DeweyKey.initial();
		DeweyKey tocKey = null;
		boolean isText = false;
		boolean passing = false;
		ItemList tocList = new ItemList();
		ItemList chapList = new ItemList();
		ItemList sectList = new ItemList();
		StartTag tocTag = null;
		StartTag chapTag = null;
		StartTag sectTag = null;
		Util.runGc();
		beginMemory = Util.usedMemory();
		token = timer.start();
		while ((item = source.next()) != null) {
		    if (isText && !Item.isContent(item)) {
			k = k.next();
			isText = false;
		    }
		    if (Item.isStartTag(item)) {
			StartTag st = (StartTag) item;
			Qname name = st.getName();
			if (name.equals(TOC_NAME)) {
			    tocKey = k;
			    tocTag = new StartTag(TOC_NAME, st.getContext());
			    tocList.append(tocTag);
			} else if (name.equals(CHAPTER_NAME)) {
			    String id = (String) st
				.getAttributeValue(ID_ATT_NAME);
			    if (id != null) {
				chapTag = new StartTag(TOCCHAP_NAME, tocTag);
				chapTag.addAttribute(REF_ATT_NAME, "#" + id);
				chapList.append(chapTag);
			    } else {
				chapTag = null;
			    }
			} else if (name.equals(SECTION_NAME)) {
			    String id = (String) st
				.getAttributeValue(ID_ATT_NAME);
			    if (id != null) {
				sectTag = new StartTag(TOCSECT_NAME, chapTag);
				sectTag.addAttribute(REF_ATT_NAME, "#" + id);
				sectList.append(sectTag);
			    } else {
				sectTag = null;
			    }
			} else if (name.equals(SUBSECTION_NAME)) {
			    String id = (String) st
				.getAttributeValue(ID_ATT_NAME);
			    if (id != null) {
				StartTag sst = new StartTag(TOCSUBSECT_NAME,
				    sectTag);
				sst.addAttribute(REF_ATT_NAME, "#" + id);
				EndTag set = new EndTag(TOCSUBSECT_NAME);
				ArrayList<Item> l = new ArrayList<Item>(2);
				l.add(sst);
				l.add(set);
				sectList.append(new XasFragment(l, sst));
			    }
			    if (index != null) {
				Index.Entry entry = index.find(k);
				if (entry != null) {
				    list
					.append(new LazyFragment(index, k, item));
				    passing = true;
				}
			    }
			}
			k = k.down();
		    } else if (Item.isEndTag(item)) {
			k = k.up();
			if (k != null) {
			    k = k.next();
			}
			Qname name = ((EndTag) item).getName();
			if (chapTag != null && name.equals(CHAPTER_NAME)) {
			    chapList.append(new EndTag(TOCCHAP_NAME));
			    tocList.append(chapList.fragment());
			    chapList = new ItemList();
			} else if (sectTag != null && name.equals(SECTION_NAME)) {
			    sectList.append(new EndTag(TOCSECT_NAME));
			    chapList.append(sectList.fragment());
			    sectList = new ItemList();
			} else if (passing && name.equals(SUBSECTION_NAME)) {
			    passing = false;
			    continue;
			}
		    } else if (Item.isContent(item)) {
			isText = true;
		    } else if (Item.isDocumentDelimiter(item)) {
			isText = false;
			continue;
		    } else {
			k = k.next();
		    }
		    if (!passing) {
			list.append(item);
		    }
		}
		list.append(EndDocument.instance());
		XasFragment fragment = list.fragment();
		if (tocKey != null && !tocList.isEmpty()) {
		    tocList.append(new EndTag(TOC_NAME));
		    MutablePointer p = fragment.query(tocKey.deconstruct());
		    p.set(tocList.fragment());
		}
		if (i >= end - 2) {
		    timer.finish(token, "TOC construction");
		}
		endMemory = Util.usedMemory();
		System.out.println("TOC memory: " + (endMemory - beginMemory));
		Util.runGc();
		endMemory = Util.usedMemory();
		System.out.println("Document size: "
			+ (endMemory - beginMemory));
		Util.runGc();
		beginMemory = Util.usedMemory();
		token = timer.start();
		XmlOutput xout = new XmlOutput(new FileOutputStream(outName),
		    source.getEncoding());
		fragment.appendTo(xout);
		xout.flush();
		if (i >= end - 2) {
		    timer.finish(token, "Output");
		}
		endMemory = Util.usedMemory();
		System.out.println("Output memory: "
			+ (endMemory - beginMemory));
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

// arch-tag: ce668ae6-cb4d-4457-8266-3dcddeb63d09
